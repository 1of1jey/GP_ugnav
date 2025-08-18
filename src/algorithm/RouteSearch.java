package algorithm;

import model.Graph;
import model.Node;
import model.Edge;
import route.Route;
import route.WeightMode;

import java.time.LocalTime;
import java.util.*;

public final class RouteSearch {
    // Returns up to k routes passing through any node whose name or tag matches keyword
    public static List<Route> routesViaKeyword(
            Graph g, int startId, int goalId, String keyword, int k, WeightMode mode, LocalTime depart
    ) {
        String kw = keyword.toLowerCase(Locale.ROOT);
        List<Integer> candidateMidpoints = new ArrayList<>();
        for (Node node : g.idToNode.values()) {
            if (node.id == startId || node.id == goalId) continue;
            if (node.name.toLowerCase(Locale.ROOT).contains(kw)) {
                candidateMidpoints.add(node.id);
                continue;
            }
            for (String tag : node.tags) {
                if (tag.toLowerCase(Locale.ROOT).contains(kw)) {
                    candidateMidpoints.add(node.id);
                    break;
                }
            }
        }
        if (kw.equals("bank") || kw.equals("banking")) {
            for (Node n : g.idToNode.values()) {
                if (n.name.toLowerCase(Locale.ROOT).contains("bank")) {
                    if (!candidateMidpoints.contains(n.id)) candidateMidpoints.add(n.id);
                }
            }
        }

        Set<String> seen = new HashSet<>();
        List<Route> routes = new ArrayList<>();
        for (int mid : candidateMidpoints) {
            Route left = AStar.shortestPath(g, startId, mid, mode, depart);
            if (left == null) continue;
            LocalTime t2 = depart.plusMinutes((long) Math.floor(left.totalMinutes));
            Route right = AStar.shortestPath(g, mid, goalId, mode, t2);
            if (right == null) continue;

            List<Integer> seq = new ArrayList<>(left.nodeSequence);
            seq.addAll(right.nodeSequence.subList(1, right.nodeSequence.size()));
            double distance = left.totalDistanceMeters + right.totalDistanceMeters;
            double minutes = left.totalMinutes + right.totalMinutes;

            String sig = seq.toString();
            if (seen.add(sig)) {
                routes.add(new Route(seq, distance, minutes));
            }
        }

        if (routes.size() < k) {
            Route direct = AStar.shortestPath(g, startId, goalId, mode, depart);
            if (direct != null && seen.add(direct.nodeSequence.toString())) {
                routes.add(direct);
            }
            for (Edge e : g.neighbors(startId)) {
                if (routes.size() >= k) break;
                int mid = e.toId;
                Route left = AStar.shortestPath(g, startId, mid, mode, depart);
                if (left == null) continue;
                LocalTime t2 = depart.plusMinutes((long) Math.floor(left.totalMinutes));
                Route right = AStar.shortestPath(g, mid, goalId, mode, t2);
                if (right == null) continue;
                List<Integer> seq = new ArrayList<>(left.nodeSequence);
                seq.addAll(right.nodeSequence.subList(1, right.nodeSequence.size()));
                double distance = left.totalDistanceMeters + right.totalDistanceMeters;
                double minutes = left.totalMinutes + right.totalMinutes;
                String sig = seq.toString();
                if (seen.add(sig)) routes.add(new Route(seq, distance, minutes));
            }
        }

        Comparator<Route> cmp = (mode == WeightMode.DISTANCE)
                ? Comparator.comparingDouble(r -> r.totalDistanceMeters)
                : Comparator.comparingDouble(r -> r.totalMinutes);
        MergeSort.sort(routes, cmp);
        if (routes.size() > k) return new ArrayList<>(routes.subList(0, k));
        return routes;
    }
}


