/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
	Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>
 
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

package org.jcae.mesh.mesher.ds;

import org.jcae.mesh.cad.*;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.io.*;
import org.apache.log4j.Logger;

/**
 * 2D discretization of a single topological surface.
 * A <code>SubMesh2D</code> instance is created by projecting a
 * <code>MMesh1D</code> instance (which is the mesh boundary) to a 2D plane,
 * and by meshing its interior.
 */

public class SubMesh2D
{
	private static Logger logger=Logger.getLogger(SubMesh2D.class);	

	//  Topological face on which mesh is applied
	private CADFace face;
	
	//  The geometrical surface describing the topological face, stored for
	//  efficiebcy reason
	private CADGeomSurface surface;
	
	//  Set of faces
	private HashSet faceset = new HashSet();
	
	//  Set of nodes
	private HashSet nodeset = new HashSet();
	
	//  Set to false when outer elements are removed
	public boolean checkInvertedTriangles = false;
	
	/**
	 * Creates an empty mesh bounded to the topological surface.
	 * Also sets <code>epsilon</code> to the bounding box diagonal 
	 * over 1000.
	 *
	 * @param  F   the topological face to mesh.
	 */
	public SubMesh2D(CADFace F)
	{
		face = F;
		surface = face.getGeomSurface();
	}
	
	/**
	 * Returns the topological face.
	 *
	 * @return the topological face.
	 */
	public CADShape getGeometry()
	{
		return face;
	}
	
	/**
	 * Returns the geometrical surface.
	 *
	 * @return the geometrical surface.
	 */
	public CADGeomSurface getGeomSurface()
	{
		return surface;
	}
	
	/**
	 * Returns the set of faces.
	 *
	 * @return the set of faces.
	 */
	public HashSet getFaces()
	{
		return faceset;
	}
	
	/**
	 * Returns the set of nodes.
	 *
	 * @return the set of nodes.
	 */
	public HashSet getNodes()
	{
		return nodeset;
	}
	
	/**
	 * Returns an iterator over the set of faces.
	 *
	 * @return an iterator over the set of faces.
	 */
	public Iterator getFacesIterator()
	{
		return faceset.iterator();
	}
	
	/**
	 * Returns an iterator over the set of nodes.
	 *
	 * @return an iterator over the set of nodes.
	 */
	public Iterator getNodesIterator()
	{
		return nodeset.iterator();
	}
	
	/**
	 * Adds a triangle defined by its 3 vertices.
	 *
	 * @param n1  first node
	 * @param n2  second node
	 * @param n3  third node
	 * @return a newly created triangle
	 */
	public MFace2D addTriangle(MNode2D n1, MNode2D n2, MNode2D n3)
	{
		nodeset.add(n1);
		nodeset.add(n2);
		nodeset.add(n3);
		MFace2D f = new MFace2D(n1, n2, n3);
		faceset.add(f);
		return f;
	}
	
	/**
	 * Remove degenerted edges.
	 */
	public void removeDegeneratedEdges()
	{
	}
	
	/**
	 * Writes current mesh to an UNV file
	 *
	 * @param file   UNV file name.
	 */
	public void writeUNV(String file)
	{
		String cr=System.getProperty("line.separator");
		PrintWriter out;
		try {
			if (file.endsWith(".gz") || file.endsWith(".GZ"))
				out = new PrintWriter(new java.util.zip.GZIPOutputStream(new FileOutputStream(file)));
			else
				out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)));
			out.println("    -1"+cr+"  2411");
			HashMap labels = new HashMap(nodeset.size());
			int count =  0;
			for(Iterator it=nodeset.iterator();it.hasNext();)
			{
				MNode2D node=(MNode2D) it.next();
				count++;
				Integer label = new Integer(node.getID());
				labels.put(node, label);
				out.println(label+"         1         1         1");
				out.println(""+node.getU()+" "+node.getV()+" 0.0");
			}
			out.println("    -1");
			out.println("    -1"+cr+"  2412");
			count =  0;
			for(Iterator it=faceset.iterator();it.hasNext();)
			{
				MFace2D face=(MFace2D)it.next();
				count++;
				out.println(""+count+"        91         1         1         1         3");
				for(Iterator itn=face.getNodesIterator();itn.hasNext();)
				{
					MNode2D node=(MNode2D) itn.next();
					Integer nodelabel =  (Integer) labels.get(node);
					out.print(" "+nodelabel.intValue());
				}
				out.println("");
			}
			out.println("    -1");
			out.close();
		} catch (FileNotFoundException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		} catch (IOException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes current mesh to an UNV file
	 *
	 * @param file   UNV file name.
	 */
	public void writeUNV3D(String file)
	{
		String cr=System.getProperty("line.separator");
		PrintWriter out;
		try {
			if (file.endsWith(".gz") || file.endsWith(".GZ"))
				out = new PrintWriter(new java.util.zip.GZIPOutputStream(new FileOutputStream(file)));
			else
				out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)));
			out.println("    -1"+cr+"  2411");
			HashMap labels = new HashMap(nodeset.size());
			int count =  0;
			for(Iterator it=nodeset.iterator();it.hasNext();)
			{
				MNode2D node=(MNode2D) it.next();
				MNode3D n3=new MNode3D(node, surface);
				count++;
				//Integer label = new Integer(count);
				Integer label = new Integer(node.getID());
				labels.put(node, label);
				out.println(label+"         1         1         1");
				out.println(""+n3.getX()+" "+n3.getY()+" "+n3.getZ());
			}
			out.println("    -1");
			out.println("    -1"+cr+"  2412");
			count =  0;
			for(Iterator it=faceset.iterator();it.hasNext();)
			{
				MFace2D face=(MFace2D)it.next();
				count++;
				out.println(""+count+"        91         1         1         1         3");
				for(Iterator itn=face.getNodesIterator();itn.hasNext();)
				{
					MNode2D node=(MNode2D) itn.next();
					Integer nodelabel =  (Integer) labels.get(node);
					out.print(" "+nodelabel.intValue());
				}
				out.println("");
			}
			out.println("    -1");
			out.close();
		} catch (FileNotFoundException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		} catch (IOException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}
	}
	
	public String toString()
	{
		String cr=System.getProperty("line.separator");
		String r="SubMesh2D";
		logger.debug("Printing "+r+"...");
		r+=cr;
		for(Iterator itn=nodeset.iterator();itn.hasNext();)
		{
			MNode2D node=(MNode2D)itn.next();
			r+=node+cr;
		}
		for(Iterator itf=faceset.iterator();itf.hasNext();)
		{
			MFace2D face=(MFace2D)itf.next();
			r+=face+cr;
		}
		logger.debug("...done");
		return r;
	}
}
