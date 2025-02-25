

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.List;
import java.awt.Point;

class CVUtil_ {

    public static final char[] CVUTIL_NCELL_TYPE = {
            'd', 't', 't', 't', 't', 'e', 't', 't', 't', 't', 'e', 'b', 'e', 'e', 'e', 'z', 't', 'e', 'e', 'e', 't', 'e',
            'b', 'e',
            'e', 'e', 'j', 'e', 'e', 'e', 'z', 'z', 't', 'e', 'e', 'e', 'e', 'j', 'e', 'e', 't', 't', 'e', 'z', 'e',
            'e', 'e', 'z',
            'e', 'j', 'j', 'j', 'e', 'j', 'z', 'z', 'e', 'e', 'j', 'z', 'e', 'e', 'z', 'z', 't', 'e', 'e', 'e', 'e',
            'j', 'e', 'e',
            'e', 'e', 'j', 'z', 'j', 'j', 'j', 'z', 'e', 'j', 'j', 'j', 'e', 'j', 'z', 'z', 'j', 'j', 'f', 'z', 'j',
            'j', 'z', 'z',
            't', 'e', 'e', 'e', 'e', 'j', 'e', 'e', 'b', 'z', 'z', 'z', 'z', 'z', 'z', 'z', 'e', 'j', 'j', 'j', 'e',
            'j', 'z', 'z',
            'z', 'z', 'z', 'z', 'z', 'z', 'z', 'z', 't', 'e', 'e', 'e', 'e', 'j', 'e', 'e', 'e', 'e', 'j', 'z', 'j',
            'j', 'j', 'z',
            't', 'e', 'e', 'e', 't', 'e', 'z', 'z', 'e', 'e', 'j', 'z', 'e', 'e', 'z', 'z', 'e', 'j', 'j', 'j', 'j',
            'f', 'j', 'j',
            'e', 'e', 'j', 'z', 'j', 'j', 'j', 'z', 'e', 'j', 'j', 'j', 'e', 'j', 'z', 'z', 'e', 'e', 'j', 'z', 'e',
            'e', 'z', 'z',
            't', 'e', 'e', 'e', 'e', 'j', 'e', 'e', 'e', 'e', 'j', 'z', 'j', 'j', 'j', 'z', 'z', 'z', 'z', 'z', 'z',
            'z', 'z', 'z',
            'z', 'z', 'z', 'z', 'z', 'z', 'z', 'z', 't', 'e', 'e', 'e', 'e', 'j', 'e', 'e', 'b', 'z', 'z', 'z', 'z',
            'z', 'z', 'z',
            'z', 'z', 'z', 'z', 'z', 'z', 'z', 'z', 'z', 'z', 'z', 'z', 'z', 'z', 'z', 'z'
    };

    private static final byte WHITE = -1;
    private static final byte GRAY = -128;

    public static void setCtypes(ImagePlus imp, char[] ctypes) {
        ImageProcessor ip = imp.getProcessor();
        byte[] pixels = (byte[]) ip.getPixels(); // Assuming 8-bit image
        int w = imp.getWidth();

        IJ.log("   (Set_Ctype) mw   " + imp.getWidth() + "  " + imp.getHeight());

        for (int y = 1; y < imp.getHeight() - 1; y++) {
            for (int x = 1; x < imp.getWidth() - 1; x++) {
                int id = w * y + x;
                if (pixels[id] == 0) {
                    ctypes[id] = 'i';
                } else {
                    int cindex = (
                            (pixels[id - w - 1] == WHITE ? 1 : 0)
                                    + 2 * (pixels[id - w] == WHITE ? 1 : 0)
                                    + 4 * (pixels[id - w + 1] == WHITE ? 1 : 0)
                                    + 8 * (pixels[id - 1] == WHITE ? 1 : 0)
                                    + 16 * (pixels[id + 1] == WHITE ? 1 : 0)
                                    + 32 * (pixels[id + w - 1] == WHITE ? 1 : 0)
                                    + 64 * (pixels[id + w] == WHITE ? 1 : 0)
                                    + 128 * (pixels[id + w + 1] == WHITE ? 1 : 0)
                    );
                    ctypes[id] = CVUTIL_NCELL_TYPE[cindex];
                }

                if (ctypes[id] == 'z') {
                    IJ.log("!!! 'z' pixel found at " + x + "  " + y);
                    return;
                }

                if (ctypes[id] == 'b') {
                    IJ.log("!!! 'b' pixel found at " + x + "  " + y);
                    return;
                }
            }
        }
    }

