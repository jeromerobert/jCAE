/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2006, by EADS CRC
 
    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.
 
    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.
 
    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.bora.xmldata;

import org.jcae.mesh.amibe.ds.VolMesh;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADSolid;
import org.jcae.mesh.cad.CADGeomCurve3D;
import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.mesher.ds.SubMesh1D;
import org.jcae.mesh.mesher.ds.MNode1D;
import org.jcae.mesh.mesher.ds.MEdge1D;
import org.jcae.mesh.xmldata.MeshExporter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Iterator;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TIntObjectHashMap;
import org.apache.log4j.Logger;

public class Storage
{
	private static Logger logger=Logger.getLogger(Storage.class);
	static final String dir1d = "1d";
	static final String dir2d = "2d";
	static final String dir3d = "3d";

	public static void writeEdge(BDiscretization d, String outDir)
	{
		BCADGraphCell edge = d.getGraphCell();
		CADEdge E = (CADEdge) edge.getShape();
		if (E.isDegenerated())
			return;
		SubMesh1D submesh = (SubMesh1D) d.getMesh();
		if (null == submesh)
			return;

		try
		{
			File dir = new File(outDir, dir1d);

			// Create the output directory if it does not exist
			if(!dir.exists())
				dir.mkdirs();

			Collection nodelist = submesh.getNodes();
			// Write node references and compute local indices
			TObjectIntHashMap localIdx = write1dNodeReferences(dir, edge.getId(), nodelist, edge);
			// Write node coordinates
			write1dCoordinates(dir, edge.getId(), nodelist, CADShapeBuilder.factory.newCurve3D(E));
			// Write edge connectivity
			write1dEdges(dir, edge.getId(), submesh.getEdges(), localIdx);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException();
		}
	}

	public static void writeFace(BDiscretization d, String outDir)
	{
		BCADGraphCell face = d.getGraphCell();
		Mesh2D submesh = (Mesh2D) d.getMesh();
		if (null == submesh)
			return;

		try
		{
			File dir = new File(outDir, dir2d);

			// Create the output directory if it does not exist
			if(!dir.exists())
				dir.mkdirs();

			CADFace F = (CADFace) face.getShape();
			Collection trianglelist = submesh.getTriangles();
			Collection nodelist = submesh.quadtree.getAllVertices(trianglelist.size() / 2);
			TObjectIntHashMap localIdx = write2dNodeReferences(dir, face.getId(), nodelist, submesh.outerVertex);
			write2dCoordinates(dir, face.getId(), nodelist, submesh.outerVertex, F.getGeomSurface());
			write2dTriangles(dir, face.getId(), trianglelist, localIdx);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException();
		}
	}

	public static void writeVolume(BDiscretization d, String outDir)
	{
		BCADGraphCell solid = d.getGraphCell();
		VolMesh submesh = (VolMesh) d.getMesh();
		if (null == submesh)
			return;

		try
		{
			File dir = new File(outDir, dir3d);

			// Create the output directory if it does not exist
			if(!dir.exists())
				dir.mkdirs();

			Collection nodelist = submesh.getNodes();
			TObjectIntHashMap localIdx = write2dNodeReferences(dir, solid.getId(), nodelist, submesh.outerVertex);
			write2dCoordinates(dir, solid.getId(), nodelist, submesh.outerVertex, null);
			write2dTriangles(dir, solid.getId(), submesh.getTriangles(), localIdx);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException();
		}
	}

	/**
	 * Creates a Mesh instance by reading all faces.
	 * @param root    root shape
	 * @return a Mesh instance
	 * @throws  RuntimeException if an error occurred
	 */
	public static Mesh readAllFaces(BCADGraphCell root, BSubMesh s)
	{
		Mesh m = new Mesh();
		TIntObjectHashMap vertMap = new TIntObjectHashMap();
		for (Iterator it = root.uniqueShapesExplorer(CADShapeEnum.FACE); it.hasNext(); )
			readFace(m, (BCADGraphCell) it.next(), s, vertMap);
		return m;
	}

