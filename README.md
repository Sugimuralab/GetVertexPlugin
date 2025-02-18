# GetVertex Plugin

GetVertex is a plugin for Fiji/ImageJ designed to analyze images that highlight the cell boundary. It reads binary, skeletonized images where white represents the cell boundary, and black represents the cell interior. The plugin detects vertices and junctions along the boundary and identifies the positions of these elements within the image. It then outputs a text file containing detailed information about the detected vertices, junctions, and cells, including their coordinates in the image.

# How to Use

1. Place `Get_Vertex.jar` directly in the `plugins` directory of Fiji/ImageJ.
2. Open Fiji/ImageJ.
3. Open a binary, skeletnoized image in Fiji/ImageJ.
4. Ensure that the input image is free of errors (see here for examples of errors).
5. Run `Get Vertex` from the `Plugins` menu.
6. Select the directory where you want to save the output.
7. The generated text file can be used as input for Bayesian force inference (Python, Google Colab) and Image-based parameter inference (Least-squares, Bayes).
