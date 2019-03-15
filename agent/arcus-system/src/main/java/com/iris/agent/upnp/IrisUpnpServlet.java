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
package com.iris.agent.upnp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.iris.agent.hal.IrisHal;

final class IrisUpnpServlet extends HttpServlet {
   private static final long serialVersionUID = 499299404785421634L;

   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse rsp) throws ServletException, IOException {
      switch (req.getPathInfo()) {
      case "/device.xml":
         handleDevice(req, rsp);
         break;

      default:
         handleNotFound(req, rsp);
         break;
      }
   }

   protected void handleDevice(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
      rsp.setDateHeader("Date", System.currentTimeMillis());
      rsp.addHeader("Connection", "close");
      rsp.setContentType("text/xml");
      rsp.setStatus(200);

      String ip = IrisHal.getPrimaryIP();
      String model = IrisHal.getModel();
      String vendor = IrisHal.getVendor();

      rsp.getWriter().append(
         "<root xmlns=\"urn:schemas-upnp-org:device-1-0\">" + 
            "<specVersion>" + 
               "<major>1</major>" + 
               "<minor>0</minor>" + 
            "</specVersion>" + 
            "<URLBase>http://" + ip + ":8080/upnp</URLBase>" + 
            "<device>" + 
               "<deviceType>urn:schemas-upnp-org:device:Basic:1</deviceType>" + 
               "<friendlyName>Iris Hub</friendlyName>" + 
               "<manufacturer>" + vendor + "</manufacturer>" + 
               "<modelDescription>Iris Home Automation Hub</modelDescription>" + 
               "<modelName>Iris " + model + " Hub</modelName>" + 
               "<modelNumber>" + model + "</modelNumber>" + 
               "<UDN>uuid:" + IrisUpnpService.uuid + "</UDN>" + 
               "<presentationURL>http://" + ip + ":8080</presentationURL>" + 
            "</device>" + 
         "</root>"
      );
   }

   protected void handleNotFound(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
      rsp.setDateHeader("Date", System.currentTimeMillis());
      rsp.addHeader("Connection", "close");
      rsp.setContentType("text/html");
      rsp.setStatus(404);

      rsp.getWriter().append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0 Frameset//EN\">" +
         "<html><head></head><body>404 - NOT FOUND</body></html>");
   }
}

