/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.client.server.rest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;

import static com.iris.util.Objects.equalsAny;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.billing.client.BillingClient;
import com.iris.billing.client.model.Account;
import com.iris.billing.client.model.Address;
import com.iris.billing.client.model.Adjustment;
import com.iris.billing.client.model.BillingInfo;
import com.iris.billing.client.model.Invoice;
import com.iris.billing.client.model.Invoices;
import com.iris.billing.client.model.Transaction;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.http.HttpException;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.AsyncHttpResource;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.client.invoice.InvoiceContext;
import com.iris.core.template.TemplateService;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.util.IrisUUID;

@Singleton
@HttpGet("/invoice/GetInvoice*")
public class GetInvoiceRESTHandler extends AsyncHttpResource {
	public static final String NAME_EXECUTOR = "executor.getinvoices";
	
	private static final String INVOICE_TEMPLATE = "invoice";
	
	@Inject(optional = true)
   @Named(value = "billing.timeout")
   private int billingTimeoutSec = 30;
	
	@Inject
	@Named(value = "invoice.resource.base.url")
	private String staticResourceBaseUrl;
	
	private final BillingClient billingClient;
   private final TemplateService templateService;
   
   private final DateFormat longFormat = DateFormat.getDateInstance(DateFormat.LONG);
   private final DateFormat medFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
   private final NumberFormat usdCurrency = NumberFormat.getCurrencyInstance(Locale.US);
	
   
	@Inject
	public GetInvoiceRESTHandler(
         SessionAuth sessionAuth,
         BridgeMetrics metrics,
         @Named(NAME_EXECUTOR) Executor executor,
         BillingClient billingClient,
         TemplateService templateService
    ) {

		super(sessionAuth, new HttpSender(GetInvoiceRESTHandler.class, metrics), executor);
      this.templateService = templateService;
      this.billingClient = billingClient;
   }


   @Override
   public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
   	Client client = Client.get(ctx.channel());
   	if(client == null) {
   		throw new HttpException(HttpResponseStatus.NOT_FOUND, "Client not authorized");
   	}
   	
      QueryStringDecoder decoder = new QueryStringDecoder(req.getUri());
      List<String> invoices = decoder.parameters().get("in");
      List<String> accounts = decoder.parameters().get("ac");
      if (invoices == null || invoices.size() != 1) {
         throw new HttpException(HttpResponseStatus.BAD_REQUEST);
      }
      if (accounts == null || accounts.size() != 1) {
         throw new HttpException(HttpResponseStatus.BAD_REQUEST);
      }
      
      String invoiceNumber = invoices.get(0);
      String accountId = accounts.get(0);
      
      assertAccountOwner(client, accountId);
      
      ListenableFuture<BillingInfo> billingInfoFuture = billingClient.getBillingInfoForAccount(accountId);
      BillingInfo billingInfo = null;
      try {
         billingInfo = billingInfoFuture.get(billingTimeoutSec, TimeUnit.SECONDS);
      } catch (TimeoutException ex) {
         throw new HttpException(HttpResponseStatus.REQUEST_TIMEOUT, ex); 
      } catch (Exception ex) {
         billingInfo = null;
      }
      
      Account account = null;
      if (billingInfo == null) {
         // No billing info. Get account info instead.
         ListenableFuture<Account> accountFuture = billingClient.getAccount(accountId);
         try {
            account = accountFuture.get(billingTimeoutSec, TimeUnit.SECONDS);
         } catch (TimeoutException ex) {
         	throw new HttpException(HttpResponseStatus.REQUEST_TIMEOUT, ex);
         } catch (Exception ex) {
         	throw new HttpException(HttpResponseStatus.NOT_FOUND, ex);
         }
      }
      
      BilleeInfo billeeInfo = createBilleeInfo(account, billingInfo);
      
