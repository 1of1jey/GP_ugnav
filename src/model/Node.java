package model;
import java.util.Set;

public final class Node {
    public final int id;
    public final String name;
    public final double x;
    public final double y;
    public final Set<String> tags;

    public Node(int id, String name, double x, double y, Set<String> tags) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.tags = tags;
    }
}


