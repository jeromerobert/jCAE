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

import org.jcae.mesh.mesher.ds.MMesh1D;
import org.jcae.mesh.mesher.ds.tools.*;
import org.jcae.mesh.mesher.metrics.Metric2D;
import org.jcae.mesh.cad.*;
import org.jcae.mesh.util.Calculs;
import java.util.Iterator;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Stack;
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
	
	//  Stack of methods to compute geometrical values
	private Stack compGeomStack;
	
	//  Set of faces
	private HashSet faceset = new HashSet();
	
	//  Set of edges
	private HashSet edgeset = new HashSet();
	
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
		compGeomStack = new Stack();
	}
	
	public void pushCompGeom(int i)
	{
		if (i == 2)
			compGeomStack.push(new Calculus2D(this));
		else if (i == 3)
			compGeomStack.push(new Calculus3D(this));
		else
			throw new java.lang.IllegalArgumentException("pushCompGeom argument must be either 2 or 3, current value is: "+i);
	}
	
	public void pushCompGeom(String type)
	{
		if (type.equals("2"))
			compGeomStack.push(new Calculus2D(this));
		else if (type.equals("3"))
			compGeomStack.push(new Calculus3D(this));
		else
			throw new java.lang.IllegalArgumentException("pushCompGeom argument must be either 2 or 3, current value is: "+type);
	}
	
	public Calculus popCompGeom()
	{
		return (Calculus) compGeomStack.pop();
	}
	
	public Calculus popCompGeom(int i)
	{
		Object ret = compGeomStack.pop();
		if (i == 2)
		{
			if (!(ret instanceof Calculus2D))
				throw new java.lang.RuntimeException("Internal error.  Expected value: 2, found: 3");
		}
		else if (i == 3)
		{
			if (!(ret instanceof Calculus3D))
				throw new java.lang.RuntimeException("Internal error.  Expected value: 3, found: 2");
		}
		else
			throw new java.lang.IllegalArgumentException("pushCompGeom argument must be either 2 or 3, current value is: "+i);
		return (Calculus) ret;
	}
	
	public Calculus compGeom()
	{
		return (Calculus) compGeomStack.peek();
	}
	
	/**
	 * Update node labels.
	 */
	public void updateNodeLabels()
	{
		for (Iterator itn = nodeset.iterator(); itn.hasNext(); )
		{
			MNode2D n = (MNode2D) itn.next();
			if (null == n.getRef())
				n.setLabel(-1);
			else
				n.setLabel(n.getRef().getLabel());
		}
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
	 * Returns the set of edges.
	 *
	 * @return the set of edges.
	 */
	public HashSet getEdges()
	{
		return edgeset;
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
	 * Returns an iterator over the set of edges.
	 *
	 * @return an iterator over the set of edges.
	 */
	public Iterator getEdgesIterator()
	{
		return edgeset.iterator();
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
	 * Adds a face to the mesh, and returns it.
	 *
	 * @return the face being added.
	 */
	public MFace2D addFace(MFace2D f)
	{
		faceset.add(f);
		return f;
	}
	
	/**
	 * Adds an edge to the mesh, and returns it.
	 *
	 * @return the edge being added.
	 */
	public MEdge2D addEdge(MEdge2D e)
	{
		edgeset.add(e);
		return e;
	}
	
	/**
	 * Adds a node to the mesh, and returns it.
	 *
	 * @return the edge being added.
	 */
	public MNode2D addNode(MNode2D n)
	{
		nodeset.add(n);
		return n;
	}
	
	/**
	 * Returns the edge defined bu its end points, or <code>null</code>
	 * if there is no such edge.
	 *
	 * @param  n1  start node,
	 * @param  n2  end node,
	 * @return the edge defined by these 2 nodes.
	 */
	public static MEdge2D getEdgeDefinedByNodes(MNode2D n1, MNode2D n2)
	{
		for(Iterator it=n1.getEdgesIterator(); it.hasNext();)
		{
			MEdge2D e = (MEdge2D) it.next();
			if (e.getNodes1() == n2 || e.getNodes2() == n2)
				return e;
		}
		return null;
	}
	
	/**
	 * Adds an edge defined by its two end points if it was not already defined.
	 *
	 * @return the edge being added.
	 */
	public MEdge2D addEdgeIfNotDefined(MNode2D pt1, MNode2D pt2)
	{
		MEdge2D e = getEdgeDefinedByNodes(pt1, pt2);
		if (null == e)
			e = new MEdge2D(pt1, pt2);
		edgeset.add(e);
		return e;
	}
	
	/**
	 * Remove a face from the <code>Submesh2D</code>.
	 *
	 * @param face  the face to remove
	 */
	public void rmFace(MFace2D face)
	{
		faceset.remove(face);
		HashSet nodes = new HashSet(face.getNodes());
		for(Iterator itn=nodes.iterator(); itn.hasNext();)
		{
			MNode2D n = (MNode2D) itn.next();
			n.unlink(face);
		}
		for(Iterator ite=face.getEdgesIterator(); ite.hasNext();)
		{
			MEdge2D e = (MEdge2D) ite.next();
			if (e.canDestroy())
				edgeset.remove(e);
		}
		for(Iterator itn=nodes.iterator(); itn.hasNext();)
		{
			MNode2D n = (MNode2D) itn.next();
			if (n.canDestroy())
				nodeset.remove(n);
		}
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
		MEdge2D e1 = addEdgeIfNotDefined(n1, n2);
		MEdge2D e2 = addEdgeIfNotDefined(n2, n3);
		MEdge2D e3 = addEdgeIfNotDefined(n3, n1);
		MFace2D f = new MFace2D(e1, e2, e3);
		faceset.add(f);
		f.link();
		return f;
	}
	
	/**
	 * Remove degenerted edges.
	 */
	public void removeDegeneratedEdges()
	{
		HashSet oldedges = new HashSet(edgeset);
		for (Iterator ite = oldedges.iterator(); ite.hasNext(); )
		{
			MEdge2D e = (MEdge2D) ite.next();
			if (!edgeset.contains(e))
				//  This edge has been removed by a previous call
				//  to collapseDegeneratedEdge()
				continue;
			MNode2D pt1 = e.getNodes1();
			MNode2D pt2 = e.getNodes2();
			MNode1D ref1 = pt1.getRef();
			MNode1D ref2 = pt2.getRef();
			int l1 = pt1.getLabel();
			int l2 = pt2.getLabel();
			if (l1 != -1 || l2 != -1)
			{
				if (l1 != l2)
					continue;
			}
			else
			{
				if (null == ref1 || null == ref2)
					continue;
				if (null == ref1.getRef() || null == ref2.getRef())
					continue;
				if (!ref1.getRef().isSame(ref2.getRef()))
					continue;
			}
			logger.debug("Removing "+e);
			collapse(e);
		}
		assert isValid() : "Invalid 2D mesh";
	}
	
	/**
	 * Collapse an edge.
	 *
	 * @param  edge the edge to collapse
	 */
	private void collapse(MEdge2D edge)
	{
		HashSet faces = edge.getFaces();
		assert 1 == faces.size() : edge;
		
		MNode2D pt1 = edge.getNodes1();
		MNode2D pt2 = edge.getNodes2();
		MFace2D face = (MFace2D) faces.iterator().next();
		MNode2D apex = face.apex(edge);
		MEdge2D e1 = getEdgeDefinedByNodes(pt1, apex);
		assert null != e1 : "null expected, "+e1+" found";
		MEdge2D e2 = getEdgeDefinedByNodes(pt2, apex);
		assert null != e2 : "null expected, "+e2+" found";
		assert e1.isMutable() || e2.isMutable() : "e1 or e2 is mutable";
		if (!e1.isMutable())
		{
			MEdge2D temp = e1;
			e1 = e2;
			e2 = temp;
		}
		//  Now e1 is mutable and will be replaced by e2 later
		Iterator neighboursIterator = e1.getFacesIterator();
		MFace2D nf = (MFace2D) neighboursIterator.next();
		if (nf == face)
			nf = (MFace2D) neighboursIterator.next();
		
		//  Replace pt1 by pt2 everywhere
		for (Iterator ite = pt1.getEdgesIterator(); ite.hasNext(); )
		{
			MEdge2D e = (MEdge2D) ite.next();
			e.substNode(pt1, pt2);
		}
		//  Replace e1 by e2 in nf
		nf.substEdge(e1, e2);
		
		//  Re-link elements
		for (Iterator itf = pt1.getElements2DIterator(); itf.hasNext(); )
		{
			MFace2D f = (MFace2D) itf.next();
			pt1.unlink(f);
			pt2.link(f);
		}
		rmFace(face);
		edgeset.remove(e1);
		edgeset.remove(edge);
		nodeset.remove(pt1);
	}
	
	/**
	 * Returns an iterator over all references.
	 *
	 * @return an iterator over all references.
	 */
	public Iterator getRefIterator()
	{
		HashSet set = new HashSet();
		Iterator itn = nodeset.iterator();
		while (itn.hasNext())
		{
			MNode2D pt = (MNode2D) itn.next();
			if (null != pt.getRef())
				set.add(pt.getRef());
		}
		return set.iterator();
	}
	
	/**
	 * Returns the list of nodes which are linked to the same vertex as its
	 * argument.  The <i>master</i> node for <code>that</code> is first
	 * retrieved, then if it is linked to a topological vertex, all
	 * <code>MNode2D</code> which are also linked to this vertex are returned.
	 *
	 * @param that  a <code>MNode1D</code> instance.
	 * @return an array of all <code>MNode2D</code> which have a reference to
	 * <code>that</code>.
	 */
	public MNode2D[] getArrayHasRef(MNode1D that)
	{
		MNode1D master = that.getMaster();
		HashSet set = new HashSet();
		Iterator itn = getNodesIterator();
		while (itn.hasNext())
		{
			MNode2D pt = (MNode2D) itn.next();
			if (master == pt.getRef())
				set.add(pt);
			else if (master.getRef() != null && pt.getRef() != null && pt.getRef().getRef() != null && master.getRef().isSame(pt.getRef().getRef()))
				set.add(pt);
		}
		MNode2D [] toreturn = new MNode2D[set.size()];
		System.arraycopy(set.toArray(), 0, toreturn, 0, toreturn.length);
		return toreturn;
	}
	
	/**
	 * Creates a triangle containing all nodes and edges.
	 * It computes the centroid and radius of outer circle, and determines
	 * a large triangle containing all nodes of the mesh.
	 *
	 * @return a large triangle containing all nodes and edges of the mesh.
	 */
	public MFace2D computeBoundingTriangle()
	{
		double u0 = 0.0, v0 = 0.0;

		for (Iterator it = nodeset.iterator(); it.hasNext(); )
		{
			MNode2D node = (MNode2D) it.next();
			u0 += node.getU();
			v0 += node.getV();
		}
		u0 /= nodeset.size();
		v0 /= nodeset.size();
		
		double rayon = 0.0;
		double d2;
		for (Iterator it = nodeset.iterator(); it.hasNext(); )
		{
			MNode2D node = (MNode2D) it.next();
			d2 = Math.pow(node.getU() - u0, 2) + Math.pow(node.getV() - v0, 2);
			if (d2 > rayon)
				rayon = d2;
		}
		rayon = Math.sqrt(rayon);
		// Enlarge rayon a little bit
		rayon *= 1.05;

		MNode2D n1 = new MNode2D(u0, v0 + 2.0 * rayon);
		MNode2D n2 = new MNode2D(u0 - rayon * Math.sqrt(3.0), v0 - rayon);
		MNode2D n3 = new MNode2D(u0 + rayon * Math.sqrt(3.0), v0 - rayon);

		MFace2D f = addTriangle(n1, n2, n3);
		return f;
	}
	
	/**
	 * Computes mesh quality.
	 *
	 * @return mesh quality.
	 */
	public double quality()
	{
		double q = Float.MAX_VALUE;
		double q2;
		for(Iterator it=faceset.iterator();it.hasNext();)
		{
			MFace2D f= (MFace2D) it.next();
			Iterator itn = f.getNodesIterator();
			MNode2D n1 = (MNode2D) itn.next();
			MNode2D n2 = (MNode2D) itn.next();
			MNode2D n3 = (MNode2D) itn.next();

			q2 = compGeom().qualityAniso(n1, n2, n3);
			if (q2 < q)
				q = q2;
		}
		return q;
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
	
	/**
	 * Checks the validity of a 2D discretization.
	 * This method is called within assertions, this is why it returns a
	 * <code>boolean</code>.
	 *
	 * @return <code>true</code> if all checks pass.
	 * @throws AssertException if a check fails.
	 */
	public boolean isValid()
	{
		HashSet tempset = new HashSet(nodeset);
		for (Iterator ite = edgeset.iterator(); ite.hasNext(); )
		{
			MEdge2D e = (MEdge2D) ite.next();
			assert nodeset.contains(e.getNodes1()) : e;
			assert nodeset.contains(e.getNodes2()) : e;
			if (checkInvertedTriangles)
				assert (!e.isMutable() && e.getFaces().size() == 1) ||
			        	(e.isMutable() && e.getFaces().size() == 2) : e+" has "+e.getFaces().size()+" face(s) ";
			assert e == getEdgeDefinedByNodes(e.getNodes1(), e.getNodes2()) : e+" is not equal to "+getEdgeDefinedByNodes(e.getNodes1(), e.getNodes2());
			tempset.remove(e.getNodes1());
			tempset.remove(e.getNodes2());
		}
		assert tempset.isEmpty() : tempset;
		
		tempset.clear();
		tempset = new HashSet(edgeset);
		for (Iterator itf = faceset.iterator(); itf.hasNext(); )
		{
			MFace2D f = (MFace2D) itf.next();
			assert f.isValid() : f;
			for(Iterator ite=f.getEdgesIterator(); ite.hasNext();)
			{
				MEdge2D e = (MEdge2D) ite.next();
				assert edgeset.contains(e) : f+" does not contain "+e;
				tempset.remove(e);
			}
			for(Iterator itn=f.getNodesIterator(); itn.hasNext();)
			{
				MNode2D n = (MNode2D) itn.next();
				assert n.getElements2D().contains(f) : n+" is not linked to "+f;
			}
		}
		assert tempset.isEmpty() : tempset;
		if (checkInvertedTriangles)
			for (Iterator ite = edgeset.iterator(); ite.hasNext(); )
			{
				MEdge2D e = (MEdge2D) ite.next();
				assert isEdgeValid(e) : e + " " + e.getNodes1() + " " + e.getNodes2();
			}
		return true;
	}
	
	public boolean isEdgeValid(MEdge2D e)
	{
		if (!e.isMutable())
			return true;
		MNode2D pt1 = e.getNodes1();
		MNode2D pt2 = e.getNodes2();
		if (!pt1.isMutable() && !pt2.isMutable())
			return true;
		Iterator itf = e.getFacesIterator();
		MFace2D f1 = (MFace2D) itf.next();
		MFace2D f2 = (MFace2D) itf.next();
		MNode2D apex1 = f1.apex(e);
		MNode2D apex2 = f2.apex(e);
		
		MNode3D pt1_3 = new MNode3D(pt1, surface);
		MNode3D pt2_3 = new MNode3D(pt2, surface);
		MNode3D apex1_3 = new MNode3D(apex1, surface);
		MNode3D apex2_3 = new MNode3D(apex2, surface);
		double [] x1 = pt1_3.getXYZ();
		double [] x2 = pt2_3.getXYZ();
		double [] xa1 = apex1_3.getXYZ();
		double [] xa2 = apex2_3.getXYZ();
		double [] v1 = new double[3];
		double [] v2 = new double[3];
		double [] v3 = new double[3];
		for (int i = 0; i < 3; i++)
		{
			v1[i] = x2[i] - x1[i];
			v2[i] = xa1[i] - x1[i];
			v3[i] = xa2[i] - x1[i];
		}
		/*
		 *   n1 = v1 wedge v2 / || v1 wedge v2 ||
		 *   n2 = v3 wedge v1 / || v3 wedge v1 ||
		 *   n1.n2 = ((v1.v2)(v1.v3) - (v1.v1)(v2.v3))/ || v1 wedge v2 || / || v1 wedge v3 ||
		 */
		return Calculs.prodSca(v1, v2) * Calculs.prodSca(v1, v3)
		       - Calculs.prodSca(v1, v1) * Calculs.prodSca(v2, v3)
			>= - 0.8 * Calculs.norm(Calculs.prodVect3D(v1, v2)) *
		              Calculs.norm(Calculs.prodVect3D(v1, v3));
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
		for(Iterator ite=edgeset.iterator();ite.hasNext();)
		{
			MEdge2D edge=(MEdge2D)ite.next();
			r+=edge+cr;
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
