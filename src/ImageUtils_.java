

// ImageUtils.java
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.FloatProcessor;
import ij.process.ColorProcessor;
import ij.gui.Roi;

import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.Point;
import java.awt.Color;
import java.util.*;



public class ImageUtils_ {

    // Constants
    private static final byte WHITE = -1; // Equivalent to -1
    private static final byte GRAY = -128;  // Equivalent to -128

    /**
     * Utility class to hold a pair of objects.
     *
     * @param <A> Type of the first object.
     * @param <B> Type of the second object.
     */
    public static class Pair<A, B> {
        public final A first;
        public final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
    }

    /**
     * Triple class to hold three related objects.
     */
    public static class Triple<A, B, C> {
        public final A first;
        public final B second;
        public final C third;

        public Triple(A first, B second, C third){
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }

    /**
     * Performs flood fill on tpimg starting from (x, y), labeling the region with cell_num.
     * Also computes the bounding rectangle of the filled region.
     *
     * @param tpimg     The ImageProcessor to perform flood fill on.
     * @param x         Starting x coordinate.
     * @param y         Starting y coordinate.
     * @param cell_num  The label number to assign to the filled region.
     * @param rect      A Rectangle object to store the bounding rectangle of the filled region.
     */
    public static void floodFillIP(ImageProcessor tpimg, int x, int y, int cell_num, Rectangle rect) {
        Stack<Point> stack = new Stack<>();
        stack.push(new Point(x, y));

        int color = tpimg.getPixel(x, y);
        tpimg.set(x, y, cell_num);

        int min_x = x, max_x = x;
        int min_y = y, max_y = y;

        int width = tpimg.getWidth();
        int height = tpimg.getHeight();

        while (!stack.isEmpty()) {
            Point p = stack.pop();
            int cx = p.x;
            int cy = p.y;

            // Define 4-connected neighbors
            Point[] neighbors = {
                new Point(cx - 1, cy),
                new Point(cx + 1, cy),
                new Point(cx, cy - 1),
                new Point(cx, cy + 1)
            };

            for (Point n : neighbors) {
                int nx = n.x;
                int ny = n.y;
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    int pixel = tpimg.getPixel(nx, ny);
                    if (pixel == color) {
                        tpimg.set(nx, ny, cell_num);
                        stack.push(new Point(nx, ny));

                        // Update bounding rectangle
                        min_x = Math.min(min_x, nx);
                        max_x = Math.max(max_x, nx);
                        min_y = Math.min(min_y, ny);
                        max_y = Math.max(max_y, ny);
                    }
                }
            }
        }

        // Set the bounds of the rectangle
        rect.setBounds(min_x, min_y, max_x - min_x + 1, max_y - min_y + 1);
    }

    /**
     * Crops the image to the bounding box of non-zero pixels.
     *
     * @param src  Source ImagePlus object.
     * @param CROP Whether to perform cropping or just clone the image.
     * @return A Pair containing the cropped ImagePlus and the minimum Point.
     */
    public static Pair<ImagePlus, Point> utlCropImage(ImagePlus src, boolean CROP) {
        if (CROP) {
            int minx = src.getWidth();
            int miny = src.getHeight();
            int maxx = 0;
            int maxy = 0;
            ImageProcessor ip = src.getProcessor();

            // Iterate through each pixel to find the bounding box
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int pixel = ip.getPixel(x, y);
                    if (pixel != 0) {
                        if (x < minx) minx = x;
                        if (x > maxx) maxx = x;
                        if (y < miny) miny = y;
                        if (y > maxy) maxy = y;
                    }
                }
            }

            // Check if any non-zero pixel was found
            if (minx == src.getWidth() || miny == src.getHeight()) {
                throw new IllegalArgumentException("No non-zero pixels found in the image.");
            }

            // Define the width and height of the cropped region
            int width = maxx - minx + 1;  // +1 to include the max pixel
            int height = maxy - miny + 1;

            // Set the Region of Interest (ROI) for cropping
            src.setRoi(new Roi(minx, miny, width, height));

            // Perform the crop operation
            ImageProcessor cropped_ip = src.crop().getProcessor();

            // Create a new ImagePlus object for the cropped image
            ImagePlus cropped = new ImagePlus("Cropped", cropped_ip);

