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

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.http.servlet.SpyApiServlet;
import com.iris.agent.http.servlet.SpyServlet;
import com.iris.agent.spy.SpyService;
import com.iris.agent.storage.StorageService;

public final class HttpServer {
   private static final Logger log = LoggerFactory.getLogger(HttpServer.class);
   private static @Nullable Server server;
   private static @Nullable ContextHandlerCollection contexts;
   private static @Nullable Map<String,Map<Integer,Connector>> connectors;
   private static @Nullable Context mainContext;
   private static @Nullable Connector mainConnector;

   public static final int PORT = 8080;

   private HttpServer() {
   }

   public static void addServlet(String path, Servlet servlet) {
      Context ctx = getMainContext();
      ctx.addServlet(path, servlet);
   }

   public static Context registerContext(String contextPath) {
      DefaultContext ctx = new DefaultContext(contextPath);
      getContexts().addHandler(ctx.context);

      ctx.start();
      return ctx;
   }

   public static Connector registerConnector(int port) {
      return registerConnector("", port);
   }

   public static Connector registerConnector(String host, int port) {
      Map<String,Map<Integer,Connector>> conns = connectors;
      if (conns == null) {
         throw new RuntimeException("http server not started");
      }

      synchronized (conns) {
         Map<Integer,Connector> existingHost = conns.get(host);
         Connector existingConn = (existingHost != null) ? existingHost.get(port) : null;
         if (existingConn != null) {
            return existingConn;
         }

         DefaultConnector conn = (host.isEmpty()) ? new DefaultConnector(getServer(),port)
                                                  : new DefaultConnector(getServer(),host,port);
         if (existingHost == null) {
            existingHost = new LinkedHashMap<>();
            conns.put(host, existingHost);
         }

         existingHost.put(port, conn);

         conn.start();
         return conn;
      }
   }

   @Nullable
   public static Connector getConnector(int port) {
      return getConnector("", port);
   }

