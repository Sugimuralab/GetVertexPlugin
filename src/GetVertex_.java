


import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.SaveDialog;
import ij.process.ImageProcessor;
import ij.io.FileSaver;
import ij.gui.Roi;
import java.awt.Point;
import java.io.File;
import java.util.List;


public class GetVertex_ implements ij.plugin.PlugIn {

    // Parameters
    private static final boolean CROP = true;           // Enable cropping
    private static final int MINIMAL_CELL_SIZE = 4;     // Minimum cell area
    private static final int WAITING_TIME = 1500;       // Waiting time in ms (unused in this example)

    @Override
    public void run(String arg) {
        // Get the current image
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.error("No image open.");
            return;
        }

        // Log summary of inputs
        IJ.log("# CROP? " + CROP + " (false: off / true: on)");
        IJ.log("# minimal_cell_size " + MINIMAL_CELL_SIZE);
        IJ.log("# waiting_time " + WAITING_TIME + " (Duration time to show images. Not important for output.)");

        // Extract filename without extension
        String imageTitle = imp.getTitle();
        String title = imageTitle.contains(".") ? imageTitle.substring(0, imageTitle.lastIndexOf('.')) : imageTitle;
        IJ.log("# filename (w/o path): " + title);

        // Get image stack
        int stackSize = imp.getStackSize();
        IJ.log("Image size ");
        IJ.log(" Frame = " + stackSize);
        IJ.log(" , W x H = " + imp.getWidth() + " x " + imp.getHeight());
        String directory = "";
        String filename_index = "";
        // Process each frame
        for (int num = 1; num <= stackSize; num++) {
            
            if(stackSize >= 2){
                filename_index = "_" + String.format("%04d", num);
            }

            IJ.log("## frame: " + num + " / " + stackSize);

            // Get ImageProcessor for the current frame
            ImageProcessor ip = imp.getStack().getProcessor(num);
            ImagePlus currentFrame = new ImagePlus("Slice " + num, ip);

            // Crop the image
            ImageUtils_.Pair<ImagePlus, Point> croppedResult = ImageUtils_.utlCropImage(currentFrame, CROP);
            ImagePlus croppedImage = croppedResult.first;
            Point cpt = croppedResult.second;
            IJ.log(" img: W x H = " + croppedImage.getWidth() + " x " + croppedImage.getHeight());

            // Display cropped image
            // croppedImage.show();

            // Check for four-block patterns
            if(ImageUtils_.utlCheckFourBlock(croppedImage.getProcessor(), num, cpt)){
                return;
            }

            // Boundary processing
            IJ.log(" > Start Boundary Deletion");
            ImageProcessor boundaryProcessedIP = ImageUtils_.utlBoundaryProcessing(croppedImage, cpt);
            if(boundaryProcessedIP == null){
                return;
            }
            croppedImage.setProcessor(boundaryProcessedIP);
            IJ.log(" ... Finish Boundary Deletion");

            // Show and save the boundary-processed image
            IJ.log(" Show input images, surroundings are processed");
            // croppedImage.show();
            String bmpFilename = title + filename_index + ".bmp";

            if(num == 1){
                SaveDialog sd = new SaveDialog("Save Output", bmpFilename, ".bmp");
                
                // Get the directory and file name from the save dialog
                directory = sd.getDirectory();
            }
            FileSaver bmpSaver = new FileSaver(croppedImage);
            bmpSaver.saveAsBmp(directory + bmpFilename);
            IJ.log(" Saved boundary-processed image as: " + directory + bmpFilename);

            // Get Vertex properties
            IJ.log(" > Start Getting Vertex properties");
            ImageUtils_.Triple<List<VCell_>, List<Vertex_>, List<Edge_>> vertexResult =
                ImageUtils_.vxSet_Vertex(croppedImage, MINIMAL_CELL_SIZE, cpt);
            if(vertexResult == null){
                return;
            }
            List<VCell_> cells = vertexResult.first;
            List<Vertex_> junctions = vertexResult.second;
            List<Edge_> edges = vertexResult.third;
            IJ.log(" ... Finish Getting Vertex properties");

            // Draw Polygon (Optional: Visualize edges)
            ImageUtils_.vxDraw_Polygon(edges, croppedImage, directory + title + "_Polygon_Frame_" + String.format("%04d", num) + ".png", cpt);
            IJ.log(" ... Draw Polygon and save as PNG");

            // Output data to file
            String outputFilename = title + filename_index + ".txt";

            ImageUtils_.vxOutputDatas(directory + outputFilename, junctions, edges, cells, cpt);
            IJ.log(" > Output data file: " + directory + outputFilename);

            // Draw Vertex image and save
            String vertexImageFilename = "Vertex_" + title + filename_index + ".png";
            ImageUtils_.vxDraw_Vertex(ip, edges, directory + vertexImageFilename, WAITING_TIME, cpt);
            IJ.log(" > Output vertex image: " + directory + vertexImageFilename);

            // Optional: Wait for user input or delay
            // IJ.wait(WAITING_TIME); // Uncomment if you want to add a delay
            IJ.log("");
        }

        IJ.log("Processing completed.");
    }
}
