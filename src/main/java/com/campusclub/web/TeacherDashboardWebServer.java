package com.campusclub.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP Web Server hosting the /teacherDashboard route
 * and serving frontend files (HTML / JSP views in src/main/webapp).
 */
public final class TeacherDashboardWebServer {

    private static HttpServer server;
    private static int activePort = 8080;
    private static final Object lock = new Object();

    private TeacherDashboardWebServer() {}

    /**
     * Starts the HTTP Server if not already active.
     *
     * @return active port number
     */
    public static int startServerIfNotRunning() {
        int defaultPort = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                defaultPort = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }
        return startServerIfNotRunning(defaultPort);
    }

    /**
     * Starts the HTTP Server on the requested port (or attempts next open port).
     *
     * @param initialPort Starting port to bind
     * @return active port number
     */
    public static int startServerIfNotRunning(int initialPort) {
        synchronized (lock) {
            if (server != null) {
                return activePort;
            }

            int port = initialPort;
            for (int attempt = 0; attempt < 10; attempt++) {
                try {
                    server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
                    activePort = port;
                    break;
                } catch (IOException e) {
                    port++;
                }
            }

            if (server == null) {
                throw new IllegalStateException("Could not bind HTTP server to ports " + initialPort + "-" + (initialPort + 9));
            }

            TeacherDashboardController teacherController = new TeacherDashboardController();
            WebPortalController portalController = new WebPortalController();

            // 1. Existing Teacher Dashboard Routes (100% backward compatible)
            server.createContext("/teacherDashboard", teacherController);
            server.createContext("/teacher-dashboard", teacherController);
            server.createContext("/api/teacherDashboard", teacherController);
            server.createContext("/teacherDashboard.jsp", teacherController);
            server.createContext("/teacherDashboard.html", teacherController);
            server.createContext("/teacherdashboard", teacherController);
            server.createContext("/TeacherDashboard", teacherController);

            // 2. Multi-User Web Portal & Authentication Routes
            server.createContext("/login", portalController);
            server.createContext("/portal", portalController);
            server.createContext("/adminPortal", portalController);
            server.createContext("/api/auth", portalController);
            server.createContext("/api/clubs", portalController);
            server.createContext("/api/events", portalController);
            server.createContext("/api/announcements", portalController);
            server.createContext("/api/memberships", portalController);
            server.createContext("/api/team", portalController);
            server.createContext("/api/db-status", portalController);

            // 3. Health check
            server.createContext("/healthz", exchange -> {
                byte[] response = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            });

            // 4. Root Context ("/") Handler
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.toLowerCase().contains("format=json")) {
                    teacherController.handle(exchange);
                    return;
                }
                if (path == null || path.isEmpty() || path.equals("/") ||
                    path.equalsIgnoreCase("/index.html") ||
                    path.equalsIgnoreCase("/index.jsp")) {
                    portalController.handle(exchange);
                } else if (path.toLowerCase().startsWith("/teacher")) {
                    teacherController.handle(exchange);
                } else if (path.toLowerCase().startsWith("/api/") ||
                           path.toLowerCase().startsWith("/login") ||
                           path.toLowerCase().startsWith("/portal") ||
                           path.toLowerCase().startsWith("/dashboard")) {
                    portalController.handle(exchange);
                } else if (path.startsWith("/webapp")) {
                    new WebAppFileHandler(teacherController).handle(exchange);
                } else {
                    portalController.handle(exchange);
                }
            });

            // 5. Static / JSP WebApp View Handler: /webapp/
            server.createContext("/webapp", new WebAppFileHandler(teacherController));

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            return activePort;
        }
    }

    /**
     * Stops the running server.
     */
    public static void stopServer() {
        synchronized (lock) {
            if (server != null) {
                server.stop(0);
                server = null;
            }
        }
    }

    public static boolean isRunning() {
        synchronized (lock) {
            return server != null;
        }
    }

    public static int getActivePort() {
        return activePort;
    }

    /**
     * Handler to serve frontend view files from src/main/webapp.
     */
    private static class WebAppFileHandler implements HttpHandler {
        private final TeacherDashboardController controller;

        public WebAppFileHandler(TeacherDashboardController controller) {
            this.controller = controller;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith(".jsp") || path.endsWith("teacherDashboard.html") || path.endsWith("/webapp") || path.endsWith("/webapp/")) {
                controller.handle(exchange);
                return;
            }

            // Otherwise serve static assets
            String relativePath = path.replace("/webapp", "").replace("/", File.separator);
            File baseDir = new File("src" + File.separator + "main" + File.separator + "webapp");
            File targetFile = new File(baseDir, relativePath);

            if (!targetFile.exists() || targetFile.isDirectory()) {
                controller.handle(exchange);
                return;
            }

            byte[] content = Files.readAllBytes(targetFile.toPath());
            String contentType = targetFile.getName().endsWith(".html") ? "text/html; charset=UTF-8" : "text/plain";
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        }
    }
}
