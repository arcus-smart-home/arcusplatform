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
package com.iris.messages.model;

import java.net.URI;
import java.util.Date;

public class Invoice {
   private Date invoiceDate;
   private double totalAmount;
   private URI invoiceAddress;
   
   public Date getInvoiceDate() {
      return invoiceDate;
   }
   
   public void setInvoiceDate(Date invoiceDate) {
      this.invoiceDate = invoiceDate;
   }
   
   public double getTotalAmount() {
      return totalAmount;
   }
   
   public void setTotalAmount(double totalAmount) {
      this.totalAmount = totalAmount;
   }
   
   public URI getInvoiceAddress() {
      return invoiceAddress;
   }
   
   public void setInvoiceAddress(URI invoiceAddress) {
      this.invoiceAddress = invoiceAddress;
   }
   
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((invoiceAddress == null) ? 0 : invoiceAddress.hashCode());
      result = prime * result
            + ((invoiceDate == null) ? 0 : invoiceDate.hashCode());
      long temp;
      temp = Double.doubleToLongBits(totalAmount);
      result = prime * result + (int) (temp ^ (temp >>> 32));
      return result;
   }
   
   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      Invoice other = (Invoice) obj;
      if (invoiceAddress == null) {
         if (other.invoiceAddress != null)
            return false;
      } else if (!invoiceAddress.equals(other.invoiceAddress))
         return false;
      if (invoiceDate == null) {
         if (other.invoiceDate != null)
            return false;
      } else if (!invoiceDate.equals(other.invoiceDate))
         return false;
      if (Double.doubleToLongBits(totalAmount) != Double
            .doubleToLongBits(other.totalAmount))
         return false;
      return true;
   }
   
   @Override
   public String toString() {
      return "Invoice [invoiceDate=" + invoiceDate + ", totalAmount="
            + totalAmount + ", invoiceAddress=" + invoiceAddress + "]";
   }
   
}

