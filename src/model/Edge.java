package model;
public final class Edge {
    public final int fromId;
    public final int toId;
    public final double distanceMeters;
    public final double baseMinutes;
    public final boolean bidirectional;
    public final double peakMultiplier;
    public final double offPeakMultiplier;

    public Edge(
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


