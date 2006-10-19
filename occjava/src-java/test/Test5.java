package test;

import java.util.HashMap;
import org.jcae.opencascade.jni.*;

/**
 * Workshop JINA 2006, Finite tilted Grid in front of a cavity
 * @author Jerome Robert and Guillaume Sylvand
 */
public class Test5
{
	// Size of the grid
	public final static int I_MAX=1;
	public final static int J_MAX=10;
	public final static int K_MAX=10;
	
	/**
	 * Used as index of edges of the grid  
	 */
	static class IntEdge
	{
		int i, j, k, i2, j2, k2;
		public IntEdge(int i, int j, int k, int i2, int j2, int k2)
		{
			this.i=i;
			this.j=j;
			this.k=k;
			this.i2=i2;
			this.j2=j2;
			this.k2=k2;

		}
		
		public boolean equals(Object obj)
		{
			IntEdge other=(IntEdge)obj;			
			return (i==other.i)&&(j==other.j)&&(k==other.k)
				&&(i2==other.i2)&&(j2==other.j2)&&(k2==other.k2);
		}
		
		public int hashCode()
		{
			return i+j+k+i2+j2+k2;
		}
	}
	
	/** Coordinates on the grid */
	static double[] coordinates(int i, int j, int k)
	{
		double x=-(k-5)*0.03*Math.sin(15*Math.PI/180)+(i-1)*0.01;
		double y=(j-5)*0.03;
		double z=(k-5)*0.03;
		return new double[]{x, y, z};
	}
	
	/** Easy creation of vertices */
	static TopoDS_Vertex createVertex(double x, double y, double z)
	{
		return (TopoDS_Vertex) new BRepBuilderAPI_MakeVertex(new double[]{x, y, z}).shape();
	}

	/** Easy creation of vertices */
	static TopoDS_Vertex createVertex(double[] coords)
	{
		return (TopoDS_Vertex) new BRepBuilderAPI_MakeVertex(coords).shape();
	}

	/** Easy creation of edges */
	static TopoDS_Edge createEdge(TopoDS_Vertex v1, TopoDS_Vertex v2)
	{
		return (TopoDS_Edge) new BRepBuilderAPI_MakeEdge(v1, v2).shape();
	}
	
	/** Easy creation of faces */
	static TopoDS_Face createFace(TopoDS_Edge e1, TopoDS_Edge e2, TopoDS_Edge e3, TopoDS_Edge e4)
	{
		TopoDS_Wire wirePlate=
			(TopoDS_Wire) new BRepBuilderAPI_MakeWire(e1, e2, e3, e4).shape();
		return (TopoDS_Face) new BRepBuilderAPI_MakeFace(wirePlate, true).shape();
	}
	
	/** Easy creation of faces */
	static TopoDS_Face createFace(TopoDS_Shape wire)
	{
		return (TopoDS_Face) new BRepBuilderAPI_MakeFace((TopoDS_Wire)wire, true).shape();
	}
	
