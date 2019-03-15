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
package com.iris.client.invoice;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class InvoiceContext {
   public final static String TYPE_PAID = "PAID";
   public final static String TYPE_DUE = "DUE";
   public final static String TYPE_REFUND = "REFUND";
   
   private String staticResourceBaseUrl; 
   private String invoiceNumber;
   private String billDate;
   private String dueDate;
   private String billee;
   private String address1;
   private String address2;
   private String city;
   private String state;
   private String zipcode;
   private String status;
   private String paidDate;
   private String total;
   private String currency;
   private List<Adjustment> adjustments = new ArrayList<>();
   private String subtotal;
   private String amountDue;
   private String country;
   private String amountPaid;
   private String tax;
   private String taxLabel;
   private String accountCredit;
   private String type;
   private boolean hasPayment;
   private boolean hasTax;
   private boolean hasAccountCredit;
   
   public boolean getHasRefund() {
      return TYPE_REFUND.equals(type);
   }
   
   public String getTypeClass() {
      if (StringUtils.isEmpty(type)) {
         return "";
      }
      return type.toLowerCase();
   }
   
   public String getCityAndState() {
      StringBuffer sb = new StringBuffer(city);
      if (sb.length() > 0) {
         sb.append(", ");
      }
      sb.append(state);
      if (sb.length() > 0) {
         sb.append(' ');
      }
      sb.append(zipcode);
      return sb.toString();
   }
   
   public String getInvoiceNumber() {
      return invoiceNumber;
   }
   
   public void setInvoiceNumber(String invoiceNumber) {
      this.invoiceNumber = invoiceNumber;
   }

   public String getBillDate() {
      return billDate;
   }

   public void setBillDate(String billDate) {
      this.billDate = billDate;
   }

   public String getDueDate() {
      return dueDate;
   }

   public void setDueDate(String dueDate) {
      this.dueDate = dueDate;
   }

   public String getBillee() {
      return billee;
   }

   public void setBillee(String billee) {
      this.billee = billee;
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

   public String getZipcode() {
      return zipcode;
   }

   public void setZipcode(String zipcode) {
      this.zipcode = zipcode;
   }

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public String getPaidDate() {
      return paidDate;
   }

   public void setPaidDate(String paidDate) {
      this.paidDate = paidDate;
   }

   public String getTotal() {
      return total;
   }

   public void setTotal(String total) {
      this.total = total;
   }

   public String getCurrency() {
      return currency;
   }

   public void setCurrency(String currency) {
      this.currency = currency;
   }

   public List<Adjustment> getAdjustments() {
      return adjustments;
   }

   public void setAdjustments(List<Adjustment> adjustment) {
      this.adjustments = adjustment;
   }

   public String getSubtotal() {
      return subtotal;
   }

   public void setSubtotal(String subtotal) {
      this.subtotal = subtotal;
   }

   public String getAmountDue() {
      return amountDue;
   }

   public void setAmountDue(String amountDue) {
      this.amountDue = amountDue;
   }
   
   public String getCountry() {
      return country;
   }

   public void setCountry(String country) {
      this.country = country;
   }
   
   public String getAmountPaid() {
      return amountPaid;
   }

   public void setAmountPaid(String amountPaid) {
      this.amountPaid = amountPaid;
   }

   public boolean isHasPayment() {
      return hasPayment;
   }

   public void setHasPayment(boolean hasPayment) {
      this.hasPayment = hasPayment;
   }
   
   public String getTaxLabel() {
      return taxLabel;
   }

   public void setTaxLabel(String taxLabel) {
      this.taxLabel = taxLabel;
   }

   public String getTax() {
      return tax;
   }

   public void setTax(String tax) {
      this.tax = tax;
   }

   public boolean isHasTax() {
      return hasTax;
   }

   public void setHasTax(boolean hasTax) {
      this.hasTax = hasTax;
   }

   public String getAccountCredit() {
      return accountCredit;
   }

   public void setAccountCredit(String accountCredit) {
      this.accountCredit = accountCredit;
   }

   public boolean isHasAccountCredit() {
      return hasAccountCredit;
   }

   public void setHasAccountCredit(boolean hasAccountCredit) {
      this.hasAccountCredit = hasAccountCredit;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public static class Adjustment {
      private String dateRange;
      private String description;
      private String quantity;
      private String unitPrice;
      private String subtotal;
      private String tax;
      
      public String getDateRange() {
         return dateRange;
      }
      public void setDateRange(String dateRange) {
         this.dateRange = dateRange;
      }
      public String getDescription() {
         return description;
      }
      public void setDescription(String description) {
         this.description = description;
      }
      public String getQuantity() {
         return quantity;
      }
      public void setQuantity(String quantity) {
         this.quantity = quantity;
      }
      public String getUnitPrice() {
         return unitPrice;
      }
      public void setUnitPrice(String unitPrice) {
         this.unitPrice = unitPrice;
      }
      public String getSubtotal() {
         return subtotal;
      }
      public void setSubtotal(String subtotal) {
         this.subtotal = subtotal;
      }
      public String getTax() {
         return tax;
      }
      public void setTax(String tax) {
         this.tax = tax;
      }
   }

	public String getStaticResourceBaseUrl() {
		return staticResourceBaseUrl;
	}

	public void setStaticResourceBaseUrl(String staticResourceBaseUrl) {
		this.staticResourceBaseUrl = staticResourceBaseUrl;
	}
   
}

