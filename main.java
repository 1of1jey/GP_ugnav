import java.util.*;
import java.time.*;

// Single-file Java app with nested classes for easy compile/run.
// Target: Java 11+

public class App {

    // ===================== Domain Model =====================
    static final class Node {
        final int id;
        final String name;
        final double x;
        final double y;
        final Set<String> tags;

        Node(int id, String name, double x, double y, Set<String> tags) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.tags = tags;
        }
    }

    static final class Edge {
        final int fromId;
        final int toId;
        final double distanceMeters;
        final double baseMinutes;
        final boolean bidirectional;

        // Simplified traffic model: peak vs off-peak multiplier
        final double peakMultiplier;
        final double offPeakMultiplier;

        Edge(
                int fromId,
                int toId,
                double distanceMeters,
                double baseMinutes,
                boolean bidirectional,
                double peakMultiplier,
                double offPeakMultiplier
        ) {
            this.fromId = fromId;
            this.toId = toId;
            this.distanceMeters = distanceMeters;
            this.baseMinutes = baseMinutes;
            this.bidirectional = bidirectional;
            this.peakMultiplier = peakMultiplier;
            this.offPeakMultiplier = offPeakMultiplier;
        }
    }

    static final class Graph {
        final Map<Integer, Node> idToNode = new HashMap<>();
        final Map<Integer, List<Edge>> adjacency = new HashMap<>();

        void addNode(Node node) {
            idToNode.put(node.id, node);
            adjacency.computeIfAbsent(node.id, k -> new ArrayList<>());
        }

        void addEdge(Edge edge) {
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

        List<Edge> neighbors(int nodeId) {
            return adjacency.getOrDefault(nodeId, Collections.emptyList());
        }

        static double euclidean(Node a, Node b) {
            double dx = a.x - b.x;
            double dy = a.y - b.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        static double trafficMultiplierFor(LocalTime time, Edge edge) {
            int hour = time.getHour();
            boolean peak = (hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 18);
            return peak ? edge.peakMultiplier : edge.offPeakMultiplier;
        }
    }

    // ===================== Routes and Utilities =====================
    static final class Route {
        final List<Integer> nodeSequence; // node IDs
        final double totalDistanceMeters;
        final double totalMinutes;

        Route(List<Integer> nodeSequence, double totalDistanceMeters, double totalMinutes) {
            this.nodeSequence = nodeSequence;
            this.totalDistanceMeters = totalDistanceMeters;
            this.totalMinutes = totalMinutes;
        }

        String pretty(Graph g) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nodeSequence.size(); i++) {
                if (i > 0) sb.append(" -> ");
                sb.append(g.idToNode.get(nodeSequence.get(i)).name);
            }
            sb.append(String.format(" | distance: %.2f km, time: %.1f min",
                    totalDistanceMeters / 1000.0, totalMinutes));
            return sb.toString();
        }
    }

    enum WeightMode {
        DISTANCE, // meters
        TIME // minutes including traffic
    }

    // Merge Sort for routes
    static final class MergeSort {
        static <T> void sort(List<T> list, Comparator<T> comparator) {
            if (list.size() < 2) return;
            List<T> aux = new ArrayList<>(list);
            mergeSort(list, aux, 0, list.size() - 1, comparator);
        }

        private static <T> void mergeSort(List<T> a, List<T> aux, int lo, int hi, Comparator<T> cmp) {
            if (lo >= hi) return;
            int mid = (lo + hi) >>> 1;
            mergeSort(a, aux, lo, mid, cmp);
            mergeSort(a, aux, mid + 1, hi, cmp);
            merge(a, aux, lo, mid, hi, cmp);
        }

        private static <T> void merge(List<T> a, List<T> aux, int lo, int mid, int hi, Comparator<T> cmp) {
            for (int i = lo; i <= hi; i++) aux.set(i, a.get(i));
            int i = lo, j = mid + 1, k = lo;
            while (i <= mid && j <= hi) {
                if (cmp.compare(aux.get(i), aux.get(j)) <= 0) {
                    a.set(k++, aux.get(i++));
                } else {
                    a.set(k++, aux.get(j++));
                }
            }
            while (i <= mid) a.set(k++, aux.get(i++));
            while (j <= hi) a.set(k++, aux.get(j++));
        }
    }

    // ===================== Shortest Path Algorithms =====================
    static final class Dijkstra {
        static Route shortestPath(Graph g, int startId, int goalId, WeightMode mode, LocalTime depart) {
            Map<Integer, Double> dist = new HashMap<>();
            Map<Integer, Integer> parent = new HashMap<>();
            for (int id : g.idToNode.keySet()) dist.put(id, Double.POSITIVE_INFINITY);
            dist.put(startId, 0.0);

            // Track time along the path for TIME mode
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
                        // arrival time at start of edge
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
                Edge edge = findEdge(g, a, b);
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

    static final class AStar {
        static Route shortestPath(Graph g, int startId, int goalId, WeightMode mode, LocalTime depart) {
            Map<Integer, Double> gScore = new HashMap<>();
            Map<Integer, Double> fScore = new HashMap<>();
            Map<Integer, Integer> parent = new HashMap<>();

            for (int id : g.idToNode.keySet()) {
                gScore.put(id, Double.POSITIVE_INFINITY);
                fScore.put(id, Double.POSITIVE_INFINITY);
            }
            gScore.put(startId, 0.0);
            fScore.put(startId, heuristic(g, startId, goalId, mode));

            // Track time along the path for TIME mode
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
                Edge edge = findEdge(g, a, b);
                if (edge == null) continue;
                distance += edge.distanceMeters;
                double mult = Graph.trafficMultiplierFor(t, edge);
                double dt = edge.baseMinutes * mult;
                minutes += dt;
                t = t.plusMinutes((long) Math.floor(dt));
            }
            return new Route(seq, distance, minutes);
        }

        // Heuristic: straight-line distance; if TIME, estimate by average campus walking speed
        private static double heuristic(Graph g, int fromId, int toId, WeightMode mode) {
            Node a = g.idToNode.get(fromId);
            Node b = g.idToNode.get(toId);
            double meters = Graph.euclidean(a, b);
            if (mode == WeightMode.DISTANCE) return meters;
            double avgMetersPerMinute = 80.0; // ~4.8 km/h walking
            return meters / avgMetersPerMinute;
        }
    }

    static Edge findEdge(Graph g, int a, int b) {
        for (Edge e : g.neighbors(a)) {
            if (e.toId == b) return e;
        }
        return null;
    }

    // ===================== All-Pairs (Floyd–Warshall) =====================
    static final class FloydWarshall {
        static double[][] allPairsShortestDistance(Graph g) {
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

    // ===================== Landmark-based Route Generation =====================
    static final class RouteSearch {
        // Returns up to k routes passing through any node whose name or tag matches keyword
        static List<Route> routesViaKeyword(
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
            // For "bank" keyword, also consider known banking hubs
            if (kw.equals("bank") || kw.equals("banking")) {
                // Try to add "Banking Square" if exists
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

                // Join sequences; avoid duplicating mid
                List<Integer> seq = new ArrayList<>(left.nodeSequence);
                seq.addAll(right.nodeSequence.subList(1, right.nodeSequence.size()));
                double distance = left.totalDistanceMeters + right.totalDistanceMeters;
                double minutes = left.totalMinutes + right.totalMinutes;

                String sig = seq.toString();
                if (seen.add(sig)) {
                    routes.add(new Route(seq, distance, minutes));
                }
            }

            // Ensure at least 3 options: add direct best if needed by perturbing via neighbors
            if (routes.size() < k) {
                Route direct = AStar.shortestPath(g, startId, goalId, mode, depart);
                if (direct != null && seen.add(direct.nodeSequence.toString())) {
                    routes.add(direct);
                }
                // Try via neighbors of start
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

    // ===================== Transportation Problem (NW Corner, VAM) =====================
    static final class Transportation {
        // Returns allocation matrix using Northwest Corner Method
        static int[][] northwestCorner(int[] supply, int[] demand) {
            int m = supply.length, n = demand.length;
            int[][] alloc = new int[m][n];
            int i = 0, j = 0;
            int[] s = Arrays.copyOf(supply, m);
            int[] d = Arrays.copyOf(demand, n);
            while (i < m && j < n) {
                int x = Math.min(s[i], d[j]);
                alloc[i][j] = x;
                s[i] -= x;
                d[j] -= x;
                if (s[i] == 0) i++;
                if (d[j] == 0) j++;
            }
            return alloc;
        }

        // Vogel Approximation Method: returns allocation matrix for cost matrix
        static int[][] vogelApproximation(int[] supply, int[] demand, int[][] cost) {
            int m = supply.length, n = demand.length;
            int[] s = Arrays.copyOf(supply, m);
            int[] d = Arrays.copyOf(demand, n);
            int[][] alloc = new int[m][n];
            boolean[] rowDone = new boolean[m];
            boolean[] colDone = new boolean[n];
            int remainingRows = m, remainingCols = n;

            while (remainingRows > 0 && remainingCols > 0) {
                int bestRow = -1, bestRowPen = -1;
                int bestCol = -1, bestColPen = -1;

                // Row penalties
                for (int i = 0; i < m; i++) {
                    if (rowDone[i] || s[i] == 0) continue;
                    int min1 = Integer.MAX_VALUE, min2 = Integer.MAX_VALUE;
                    for (int j = 0; j < n; j++) {
                        if (colDone[j] || d[j] == 0) continue;
                        int c = cost[i][j];
                        if (c < min1) {
                            min2 = min1;
                            min1 = c;
                        } else if (c < min2) {
                            min2 = c;
                        }
                    }
                    int pen = (min2 == Integer.MAX_VALUE) ? min1 : (min2 - min1);
                    if (pen > bestRowPen) {
                        bestRowPen = pen;
                        bestRow = i;
                    }
                }

                // Column penalties
                for (int j = 0; j < n; j++) {
                    if (colDone[j] || d[j] == 0) continue;
                    int min1 = Integer.MAX_VALUE, min2 = Integer.MAX_VALUE;
                    for (int i = 0; i < m; i++) {
                        if (rowDone[i] || s[i] == 0) continue;
                        int c = cost[i][j];
                        if (c < min1) {
                            min2 = min1;
                            min1 = c;
                        } else if (c < min2) {
                            min2 = c;
                        }
                    }
                    int pen = (min2 == Integer.MAX_VALUE) ? min1 : (min2 - min1);
                    if (pen > bestColPen) {
                        bestColPen = pen;
                        bestCol = j;
                    }
                }

                boolean pickRow = bestRowPen >= bestColPen;
                if (bestRow == -1 && bestCol == -1) break;

                if (pickRow) {
                    int i = bestRow;
                    int bestJ = -1, bestC = Integer.MAX_VALUE;
                    for (int j = 0; j < n; j++) {
                        if (colDone[j] || d[j] == 0) continue;
                        if (cost[i][j] < bestC) {
                            bestC = cost[i][j];
                            bestJ = j;
                        }
                    }
                    int x = Math.min(s[i], d[bestJ]);
                    alloc[i][bestJ] += x;
                    s[i] -= x;
                    d[bestJ] -= x;
                    if (s[i] == 0) {
                        rowDone[i] = true;
                        remainingRows--;
                    }
                    if (d[bestJ] == 0) {
                        colDone[bestJ] = true;
                        remainingCols--;
                    }
                } else {
                    int j = bestCol;
                    int bestI = -1, bestC = Integer.MAX_VALUE;
                    for (int i = 0; i < m; i++) {
                        if (rowDone[i] || s[i] == 0) continue;
                        if (cost[i][j] < bestC) {
                            bestC = cost[i][j];
                            bestI = i;
                        }
                    }
                    int x = Math.min(s[bestI], d[j]);
                    alloc[bestI][j] += x;
                    s[bestI] -= x;
                    d[j] -= x;
                    if (s[bestI] == 0) {
                        rowDone[bestI] = true;
                        remainingRows--;
                    }
                    if (d[j] == 0) {
                        colDone[j] = true;
                        remainingCols--;
                    }
                }
            }
            return alloc;
        }
    }

    // ===================== Critical Path Method (CPM) =====================
    static final class CPMScheduler {
        // DAG represented as adjacency map with duration minutes on each task (node)
        // Returns critical path node sequence
        static List<String> criticalPath(Map<String, List<String>> dag, Map<String, Double> duration) {
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

    // ===================== Sample UG Campus Graph =====================
    static Graph buildUGGraph() {
        Graph g = new Graph();

        // Coordinates are rough and only support A* heuristic
        addNode(g, 1, "Main Gate", 0, 0, tags("gate"));
        addNode(g, 2, "Balme Library", 2, 2, tags("library", "landmark"));
        addNode(g, 3, "Night Market", 4, 1, tags("market", "food"));
        addNode(g, 4, "Commonwealth Hall", 1, -1, tags("hall"));
        addNode(g, 5, "Legon Hall", 3, 0, tags("hall"));
        addNode(g, 6, "Akuafo Hall", 4, 0, tags("hall"));
        addNode(g, 7, "UGCS (Computing)", 3, 3, tags("computing", "lab"));
        addNode(g, 8, "Business School", 4, 3, tags("business", "bank"));
        addNode(g, 9, "Banking Square", 5, 3, tags("bank"));
        addNode(g, 10, "JQB", 1, 1, tags("lecture", "block"));
        addNode(g, 11, "Central Cafeteria", 5, 1, tags("food", "cafeteria"));
        addNode(g, 12, "Law School", 6, 2, tags("law", "faculty"));
        addNode(g, 13, "UG Hospital", 7, 2, tags("hospital", "clinic"));
        addNode(g, 14, "Gym", 7, 0, tags("gym", "sports"));
        addNode(g, 15, "Stadium", 7, 1, tags("stadium", "sports"));
        addNode(g, 16, "N Block (Engineering)", 5, 4, tags("engineering", "block"));
        addNode(g, 17, "Volta Hall", 3, 1, tags("hall"));
        addNode(g, 18, "University Basic School", 2, -1, tags("school"));
        addNode(g, 19, "Noguchi", 8, 3, tags("research", "lab"));
        addNode(g, 20, "UPSA Road Junction", -1, 0, tags("junction"));

        // Helper for walking time: distance / 80 m per minute
        java.util.function.Function<Double, Double> minutes = meters -> meters / 80.0;

        // Edges: distance in meters (rough), peak multiplier 1.3, off-peak 1.0
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

    static void addNode(Graph g, int id, String name, double x, double y, Set<String> tags) {
        g.addNode(new Node(id, name, x, y, tags));
    }

    static void addEdge(Graph g, int from, int to, double meters, double baseMinutes) {
        g.addEdge(new Edge(from, to, meters, baseMinutes, true, 1.3, 1.0));
    }

    static Set<String> tags(String... t) {
        return new HashSet<>(Arrays.asList(t));
    }

    // ===================== CLI Helpers =====================
    static void printLandmarks(Graph g) {
        System.out.println("Landmarks (ID: Name):");
        List<Integer> ids = new ArrayList<>(g.idToNode.keySet());
        Collections.sort(ids);
        for (int id : ids) {
            Node n = g.idToNode.get(id);
            System.out.printf("  %d: %s%n", id, n.name);
        }
    }

    static int pickNode(Scanner sc, Graph g, String prompt) {
        while (true) {
            System.out.print(prompt + " (enter ID): ");
            String line = sc.nextLine().trim();
            try {
                int id = Integer.parseInt(line);
                if (g.idToNode.containsKey(id)) return id;
            } catch (NumberFormatException ignored) {}
            System.out.println("Invalid ID. Try again.");
        }
    }

    static LocalTime pickTime(Scanner sc) {
        System.out.print("Enter departure time HH:MM (24h), or blank for now: ");
        String s = sc.nextLine().trim();
        if (s.isEmpty()) return LocalTime.now();
        try {
            return LocalTime.parse(s);
        } catch (Exception e) {
            System.out.println("Invalid time. Using current time.");
            return LocalTime.now();
        }
    }

    // ===================== Menu and Demos =====================
    static void menu() {
        System.out.println("\nUG Campus Router");
        System.out.println("1) Shortest distance (Dijkstra)");
        System.out.println("2) Fastest arrival (A* with traffic)");
        System.out.println("3) Routes via landmark keyword (3 options)");
        System.out.println("4) All-pairs shortest distances (Floyd–Warshall summary)");
        System.out.println("5) Traffic assignment demo (Northwest Corner & VAM)");
        System.out.println("6) Critical Path demo");
        System.out.println("0) Exit\n");
    }

    static void runNorthwestAndVAMDemo() {
        int[] supply = {30, 40, 20}; // e.g., flows from 3 origin gates
        int[] demand = {20, 30, 25, 15}; // e.g., destination zones or time slots
        int[][] cost = {
                {8, 6, 10, 9},
                {9, 7, 4, 2},
                {3, 4, 2, 5}
        };

        int[][] nw = Transportation.northwestCorner(supply, demand);
        int[][] vam = Transportation.vogelApproximation(supply, demand, cost);

        System.out.println("Northwest Corner Allocation:");
        printMatrix(nw);
        System.out.println("Vogel Approximation Allocation:");
        printMatrix(vam);
    }

    static void printMatrix(int[][] m) {
        for (int[] row : m) {
            for (int v : row) System.out.printf("%4d", v);
            System.out.println();
        }
    }

    static void runCriticalPathDemo() {
        // Simple trip with tasks: WalkToStop -> WaitShuttle -> ShuttleRide -> FinalWalk
        Map<String, List<String>> dag = new HashMap<>();
        dag.put("WalkToStop", Arrays.asList("WaitShuttle"));
        dag.put("WaitShuttle", Arrays.asList("ShuttleRide"));
        dag.put("ShuttleRide", Arrays.asList("FinalWalk"));
        dag.put("FinalWalk", Collections.emptyList());

        Map<String, Double> duration = new HashMap<>();
        duration.put("WalkToStop", 8.0);
        duration.put("WaitShuttle", 6.0);
        duration.put("ShuttleRide", 12.0);
        duration.put("FinalWalk", 5.0);

        List<String> cp = CPMScheduler.criticalPath(dag, duration);
        System.out.println("Critical Path: " + cp);
        double total = 0.0;
        for (String t : cp) total += duration.getOrDefault(t, 0.0);
        System.out.printf("Total duration along critical path: %.1f min%n", total);
    }

    // ===================== Main =====================
    public static void main(String[] args) {
        Graph g = buildUGGraph();
        Scanner sc = new Scanner(System.in);
        System.out.println("Welcome to the UG Campus Router.");
        printLandmarks(g);

        while (true) {
            menu();
            System.out.print("Choose option: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": {
                    int start = pickNode(sc, g, "Start");
                    int goal = pickNode(sc, g, "Goal");
                    LocalTime t = pickTime(sc);
                    Route r = Dijkstra.shortestPath(g, start, goal, WeightMode.DISTANCE, t);
                    if (r == null) {
                        System.out.println("No route found.");
                    } else {
                        System.out.println("Shortest distance route:");
                        System.out.println(r.pretty(g));
                    }
                    break;
                }
                case "2": {
                    int start = pickNode(sc, g, "Start");
                    int goal = pickNode(sc, g, "Goal");
                    LocalTime t = pickTime(sc);
                    Route r = AStar.shortestPath(g, start, goal, WeightMode.TIME, t);
                    if (r == null) {
                        System.out.println("No route found.");
                    } else {
                        System.out.println("Fastest arrival route (with traffic):");
                        System.out.println(r.pretty(g));
                    }
                    break;
                }
                case "3": {
                    int start = pickNode(sc, g, "Start");
                    int goal = pickNode(sc, g, "Goal");
                    System.out.print("Enter landmark keyword (e.g., bank, food, library): ");
                    String kw = sc.nextLine().trim();
                    LocalTime t = pickTime(sc);
                    List<Route> routes = RouteSearch.routesViaKeyword(g, start, goal, kw, 3, WeightMode.TIME, t);
                    if (routes.isEmpty()) {
                        System.out.println("No routes found for keyword: " + kw);
                    } else {
                        System.out.println("Top routes via \"" + kw + "\":");
                        for (int i = 0; i < routes.size(); i++) {
                            System.out.printf("%d) %s%n", i + 1, routes.get(i).pretty(g));
                        }
                    }
                    break;
                }
                case "4": {
                    double[][] d = FloydWarshall.allPairsShortestDistance(g);
                    System.out.println("All-pairs distances ready. Sample (km) for first 5x5:");
                    int n = Math.min(5, d.length);
                    for (int i = 0; i < n; i++) {
                        for (int j = 0; j < n; j++) {
                            double km = Double.isInfinite(d[i][j]) ? -1.0 : (d[i][j] / 1000.0);
                            System.out.printf("%6.2f", km);
                        }
                        System.out.println();
                    }
                    break;
                }
                case "5": {
                    runNorthwestAndVAMDemo();
                    break;
                }
                case "6": {
                    runCriticalPathDemo();
                    break;
                }
                case "0":
                    System.out.println("Goodbye.");
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
}