	public static void main(String[] args)
	{
		HashMap int2edges=new HashMap();
		TopoDS_Vertex[][][] vertices=new TopoDS_Vertex[I_MAX+2][J_MAX+2][K_MAX+2];
		
		//The compound to return
		BRep_Builder bb=new BRep_Builder();
		TopoDS_Compound compound=new TopoDS_Compound();
		bb.makeCompound(compound);

		//Create vertices of the grid
		for(int i=0; i<I_MAX+2; i++)
		{
			for(int j=0; j<J_MAX+2; j++)
			{
				for(int k=0; k<K_MAX+2; k++)
					vertices[i][j][k]=createVertex(coordinates(i,j,k));
			}
		}

		//Create edges of the grid
		for(int i=0; i<I_MAX+1; i++)
		{
			for(int j=0; j<J_MAX+1; j++)
			{
				for(int k=0; k<K_MAX+1; k++)
				{					
					TopoDS_Edge edge1=createEdge(vertices[i][j][k], vertices[i+1][j][k]);
					int2edges.put(new IntEdge(i,j,k,i+1,j,k), edge1);
					TopoDS_Edge edge2=createEdge(vertices[i][j][k], vertices[i][j+1][k]);
					int2edges.put(new IntEdge(i,j,k,i,j+1,k), edge2);
					TopoDS_Edge edge3=createEdge(vertices[i][j][k], vertices[i][j][k+1]);
					int2edges.put(new IntEdge(i,j,k,i,j,k+1), edge3);
				}
			}
		}
		
		//Create faces of the grid
		for(int i=0; i<I_MAX; i++)
		for(int j=0; j<J_MAX+1; j++)
		for(int k=0; k<K_MAX+1; k++)
		{	
			{
				//face1
				TopoDS_Edge edge1=(TopoDS_Edge) int2edges.get(new IntEdge(i, j,  k, i, j+1, k));
				TopoDS_Edge edge2=(TopoDS_Edge) int2edges.get(new IntEdge(i, j+1,k, i+1, j+1, k));

				TopoDS_Edge edge3=(TopoDS_Edge) int2edges.get(new IntEdge(i+1, j,  k, i+1, j+1, k));

				TopoDS_Edge edge4=(TopoDS_Edge) int2edges.get(new IntEdge(i, j,  k, i+1, j,   k));				
				if(edge1!=null && edge2!=null && edge3!=null && edge4!=null)
				{
					bb.add(compound, createFace(edge1, edge2, edge3, edge4));
				}
			}
			{
				//face2
				TopoDS_Edge edge1=(TopoDS_Edge) int2edges.get(new IntEdge(i, j,  k, i, j, k+1));
				TopoDS_Edge edge2=(TopoDS_Edge) int2edges.get(new IntEdge(i, j,k+1, i+1, j, k+1));
				TopoDS_Edge edge3=(TopoDS_Edge) int2edges.get(new IntEdge(i+1, j,  k, i+1, j, k+1));
				TopoDS_Edge edge4=(TopoDS_Edge) int2edges.get(new IntEdge(i, j,  k, i+1, j,   k));				
				if(edge1!=null && edge2!=null && edge3!=null && edge4!=null)
				{
					bb.add(compound, createFace(edge1, edge2, edge3, edge4));
				}
			}			
		}
		
		// Face X-
		TopoDS_Vertex p1=createVertex(-0.15,0.15,0.15);
		TopoDS_Vertex p2=createVertex(-0.15,0.15,-0.15);
		TopoDS_Vertex p3=createVertex(-0.15,-0.15,-0.15);
		TopoDS_Vertex p4=createVertex(-0.15,-0.15,0.15);
		TopoDS_Edge e1=createEdge(p1,p2);
		TopoDS_Edge e2=createEdge(p2,p3);
		TopoDS_Edge e3=createEdge(p3,p4);
		TopoDS_Edge e4=createEdge(p4,p1);
		bb.add(compound, createFace(e1, e2, e3, e4));
		
		TopoDS_Edge eYpZp=createEdge(vertices[0][J_MAX][K_MAX], p1);
		TopoDS_Edge eYpZm=createEdge(vertices[0][J_MAX][0], p2);
		TopoDS_Edge eYmZm=createEdge(vertices[0][0][0], p3);
		TopoDS_Edge eYmZp=createEdge(vertices[0][0][K_MAX], p4);
		
		// Face Z-
		BRepBuilderAPI_MakeWire mw=new BRepBuilderAPI_MakeWire();
		for(int j=0; j<J_MAX ; j++)
		{
			TopoDS_Edge aEdge=(TopoDS_Edge) int2edges.get(new IntEdge(0, j,  0, 0, j+1, 0));
			mw.add(aEdge);
		} // end on 0,11,00
		mw.add(eYpZm); 
		mw.add(e2);
		mw.add((TopoDS_Edge) eYmZm.reversed());
		bb.add(compound, createFace(mw.shape()));
		
		// Face Z+
		mw=new BRepBuilderAPI_MakeWire();
		for(int j=0; j<J_MAX ; j++)
		{
			TopoDS_Edge aEdge=(TopoDS_Edge) int2edges.get(new IntEdge(0, j,  K_MAX, 0, j+1, K_MAX));
			mw.add(aEdge);
		} //end on 0, 11, 11
		mw.add(eYpZp); 
		mw.add((TopoDS_Edge) e4.reversed());
		mw.add((TopoDS_Edge) eYmZp.reversed());
		bb.add(compound, createFace(mw.shape()));

		// Face Y-
		mw=new BRepBuilderAPI_MakeWire();
		for(int k=0; k<K_MAX ; k++)
		{
			TopoDS_Edge aEdge=(TopoDS_Edge) int2edges.get(new IntEdge(0, 0,  k, 0, 0, k+1));
			mw.add(aEdge);
		} // end on 0,0,11
		mw.add(eYmZp); 
		mw.add((TopoDS_Edge) e3.reversed());
		mw.add((TopoDS_Edge) eYmZm.reversed());
		bb.add(compound, createFace(mw.shape()));
		
		// Face Y+
		mw=new BRepBuilderAPI_MakeWire();
		for(int k=0; k<K_MAX ; k++)
		{
			TopoDS_Edge aEdge=(TopoDS_Edge) int2edges.get(new IntEdge(0, J_MAX,  k, 0, J_MAX, k+1));
			mw.add(aEdge);
		} // end on 0,11,11
		mw.add(eYpZp); 
		mw.add(e1);
		mw.add((TopoDS_Edge) eYpZm.reversed());
		bb.add(compound, createFace(mw.shape()));
		
		BRepTools.write(compound, "/tmp/JINA-2006-6.brep");
	}
}