    public static List<List<int[]>> trace(ImagePlus imp, char[] ctypes, Point cpt) {
        int W = imp.getWidth();
        int H = imp.getHeight();
        byte[] tV = (byte[]) imp.getProcessor().getPixelsCopy(); // Use getPixelsCopy()
        int[] ejnum = new int[W * H];
        List<List<int[]>> conts = new ArrayList<>();

        // (tV initialization is already handled by getPixelsCopy())

        int[] npb = { -W, -1, 1, W, -W - 1, -W + 1, W - 1, W + 1 };

        for (int y = 1; y < H - 1; y++) {
            for (int x = 1; x < W - 1; x++) {
                int sid = W * y + x;
                List<Integer> termid = new ArrayList<>(); // Use ArrayList for termid

                if (ctypes[sid] == 't' || ctypes[sid] == 'j' || ctypes[sid] == 'f') {
                    while ((ctypes[sid] == 't' && ejnum[sid] < 1) ||
                            (ctypes[sid] == 'j' && ejnum[sid] < 3) ||
                            (ctypes[sid] == 'f' && ejnum[sid] < 4)) {
                        int tid = sid;
                        int counter = 0;
                        int tedge_length = 0;
                        List<int[]> pts = new ArrayList<>();
                        pts.add(new int[] { tid % W, tid / W });
                        int tx = 0;
                        int ty = 0;

                        while (true) {
                            boolean is_moved = false;
                            for (int k = 0; k < 8; k++) {
                                int neighbor_idx = tid + npb[k];
                                if (tV[neighbor_idx] == WHITE) {
                                    if (tedge_length == 1 && neighbor_idx == sid + W) {
                                        continue; // Exception handling
                                    }
                                    // Diagonal checks
                                    if (k == 4 && (tV[tid - W] != 0 || tV[tid - 1] != 0)) {
                                        continue;
                                    }
                                    if (k == 5 && (tV[tid - W] != 0 || tV[tid + 1] != 0)) {
                                        continue;
                                    }
                                    if (k == 6 && (tV[tid - 1] != 0 || tV[tid + W] != 0)) {
                                        continue;
                                    }
                                    if (k == 7 && (tV[tid + 1] != 0 || tV[tid + W] != 0)) {
                                        continue;
                                    }

                                    tV[tid] = 1;
                                    tid = neighbor_idx;
                                    tedge_length++;
                                    is_moved = true;
                                    break;
                                }
                            }
                            if(!is_moved){
                                IJ.log(String.format("An irregular loop was detected at %d %d", tx, ty));
                                IJ.error(String.format("An irregular loop was detected at %d %d", tx, ty));
                                return null;
                            }
                            tx = tid % W;
                            ty = tid / W;
                            pts.add(new int[] { tx, ty });
                            if (counter > 150) {
                                IJ.log(String.format("%d %d %c", tx + cpt.x, ty + cpt.y, ctypes[tid]));
                            }
                            if (counter > 200) {
                                IJ.log("Unexpected Loop");
                                return null; // Or handle the error appropriately
                            }
                            counter++;
                            if (!(1 < tx && tx < W - 1 && 1 < ty && ty < H - 1 &&
                                    (ctypes[tid] != 't' && ctypes[tid] != 'j' && ctypes[tid] != 'f'))) {
                                break;
                            }
                        }
                        ejnum[sid]++;
                        ejnum[tid]++;

                        if (ejnum[sid] > 5) {
                            IJ.log(String.format("Error at %d %d", tx, ty));
                        } else if (ejnum[tid] > 5) {
                            IJ.log(String.format("Error at %d %d", x, y));
                            return null; // Or handle the error appropriately
                        }

                        conts.add(pts);
                        tV[tid] = 1;
                        termid.add(tid);
                    }
                }

                for (int idx : termid) {
                    tV[idx] = (byte) WHITE;
                }
            }
        }
        return conts;
    }

    public static void checkCtypes() {
        for (int i = 0; i < 256; i++) {
            IJ.log(i + " " + CVUTIL_NCELL_TYPE[i]);

            int b0 = i % 2;
            int b1 = (i >> 1) % 2;
            int b2 = (i >> 2) % 2;
            int b3 = (i >> 3) % 2;
            int b5 = (i >> 4) % 2;
            int b6 = (i >> 5) % 2;
            int b7 = (i >> 6) % 2;
            int b8 = (i >> 7) % 2;

            IJ.log(String.format("%d %d %d", b0, b1, b2));
            IJ.log(String.format("%d   %d", b3, b5)); // Added space for formatting
            IJ.log(String.format("%d %d %d", b6, b7, b8));
            IJ.log("");
        }
    }
}
