


import java.util.ArrayList;
import java.util.List;

class VCell_ {
    public int id;
    public int pid;
    public double area;
    public List<Vertex_> VERTEX; // List to store Vertex objects
    public List<Edge_> EDGE; // List to store Edge objects
    public char inout; // Changed to char
    public double cx;
    public double cy;

    public VCell_() {
        this.id = 0;
        this.pid = 0;
        this.area = 0.0;
        this.VERTEX = new ArrayList<>();
        this.EDGE = new ArrayList<>();
        this.inout = 'i';
        this.cx = 0.0;
        this.cy = 0.0;
    }
}