   @Nullable
   public static Connector getConnector(String host, int port) {
      Map<String,Map<Integer,Connector>> conns = connectors;
      if (conns == null) {
         throw new RuntimeException("http server not started");
      }

      synchronized (conns) {
         Map<Integer,Connector> existingHost = conns.get(host);
         return (existingHost != null) ? existingHost.get(port) : null;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Lifecycle support
   /////////////////////////////////////////////////////////////////////////////

   public static void start() {
      try {
         DefaultContext main = new DefaultContext();
         String base = StorageService.getFile("agent:///www").getPath();
         
         org.eclipse.jetty.servlet.DefaultServlet defServlet = new org.eclipse.jetty.servlet.DefaultServlet();
         main.addServlet("/", defServlet, ImmutableMap.<String,String>of(
            "dirAllowed", "false",
            "welcomeServlets", "true",
            "resourceBase", base
         ));

         main.context.setWelcomeFiles(new String[] { "index.html" });
         main.addServlet("/index.html", new DefaultServlet());
         if (SpyService.INSTANCE.isActive()) {
         	main.addServlet("/spy/api", new SpyApiServlet());
         	main.addServlet("/spy", new SpyServlet());
         }

         main.context.setErrorHandler(new ErrorPage());

         ContextHandlerCollection ctxs = new ContextHandlerCollection();
         ctxs.addHandler(main.context);

         ThreadFactory tf = new HttpThreadFactory();
         BlockingQueue<Runnable> queue = new SynchronousQueue<>();
         ThreadPoolExecutor exec = new ThreadPoolExecutor(4,16,60,TimeUnit.SECONDS,queue,tf);

         Server srv = new Server(new ExecutorThreadPool(exec));
         srv.setHandler(ctxs);
         srv.setStopAtShutdown(false);
         srv.setStopTimeout(500);

         Map<String,Map<Integer,Connector>> conns = new LinkedHashMap<>();
         Map<Integer,Connector> dconns = new LinkedHashMap<>();
         conns.put("", dconns);

         DefaultConnector conn = new DefaultConnector(srv,PORT);
         srv.setConnectors(new ServerConnector[] { conn.connector });
         dconns.put(PORT, conn);

         mainConnector = conn;
         connectors = conns;

         mainContext = main;
         contexts = ctxs;

         server = srv;
         srv.start();
      } catch (Exception ex) {
         log.warn("failed to start http server:", ex);
      }
   }

   public static void shutdown() {
      try {
         Server srv = server;
         if (srv != null) {
            srv.stop();
         }

         server = null;
         contexts = null;
         mainContext = null;
      } catch (Exception ex) {
         log.warn("failed to stop http server:", ex);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utility methods
   /////////////////////////////////////////////////////////////////////////////

   private static Server getServer() {
      Server srv = server;
      if (srv == null) {
         throw new RuntimeException("http server not started");
      }

      return srv;
   }

   private static ContextHandlerCollection getContexts() {
      ContextHandlerCollection ctxs = contexts;
      if (ctxs == null) {
         throw new RuntimeException("http server not started");
      }

      return ctxs;
   }

   private static Context getMainContext() {
      Context ctx = mainContext;
      if (ctx == null) {
         throw new RuntimeException("http server not started");
      }

      return ctx;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Externally accessible handles
   /////////////////////////////////////////////////////////////////////////////

   public interface Connector {
      void shutdown();
      int getLocalPort();
   }

   public interface Context {
      void addServlet(String path, Servlet servlet);
      void addServlet(String path, Servlet servlet, Map<String,String> initParams);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation details
   /////////////////////////////////////////////////////////////////////////////

   private static final class DefaultServlet extends HttpServlet {
      private static final long serialVersionUID = -8339237275174978543L;

      @Override
      protected void doGet(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp) throws ServletException, IOException {
         resp.setContentType("text/html");
         resp.setStatus(HttpServletResponse.SC_OK);
         resp.getWriter().println(
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0 Frameset//EN\">" +
            "<html><head></head><body>" + 
            IrisHal.getOperatingSystemVersion() + 
            "</body></html>"
         );
      }
   }

   private static final class ErrorPage extends ErrorHandler {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
         super.handle(target, baseRequest, request, response);
      }

      @Override
      protected void handleErrorPage(HttpServletRequest request, Writer writer, int code, String message) throws IOException {
         switch (code) {
         case 403: handleForbidden(request, writer, code, message); break;
         case 404: handleNotFound(request, writer, code, message); break;
         default:  handleDefault(request, writer, code, message); break;
         }

      }

      protected void handleForbidden(HttpServletRequest request, Writer writer, int code, String message) throws IOException {
         writer.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0 Frameset//EN\">" +
            "<html><head></head><body>403 - FORBIDDEN</body></html>");
      }

      protected void handleNotFound(HttpServletRequest request, Writer writer, int code, String message) throws IOException {
         writer.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0 Frameset//EN\">" +
            "<html><head></head><body>404 - NOT FOUND</body></html>");
      }

      protected void handleDefault(HttpServletRequest request, Writer writer, int code, String message) throws IOException {
         writer.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0 Frameset//EN\">" +
            "<html><head></head><body>" + code + "</body></html>");
      }
   }

   private static final class DefaultContext implements Context {
      private final ServletContextHandler context;

      private DefaultContext() {
         this.context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
      }

      private DefaultContext(String path) {
         this();
         this.context.setContextPath(path);
      }

      private void start() {
         try {
            context.start();
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         }
      }

      @Override
      public void addServlet(String path, Servlet servlet) {
         try {
            context.addServlet(new ServletHolder(servlet), path);
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         }
      }

      @Override
      public void addServlet(String path, Servlet servlet, Map<String,String> initParams) {
         try {
            ServletHolder holder = new ServletHolder(servlet);
            holder.setInitParameters(initParams);

            context.addServlet(holder, path);
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         }
      }
   }

   private static final class DefaultConnector implements Connector {
      private final ServerConnector connector;

      public DefaultConnector(Server server, int port) {
         HttpConfiguration config = new HttpConfiguration();
         config.setSendServerVersion(false);
         config.setSendXPoweredBy(false);

         HttpConnectionFactory factory = new HttpConnectionFactory(config);

         this.connector = new ServerConnector(server, factory);
         this.connector.setPort(port);
         this.connector.setReuseAddress(true);
      }

      public DefaultConnector(Server server, String host, int port) {
         this(server,port);
         this.connector.setHost(host);
      }

      private void start() {
         try {
            connector.start();
         } catch (Exception ex) {
            throw new RuntimeException();
         }
      }

      @Override
      public int getLocalPort() {
         return connector.getPort();
      }

      @Override
      public void shutdown() {
         try {
            connector.stop();

            Server srv = server;
            if (srv != null) {
               srv.removeConnector(connector);
            }
         } catch (Exception ex) {
            throw new RuntimeException();
         }
      }
   }

   private static final class HttpThreadFactory implements ThreadFactory {
      private final AtomicLong next = new AtomicLong();

      @Override
      public Thread newThread(@Nullable Runnable r) {
         Thread thr = new Thread(r);
         thr.setName("http" + next.getAndIncrement());
         thr.setDaemon(true);

         return thr;
      }
   }
}

