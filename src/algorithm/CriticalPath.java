package algorithm;

import java.util.*;

public final class CriticalPath {
    // DAG represented as adjacency map with duration minutes on each task (node)
    // Returns critical path node sequence
    public static List<String> criticalPath(Map<String, List<String>> dag, Map<String, Double> duration) {
        List<String> topo = topologicalSort(dag);
        Map<String, Double> earliestFinish = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        for (String v : topo) {
            double best = 0.0;
            String bestPred = null;
            for (String u : reverseNeighbors(dag, v)) {
                double val = earliestFinish.getOrDefault(u, 0.0);
                if (val > best) {
                    best = val;
                    bestPred = u;
                }
            }
            earliestFinish.put(v, best + duration.getOrDefault(v, 0.0));
            if (bestPred != null) prev.put(v, bestPred);
        }
        String end = null;
        double bestEF = -1;
        for (String v : topo) {
            double ef = earliestFinish.getOrDefault(v, 0.0);
            if (ef > bestEF) {
                bestEF = ef;
                end = v;
            }
        }
        List<String> path = new ArrayList<>();
        while (end != null) {
            path.add(end);
            end = prev.get(end);
        }
        Collections.reverse(path);
        return path;
    }

    private static List<String> topologicalSort(Map<String, List<String>> dag) {
        Map<String, Integer> indeg = new HashMap<>();
        for (String v : dag.keySet()) indeg.putIfAbsent(v, 0);
        for (String u : dag.keySet()) {
            for (String v : dag.get(u)) {
                indeg.put(v, indeg.getOrDefault(v, 0) + 1);
            }
        }
        Deque<String> dq = new ArrayDeque<>();
        for (Map.Entry<String, Integer> e : indeg.entrySet()) {
            if (e.getValue() == 0) dq.add(e.getKey());
        }
        List<String> order = new ArrayList<>();
        while (!dq.isEmpty()) {
            String u = dq.removeFirst();
            order.add(u);
            for (String v : dag.getOrDefault(u, Collections.emptyList())) {
                indeg.put(v, indeg.get(v) - 1);
                if (indeg.get(v) == 0) dq.add(v);
            }
        }
        return order;
    }

    private static List<String> reverseNeighbors(Map<String, List<String>> dag, String v) {
        List<String> res = new ArrayList<>();
        for (String u : dag.keySet()) {
            for (String w : dag.get(u)) if (w.equals(v)) res.add(u);
        }
        return res;
    }
}


