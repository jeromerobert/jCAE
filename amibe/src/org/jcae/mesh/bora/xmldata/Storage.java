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

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.Vertex2D;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.cad.CADGeomCurve3D;
import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.mesher.ds.SubMesh1D;
import org.jcae.mesh.mesher.ds.MNode1D;
import org.jcae.mesh.mesher.ds.MEdge1D;
import org.jcae.mesh.xmldata.UNVConverter;
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
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TIntObjectHashMap;
import org.apache.log4j.Logger;

public class Storage
{
	private static Logger logger=Logger.getLogger(Storage.class);

	public static void writeEdge(BCADGraphCell edge, String outDir)
	{
		CADEdge E = (CADEdge) edge.getShape();
		if (E.isDegenerated())
			return;
		SubMesh1D submesh = (SubMesh1D) edge.mesh;
		if (null == submesh)
			return;

		try
		{
			File dir = new File(outDir);

			// Create the output directory if it does not exist
			if(!dir.exists())
				dir.mkdirs();

			List nodelist = submesh.getNodes();
			// Write node references and compute local indices
			TObjectIntHashMap localIdx = write1dNodeReferences(outDir, edge.getId(), nodelist, edge);
			// Write node coordinates
			write1dCoordinates(outDir, edge.getId(), nodelist, CADShapeBuilder.factory.newCurve3D(E));
			// Write edge connectivity
			write1dEdges(outDir, edge.getId(), submesh.getEdges(), localIdx);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException();
		}
	}

	public static void writeFace(BCADGraphCell face, String outDir)
	{
		Mesh submesh = (Mesh) face.mesh;
		if (null == submesh)
			return;

		try
		{
			File dir = new File(outDir);

			// Create the output directory if it does not exist
			if(!dir.exists())
				dir.mkdirs();

			CADFace F = (CADFace) face.getShape();
			Collection trianglelist = submesh.getTriangles();
			List nodelist = submesh.quadtree.getAllVertices(trianglelist.size() / 2);
			TObjectIntHashMap localIdx = write2dNodeReferences(outDir, face.getId(), nodelist);
			write2dCoordinates(outDir, face.getId(), nodelist, F.getGeomSurface());
			write2dTriangles(outDir, face.getId(), trianglelist, localIdx);
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
	public static Mesh readAllFaces(BCADGraphCell root)
	{
		Mesh m = new Mesh();
		m.setType(Mesh.MESH_3D);
		TIntObjectHashMap vertMap = new TIntObjectHashMap();
		for (Iterator it = root.uniqueShapesExplorer(CADShapeEnum.FACE); it.hasNext(); )                                                        
			readFace(m, (BCADGraphCell) it.next(), vertMap);
		return m;
	}

	/**
	 * Append a discretized face into a Mesh instance.
	 * @param mesh    original mesh
	 * @param root    cell graph containing a CAD face
	 * @param mapRefVertex    map between references and Vertex instances
	 * @throws  RuntimeException if an error occurred
	 */
	public static void readFace(Mesh mesh, BCADGraphCell root, TIntObjectHashMap mapRefVertex)
	{
		assert root.getShape() instanceof CADFace;
		BModel model = root.getGraph().getModel();
		String dir = model.getOutputDir()+File.separator+model.get2dDir();
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
			// Read vertex references
			int [] refs = read2dNodeReferences(dir, id);
			// Create a Vertex array, amd insert new references
			// into mapRefVertex.
			Vertex [] nodelist = read2dCoordinates(dir, id, mesh, refs, mapRefVertex);
			// Read triangles and appends them to the mesh.
			read2dTriangles(dir, id, mesh, reversed, nodelist);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		logger.debug("end reading cell "+id);
	}

	private static TObjectIntHashMap write1dNodeReferences(String dir, int id, List nodelist, BCADGraphCell edge)
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

	private static TObjectIntHashMap write2dNodeReferences(String dir, int id, List nodelist)
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
			if (n == Vertex2D.outer)
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

	private static void write1dCoordinates(String dir, int id, List nodelist, CADGeomCurve3D curve)
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

	private static void write2dCoordinates(String dir, int id, List nodelist, CADGeomSurface surface)
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
		for (Iterator itn = nodelist.iterator(); itn.hasNext(); )
		{
			Vertex n = (Vertex) itn.next();
			if (n == Vertex2D.outer)
				continue;
			double [] p = n.getUV();
			for (int d = 0; d < p.length; d++)
				parasout.writeDouble(p[d]);
			double [] xyz = surface.value(p[0], p[1]);
			for (int k = 0; k < 3; k++)
				nodesout.writeDouble(xyz[k]);
		}
		nodesout.close();
		parasout.close();
	}

	private static void write1dEdges(String dir, int id, List edgelist, TObjectIntHashMap localIdx)
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

	private static void write2dTriangles(String dir, int id, Collection trianglelist, TObjectIntHashMap localIdx)
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
			for (int j = 0; j < 3; j++)
				facesout.writeInt(localIdx.get(f.vertex[j]));
		}
		facesout.close();
	}

	private static int [] read2dNodeReferences(String dir, int id)
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
		UNVConverter.clean(bbR);
		return refs;
	}

	private static Vertex [] read2dCoordinates(String dir, int id, Mesh mesh, int [] refs, TIntObjectHashMap mapRefVertex)
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
			nodelist[i] = Vertex.valueOf(mesh, coord);
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
		UNVConverter.clean(bbN);
		logger.debug("end reading "+dir+File.separator+"n"+id);
		return nodelist;
	}

	private static void read2dTriangles(String dir, int id, Mesh mesh, boolean reversed, Vertex [] nodelist)
		throws IOException, FileNotFoundException
	{
		File trianglesFile = new File(dir, "f"+id);
		FileChannel fcT = new FileInputStream(trianglesFile).getChannel();
		MappedByteBuffer bbT = fcT.map(FileChannel.MapMode.READ_ONLY, 0L, fcT.size());
		IntBuffer trianglesBuffer = bbT.asIntBuffer();

		int numberOfTriangles = (int) trianglesFile.length() / 12;
		logger.debug("Reading "+numberOfTriangles+" elements");
		Triangle face;
		for (int i=0; i < numberOfTriangles; i++)
		{
			Vertex pt1 = nodelist[trianglesBuffer.get()-1];
			Vertex pt2 = nodelist[trianglesBuffer.get()-1];
			Vertex pt3 = nodelist[trianglesBuffer.get()-1];
			if (!reversed)
				face = new Triangle(pt1, pt2, pt3);
			else
				face = new Triangle(pt1, pt3, pt2);
			mesh.add(face);
			pt1.setLink(face);
			pt2.setLink(face);
			pt3.setLink(face);
		}
		fcT.close();
		UNVConverter.clean(bbT);
	}
}