	/**
	 * Append a discretized face into a Mesh instance.
	 * @param mesh    original mesh
	 * @param root    cell graph containing a CAD face
	 * @param mapRefVertex    map between references and Vertex instances
	 * @throws  RuntimeException if an error occurred
	 */
	public static void readFace(Mesh mesh, BCADGraphCell root, BSubMesh s, TIntObjectHashMap mapRefVertex)
	{
		assert root.getShape() instanceof CADFace;
		BModel model = root.getGraph().getModel();
		boolean reversed = false;
		if (root.getOrientation() != 0)
		{
			reversed = true;
			if (root.getReversed() != null)
				root = root.getReversed();
		}
		if (null == root.getDiscretizationSubMesh(s))
			return;
		int id = root.getId();
		try
		{
			File dir = new File(model.getOutputDir(s), dir2d);
			// Read vertex references
			int [] refs = read2dNodeReferences(dir, id);
			// Create a Vertex array, amd insert new references
			// into mapRefVertex.
			Vertex [] nodelist = read2dCoordinates(dir, id, mesh, refs, mapRefVertex);
			// Read triangles and appends them to the mesh.
			read2dTriangles(dir, id, 3, mesh, reversed, nodelist);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		logger.debug("end reading cell "+id);
	}

	/**
	 * Creates a VolMesh instance by reading all volumes.
	 * @param root    root shape
	 * @return a VolMesh instance
	 * @throws  RuntimeException if an error occurred
	 */
	public static VolMesh readAllVolumes(BCADGraphCell root, BSubMesh s)
	{
		VolMesh m = new VolMesh();
		TIntObjectHashMap vertMap = new TIntObjectHashMap();
		for (Iterator it = root.uniqueShapesExplorer(CADShapeEnum.SOLID); it.hasNext(); )
			readVolume(m, (BCADGraphCell) it.next(), s, vertMap);
		return m;
	}

	/**
	 * Append a discretized solid into a VolMesh instance.
	 * @param mesh    original mesh
	 * @param root    cell graph containing a CAD solid
	 * @param mapRefVertex    map between references and Vertex instances
	 * @throws  RuntimeException if an error occurred
	 */
	public static void readVolume(VolMesh mesh, BCADGraphCell root, BSubMesh s, TIntObjectHashMap mapRefVertex)
	{
		assert root.getShape() instanceof CADSolid;
		BModel model = root.getGraph().getModel();
		boolean reversed = false;
		if (root.getOrientation() != 0)
		{
			reversed = true;
			if (root.getReversed() != null)
				root = root.getReversed();
		}
		int id = root.getId();
		try
		{
			File dir = new File(model.getOutputDir(s), dir3d);
			// Read vertex references
			int [] refs = read2dNodeReferences(dir, id);
			// Create a Vertex array, amd insert new references
			// into mapRefVertex.
			Vertex [] nodelist = read2dCoordinates(dir, id, mesh, refs, mapRefVertex);
			for (int i = 0, n = nodelist.length; i < n; i++)
				mesh.add(nodelist[i]);
			// Read triangles and appends them to the mesh.
			read2dTriangles(dir, id, 4, mesh, reversed, nodelist);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		logger.debug("end reading cell "+id);
	}

	private static TObjectIntHashMap write1dNodeReferences(File dir, int id, Collection nodelist, BCADGraphCell edge)
		throws IOException, FileNotFoundException
	{
		File refFile = new File(dir, "r"+id);
		if(refFile.exists())
			refFile.delete();
		
		// Save references
		logger.debug("begin writing "+refFile);
		DataOutputStream refsout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(refFile, true)));
		TObjectIntHashMap localIdx = new TObjectIntHashMap(nodelist.size());

		int i = 0;
		for (Iterator itn = nodelist.iterator(); itn.hasNext(); )
		{
			MNode1D n = (MNode1D) itn.next();
			// Set first index to 1; a null index in
			// localIdx is thus an error
			localIdx.put(n, i+1);
			CADVertex v = n.getCADVertex();
			if (null != v)
			{
				// TODO: replace this id by Vertex.getRef()
				BCADGraphCell vv = edge.getGraph().getByShape(v);
				refsout.writeInt(i);
				refsout.writeInt(vv.getId());
			}
			i++;
		}
		refsout.close();
		return localIdx;
	}

