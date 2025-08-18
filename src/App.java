import algorithm.*;
import model.Graph;
import route.Route;
import route.WeightMode;
import util.GraphBuilder;
import util.TimeUtil;

import java.time.LocalTime;
import java.util.*;

public class App {
    static void printLandmarks(Graph g) {
        System.out.println("Landmarks (ID: Name):");
        List<Integer> ids = new ArrayList<>(g.idToNode.keySet());
        Collections.sort(ids);
        for (int id : ids) {
            System.out.printf("  %d: %s%n", id, g.idToNode.get(id).name);
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
        int[] supply = {30, 40, 20};
        int[] demand = {20, 30, 25, 15};
        int[][] cost = {
                {8, 6, 10, 9},
                {9, 7, 4, 2},
                {3, 4, 2, 5}
        };

        int[][] nw = TransportationProblem.northwestCorner(supply, demand);
        int[][] vam = TransportationProblem.vogelApproximation(supply, demand, cost);

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

        List<String> cp = CriticalPath.criticalPath(dag, duration);
        System.out.println("Critical Path: " + cp);
        double total = 0.0;
        for (String t : cp) total += duration.getOrDefault(t, 0.0);
        System.out.printf("Total duration along critical path: %.1f min%n", total);
    }

    public static void main(String[] args) {
        Graph g = GraphBuilder.buildUGGraph();
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
                    LocalTime t = TimeUtil.pickTime(sc);
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
                    LocalTime t = TimeUtil.pickTime(sc);
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
                    LocalTime t = TimeUtil.pickTime(sc);
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


