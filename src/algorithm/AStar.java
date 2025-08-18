package algorithm;

import model.Edge;
import model.Graph;
import model.Node;
import route.Route;
import route.WeightMode;

import java.time.LocalTime;
import java.util.*;

public final class AStar {
    public static Route shortestPath(Graph g, int startId, int goalId, WeightMode mode, LocalTime depart) {
        Map<Integer, Double> gScore = new HashMap<>();
        Map<Integer, Double> fScore = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();

        for (int id : g.idToNode.keySet()) {
            gScore.put(id, Double.POSITIVE_INFINITY);
            fScore.put(id, Double.POSITIVE_INFINITY);
        }
        gScore.put(startId, 0.0);
        fScore.put(startId, heuristic(g, startId, goalId, mode));

        Map<Integer, Double> timeSoFar = new HashMap<>();
        timeSoFar.put(startId, 0.0);

        PriorityQueue<int[]> open = new PriorityQueue<>(Comparator.comparingDouble(a -> fScore.get(a[0])));
        open.add(new int[]{startId});

        Set<Integer> closed = new HashSet<>();

        while (!open.isEmpty()) {
            int current = open.poll()[0];
            if (current == goalId) break;
            closed.add(current);

            for (Edge e : g.neighbors(current)) {
                if (closed.contains(e.toId)) continue;

                double cost;
                if (mode == WeightMode.DISTANCE) {
                    cost = e.distanceMeters;
                } else {
                    double minutesIntoTrip = timeSoFar.getOrDefault(current, 0.0);
                    LocalTime t = depart.plusMinutes((long) Math.floor(minutesIntoTrip));
                    double mult = Graph.trafficMultiplierFor(t, e);
                    cost = e.baseMinutes * mult;
                }

                double tentativeG = gScore.get(current) + cost;
                if (tentativeG < gScore.get(e.toId)) {
                    parent.put(e.toId, current);
                    gScore.put(e.toId, tentativeG);
                    if (mode == WeightMode.TIME) {
                        double minutesIntoTrip = timeSoFar.getOrDefault(current, 0.0);
                        timeSoFar.put(e.toId, minutesIntoTrip + cost);
                    }
                    double h = heuristic(g, e.toId, goalId, mode);
                    fScore.put(e.toId, tentativeG + h);
                    open.add(new int[]{e.toId});
                }
            }
        }

        if (!parent.containsKey(goalId) && startId != goalId) return null;

        List<Integer> seq = new ArrayList<>();
        int cur = goalId;
        seq.add(cur);
        while (cur != startId) {
            Integer p = parent.get(cur);
            if (p == null) break;
            cur = p;
            seq.add(cur);
        }
        Collections.reverse(seq);

        double distance = 0.0;
        double minutes = 0.0;
        LocalTime t = depart;
        for (int i = 0; i < seq.size() - 1; i++) {
            int a = seq.get(i);
            int b = seq.get(i + 1);
            Edge edge = g.getEdge(a, b);
            if (edge == null) continue;
            distance += edge.distanceMeters;
            double mult = Graph.trafficMultiplierFor(t, edge);
            double dt = edge.baseMinutes * mult;
            minutes += dt;
            t = t.plusMinutes((long) Math.floor(dt));
        }
        return new Route(seq, distance, minutes);
    }

    private static double heuristic(Graph g, int fromId, int toId, WeightMode mode) {
        Node a = g.idToNode.get(fromId);
        Node b = g.idToNode.get(toId);
        double meters = Graph.euclidean(a, b);
        if (mode == WeightMode.DISTANCE) return meters;
        double avgMetersPerMinute = 80.0;
        return meters / avgMetersPerMinute;
    }
}