	private static TObjectIntHashMap write2dNodeReferences(File dir, int id, Collection nodelist, Vertex outer)
		throws IOException, FileNotFoundException
	{
		File refFile = new File(dir, "r"+id);
		if(refFile.exists())
			refFile.delete();

		// Save references
		logger.debug("begin writing "+refFile);
		DataOutputStream refsout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(refFile, true)));

		TObjectIntHashMap localIdx = new TObjectIntHashMap(nodelist.size());
		int i = 0;
		for (Iterator itn = nodelist.iterator(); itn.hasNext(); )
		{
			Vertex n = (Vertex) itn.next();
			if (n == outer)
				continue;
			// Set first index to 1; a null index in
			// localIdx is thus an error
			localIdx.put(n, i+1);
			int ref1d = n.getRef();
			if (ref1d != 0)
			{
				refsout.writeInt(i);
				refsout.writeInt(Math.abs(ref1d));
			}
			i++;
		}
		refsout.close();
		return localIdx;
	}

	private static void write1dCoordinates(File dir, int id, Collection nodelist, CADGeomCurve3D curve)
		throws IOException, FileNotFoundException
	{
		File nodesFile = new File(dir, "n"+id);
		if(nodesFile.exists())
			nodesFile.delete();
		File parasFile = new File(dir, "p"+id);
		if(parasFile.exists())
			parasFile.delete();
		
		logger.debug("begin writing "+nodesFile+" and "+parasFile);
		DataOutputStream nodesout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(nodesFile, true)));
		DataOutputStream parasout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(parasFile, true)));
		for (Iterator itn = nodelist.iterator(); itn.hasNext(); )
		{
			MNode1D n = (MNode1D)itn.next();
			double p = n.getParameter();
			parasout.writeDouble(p);
			double [] xyz = curve.value(p);
			for (int k = 0; k < 3; k++)
				nodesout.writeDouble(xyz[k]);
		}
		nodesout.close();
		parasout.close();
	}

	private static void write2dCoordinates(File dir, int id, Collection nodelist, Vertex outer, CADGeomSurface surface)
		throws IOException, FileNotFoundException
	{
		File nodesFile = new File(dir, "n"+id);
		if(nodesFile.exists())
			nodesFile.delete();
		File parasFile = new File(dir, "p"+id);
		if(parasFile.exists())
			parasFile.delete();

		// Save nodes
		logger.debug("begin writing "+nodesFile+" and "+parasFile);
		DataOutputStream nodesout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(nodesFile, true)));
		DataOutputStream parasout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(parasFile, true)));
		double [] xyz;
		for (Iterator itn = nodelist.iterator(); itn.hasNext(); )
		{
			Vertex n = (Vertex) itn.next();
			if (n == outer)
				continue;
			if (surface == null)
				xyz = n.getUV();
			else
			{
				double [] p = n.getUV();
				for (int d = 0; d < p.length; d++)
					parasout.writeDouble(p[d]);
				xyz = surface.value(p[0], p[1]);
			}
			for (int k = 0; k < 3; k++)
				nodesout.writeDouble(xyz[k]);
		}
		nodesout.close();
		parasout.close();
	}

	private static void write1dEdges(File dir, int id, Collection edgelist, TObjectIntHashMap localIdx)
		throws IOException, FileNotFoundException
	{
		File beamsFile=new File(dir, "b"+id);
		if(beamsFile.exists())
			beamsFile.delete();
		
		logger.debug("begin writing "+beamsFile);
		DataOutputStream beamsout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(beamsFile, true)));
		for (Iterator ite = edgelist.iterator(); ite.hasNext(); )
		{
			MEdge1D e = (MEdge1D) ite.next();
			MNode1D pt1 = e.getNodes1();
			MNode1D pt2 = e.getNodes2();
			beamsout.writeInt(localIdx.get(pt1));
			beamsout.writeInt(localIdx.get(pt2));
		}
		beamsout.close();
	}

	private static void write2dTriangles(File dir, int id, Collection trianglelist, TObjectIntHashMap localIdx)
		throws IOException, FileNotFoundException
	{
		File facesFile=new File(dir, "f"+id);
		if(facesFile.exists())
			facesFile.delete();

		// Save faces
		logger.debug("begin writing "+facesFile);
		DataOutputStream facesout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(facesFile, true)));
		for (Iterator itf = trianglelist.iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			for (int j = 0, n = f.vertex.length; j < n; j++)
				facesout.writeInt(localIdx.get(f.vertex[j]));
		}
		facesout.close();
	}

	private static int [] read2dNodeReferences(File dir, int id)
		throws IOException, FileNotFoundException
	{
		File refFile = new File(dir, "r"+id);
		FileChannel fcR = new FileInputStream(refFile).getChannel();
		MappedByteBuffer bbR = fcR.map(FileChannel.MapMode.READ_ONLY, 0L, fcR.size());
		IntBuffer refsBuffer = bbR.asIntBuffer();

		int numberOfReferences = (int) refFile.length() / 4;
		int [] refs = new int[numberOfReferences];
		refsBuffer.get(refs);
		fcR.close();
		MeshExporter.clean(bbR);
		return refs;
	}

	private static Vertex [] read2dCoordinates(File dir, int id, Mesh mesh, int [] refs, TIntObjectHashMap mapRefVertex)
		throws IOException, FileNotFoundException
	{
		File nodesFile = new File(dir, "n"+id);
		FileChannel fcN = new FileInputStream(nodesFile).getChannel();
		MappedByteBuffer bbN = fcN.map(FileChannel.MapMode.READ_ONLY, 0L, fcN.size());
		DoubleBuffer nodesBuffer = bbN.asDoubleBuffer();
		
		int numberOfNodes = (int) nodesFile.length() / 24;
		int numberOfReferences = refs.length / 2;
		Vertex [] nodelist = new Vertex[numberOfNodes];
		double [] coord = new double[3];
		logger.debug("Reading "+numberOfNodes+" nodes");
		for (int i = 0; i < numberOfNodes; i++)
		{
			nodesBuffer.get(coord);
			nodelist[i] = (Vertex) mesh.factory.createVertex(coord);
		}
		for (int i = 0; i < numberOfReferences; i++)
		{
			int ind = refs[2*i];
			int label = refs[2*i+1];
			Object o = mapRefVertex.get(label);
			if (o == null)
				mapRefVertex.put(label, nodelist[ind]);
			else
				nodelist[ind] = (Vertex) o;
			nodelist[ind].setRef(label);
		}
		fcN.close();
		MeshExporter.clean(bbN);
		logger.debug("end reading "+dir+File.separator+"n"+id);
		return nodelist;
	}

	private static void read2dTriangles(File dir, int id, int nr, Mesh mesh, boolean reversed, Vertex [] nodelist)
		throws IOException, FileNotFoundException
	{
		File trianglesFile = new File(dir, "f"+id);
		FileChannel fcT = new FileInputStream(trianglesFile).getChannel();
		MappedByteBuffer bbT = fcT.map(FileChannel.MapMode.READ_ONLY, 0L, fcT.size());
		IntBuffer trianglesBuffer = bbT.asIntBuffer();

		int numberOfTriangles = (int) trianglesFile.length() / (4*nr);
		logger.debug("Reading "+numberOfTriangles+" elements");
		Triangle face;
		Vertex [] pts = new Vertex[nr];
		for (int i = 0; i < numberOfTriangles; i++)
		{
			for (int j = 0; j < nr; j++)
				pts[j] = nodelist[trianglesBuffer.get()-1];
			if (reversed)
			{
				Vertex temp = pts[1];
				pts[1] = pts[2];
				pts[2] = temp;
			}
			face = (Triangle) mesh.factory.createTriangle(pts);
			mesh.add(face);
			for (int j = 0; j < nr; j++)
				pts[j].setLink(face);
		}
		fcT.close();
		MeshExporter.clean(bbT);
	}
}

