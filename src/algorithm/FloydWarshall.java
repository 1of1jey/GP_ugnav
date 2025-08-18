package algorithm;

import model.Edge;
import model.Graph;

import java.util.*;

public final class FloydWarshall {
    public static double[][] allPairsShortestDistance(Graph g) {
        int n = g.idToNode.size();
        List<Integer> ids = new ArrayList<>(g.idToNode.keySet());
        Collections.sort(ids);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) idx.put(ids.get(i), i);

        double[][] dist = new double[n][n];
        for (int i = 0; i < n; i++) Arrays.fill(dist[i], Double.POSITIVE_INFINITY);
        for (int i = 0; i < n; i++) dist[i][i] = 0.0;

        for (int u : ids) {
            int iu = idx.get(u);
            for (Edge e : g.neighbors(u)) {
                int iv = idx.get(e.toId);
                dist[iu][iv] = Math.min(dist[iu][iv], e.distanceMeters);
            }
        }

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                    }
                }
            }
        }
        return dist;
    }
}


