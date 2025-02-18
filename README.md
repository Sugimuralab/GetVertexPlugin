# GetVertex Plugin

GetVertex is a plugin for ImageJ/Fiji designed to analyze images with distinct cell boundaries. It reads binary images where white represents the cell boundary, and black represents the cell interior. The plugin detects vertices and edges along the boundary and identifies the positions of these elements within the image. It then outputs a text file containing detailed information about the detected vertices, edges, and cells, including their coordinates in the image space.

# How to Use

1. Place `Get_Vertex.jar` directly in the `plugins` directory of Fiji/ImageJ.
2. Open Fiji/ImageJ.
3. Open a binary, skeletnoized image in Fiji/ImageJ.
4. Run `Get Vertex` from the `Plugins` menu.
5. Select the directory where you want to save the output.
