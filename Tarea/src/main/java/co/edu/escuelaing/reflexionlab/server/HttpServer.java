package co.edu.escuelaing.reflexionlab.server;

import co.edu.escuelaing.reflexionlab.annotations.GetMapping;
import co.edu.escuelaing.reflexionlab.annotations.RequestParam;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HttpServer {

    private static final int DEFAULT_PORT = 8080;
    private final Map<String, Object[]> routeMap = new HashMap<>();
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private ExecutorService threadPool;

    public void registerController(Object controller) {
        Class<?> clazz = controller.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                String path = method.getAnnotation(GetMapping.class).value();
                routeMap.put(path, new Object[]{controller, method});
                System.out.println("[MicroSpring] Registered route: GET " + path
                        + " → " + clazz.getSimpleName() + "." + method.getName() + "()");
            }
        }
    }

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        threadPool = Executors.newFixedThreadPool(10);

        // Apagado elegante con Runtime Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[MicroSpring] Shutting down server...");
            running = false;
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                threadPool.shutdown();
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (Exception e) {
                threadPool.shutdownNow();
            }
            System.out.println("[MicroSpring] Server stopped gracefully.");
        }));

        System.out.println("[MicroSpring] Server started on http://localhost:" + port);
        System.out.println("[MicroSpring] Registered routes: " + routeMap.keySet());

        // Manejo concurrente — cada petición en su propio hilo
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> {
                    try {
                        handleRequest(clientSocket);
                    } catch (IOException e) {
                        System.err.println("[MicroSpring] Error handling request: " + e.getMessage());
                    } finally {
                        try { clientSocket.close(); } catch (IOException ignored) {}
                    }
                });
            } catch (IOException e) {
                if (running) {
                    System.err.println("[MicroSpring] Error: " + e.getMessage());
                }
            }
        }
    }

    public void start() throws IOException {
        start(DEFAULT_PORT);
    }

    private void handleRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream out = clientSocket.getOutputStream();

        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isEmpty()) return;

        System.out.println("[MicroSpring] " + requestLine);

        String[] parts = requestLine.split(" ");
        if (parts.length < 2) return;

        String fullPath = parts[1];
        String path = fullPath.contains("?") ? fullPath.substring(0, fullPath.indexOf("?")) : fullPath;
        String queryString = fullPath.contains("?") ? fullPath.substring(fullPath.indexOf("?") + 1) : "";

        Map<String, String> queryParams = parseQueryParams(queryString);

        if (routeMap.containsKey(path)) {
            handleControllerRoute(path, queryParams, out);
        } else {
            handleStaticFile(path, out);
        }
    }

    private void handleControllerRoute(String path, Map<String, String> queryParams, OutputStream out)
            throws IOException {
        Object[] entry = routeMap.get(path);
        Object controller = entry[0];
        Method method = (Method) entry[1];

        try {
            Object[] args = resolveMethodArgs(method, queryParams);
            String result = (String) method.invoke(controller, args);
            sendResponse(out, 200, "text/html", result.getBytes());
        } catch (Exception e) {
            String error = "Internal Server Error: " + e.getMessage();
            sendResponse(out, 500, "text/plain", error.getBytes());
        }
    }

    private Object[] resolveMethodArgs(Method method, Map<String, String> queryParams) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(RequestParam.class)) {
                RequestParam rp = parameters[i].getAnnotation(RequestParam.class);
                args[i] = queryParams.getOrDefault(rp.value(), rp.defaultValue());
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    private void handleStaticFile(String path, OutputStream out) throws IOException {
        if (path.equals("/")) path = "/index.html";

        InputStream fileStream = getClass().getResourceAsStream("/webroot" + path);

        if (fileStream == null) {
            String notFound = "<html><body><h1>404 - Not Found</h1></body></html>";
            sendResponse(out, 404, "text/html", notFound.getBytes());
            return;
        }

        byte[] fileBytes = fileStream.readAllBytes();
        sendResponse(out, 200, getContentType(path), fileBytes);
    }

    private void sendResponse(OutputStream out, int statusCode, String contentType, byte[] body)
            throws IOException {
        String statusText = statusCode == 200 ? "OK" : statusCode == 404 ? "Not Found" : "Internal Server Error";
        String header = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";
        out.write(header.getBytes());
        out.write(body);
        out.flush();
    }

    private Map<String, String> parseQueryParams(String queryString) {
        Map<String, String> params = new HashMap<>();
        if (queryString == null || queryString.isEmpty()) return params;

        for (String pair : queryString.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) params.put(kv[0], kv[1]);
            else if (kv.length == 1) params.put(kv[0], "");
        }
        return params;
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css"))  return "text/css";
        if (path.endsWith(".js"))   return "application/javascript";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg"))  return "image/jpeg";
        return "text/plain";
    }
}