            return new Pair<>(cropped, new Point(minx, miny));
        } else {
            // Clone the original image without cropping
            ImagePlus duplicated = src.duplicate();
            return new Pair<>(duplicated, new Point(0, 0));
        }
    }

    /**
     * Checks for unexpected four-block patterns in an ImageProcessor.
     *
     * @param ip   The ImageProcessor containing the image data.
     * @param num  Frame number (for reporting).
     * @param cpt  Coordinate offset (optional, default is (0, 0)).
     */
    public static void utlCheckFourBlock(ImageProcessor ip, int num, Point cpt) {
        int bnum = 0;
        int width = ip.getWidth();
        int height = ip.getHeight();

        for (int y = 0; y < height - 1; y++) {
            for (int x = 0; x < width - 1; x++) {
                if ((ip.getPixel(x, y) & 0xFF) == (WHITE & 0xFF)) { // Convert byte to unsigned
                    if ((ip.getPixel(x + 1, y) & 0xFF) == (WHITE & 0xFF) &&
                        (ip.getPixel(x, y + 1) & 0xFF) == (WHITE & 0xFF) &&
                        (ip.getPixel(x + 1, y + 1) & 0xFF) == (WHITE & 0xFF)) {
                        IJ.error(String.format("!! Unexpected Four-Block: frame %d, (%d %d)", num + 1, x + cpt.x, y + cpt.y));
                        bnum++;
                    }
                }
            }
        }

        if (bnum > 0) {
            IJ.error("FOUR BLOCK PIXELS APPEAR, Modify Image");
        }
    }

    /**
     * Processes the boundaries of a binary image.
     *
     * @param timg The binary ImagePlus to process.
     * @param cpt  Coordinate offset (optional, default is (0,0)).
     * @return The processed ImageProcessor.
     */
    public static ImageProcessor utlBoundaryProcessing(ImagePlus timg, Point cpt) {
        // Clone the image processor for manipulation
        ImageProcessor ip = timg.getProcessor();
        int width = ip.getWidth();
        int height = ip.getHeight();

        // Define 8-connected neighbor offsets
        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dy = {0, -1, -1, -1, 0, 1, 1, 1};

        // Initialize ctypes arrays
        char[] ctypes = new char[width * height];
        char[] ctypesP = new char[width * height];

        List<List<int[]>> all_edges = new ArrayList<>(); // List to store all contours
        // tmp_edges is unused in the original code
        List<int[]> tPoints = new ArrayList<>();
        List<int[]> jPoints = new ArrayList<>();

        // Delete all the image periphery by setting border pixels to 0
        for (int y = 0; y < height; y++) {
            ip.putPixel(0, y, 0);
            ip.putPixel(width - 1, y, 0);
        }
        for (int x = 0; x < width; x++) {
            ip.putPixel(x, 0, 0);
            ip.putPixel(x, height - 1, 0);
        }

        // First step: Set ctypesP
        CVUtil_.setCtypes(new ImagePlus("", ip), ctypesP); // Assuming CVUtil_.setCtypes accepts ImagePlus

        // Perform flood fill from (0,0) with value 1
        Rectangle rect = new Rectangle();
        floodFillIP(ip, 0, 0, 1, rect);

        // Iterate through each pixel to find boundary points
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if ((ip.getPixel(x, y) & 0xFF) == (WHITE & 0xFF)) { // Unsigned comparison
                    boolean bflag = false;
                    for (int k = 0; k < 8; k++) {
                        int nx = x + dx[k];
                        int ny = y + dy[k];
                        if ((ip.getPixel(nx, ny) & 0xFF) == 1) { // Assuming '1' is background
                            bflag = true;
                            break;
                        }
                    }
                    if (bflag) {
                        char ctype = ctypesP[y * width + x];
                        if (ctype == 't' || ctype == 'e') {
                            tPoints.add(new int[]{x, y});
                        } else if (ctype == 'j') {
                            jPoints.add(new int[]{x, y});
                        } else if (ctype == 'f') {
                            tPoints.add(new int[]{x, y});
                            tPoints.add(new int[]{x, y - 1});
                            tPoints.add(new int[]{x, y + 1});
                            tPoints.add(new int[]{x - 1, y});
                            tPoints.add(new int[]{x + 1, y});
                        }
                    }
                }
            }
        }

        // Remove tPoints from the image by setting them to 0
        for (int[] pt : tPoints) {
            ip.putPixel(pt[0], pt[1], 0);
        }
        tPoints.clear();

        // Process jPoints: Remove close pairs
        for (int i = 0; i < jPoints.size(); i++) {
            for (int j = i + 1; j < jPoints.size(); j++) {
                if (Math.abs(jPoints.get(i)[0] - jPoints.get(j)[0]) <= 1 &&
                    Math.abs(jPoints.get(i)[1] - jPoints.get(j)[1]) <= 1) {
                    ip.putPixel(jPoints.get(i)[0], jPoints.get(i)[1], 0);
                    ip.putPixel(jPoints.get(j)[0], jPoints.get(j)[1], 0);
                    tPoints.add(jPoints.get(i));
                    tPoints.add(jPoints.get(j));
                }
            }
        }

        // Flood fill background again with 0
        floodFillIP(ip, 0, 0, 0, rect);

        // Second step: Set ctypes
        CVUtil_.setCtypes(new ImagePlus("", ip), ctypes);

        // Iterate through the image to modify pixels based on ctypes
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (ctypes[y * width + x] == 'd') {
                    ip.putPixel(x, y, 0);
                    ctypes[y * width + x] = 'i';
                }
            }
        }

        // Trace contours using CVUtil_.trace (assuming it returns List<List<int[]>>)
        List<List<int[]>> all_edges_traced = CVUtil_.trace(new ImagePlus("", ip), ctypes, cpt);

        // Flood fill background with 1 again
        floodFillIP(ip, 0, 0, 1, rect);

        // Set boundary pixels to 128 (GRAY)
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if ((ip.getPixel(x, y) & 0xFF) == (WHITE & 0xFF)) { // Unsigned comparison
                    boolean bflag = false;
                    for (int k = 0; k < 8; k++) {
                        int nx = x + dx[k];
                        int ny = y + dy[k];
                        if ((ip.getPixel(nx, ny) & 0xFF) == 0) { // Background
                            bflag = true;
                            break;
                        }
                    }
                    if (bflag) {
                        ip.putPixel(x, y, GRAY & 0xFF);
                    }
                }
            }
        }

        // Remove certain edges from all_edges_traced
        List<List<int[]>> filtered_edges = new ArrayList<>();
        for (List<int[]> contour : all_edges_traced) {
            if (contour.isEmpty()) continue;
            int[] firstPt = contour.get(0);
            int[] lastPt = contour.get(contour.size() - 1);
            if (!((ip.getPixel(firstPt[0], firstPt[1]) & 0xFF) == (WHITE & 0xFF) &&
                  (ip.getPixel(lastPt[0], lastPt[1]) & 0xFF) == (WHITE & 0xFF))) {
                filtered_edges.add(contour);
            }
        }

        // Clear the image by setting all pixels to 0
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                ip.putPixel(x, y, 0);
            }
        }

        // Redraw all_edges onto the image by setting pixels to 255
        for (List<int[]> contour : filtered_edges) {
            for (int[] pt : contour) {
                ip.putPixel(pt[0], pt[1], WHITE & 0xFF);
            }
        }

        return ip;
    }

    /**
     * Assigns unique Cell IDs to connected regions in a binary image.
     *
     * @param ip      The ImageProcessor of the binary image where non-zero pixels represent cell membranes.
     * @param smallC  Threshold for the minimum allowable cell area.
     * @return A Pair containing the total number of cells detected and the CellID array.
     */
    public static Pair<Integer, int[]> utlSet_CellID(ImageProcessor ip, int smallC) {
        int width = ip.getWidth();
        int height = ip.getHeight();

        // Initialize tpimg as a 1D array filled with 0
        int[] tpimg = new int[width * height];

        // Set tpimg[id] = 1 where ip != 0
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int id = y * width + x;
                tpimg[id] = (ip.getPixel(x, y) != 0) ? 1 : 0;
            }
        }

        int cell_num = 2;  // Starting label
        int[] CellID = new int[width * height];  // Initialize CellID array

        // Flood Fill function
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int id = y * width + x;
                if (tpimg[id] == 0) {
                    Rectangle rect = new Rectangle();
                    floodFill(tpimg, x, y, cell_num, width, height, rect);

                    if (cell_num > 2) {
                        // Calculate area by counting the number of pixels labeled with cell_num
                        int area = 0;
                        for (int ry = rect.y; ry < rect.y + rect.height; ry++) {
                            for (int rx = rect.x; rx < rect.x + rect.width; rx++) {
                                int rid = ry * width + rx;
                                if (tpimg[rid] == cell_num) {
                                    area++;
                                }
                            }
                        }

                        if (area <= smallC) {
                            // Raise an error if the area is smaller than or equal to smallC
                            String error_message = String.format("!!! Area Smaller than %d around (%d, %d)\nExit!", smallC, x, y);
                            IJ.error(error_message);
                            System.exit(1);
                        }
                    }

                    cell_num++;  // Increment cell number after labeling
                }
            }
        }

        // Assign CellID by flattening tpimg and subtracting 1
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int id = y * width + x;
                CellID[id] = tpimg[id] - 1;
            }
        }

        int total_cells = cell_num - 3;  // Adjusting for initial offset

        return new Pair<>(total_cells, CellID);
    }

    /**
     * Helper method for flood fill.
     *
     * @param tpimg      The label array.
     * @param x          Starting x coordinate.
     * @param y          Starting y coordinate.
     * @param cell_num   The label number to assign to the filled region.
     * @param width      Image width.
     * @param height     Image height.
     * @param rect       Rectangle to store the bounding box.
     */
    private static void floodFill(int[] tpimg, int x, int y, int cell_num, int width, int height, Rectangle rect) {
        Stack<Point> stack = new Stack<>();
        stack.push(new Point(x, y));

        int color = tpimg[y * width + x];
        tpimg[y * width + x] = cell_num;

        int min_x = x, max_x = x;
        int min_y = y, max_y = y;

        while (!stack.isEmpty()) {
            Point p = stack.pop();
            int cx = p.x;
            int cy = p.y;

            // Define 4-connected neighbors
            Point[] neighbors = {
                new Point(cx - 1, cy),
                new Point(cx + 1, cy),
                new Point(cx, cy - 1),
                new Point(cx, cy + 1)
            };

            for (Point n : neighbors) {
                int nx = n.x;
                int ny = n.y;
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    int nid = ny * width + nx;
                    if (tpimg[nid] == color) {
                        tpimg[nid] = cell_num;
                        stack.push(new Point(nx, ny));

                        // Update bounding rectangle
                        min_x = Math.min(min_x, nx);
                        max_x = Math.max(max_x, nx);
                        min_y = Math.min(min_y, ny);
                        max_y = Math.max(max_y, ny);
                    }
                }
            }
        }

        // Set the bounds of the rectangle
        rect.setBounds(min_x, min_y, max_x - min_x + 1, max_y - min_y + 1);
    }

    // Inside ImageUtils.java
    /**
     * Counts the number of 'o' (presumably "outer") vertices.
     *
     * @param vtx A list of Vertex_ objects.
     * @return The count of 'o' vertices.
     */
    public static int vxSet_RNUM(List<Vertex_> vtx) {
        int r_num = 0;
        for (Vertex_ j : vtx) {
            if (j.inout == 'o') {
                r_num++;
            }
        }
        return r_num;
    }
    
    /**
     * Comparator for Vertex_ objects based on the number of cells they belong to.
     */
    public static Comparator<Vertex_> JInOutComparator = new Comparator<Vertex_>() {
        @Override
        public int compare(Vertex_ left, Vertex_ right) {
            return Integer.compare(left.Cells.size(), right.Cells.size());
        }
    };
    
    /**
     * Comparator for Edge_ objects based on their 'inout' attribute.
     * 'o' (outer) edges are prioritized over 'i' (inner).
     */
    public static Comparator<Edge_> EInOutComparator = new Comparator<Edge_>() {
        @Override
        public int compare(Edge_ left, Edge_ right) {
            if (left.inout == 'o' && right.inout != 'o') {
                return -1;
            } else if (left.inout != 'o' && right.inout == 'o') {
                return 1;
            } else {
                return 0;
            }
        }
    };
    
    
    /**
     * Identifies and sets vertex points in an image.
     *
     * @param ip                The ImageProcessor containing the image data.
     * @param ctypes            A char array representing the 8-neighbor types of each pixel.
     * @param CellID            An int array storing the cell ID for each pixel.
     * @param isolated_terminals A list to store indices of isolated terminals.
     * @return A list of Vertex_ objects representing the identified vertices.
     */
    public static List<Vertex_> Set_Vertex_(ImageProcessor ip, char[] ctypes, int[] CellID, List<Integer> isolated_terminals) {
        int j_num = 0;
        int width = ip.getWidth();
        List<Vertex_> vvtxs = new ArrayList<>();
        int[] npb = {-width, -1, 1, width, -width - 1, -width + 1, width - 1, width + 1}; // Neighbor pixel offsets

        // Identify crossing, bifurcating, and terminal pixels
        for (int y = 1; y < ip.getHeight() - 1; y++) {
            for (int x = 1; x < ip.getWidth() - 1; x++) {
                int id = y * width + x;
                if (ctypes[id] == 't' || ctypes[id] == 'j' || ctypes[id] == 'f') {
                    Vertex_ tvtx = new Vertex_();
                    tvtx.Cells = new ArrayList<>();
                    tvtx.x = x;
                    tvtx.y = y;

                    for (int k = 0; k < 8; k++) {
                        int neighbor_id = id + npb[k];
                        tvtx.Cells.add(CellID[neighbor_id]);
                    }

                    // Remove duplicates
                    Set<Integer> set = new HashSet<>(tvtx.Cells);
                    tvtx.Cells = new ArrayList<>(set);
                    Collections.sort(tvtx.Cells);

                    // Handle isolated terminals
                    if (tvtx.Cells.size() == 2 && tvtx.Cells.get(0) == 0 && tvtx.Cells.get(1) != 1) {
                        System.out.println(x + " " + y);
                        isolated_terminals.add(id);
                        continue;
                    }

                    // Remove cells with IDs 0 and 1
                    tvtx.Cells.removeIf(cellId -> cellId == 0 || cellId == 1);

                    // Set inout based on the number of cells
                    tvtx.inout = (tvtx.Cells.size() == 0) ? 'o' : 'i';
                    tvtx.ctype = ctypes[id];
                    vvtxs.add(tvtx);
                    j_num++;
                }
            }
        }

        // Sort by inout
        vvtxs.sort(JInOutComparator);
        // Assign unique IDs
        for (int i = 0; i < vvtxs.size(); i++) {
            vvtxs.get(i).id = i;
        }

        return vvtxs;
    }

    // Comparator for Point to use in TreeMap
    static class PointComparator implements Comparator<Point> {
        @Override
        public int compare(Point a, Point b) {
            if (a.y != b.y) {
                return Integer.compare(a.y, b.y);
            } else {
                return Integer.compare(a.x, b.x);
            }
        }
    }

    /**
     * Reconnects contours in the image by handling isolated terminals.
     *
     * @param ip                The ImageProcessor containing the image data.
     * @param ctypes            A char array representing the 8-neighbor types of each pixel.
     * @param conts             A list of contours, each contour is a list of int[] points.
     * @param ijunc             A list of Vertex_ objects representing junctions.
     * @param isolated_terminals A list of indices of isolated terminals.
     */
    public static List<List<int[]>> Reconnect_Contours(ImageProcessor ip, char[] ctypes, List<List<int[]>> conts, List<Vertex_> ijunc, List<Integer> isolated_terminals) {
        int width = ip.getWidth();

        // For each isolated terminal
        for (int i = 0; i < isolated_terminals.size(); i++) {

            Set<Integer> contours_to_remove = new HashSet<>();
            List<List<int[]>> contours_to_add = new ArrayList<>();

            // Map from String (point key) to list of contour indices
            Map<String, List<Integer>> point_to_contours = new HashMap<>();

            // Build the map of points to contours
            for (int ci = 0; ci < conts.size(); ci++) {
                List<int[]> c = conts.get(ci);
                int[] p_front = c.get(0);
                int[] p_back = c.get(c.size() - 1);

                String key_front = p_front[0] + "," + p_front[1];
                point_to_contours.computeIfAbsent(key_front, k -> new ArrayList<>()).add(ci);

                if (p_back[0] != p_front[0] || p_back[1] != p_front[1]) { // Avoid duplicating if contour is a loop
                    String key_back = p_back[0] + "," + p_back[1];
                    point_to_contours.computeIfAbsent(key_back, k -> new ArrayList<>()).add(ci);
                }
            }

            int idx = isolated_terminals.get(i);
            int x = idx % width;
            int y = idx / width;
            int[] T1 = new int[]{x, y};
            String key_T1 = T1[0] + "," + T1[1];

            // IJ.log(String.format("Trying to delete %s", key_T1));

            // Find contours connected to T1
            List<Integer> c1_indices = point_to_contours.get(key_T1);
            if (c1_indices == null) {
                continue; // No contours connected to T1
            }
            if (c1_indices.size() != 1) {
                throw new RuntimeException("More than one contour connected to isolated terminal.");
            }
            int c1_idx = c1_indices.get(0);
            List<int[]> c1 = conts.get(c1_idx);

            for (int[] j : c1) {
                int idx_j = j[1] * width + j[0];
                ctypes[idx_j] = 'd';
            }

            int[] V1 = (c1.get(0)[0] == T1[0] && c1.get(0)[1] == T1[1]) ? c1.get(c1.size() -1) : c1.get(0);
            String key_V1 = V1[0] + "," + V1[1];

            // Now find contours c2 (other than c1) that share V1
            List<Integer> c2_indices = new ArrayList<>(point_to_contours.get(key_V1));

            if (!c2_indices.remove((Integer) c1_idx)) {
                throw new RuntimeException("Not found c1 at V1");
            }

            if (c2_indices.size() != 2) {
                // More than 2 contours meet at one junction or island
                contours_to_remove.add(c1_idx);

                // Remove contours and proceed
                List<List<int[]>> new_conts = new ArrayList<>();
                for (int ci = 0; ci < conts.size(); ci++) {
                    if (!contours_to_remove.contains(ci)) {
                        new_conts.add(conts.get(ci));
                    }else{
                        //IJ.log(String.format("contour %d, deleted", ci));
                    }
                }
                // Replace conts with new_conts
                conts = new_conts;
                continue;
            }

            int c2_idx = c2_indices.get(0);
            List<int[]> c2 = conts.get(c2_idx);

            List<int[]> c2_points = new ArrayList<>(c2);
            if (c2.get(0)[0] == V1[0] && c2.get(0)[1] == V1[1]) {
                Collections.reverse(c2_points);
            }

            int c3_idx = c2_indices.get(1);
            List<int[]> c3 = conts.get(c3_idx);

            List<int[]> c3_points = new ArrayList<>(c3);
            if (c3.get(c3.size() - 1)[0] == V1[0] && c3.get(c3.size() - 1)[1] == V1[1]) {
                c3_points.remove(c3_points.size() - 1);
                Collections.reverse(c3_points);
            } else {
                c3_points.remove(0);
            }

            List<int[]> c_new = new ArrayList<>(c2_points);
            c_new.addAll(c3_points);

            ctypes[idx] = 'e';
            contours_to_remove.add(c1_idx);
            contours_to_remove.add(c2_idx);
            contours_to_remove.add(c3_idx);

            contours_to_add.add(c_new);

            // Remove contours to be removed
            List<List<int[]>> new_conts = new ArrayList<>();
            for (int ci = 0; ci < conts.size(); ci++) {
                if (!contours_to_remove.contains(ci)) {
                    new_conts.add(conts.get(ci));
                }else{
                    // IJ.log(String.format("contour %d, deleted", ci));
                }
            }
            // Add the new contours
            new_conts.addAll(contours_to_add);
            // IJ.log(String.format("the size of contours %d to %d", conts.size(), new_conts.size()));
            // Replace conts with new_conts
            conts = new_conts;

            // Remove the junction V1 from ijunc
            Iterator<Vertex_> ijunc_it = ijunc.iterator();
            boolean found_junction = false;
            while (ijunc_it.hasNext()) {
                Vertex_ j = ijunc_it.next();
                if (j.x == V1[0] && j.y == V1[1]) {
                    ijunc_it.remove();
                    found_junction = true;
                    break;
                }
            }
            if (!found_junction) {
                throw new RuntimeException("Unrecognized junction");
            }
        }

        return conts;

    }

    
    /**
     * Sets edge information based on contours and vertices.
     *
     * @param ip     The ImageProcessor containing the image data.
     * @param ctypes A char array representing the 8-neighbor types of each pixel.
     * @param conts  A list of contours (lists of points).
     * @param ivtx  A list of Vertex_ objects.
     * @return A list of Edge_ objects representing the identified edges.
     */
    public static List<Edge_> Set_Edge_(ImageProcessor ip, char[] ctypes, List<List<int[]>> conts, List<Vertex_> ivtx) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        List<Edge_> vedges = new ArrayList<>();
    
        // Create a mapping from pixel coordinates to vertex IDs
        int[] VtxID = new int[width * height];
        Arrays.fill(VtxID, -1);
        for (Vertex_ j : ivtx) {
            int id = (int)(j.y * width + j.x);
            VtxID[id] = j.id;
        }
    
        // Set edge information
        for (List<int[]> contour : conts) {
            if (contour.isEmpty()) continue;
            Edge_ tedge = new Edge_();
            int[] firstPt = contour.get(0);
            int[] lastPt = contour.get(contour.size() - 1);
        
            tedge.X.set(0, new int[]{firstPt[0], firstPt[1]});
            tedge.vertex_id[0] = VtxID[firstPt[1] * width + firstPt[0]];
            tedge.X.set(1, new int[]{lastPt[0], lastPt[1]});
            tedge.vertex_id[1] = VtxID[lastPt[1] * width + lastPt[0]];
            // Determine 'inout' based on ctypes
            char ctypeStart = ctypes[firstPt[1] * width + firstPt[0]];
            char ctypeEnd = ctypes[lastPt[1] * width + lastPt[0]];
            tedge.inout = (ctypeStart == 't' || ctypeEnd == 't') ? 'o' : 'i';
            // Calculate Euclidean distance
            tedge.sdist = Math.sqrt(Math.pow(firstPt[0] - lastPt[0], 2) + Math.pow(firstPt[1] - lastPt[1], 2));
            // Calculate angle
            tedge.angle = -Math.atan2(firstPt[1] - lastPt[1], firstPt[0] - lastPt[0]);
            if (tedge.angle < 0) {
                tedge.angle += Math.PI;
            }
            tedge.line_pts = new ArrayList<>(contour);
            vedges.add(tedge);
        }
    
        // Sort edges by 'inout' (prioritize 'o')
        vedges.sort(EInOutComparator);
        // Assign unique IDs
        for (int i = 0; i < vedges.size(); i++) {
            vedges.get(i).id = i;
        }
    
        // Check for consistency
        int jr_num = vxSet_RNUM(ivtx);
        long er_num = vedges.stream().filter(e -> e.inout == 'o').count();
        if (jr_num != er_num) {
            throw new RuntimeException(String.format("! Inconsistent out-vertex and out-edge, jr_num= %d, er_num= %d", jr_num, er_num));
        }

        
        // Point cpt = new Point(0,0);
        // vxDraw_Vertex(ip, vedges, "output.png", 1500,cpt);
    
        return vedges;
    }
    
    
    
    /**
     * Sets information about inner cells based on vertices and edges.
     *
     * @param vcells   A list to store VCell_ objects.
     * @param ivtx    A list of Vertex_ objects.
     * @param iedge    A list of Edge_ objects.
     * @param cell_num The total number of cells.
     */
    public static void Set_InsideCells(List<VCell_> vcells, List<Vertex_> ivtx, List<Edge_> iedge, int cell_num) {
        // Initialize a list of lists to store edges for each cell
        List<List<Integer>> nedge = new ArrayList<>(cell_num);
        for (int i = 0; i < cell_num; i++) {
            nedge.add(new ArrayList<>());
        }
    
        // Calculate edges surrounding each cell
        for (int et = 0; et < iedge.size(); et++) {
            Edge_ edge = iedge.get(et);
            Vertex_ jt1 = ivtx.get(edge.vertex_id[0]);
            Vertex_ jt2 = ivtx.get(edge.vertex_id[1]);
            if (jt1.inout == 'i' && jt2.inout == 'i') {
                for (int cell_id1 : jt1.Cells) {
                    for (int cell_id2 : jt2.Cells) {
                        if (cell_id1 == cell_id2) {
                            // Assuming cell IDs start from 2 as per previous methods
                            if (cell_id1 - 2 >= 0 && cell_id1 - 2 < nedge.size()) {
                                nedge.get(cell_id1 - 2).add(et);
                            }
                            break;
                        }
                    }
                }
            }
        }
    
        // Process each cell
        for (int i = 0; i < cell_num; i++) {
            VCell_ tcell = new VCell_();
            List<Boolean> checks = new ArrayList<>(Collections.nCopies(nedge.get(i).size(), true));
            List<Integer> edgesList = nedge.get(i);
            if (edgesList.isEmpty()) continue; // No edges to process
        
            int firstEdge_Id = edgesList.get(0);
            Edge_ firstEdge_ = iedge.get(firstEdge_Id);
            int jf = firstEdge_.vertex_id[0];
            int ps = firstEdge_.vertex_id[1];
            tcell.EDGE.add(firstEdge_);
            tcell.VERTEX.add(ivtx.get(jf));
            tcell.VERTEX.add(ivtx.get(ps));
            checks.set(0, false);
            for(int j = 1; j < edgesList.size(); j++){
                for (int k = 1; k < edgesList.size(); k++) {
                    if (!checks.get(k)) continue;
                    Edge_ currentEdge_ = iedge.get(edgesList.get(k));
                    if (currentEdge_.vertex_id[0] == ps) {
                        ps = currentEdge_.vertex_id[1];
                        tcell.EDGE.add(currentEdge_);
                        tcell.VERTEX.add(ivtx.get(ps));
                        checks.set(k, false);
                    } else if (currentEdge_.vertex_id[1] == ps) {
                        ps = currentEdge_.vertex_id[0];
                        tcell.EDGE.add(currentEdge_);
                        tcell.VERTEX.add(ivtx.get(ps));
                        checks.set(k, false);
                    }
                }
            }
        
            // Check for consistency
            if (tcell.VERTEX.get(0).id != tcell.VERTEX.get(tcell.VERTEX.size() - 1).id) {
                throw new RuntimeException(String.format("At (%d, %d) and (%d, %d): Setting vcells is inconsistent",
                        tcell.VERTEX.get(0).x, tcell.VERTEX.get(0).y,
                        tcell.VERTEX.get(tcell.VERTEX.size() - 1).x, tcell.VERTEX.get(tcell.VERTEX.size() - 1).y));
            }
        
            // Direction check and calculate center
            double darea = 0.0;
            for (int j = 0; j < tcell.VERTEX.size() - 1; j++) {
                double x1 = tcell.VERTEX.get(j).x;
                double x2 = tcell.VERTEX.get(j + 1).x;
                double y1 = -tcell.VERTEX.get(j).y; // Reverse y-coordinate
                double y2 = -tcell.VERTEX.get(j + 1).y;
                darea += (x1 * y2 - x2 * y1);
            }
            // Last segment
            int j = tcell.VERTEX.size() - 1;
            double x1 = tcell.VERTEX.get(j).x;
            double x2 = tcell.VERTEX.get(0).x;
            double y1 = -tcell.VERTEX.get(j).y; // Reverse y-coordinate
            double y2 = -tcell.VERTEX.get(0).y;
            darea += (x1 * y2 - x2 * y1);
        
            if (darea < 0) {
                Collections.reverse(tcell.VERTEX);
            }
        
            // Remove the duplicate last vertex
            if (!tcell.VERTEX.isEmpty()) {
                tcell.VERTEX.remove(tcell.VERTEX.size() - 1);
            }
        
            tcell.inout = 'i';
            vcells.add(tcell);
        }
    }
    
    
    /**
     * Sets information about outer cells based on vertices.
     *
     * @param vcells A list to store VCell_ objects.
     * @param ivtx  A list of Vertex_ objects.
     */
    public static void Set_OutsideCells(List<VCell_> vcells, List<Vertex_> ivtx) {
        for (Vertex_ vtx : ivtx) {
            if (vtx.inout == 'o') {
                VCell_ tcell = new VCell_();
                tcell.inout = 'o';
                tcell.VERTEX.add(vtx);
                int pid = vtx.id;
            
                if (vtx.nj_num != 1) {
                    throw new RuntimeException("Not a single neighbor for out vertex");
                }
            
                int neighborJid = vtx.nj.get(0);
                Vertex_ nvtx = ivtx.get(neighborJid);
            
                while (true) {
                    tcell.VERTEX.add(nvtx);
                    // Find the index of pid in nvtx.nj
                    int nid = nvtx.nj.indexOf(pid);
                    if (nid == -1) {
                        throw new RuntimeException("Neighbor vertex not found");
                    }
                    if(nvtx.nj_num == 1){
                        throw new RuntimeException("nj_num == 1 in outside cell");
                    }
                    // IJ.log(String.format("nid = %d, pid = %d, nj_num = %d", nid, pid, nvtx.nj_num));
                    nid = (nid + 1) % nvtx.nj_num;
                    pid = nvtx.id;
                    // IJ.log(String.format("nid => %d", nid));

                    nvtx = ivtx.get(nvtx.nj.get(nid));
                    if (nvtx.inout == 'o') {
                        break;
                    }
                }
            
                // // Remove the duplicate last vertex
                // if (!tcell.VERTEX.isEmpty()) {
                //     tcell.VERTEX.remove(tcell.VERTEX.size() - 1);
                // }
                tcell.VERTEX.add(nvtx);
                vcells.add(tcell);
            }
        }
    }
    
    
    /**
     * Sets neighbor vertices (nvtx) for each vertex and sorts them by angle.
     *
     * @param ivtx A list of Vertex_ objects.
     * @param iedge A list of Edge_ objects.
     */
    public static void Set_NVertices(List<Vertex_> ivtx, List<Edge_> iedge) {
        // Set nvtx and ne (neighboring edges) for each vertex
        for (Edge_ edge : iedge) {
            int j0 = edge.vertex_id[0];
            int j1 = edge.vertex_id[1];
            ivtx.get(j0).nj.add(j1);
            ivtx.get(j0).ne.add(edge.id);
            ivtx.get(j1).nj.add(j0);
            ivtx.get(j1).ne.add(edge.id);
        }
    
        // Calculate angles and sort neighbors
        for (Vertex_ vtx : ivtx) {
            int numNeighbors = vtx.nj_num = vtx.nj.size();
            double[] angles = new double[numNeighbors]; // Assuming maximum of 4 neighbors
        
            for (int i = 0; i < numNeighbors; i++) {
                int neighborId = vtx.ne.get(i);
                Edge_ edge = iedge.get(neighborId);
                List<int[]> linePts = edge.line_pts;
            
                int[] currentPt = null;
                if (linePts.get(0)[0] == vtx.x && linePts.get(0)[1] == vtx.y) {
                    currentPt = linePts.get(1);
                } else {
                    currentPt = linePts.get(linePts.size() - 2);
                }
            
                double dx = currentPt[0] - vtx.x;
                double dy = -(currentPt[1] - vtx.y); // Reverse y-coordinate
                angles[i] = Math.atan2(-dy, dx);
            }
        
            // Sort neighbors by angles using a simple selection sort
            for (int i = 0; i < numNeighbors; i++) {
                int minIndex = i;
                double minAngle = angles[i];
                for (int j = i + 1; j < numNeighbors; j++) {
                    if (angles[j] < minAngle) {
                        minAngle = angles[j];
                        minIndex = j;
                    }
                }
                // Swap angles
                double tempAngle = angles[i];
                angles[i] = angles[minIndex];
                angles[minIndex] = tempAngle;
                // Swap nj
                int tempVtx = vtx.nj.get(i);
                vtx.nj.set(i, vtx.nj.get(minIndex));
                vtx.nj.set(minIndex, tempVtx);
            }
        }
    }
    
    
    /**
     * Calculates the area and center of a VCell_ object.
     *
     * @param e_cell The VCell_ object to process.
     */
    public static void vxSet_vcell_center(VCell_ e_cell) {
        e_cell.area = 0.0;
        e_cell.cx = 0.0;
        e_cell.cy = 0.0;
    
        int numVertex_s = e_cell.VERTEX.size();
        for (int j = 0; j < numVertex_s; j++) {
            Vertex_ current = e_cell.VERTEX.get(j);
            Vertex_ next = e_cell.VERTEX.get((j + 1) % numVertex_s);
        
            double x1 = current.x;
            double y1 = -current.y; // Reverse y-coordinate
            double x2 = next.x;
            double y2 = -next.y; // Reverse y-coordinate
        
            double tarea = (x1 * y2 - x2 * y1);
            e_cell.area += tarea / 2.0;
            e_cell.cx += tarea * (x1 + x2);
            e_cell.cy += tarea * (y1 + y2);
        }
    
        if (e_cell.area != 0) {
            e_cell.cx /= (6.0 * e_cell.area);
            e_cell.cy /= (6.0 * e_cell.area);
        } else {
            e_cell.cx = 0;
            e_cell.cy = 0;
        }
    }
    
    /**
     * Sets vertex (VCell_) information based on image data.
     *
     * @param timg               The ImagePlus containing the image data.
     * @param minimal_cell_size Threshold for minimum cell area.
     * @param cpt                Coordinate offset (optional, default is (0, 0)).
     * @return A Triple containing the list of VCell_ objects, Vertex_s, and Edge_s.
     */
    public static Triple<List<VCell_>, List<Vertex_>, List<Edge_>> vxSet_Vertex(ImagePlus timg, int minimal_cell_size, Point cpt) {
        ImageProcessor ip = timg.getProcessor();
        if (ip.getNChannels() != 1) {
            throw new IllegalArgumentException("Image must be grayscale.");
        }
    
        int width = ip.getWidth();
        int height = ip.getHeight();
        int[] CellID = new int[width * height];
        char[] ctypes = new char[width * height];
        List<List<int[]>> edge_conts = new ArrayList<>();
        List<VCell_> vcells = new ArrayList<>();
        int cell_num = 0;
    
        // Set ctypes
        CVUtil_.setCtypes(timg, ctypes); // Assuming CVUtil_.setCtypes accepts ImagePlus
    
        // Trace contours
        edge_conts = CVUtil_.trace(timg, ctypes, cpt); // Assuming CVUtil_.trace returns List<List<int[]>>
    
        // Set CellID
        ImageUtils_.Pair<Integer, int[]> cellInfo = utlSet_CellID(ip, minimal_cell_size);
        cell_num = cellInfo.first;
        CellID = cellInfo.second;
        List<Integer> isolated_terminals = new ArrayList<>();
        // Set Vertex_
        List<Vertex_> ivtx = Set_Vertex_(ip, ctypes, CellID, isolated_terminals);
        IJ.log(String.format("isolated_terminals %d", isolated_terminals.size()));
        edge_conts = Reconnect_Contours(ip, ctypes, edge_conts, ivtx, isolated_terminals);

        // *** Important Change ***
        // After reconnection, we must recalculate vertices and edges because the topology has changed.
        // The previous ivtx is no longer valid after edge_conts is modified.
        List<Integer> no_isolated_terminals = new ArrayList<>();

        // Recompute vertices with the updated contours and ctypes
        ivtx = Set_Vertex_(ip, ctypes, CellID, no_isolated_terminals);

        IJ.log(String.format("The size of contours %d", edge_conts.size()));
        // Sort by inout
        ivtx.sort(JInOutComparator);
        // Assign unique IDs
        for (int i = 0; i < ivtx.size(); i++) {
            ivtx.get(i).id = i;
        }
        
        // Set Edge_
        List<Edge_> iedge = Set_Edge_(ip, ctypes, edge_conts, ivtx);
    
        // Set up vertices, outer cells, and inner cells
        Set_NVertices(ivtx, iedge);


        Set_OutsideCells(vcells, ivtx);
        Set_InsideCells(vcells, ivtx, iedge, cell_num);
    
        // Assign IDs to VCell_s
        for (int i = 0; i < vcells.size(); i++) {
            vcells.get(i).id = i;
        }
    
        // Check cell area and calculate center
        for (VCell_ cell : vcells) {
            int jnum = cell.VERTEX.size();
            double area = 0.0;
            cell.cx = 0.0;
            cell.cy = 0.0;
        
            for (int j = 0; j < jnum - 1; j++) {
                double x1 = cell.VERTEX.get(j).x;
                double x2 = cell.VERTEX.get(j + 1).x;
                double y1 =  -cell.VERTEX.get(j).y; // Reverse y-coordinate
                double y2 =  -cell.VERTEX.get(j + 1).y; // Reverse y-coordinate
            
                area += (x1 * y2 - x2 * y1);
            }
        
            // Last segment
            int j = jnum - 1;
            double x1 = cell.VERTEX.get(j).x;
            double x2 = cell.VERTEX.get(0).x;
            double y1 = -cell.VERTEX.get(j).y; // Reverse y-coordinate
            double y2 = -cell.VERTEX.get(0).y; // Reverse y-coordinate
            area += (x1 * y2 - x2 * y1);
        
            if (area < 0.0) {
                throw new RuntimeException(String.format("ERROR: Negative area at cell %d, %c area= %f: (%f, %f)",
                        cell.id, cell.inout, 0.5 * area, cell.VERTEX.get(0).x, cell.VERTEX.get(0).y));
            }
        
            // Calculate center
            vxSet_vcell_center(cell);
        }
    
        // Update ImagePlus with processed ImageProcessor
        timg.setProcessor(ip);
    
        // Return the results
        return new Triple<>(vcells, ivtx, iedge);
    }
    
    
    
    /**
     * Sets the edges surrounding each cell in vcell.
     *
     * @param vcell A list of VCell_ objects.
     * @param vedge A list of Edge_ objects.
     */
    public static void Set_Cel_Edge_(List<VCell_> vcell, List<Edge_> vedge) {
        for (VCell_ cell : vcell) {
            cell.EDGE.clear(); // Clear existing edges
        
            List<Vertex_> ctj = cell.VERTEX;
            for (int j = 0; j < ctj.size(); j++) {
                int jid = ctj.get(j).id;
                int njid = (j != ctj.size() - 1) ? ctj.get(j + 1).id : ctj.get(0).id; // Next vertex ID
            
                for (Edge_ edge : vedge) {
                    if ((edge.vertex_id[0] == jid && edge.vertex_id[1] == njid) ||
                        (edge.vertex_id[1] == jid && edge.vertex_id[0] == njid)) {
                        cell.EDGE.add(edge);
                        break; // Found the edge, no need to continue searching
                    }
                }
            }
        }
    }
    
    /**
     * Checks consistency of vertex, edge, and cell numbers.
     *
     * @param c_num  Number of cells.
     * @param e_num  Number of edges.
     * @param v_num  Number of vertices (vertices).
     * @param ex_num Number of external cells/edges/vertices.
     * @param f_num  Number of 4-way vertices.
     * @return True if numbers are consistent, False otherwise.
     */
    public static boolean check_numbers(int c_num, int e_num, int v_num, int ex_num, int f_num) {
        int tv = v_num - 2 * ex_num;
        int te = e_num - 2 * ex_num;
        int tc = c_num - ex_num;
    
        IJ.log(String.format(" v= %d  e= %d  c= %d", v_num, e_num, c_num));
        IJ.log(String.format(" 4-way vtx. = %d", f_num));
        IJ.log(String.format(" v - 2*ex_num =  tv  =  %d", tv));
        IJ.log(String.format(" e - 2*ex_num =  te  =  %d", te));
        IJ.log(String.format(" c -   ex_num =  tc  =  %d", tc));
        IJ.log(String.format(" ex = %d", ex_num));
        IJ.log(String.format(" check..  3*tv + fwj = 2*te ?  %d %d", 3 * tv + f_num, 2 * te));
        IJ.log(String.format(" check..  tv - te + tc == 1 ?  %d", tv - te + tc));
    
        return (2 * te == 3 * tv + f_num) && (tv - te + tc == 1);
    }
    
    
    /**
     * Outputs vertex data to a file.
     *
     * @param filename The name of the output file.
     * @param ovtx    A list of Vertex_ objects.
     * @param oedge    A list of Edge__ objects.
     * @param ocell    A list of VCell_ objects.
     * @param cpt      Coordinate offset (optional, default is (0, 0)).
     */
    public static void vxOutputDatas(String filename, List<Vertex_> ovtx, List<Edge_> oedge, List<VCell_> ocell, Point cpt) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            vxOutputDatas_file(writer, ovtx, oedge, ocell, cpt);
        } catch (IOException e) {
            throw new RuntimeException("Error writing to file: " + filename, e);
        }
    }
    
    /**
     * Outputs vertex data to a BufferedWriter.
     *
     * @param fp     The BufferedWriter to write to.
     * @param ovtx  A list of Vertex_ objects.
     * @param oedge  A list of Edge_ objects.
     * @param ocell  A list of VCell_ objects.
     * @param cpt    Coordinate offset (optional, default is (0, 0)).
     */
    public static void vxOutputDatas_file(BufferedWriter fp, List<Vertex_> ovtx, List<Edge_> oedge, List<VCell_> ocell, Point cpt) throws IOException {
        int c_num = ocell.size();
        int e_num = oedge.size();
        int v_num = ovtx.size();
        int f_num = 0;
        int ex_num = 0;
    
        for (Vertex_ j : ovtx) {
            if (j.ctype == 'f') {
                f_num++;
            }
        }
        for (VCell_ c : ocell) {
            if (c.inout == 'o') {
                ex_num++;
            }
        }
    
        if (!check_numbers(c_num, e_num, v_num, ex_num, f_num)) {
            throw new RuntimeException("Not consistent number of vertex, edges, and vertices.");
        }
    
        fp.write(String.format("### C_NUM %d \n", c_num));
        fp.write(String.format("###  IN_CNUM %d \n", (c_num - ex_num)));
        fp.write(String.format("###  EX_CNUM %d \n", ex_num));
        fp.write(String.format("### E_NUM %d \n", e_num));
        fp.write(String.format("###  IN_E_NUM %d \n", (e_num - ex_num)));
        fp.write(String.format("###  EX_E_NUM %d \n", ex_num));
        fp.write(String.format("### V_NUM %d \n", v_num));
        fp.write(String.format("###  IN_V_NUM %d \n", (v_num - ex_num)));
        fp.write(String.format("###  EX_V_NUM %d \n", ex_num));
    
        // Write Vertex_s
        for (Vertex_ vtx : ovtx) {
            if (vtx.id != ovtx.indexOf(vtx)) {
                throw new RuntimeException("Vertex_ ID mismatch.");
            }
            fp.write(String.format("V[%d] %f %f ", vtx.id, vtx.x + cpt.x, -(vtx.y + cpt.y)));
            if (vtx.inout == 'o') {
                fp.write("Ext");
            }
            fp.write("\n");
        }
        fp.write("\n");
    
        // Write Edge_s
        for (Edge_ edge : oedge) {
            fp.write(String.format("E[%d] %d %d ", edge.id, edge.vertex_id[0], edge.vertex_id[1]));
            if (edge.inout == 'o') {
                fp.write("Ext");
            }
            fp.write("\n");
        }
        fp.write("\n");
    
        // Write Cells
        for (VCell_ cell : ocell) {
            fp.write(String.format("C[%d] %d : ", cell.id, cell.VERTEX.size()));
            for (Vertex_ vtx : cell.VERTEX) {
                fp.write(String.format("%d ", vtx.id));
            }
            if (cell.inout == 'o') {
                fp.write(" Ext");
            }
            fp.write("\n");
        }
        fp.write("\n");
    }
    
    /**
     * Outputs vertex geometry data to a BufferedWriter.
     *
     * @param fp     The BufferedWriter to write to.
     * @param oe_num Number of outer edges.
     * @param oedge  A list of Edge_ objects.
     */
    public static void vxOutputGeometry(BufferedWriter fp, int oe_num, List<Edge_> oedge) throws IOException {
        for (int i = 0; i < oe_num; i++) {
            Edge_ edge = oedge.get(i);
            int[] pt1 = edge.X.get(0);
            int[] pt2 = edge.X.get(1);
            fp.write(String.format("#%f \n", edge.sdist));
            fp.write(String.format("%f %f \n%f %f\n\n", 
                    (double) pt1[0], (double) -pt1[1], 
                    (double) pt2[0], (double) -pt2[1])); // Reverse y-coordinate
        }
    }
    
    /**
     * Draws edges on a green RGB image based on the grayscale source image.
     *
     * @param src_img        The grayscale ImageProcessor to base the green channel on.
     * @param edges          A list of Edge_ objects to draw.
     * @param out_image_name The name/path for the output image.
     * @param waiting_time   Time to wait after drawing (0 to wait for user input).
     * @param cpt            Coordinate offset (optional, default is (0, 0)).
     */
    public static void vxDraw_Vertex(ImageProcessor src_img, List<Edge_> edges, String out_image_name, int waiting_time, Point cpt) {
        int width = src_img.getWidth();
        int height = src_img.getHeight();
        ColorProcessor vimg = new ColorProcessor(width, height);
    
        // Draw the green channel based on the grayscale source image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray_value = src_img.getPixel(x, y) & 0xFF;
                vimg.setColor(new Color(0, gray_value, 0)); // Set green channel with grayscale value
                vimg.drawPixel(x, y);
            }
        }
    
        // Draw the edges
        Color rcolor = Color.MAGENTA; // Magenta color for the lines
    
        for (Edge_ edge : edges) {
            int[] pt1 = edge.X.get(0);
            int[] pt2 = edge.X.get(1);
            Point p1 = new Point(pt1[0] + cpt.x, pt1[1] + cpt.y);
            Point p2 = new Point(pt2[0] + cpt.x, pt2[1] + cpt.y);
            vimg.setColor(rcolor);
            vimg.drawLine(p1.x, p1.y, p2.x, p2.y); // Draw line on the ColorProcessor
        }
    
        // Show the image
        ImagePlus imp_vimg = new ImagePlus("Vertex", vimg);
        imp_vimg.show();
        // Save the output image
        IJ.saveAs(imp_vimg, "png", out_image_name);
    
        // Wait for user input if waiting_time == 0
        if (waiting_time == 0) {
            IJ.log("Press any key while focusing on the image to continue...");
            IJ.wait(0); // Wait indefinitely
        }
    
        IJ.log("> Output vertex file: " + out_image_name);
    }
    
    
    
    
    /**
     * Draws a polygon defined by a list of edges on an ImagePlus in Fiji.
     *
     * @param edges      A list of Edge_ objects representing the polygon's edges.
     * @param src_img    The source ImagePlus to draw on.
     * @param outfile    (Optional) The path to save the resulting image.
     * @param cpt        (Optional) An offset point to shift the polygon's position.
     */
    public static void vxDraw_Polygon(List<Edge_> edges, ImagePlus src_img, String outfile, Point cpt) {
        int width = src_img.getWidth();
        int height = src_img.getHeight();
        ColorProcessor vimg = new ColorProcessor(width, height);
    
        // Draw the edges
        Color rcolor = Color.MAGENTA; // Magenta color for the lines
    
        for (Edge_ edge : edges) {
            int[] pt1 = edge.X.get(0);
            int[] pt2 = edge.X.get(1);
            Point p1 = new Point(pt1[0] + cpt.x, pt1[1] + cpt.y);
            Point p2 = new Point(pt2[0] + cpt.x, pt2[1] + cpt.y);
            vimg.setColor(rcolor);
            vimg.drawLine(p1.x, p1.y, p2.x, p2.y); // Draw line on the ColorProcessor
        }
    
        // Show the image
        ImagePlus imp_vimg = new ImagePlus("Polygon", vimg);
        imp_vimg.show();
        imp_vimg.getWindow().toFront(); // Bring the window to the front
    
        // Save the output image if outfile is provided
        if (outfile != null && !outfile.isEmpty()) {
            IJ.saveAs(imp_vimg, "PNG", outfile);
        }
    }
    
    
    
    
    
    /**
     * Prints information about cells and their vertices to the console.
     *
     * @param cells      A list of VCell_ objects representing the cells.
     * @param vertices  A list of Vertex_ objects.
     * @param cpt        Coordinate offset (optional, default is (0, 0)).
     */

    public static void vxOut_by_Cells(List<VCell_> cells, List<Vertex_> vertices, Point cpt) {
        for (int i = 0; i < cells.size(); i++) {
            VCell_ cell = cells.get(i);
            IJ.log(String.format("# %d %d", i, cell.VERTEX.size()));
            for (int j = 0; j < cell.VERTEX.size() - 1; j++) {
                Vertex_ current = cell.VERTEX.get(j);
                Vertex_ next = cell.VERTEX.get(j + 1);
                IJ.log(String.format("%d %d", current.x, current.y));
                IJ.log(String.format("%d %d", next.x, next.y));
            }
            // Last vertex to first
            Vertex_ last = cell.VERTEX.get(cell.VERTEX.size() - 1);
            Vertex_ first = cell.VERTEX.get(0);
            IJ.log(String.format("%d %d", last.x, last.y));
            IJ.log(String.format("%d %d\n", first.x, first.y));
        }
    }


}
