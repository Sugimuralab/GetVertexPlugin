


import java.util.ArrayList;
import java.util.List;

class Edge_ {
    public int id;
    public int pid;
    public int[] vertex_id; // Array to store two vertex IDs
    public List<int[]> X; // List to store two points as int[]
    public char inout; // Changed to char
    public List<int[]> line_pts; // List to store points along the edge
    public int[] ncell; // Array to store two cell IDs
    public int dist_along;
    public double sdist;
    public double angle;
    public double signal_conc;

    public Edge_() {
        this.id = 0;
        this.pid = 0;
        this.vertex_id = new int[2];
        this.X = new ArrayList<>();
        this.X.add(new int[] { 0, 0 }); // Initialize with two points
        this.X.add(new int[] { 0, 0 });
        this.inout = 'i';
        this.line_pts = new ArrayList<>();
        this.ncell = new int[2];
        this.dist_along = 0;
        this.sdist = 0.0;
        this.angle = 0.0;
        this.signal_conc = 0.0;
    }
}