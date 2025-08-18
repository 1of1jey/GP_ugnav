package web;

import algorithm.AStar;
import algorithm.Dijkstra;
import algorithm.RouteSearch;
import model.Graph;
import route.Route;
import route.WeightMode;
import util.GraphBuilder;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;

public final class WebServer {
    private final HttpServer server;
    private final Graph graph;
    private final Path staticRoot;

    public WebServer(int port, Path staticRoot) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.graph = GraphBuilder.buildUGGraph();
        this.staticRoot = staticRoot;
        registerContexts();
    }

    private void registerContexts() {
        server.createContext("/", new StaticHandler(staticRoot));
        server.createContext("/api/landmarks", this::handleLandmarks);
        server.createContext("/api/route", this::handleRoute);
    }

    public void start() {
        server.start();
        System.out.println("Web UI running on http://localhost:" + server.getAddress().getPort() + "/");
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
    }

    private void handleLandmarks(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("GET")) { sendText(ex, 405, "Method Not Allowed"); return; }
        List<Integer> ids = new ArrayList<>(graph.idToNode.keySet());
        Collections.sort(ids);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i);
            String name = graph.idToNode.get(id).name;
            if (i > 0) sb.append(',');
            sb.append('{')
              .append("\"id\":").append(id).append(',')
              .append("\"name\":\"").append(escapeJson(name)).append("\"")
              .append('}');
        }
        sb.append("]");
        sendJson(ex, 200, sb.toString());
    }

    private void handleRoute(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("GET")) { sendText(ex, 405, "Method Not Allowed"); return; }
        Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
        try {
            int start = Integer.parseInt(q.getOrDefault("start", "0"));
            int goal = Integer.parseInt(q.getOrDefault("goal", "0"));
            String modeStr = q.getOrDefault("mode", "TIME");
            WeightMode mode = modeStr.equalsIgnoreCase("DISTANCE") ? WeightMode.DISTANCE : WeightMode.TIME;
            String algo = q.getOrDefault("algo", "astar").toLowerCase(Locale.ROOT);
            String timeStr = q.getOrDefault("time", "");
            LocalTime depart = timeStr.isEmpty() ? LocalTime.now() : LocalTime.parse(timeStr);

            List<Route> routes = new ArrayList<>();
            if ("dijkstra".equals(algo)) {
                Route r = Dijkstra.shortestPath(graph, start, goal, mode, depart);
                if (r != null) routes.add(r);
            } else if ("keyword".equals(algo)) {
                String kw = q.getOrDefault("kw", "");
                int k = Integer.parseInt(q.getOrDefault("k", "3"));
                routes = RouteSearch.routesViaKeyword(graph, start, goal, kw, k, mode, depart);
            } else { // default astar
                Route r = AStar.shortestPath(graph, start, goal, mode, depart);
                if (r != null) routes.add(r);
            }

            String json = routesToJson(graph, routes);
            sendJson(ex, 200, json);
        } catch (Exception e) {
            sendJson(ex, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String pair : raw.split("&")) {
            int idx = pair.indexOf('=');
            String k = idx >= 0 ? pair.substring(0, idx) : pair;
            String v = idx >= 0 ? pair.substring(idx + 1) : "";
            k = urlDecode(k);
            v = urlDecode(v);
            map.put(k, v);
        }
        return map;
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String routesToJson(Graph graph, List<Route> routes) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"routes\":[");
        for (int i = 0; i < routes.size(); i++) {
            Route r = routes.get(i);
            if (i > 0) sb.append(',');
            sb.append('{');
            sb.append("\"seq\":[");
            for (int j = 0; j < r.nodeSequence.size(); j++) {
                if (j > 0) sb.append(',');
                sb.append(r.nodeSequence.get(j));
            }
            sb.append("],");
            sb.append("\"distanceMeters\":").append(r.totalDistanceMeters).append(',');
            sb.append("\"minutes\":").append(r.totalMinutes).append(',');
            sb.append("\"pretty\":\"").append(escapeJson(r.pretty(graph))).append("\"");
            sb.append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private static void sendJson(HttpExchange ex, int code, String body) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.add("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static void sendText(HttpExchange ex, int code, String body) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.add("Content-Type", "text/plain; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static final class StaticHandler implements HttpHandler {
        private final Path root;

        StaticHandler(Path root) { this.root = root; }

        @Override public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            Path file = root.resolve(path.substring(1)).normalize();
            if (!file.startsWith(root) || !Files.exists(file) || Files.isDirectory(file)) {
                sendText(ex, 404, "Not Found");
                return;
            }
            String mime = guessMime(file);
            Headers h = ex.getResponseHeaders();
            h.add("Content-Type", mime);
            byte[] bytes = Files.readAllBytes(file);
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        }

        private static String guessMime(Path p) {
            String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
            if (n.endsWith(".html")) return "text/html; charset=utf-8";
            if (n.endsWith(".css")) return "text/css; charset=utf-8";
            if (n.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (n.endsWith(".json")) return "application/json; charset=utf-8";
            return "application/octet-stream";
        }
    }

    public static void main(String[] args) throws Exception {
        Path staticRoot = Path.of("public");
        if (!Files.exists(staticRoot)) {
            Files.createDirectories(staticRoot);
        }
        WebServer ws = new WebServer(8080, staticRoot);
        ws.start();
        System.out.println("Press Ctrl+C to stop.");
    }
}


