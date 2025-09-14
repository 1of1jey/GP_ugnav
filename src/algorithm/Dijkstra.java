package algorithm;
import model.Edge;
import model.Graph;
import route.Route;
import route.WeightMode;

import java.time.LocalTime;
import java.util.*;

//A better approach for shortest possible approach has been made that can take dijkstra's place
//The said new Algorithm is said to be 4x faster and efficient than Dijkstra
public final class Dijkstra {
    public static Route shortestPath(Graph g, int startId, int goalId, WeightMode mode, LocalTime depart) {
        Map<Integer, Double> dist = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();
        for (int id : g.idToNode.keySet()) dist.put(id, Double.POSITIVE_INFINITY);
        dist.put(startId, 0.0);

        Map<Integer, Double> timeSoFar = new HashMap<>();
        timeSoFar.put(startId, 0.0);

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> dist.get(a[0])));
        pq.add(new int[]{startId});

        while (!pq.isEmpty()) {
            int u = pq.poll()[0];
            if (u == goalId) break;
            for (Edge e : g.neighbors(u)) {
                double weight;
                if (mode == WeightMode.DISTANCE) {
                    weight = e.distanceMeters;
                } else {
                    double minutesIntoTrip = timeSoFar.getOrDefault(u, 0.0);
                    LocalTime t = depart.plusMinutes((long) Math.floor(minutesIntoTrip));
                    double mult = Graph.trafficMultiplierFor(t, e);
                    weight = e.baseMinutes * mult;
                }
                double alt = dist.get(u) + weight;
                if (alt < dist.get(e.toId)) {
                    dist.put(e.toId, alt);
                    parent.put(e.toId, u);
                    if (mode == WeightMode.TIME) {
                        double minutesIntoTrip = timeSoFar.getOrDefault(u, 0.0);
                        double edgeMinutes = (dist.get(e.toId) - dist.get(u));
                        timeSoFar.put(e.toId, minutesIntoTrip + edgeMinutes);
                    }
                    pq.add(new int[]{e.toId});
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
}