      ListenableFuture<Invoices> future = billingClient.getInvoicesForAccount(accountId);
      try {
         Invoices billingInvoices = future.get(billingTimeoutSec, TimeUnit.SECONDS);
         return extractInvoice(billingInvoices, invoiceNumber, billeeInfo);
      } catch(TimeoutException ex) {
      	throw new HttpException(HttpResponseStatus.REQUEST_TIMEOUT, ex);
      }      
   }
   
	private void assertAccountOwner(Client client, String accountId) throws HttpException {
   	UUID loggedInPerson = client.getPrincipalId();
   	UUID accountUuid = null;
   	try {
   		accountUuid = IrisUUID.fromString(accountId);
   	} catch (Exception e) {
   		// this ain't a valid uuid
   		throw new HttpException(HttpResponseStatus.NOT_FOUND, e);
   	}
   	
   	// verify that the logged in user is the owner of the requested account 
   	AuthorizationContext authCtx = client.getAuthorizationContext();
   	List<AuthorizationGrant> authGrants = authCtx.getGrants();

   	for (AuthorizationGrant ag : authGrants) {
   		if (ag.getAccountId().equals(accountUuid) && ag.getEntityId().equals(loggedInPerson) && ag.isAccountOwner()) {
   			return;
   		}
   	}
   	
   	throw new HttpException(HttpResponseStatus.NOT_FOUND, "Current user is not the account owner");
   }
   
   
   private FullHttpResponse extractInvoice(Invoices invoices, String invoiceNumber, BilleeInfo billeeInfo) throws HttpException {
      Iterator<Invoice> it = invoices.iterator();
      while (it.hasNext()) {
         Invoice invoice = it.next();
         if (invoice.getInvoiceNumber().equals(invoiceNumber)) {
            return createInvoice(invoice, billeeInfo);
         }
      }
      throw new HttpException(HttpResponseStatus.NOT_FOUND, "No invoice with number " + invoiceNumber + " exists");
   }
   
   private FullHttpResponse createInvoice(Invoice invoice, BilleeInfo billeeInfo) {
      InvoiceContext invoiceContext = createContext(invoice, billeeInfo);
      String renderedInvoice = templateService.render(INVOICE_TEMPLATE, invoiceContext);
      ByteBuf buffer = Unpooled.wrappedBuffer(renderedInvoice.getBytes());
      return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
   }
   
   private InvoiceContext createContext(Invoice invoice, BilleeInfo billeeInfo) {      
      InvoiceContext context = new InvoiceContext();
      context.setStaticResourceBaseUrl(staticResourceBaseUrl);
      context.setInvoiceNumber(invoice.getInvoiceNumber());
      context.setBillDate(formatDate(invoice.getCreatedAt()));
      context.setDueDate(formatDate(invoice.getCreatedAt()));
      
      context.setBillee(billeeInfo.getName());
      context.setAddress1(billeeInfo.getAddress1());
      context.setAddress2(billeeInfo.getAddress2());
      context.setCity(billeeInfo.getCity());
      context.setState(billeeInfo.getState());
      context.setZipcode(billeeInfo.getZip());
      context.setCountry(billeeInfo.getCountry());
      
      context.setStatus(invoice.getState());
      Date paidDate = invoice.getClosedAt() != null ? invoice.getClosedAt() : invoice.getCreatedAt();
      context.setPaidDate(formatDate(paidDate));
      context.setTotal(formatCurrency(invoice.getTotalInCents()));
      context.setCurrency(invoice.getCurrency());
      // We will temporarily calculate this ourselves.  See I2-2304 note below.
      //context.setSubtotal(formatCurrency(invoice.getSubtotalInCents()));
      
      boolean hasTax = isThereTax(invoice.getTaxInCents());
      context.setHasTax(hasTax);
      if (hasTax) {
         context.setTax(formatCurrency(invoice.getTaxInCents()));
         context.setTaxLabel(createTaxLabel(invoice.getTaxRegion(), invoice.getTaxRate()));
      }
      
      // Accumulate Payments
      List<Transaction> transactions = invoice.getTransactions();
      int totalPaidInCents = 0;
      boolean hasPayment = false;
      if (transactions != null && !transactions.isEmpty()) {
         for (Transaction transaction : transactions) {
            if ("purchase".equals(transaction.getAction()) && equalsAny(transaction.getStatus(), "success", "void")) {
               hasPayment = true;
               totalPaidInCents += Integer.valueOf(transaction.getAmountInCents());
            }
         }
      }
      
      context.setHasPayment(hasPayment);
      context.setAmountPaid(usdCurrency.format((totalPaidInCents/100.0)));
      
      int totalDueInCents = Integer.valueOf(invoice.getTotalInCents());
      int netTotalInCents = totalDueInCents - totalPaidInCents;
      
      context.setAmountDue(usdCurrency.format(netTotalInCents/100.0));
      context.setType(calculateType(totalDueInCents, netTotalInCents));
      
      // I2-2304 There is a bug on the Recurly side, related to their "/invoice/subtotal_in_cents" field.  It doesn't
      // seem to include credit adjustments in the summation, so it comes across incorrectly.  We have notified Recurly
      // but, as a workaround until they fix it, we will calculate the invoice-level subtotal ourselves here and set it
      // below.
      int subtotalInCents = 0;
      
      boolean hasAccountCredit = false;
      int accountCreditInCents = 0;
      
      List<Adjustment> adjustments = invoice.getAdjustments();
      if (adjustments != null) {
         for (Adjustment adjustment : adjustments) {
            if (!"carryforward".equals(adjustment.getOrigin())) {
               InvoiceContext.Adjustment adjustmentContext = new InvoiceContext.Adjustment();
               adjustmentContext.setDateRange(formatDateRange(adjustment.getStartDate(), adjustment.getEndDate()));
               adjustmentContext.setDescription(adjustment.getDescription());
               adjustmentContext.setQuantity(adjustment.getQuantity());
               adjustmentContext.setUnitPrice(formatCurrency(adjustment.getUnitAmountInCents()));
               adjustmentContext.setSubtotal(formatCurrency(adjustment.getTotalInCents()));
               adjustmentContext.setTax(formatCurrency(adjustment.getTaxInCents()));
               context.getAdjustments().add(adjustmentContext);
               subtotalInCents += Integer.valueOf(adjustment.getTotalInCents());
            }
            else {
               hasAccountCredit = true;
               accountCreditInCents += Integer.valueOf(adjustment.getTotalInCents());
            }
         }
      }
      
      context.setSubtotal(usdCurrency.format((subtotalInCents/100.0)));
      
      context.setHasAccountCredit(hasAccountCredit);
      context.setAccountCredit(usdCurrency.format((accountCreditInCents/100.0)));
      
      return context;
   }
   
   private String createTaxLabel(String taxRegion, String taxRate) {
      StringBuffer sb = new StringBuffer();
      if (!StringUtils.isEmpty(taxRegion)) {
         sb.append(taxRegion);
      }
      
      if (sb.length() > 0) {
         sb.append(' ');
      }
      sb.append("Tax ");
      
      if (!StringUtils.isEmpty(taxRate)) {
         double dbl = Double.valueOf(taxRate);
         sb.append(dbl * 100);
         sb.append('%');
      }
      
      return sb.toString();
   }

   private String formatDate(Date date) {
      return date == null ? null : longFormat.format(date);
   }
   
   private String formatCurrency(String centsStr) {
      if (StringUtils.isEmpty(centsStr)) return null;
      int cents = Integer.valueOf(centsStr);
      return usdCurrency.format((cents/100.0));
   }

   private String formatDateRange(Date start, Date end) {
      return (start != null ? medFormat.format(start) : "") + "-" + (end != null ? medFormat.format(end) : "");
   }
   
   private boolean isThereTax(String taxCents) {
      if (StringUtils.isEmpty(taxCents)) {
         return false;
      }
      int value = Integer.valueOf(taxCents);
      return value != 0;
   }
   
   private String calculateType(int totalDueInCents, int netTotalInCents) {
      if (totalDueInCents < 0) {
         return InvoiceContext.TYPE_REFUND;
      }
      
      if (netTotalInCents > 0) {
         return InvoiceContext.TYPE_DUE;
      }
      else {
         return InvoiceContext.TYPE_PAID;
      }
   }
   
   private BilleeInfo createBilleeInfo(Account account, BillingInfo info) {
      BilleeInfo billee = new BilleeInfo();
      if (info != null) {
         billee.setName(info.getFirstName() + " " + info.getLastName());
         billee.setAddress1(info.getAddress1());
         billee.setAddress2(info.getAddress2());
         billee.setCity(info.getCity());
         billee.setState(info.getState());
         billee.setZip(info.getZip());
         billee.setCountry(info.getCountry());
      }
      else if (account != null) {
         String firstName = account.getFirstName() == null ? "" : account.getFirstName();
         String lastName = account.getLastName() == null ? "" : account.getLastName();
         billee.setName(firstName + " " + lastName);
         Address address = account.getAddress();
         if (address != null) {
            billee.setAddress1(address.getAddress1() == null ? "" : address.getAddress1());
            billee.setAddress2(address.getAddress2() == null ? "" : address.getAddress2());
            billee.setCity(address.getCity() == null ? "" : address.getCity());
            billee.setState(address.getState() == null ? "" : address.getState());
            billee.setZip(address.getZip() == null ? "" : address.getZip());
            billee.setCountry(address.getCountry() == null ? "" : address.getCountry());
         }
         else {
            billee.setAddress1("");
            billee.setAddress2("");
            billee.setCity("");
            billee.setState("");
            billee.setZip("");
            billee.setCountry("");
         }
      }
      else {
         billee.setName("");
         billee.setAddress1("");
         billee.setAddress2("");
         billee.setCity("");
         billee.setState("");
         billee.setZip("");
         billee.setCountry("");
      }
      return billee;
   }
   
   private static class BilleeInfo {
      private String name;
      private String address1;
      private String address2;
      private String city;
      private String state;
      private String zip;
      private String country;
      
      public String getName() {
         return name;
      }
      public void setName(String name) {
         this.name = name;
      }
      public String getAddress1() {
         return address1;
      }
      public void setAddress1(String address1) {
         this.address1 = address1;
      }
      public String getAddress2() {
         return address2;
      }
      public void setAddress2(String address2) {
         this.address2 = address2;
      }
      public String getCity() {
         return city;
      }
      public void setCity(String city) {
         this.city = city;
      }
      public String getState() {
         return state;
      }
      public void setState(String state) {
         this.state = state;
      }
      public String getZip() {
         return zip;
      }
      public void setZip(String zip) {
         this.zip = zip;
      }
      public String getCountry() {
         return country;
      }
      public void setCountry(String country) {
         this.country = country;
      }
   }
}

