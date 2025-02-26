javac -classpath ..\..\..\..\..\ij.jar -d .\classes\ GetVertex_.java ImageUtils_.java Edge_.java Vertex_.java VCell_.java CVUtil_.java
:: javac -cp ..\..\jars\* -d .\classes\ get_vertex\GetVertex.java get_vertex\ImageUtils.java get_vertex\Edge.java get_vertex\Junction.java get_vertex\VCell.java get_vertex\CVUtil.java

jar cf Get_Vertex.jar -C .\classes\ .
copy /y .\Get_Vertex.jar ..\..\..\
move /y .\Get_Vertex.jar ..\

"..\..\..\..\..\ImageJ.exe"