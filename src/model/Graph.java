package model;

import java.time.LocalTime;
import java.util.*;

public final class Graph {
    public final Map<Integer, Node> idToNode = new HashMap<>();
    public final Map<Integer, List<Edge>> adjacency = new HashMap<>();

    public void addNode(Node node) {
        idToNode.put(node.id, node);
        adjacency.computeIfAbsent(node.id, k -> new ArrayList<>());
    }

    public void addEdge(Edge edge) {
        adjacency.computeIfAbsent(edge.fromId, k -> new ArrayList<>()).add(edge);
        if (edge.bidirectional) {
            Edge back = new Edge(
                    edge.toId, edge.fromId,
                    edge.distanceMeters, edge.baseMinutes, true,
                    edge.peakMultiplier, edge.offPeakMultiplier
            );
            adjacency.computeIfAbsent(edge.toId, k -> new ArrayList<>()).add(back);
        }
    }

    public List<Edge> neighbors(int nodeId) {
        return adjacency.getOrDefault(nodeId, Collections.emptyList());
    }

    public Edge getEdge(int fromId, int toId) {
        for (Edge e : neighbors(fromId)) {
            if (e.toId == toId) return e;
        }
        return null;
    }

    public static double euclidean(Node a, Node b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static double trafficMultiplierFor(LocalTime time, Edge edge) {
        int hour = time.getHour();
        boolean peak = (hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 18);
        return peak ? edge.peakMultiplier : edge.offPeakMultiplier;
    }
}


