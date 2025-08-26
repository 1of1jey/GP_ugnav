package route;
import model.Graph;
import java.util.List;
public final class Route {
    public final List<Integer> nodeSequence;
    public final double totalDistanceMeters;
    public final double totalMinutes;

    public Route(List<Integer> nodeSequence, double totalDistanceMeters, double totalMinutes) {
        this.nodeSequence = nodeSequence;
        this.totalDistanceMeters = totalDistanceMeters;
        this.totalMinutes = totalMinutes;
    }

    public String pretty(Graph g) {
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


