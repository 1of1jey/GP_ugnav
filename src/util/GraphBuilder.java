package util;

import model.Edge;
import model.Graph;
import model.Node;

import java.util.*;

public final class GraphBuilder {
    public static Graph buildUGGraph() {
        Graph g = new Graph();

        addNode(g, 1, "Main Gate", 0, 0, tags("gate"));
        addNode(g, 2, "Balme Library", 2, 2, tags("library", "landmark"));
        addNode(g, 3, "Night Market", 4, 1, tags("market", "food"));
        addNode(g, 4, "Commonwealth Hall", 1, -1, tags("hall"));
        addNode(g, 5, "Legon Hall", 3, 0, tags("hall"));
        addNode(g, 6, "Akuafo Hall", 4, 0, tags("hall"));
        addNode(g, 7, "UGTD", 3, 3, tags("computing", "lab"));
        addNode(g, 8, "Business School", 4, 3, tags("business", "bank"));
        addNode(g, 9, "Banking Square", 5, 3, tags("bank"));
        addNode(g, 10, "JQB", 1, 1, tags("lecture", "block"));
        addNode(g, 11, "Central Cafeteria", 5, 1, tags("food", "cafeteria"));
        addNode(g, 12, "Law School", 6, 2, tags("law", "faculty"));
        addNode(g, 13, "UG Hospital", 7, 2, tags("hospital", "clinic"));
        addNode(g, 14, "Gym", 7, 0, tags("gym", "sports"));
        addNode(g, 15, "Stadium", 7, 1, tags("stadium", "sports"));
        addNode(g, 16, "N (Engineering)", 5, 4, tags("engineering", "block"));
        addNode(g, 17, "Volta Hall", 3, 1, tags("hall"));
        addNode(g, 18, "University Basic School", 2, -1, tags("school"));
        addNode(g, 19, "Noguchi", 8, 3, tags("research", "lab"));
        addNode(g, 20, "UPSA Road Junction", -1, 0, tags("junction"));

        java.util.function.Function<Double, Double> minutes = meters -> meters / 80.0;

        addEdge(g, 1, 10, 1000, minutes.apply(1000.0));
        addEdge(g, 1, 4, 1200, minutes.apply(1200.0));
        addEdge(g, 1, 20, 500, minutes.apply(500.0));
        addEdge(g, 10, 2, 400, minutes.apply(400.0));
        addEdge(g, 2, 7, 600, minutes.apply(600.0));
        addEdge(g, 7, 8, 700, minutes.apply(700.0));
        addEdge(g, 8, 9, 400, minutes.apply(400.0));
        addEdge(g, 8, 5, 500, minutes.apply(500.0));
        addEdge(g, 5, 6, 300, minutes.apply(300.0));
        addEdge(g, 6, 3, 200, minutes.apply(200.0));
        addEdge(g, 3, 11, 300, minutes.apply(300.0));
        addEdge(g, 11, 12, 800, minutes.apply(800.0));
        addEdge(g, 12, 13, 900, minutes.apply(900.0));
        addEdge(g, 13, 15, 1200, minutes.apply(1200.0));
        addEdge(g, 15, 14, 300, minutes.apply(300.0));
        addEdge(g, 7, 16, 800, minutes.apply(800.0));
        addEdge(g, 16, 12, 700, minutes.apply(700.0));
        addEdge(g, 5, 17, 400, minutes.apply(400.0));
        addEdge(g, 17, 2, 500, minutes.apply(500.0));
        addEdge(g, 2, 12, 900, minutes.apply(900.0));
        addEdge(g, 4, 10, 800, minutes.apply(800.0));
        addEdge(g, 4, 18, 700, minutes.apply(700.0));
        addEdge(g, 9, 12, 600, minutes.apply(600.0));
        addEdge(g, 12, 19, 1100, minutes.apply(1100.0));

        return g;
    }

    public static void addNode(Graph g, int id, String name, double x, double y, java.util.Set<String> tags) {
        g.addNode(new Node(id, name, x, y, tags));
    }

    public static void addEdge(Graph g, int from, int to, double meters, double baseMinutes) {
        g.addEdge(new Edge(from, to, meters, baseMinutes, true, 1.3, 1.0));
    }

    public static Set<String> tags(String... t) {
        return new HashSet<>(Arrays.asList(t));
    }
}


