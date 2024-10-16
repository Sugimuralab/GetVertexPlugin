


import java.util.ArrayList;
import java.util.List;

class Vertex_ {
    public int id;
    public int pid;
    public double x;
    public double y;
    public List<Integer> Cells; // List to store cell IDs
    public char ctype;
    public char inout;
    public int nj_num;
    public List<Integer> nj; // List to store neighboring vertices
    public List<Integer> ne; // List to store neighboring edges

    public Vertex_() {
        this.id = 0;
        this.pid = 0;
        this.x = 0.0;
        this.y = 0.0;
        this.Cells = new ArrayList<>();
        this.ctype = ' '; // Changed to char
        this.inout = ' '; // Changed to char
        this.nj_num = 0;
        this.nj = new ArrayList<>();
        this.ne = new ArrayList<>();
    }
}
