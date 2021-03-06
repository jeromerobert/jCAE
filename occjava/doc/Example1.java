import org.jcae.opencascade.jni.*;

/**
 * Create a square with an attached free edge
 * @author Jerome Robert
 */
public class Example1
{
	public static void main(String[] args)
	{
		// The plate
		double[] p1=new double[]{0, 0, 0};
		double[] p2=new double[]{0, 1, 0};
		double[] p3=new double[]{1, 1, 0};
		double[] p4=new double[]{1, 0, 0};
		double[] p5=new double[]{0.5, 0.5, 0};
		double[] p6=new double[]{0.5, 0.5, 1};
		
		TopoDS_Edge edge1=(TopoDS_Edge) new BRepBuilderAPI_MakeEdge(p1,p2).shape();
		TopoDS_Edge edge2=(TopoDS_Edge) new BRepBuilderAPI_MakeEdge(p2,p3).shape();
		TopoDS_Edge edge3=(TopoDS_Edge) new BRepBuilderAPI_MakeEdge(p3,p4).shape();
		TopoDS_Edge edge4=(TopoDS_Edge) new BRepBuilderAPI_MakeEdge(p4,p1).shape();
		
		TopoDS_Wire wirePlate=
			(TopoDS_Wire) new BRepBuilderAPI_MakeWire(edge1, edge2, edge3, edge4).shape();
		
		TopoDS_Face face=(TopoDS_Face) new BRepBuilderAPI_MakeFace(wirePlate, true).shape();
		
		// The wire
		TopoDS_Vertex vertex1=(TopoDS_Vertex) new BRepBuilderAPI_MakeVertex(p5).shape();
		TopoDS_Vertex vertex2=(TopoDS_Vertex) new BRepBuilderAPI_MakeVertex(p6).shape();
		TopoDS_Edge freeEdge=(TopoDS_Edge) new BRepBuilderAPI_MakeEdge(vertex1,vertex2).shape();
		
		//Connect the wire to the plate
		BRep_Builder bb=new BRep_Builder();
		bb.add(face, vertex1);
		
		//Put everything in a compound
		TopoDS_Compound compound=new TopoDS_Compound();
		bb.makeCompound(compound);
		bb.add(compound, freeEdge);
		bb.add(compound, face);
		
		//Write to to a file
		BRepTools.write(compound, "/tmp/test.brep");
	}
}