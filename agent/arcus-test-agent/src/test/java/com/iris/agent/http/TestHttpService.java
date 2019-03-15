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
package com.iris.agent.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.iris.agent.test.SystemTestCase;

@Ignore
@RunWith(JUnit4.class)
public class TestHttpService extends SystemTestCase {
   @Test
   public void testHttpGet() throws Exception {
      try (CloseableHttpResponse rsp = HttpService.get("http://httpbin.org/status/200")) {
         System.out.println("HTTP RESPONSE: " + rsp);
         Assert.assertEquals(200, rsp.getStatusLine().getStatusCode());
      }
   }

   @Test(expected=UnknownHostException.class)
   public void testHttpGetBadHost() throws Exception {
      try (CloseableHttpResponse rsp = HttpService.get("http://adflakjsdflakjasdfjalkjsadlkjasdfaaaaadflkjasdflkjasdflkj")) {
         System.out.println("HTTP RESPONSE: " + rsp);
      }
   }

   @Test
   public void testHttpGet404() throws Exception {
      try (CloseableHttpResponse rsp = HttpService.get("http://httpbin.org/status/404")) {
         System.out.println("HTTP RESPONSE: " + rsp);
         Assert.assertEquals(404, rsp.getStatusLine().getStatusCode());
      }
   }

   @Test
   public void testHttpDownload() throws Exception {
      File output = HttpService.download("http://httpbin.org/stream-bytes/1024");
      output.deleteOnExit();

      System.out.println("downloaded file: " + output);
      System.out.println("downloaded file size: " + output.length());
      try (InputStream is = IOUtils.toBufferedInputStream(new FileInputStream(output))) {
         byte[] results = IOUtils.toByteArray(is);
         Assert.assertTrue("download did not work correctly", results != null && results.length > 16);
      }
   }

   @Test
   public void testHttpDownloadContent() throws Exception {
      byte[] output = HttpService.content("http://httpbin.org/stream-bytes/1024");

      System.out.println("downloaded size: " + output.length);
      System.out.println("downloaded content: " + Arrays.toString(output));
      Assert.assertTrue("download did not work correctly", output != null && output.length > 16);
   }

   @Test
   public void testHttpDownloadContentAsString() throws Exception {
      String output = HttpService.contentAsString("http://httpbin.org/html");
      System.out.println("downloaded size: " + output.length());
      System.out.println("downloaded content: " + output);
      Assert.assertTrue("download did not work correctly", output != null && output.length() > 16);
   }

   @Test
   public void testHttpDownloadWithAuthentication() throws Exception {
      Credentials auth = HttpService.basic("testuser", "testpasswd");
      String output = HttpService.contentAsString("http://httpbin.org/basic-auth/testuser/testpasswd", auth);
      System.out.println("downloaded size: " + output.length());
      System.out.println("downloaded content: " + output);
      Assert.assertTrue("download did not work correctly", output != null && output.length() > 16);
   }

   @Test
   public void testHttpDownloadWithAuthenticationAndSsl() throws Exception {
      Credentials auth = HttpService.basic("testuser", "testpasswd");
      String output = HttpService.contentAsString("https://httpbin.org/basic-auth/testuser/testpasswd", auth);
      System.out.println("downloaded size: " + output.length());
      System.out.println("downloaded content: " + output);
      Assert.assertTrue("download did not work correctly", output != null && output.length() > 16);
   }

   @Test(expected=IOException.class)
   public void testHttpDownloadFailure() throws Exception {
      String output = HttpService.contentAsString("https://httpbin.org/status/404");
      System.out.println("downloaded content: " + output);
   }
}

