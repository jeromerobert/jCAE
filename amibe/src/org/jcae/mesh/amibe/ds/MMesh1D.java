/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
    Copyright (C) 2007, by EADS France
 
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

package org.jcae.mesh.amibe.ds;

import org.jcae.mesh.bora.ds.*;
import org.jcae.mesh.cad.*;
import org.jcae.mesh.amibe.patch.Vertex2D;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

/**
 * 1D discretization of the whole shape.
 * In order to ensure fitting in 3D space, edges are discretized only once
 * even if they appear with several orientations.
 */

public class MMesh1D extends MMesh0D
{
	private static Logger logger = Logger.getLogger(MMesh1D.class);	

	private String filename;
	
	//  Edge map.
	private Map<CADEdge, SubMesh1D> mapTEdgeToSubMesh1D;
	private Map<CADEdge, LinkedHashSet<CADFace>> mapTEdgeToFaces;
	
	// Ditto for bora data structure.
	private Map<BDiscretization, SubMesh1D> mapDiscrToSubMesh1D;
	private Map<BDiscretization, LinkedHashSet<BDiscretization>> mapDiscrToFaces;

	/**
	 * Creates a <code>MMesh1D</code> instance by discretizing all edges
	 * of a given shape.
	 *
	 * @param cadFile  file containing CAD shape
	 */
	public MMesh1D(String cadFile)
	{
		super(CADShapeFactory.getFactory().newShape(cadFile));
		filename = cadFile;

		CADExplorer expE = CADShapeFactory.getFactory().newExplorer();
		//  HashMap size will not be greater than the number of edges,
		//  so allocate them after computing their maximal size, they
		//  won't be resized.
		int edges = 0;
		for (expE.init(shape, CADShapeEnum.EDGE); expE.more(); expE.next())
			edges++;
		if (edges == 0)
			return;
		mapTEdgeToSubMesh1D = new LinkedHashMap<CADEdge, SubMesh1D>(edges);
		
		for (expE.init(shape, CADShapeEnum.EDGE); expE.more(); expE.next())
		{
			CADEdge E = (CADEdge) expE.current();
			//  Edges may get connected to several faces
			if (mapTEdgeToSubMesh1D.containsKey(E))
				continue;
			SubMesh1D submesh1d = new SubMesh1D(E, this);
			mapTEdgeToSubMesh1D.put(E, submesh1d);
		}
		mapTEdgeToFaces = new HashMap<CADEdge, LinkedHashSet<CADFace>>(edges);
		for (Iterator<CADEdge> it = mapTEdgeToSubMesh1D.keySet().iterator(); it.hasNext(); )
			mapTEdgeToFaces.put(it.next(), new LinkedHashSet<CADFace>());
		CADExplorer expF = CADShapeFactory.getFactory().newExplorer();
		for (expF.init(shape, CADShapeEnum.FACE); expF.more(); expF.next())
		{
			CADFace F = (CADFace) expF.current();
			LinkedHashSet<CADFace> set;
			for (expE.init(F, CADShapeEnum.EDGE); expE.more(); expE.next())
			{
				CADEdge E = (CADEdge) expE.current();
				set = mapTEdgeToFaces.get(E);
				set.add(F);
			}
		}
		assert(isValid());
	}
	
	public MMesh1D(BModel model)
	{
		super(model);
		BCADGraphCell root = model.getGraph().getRootCell();
		shape = root.getShape();

		int edgediscrs = 0;
		// estimation of the maximum number of nodes created on the vertices of the CAD
		for (Iterator<BCADGraphCell> itn = root.shapesExplorer(CADShapeEnum.EDGE); itn.hasNext(); )
		{
			BCADGraphCell cell = itn.next();
			for (Iterator<BDiscretization> itpd = cell.discretizationIterator(); itpd.hasNext(); itpd.next())
			{
				edgediscrs++;
			}
		}
		if (edgediscrs == 0)
			return;

		mapDiscrToSubMesh1D = new LinkedHashMap<BDiscretization, SubMesh1D>(edgediscrs);
		edgediscrs = 0;
		for (Iterator<BCADGraphCell> itn = root.shapesExplorer(CADShapeEnum.EDGE); itn.hasNext(); )
		{
			BCADGraphCell cell = itn.next();
			for (Iterator<BDiscretization> itpd = cell.discretizationIterator(); itpd.hasNext(); )
			{
				BDiscretization discr = itpd.next();
				//  Edges may get connected to several faces
				if (mapDiscrToSubMesh1D.containsKey(discr))
					continue;
				SubMesh1D submesh1d = new SubMesh1D(discr, this);
				mapDiscrToSubMesh1D.put(discr, submesh1d);
				edgediscrs++;
			}
		}
		System.out.println("Number of Edge discretizations created in MMesh1D: "+ edgediscrs);

		mapDiscrToFaces = new HashMap<BDiscretization, LinkedHashSet<BDiscretization>>(edgediscrs);
		for (Iterator<BDiscretization> it = mapDiscrToSubMesh1D.keySet().iterator(); it.hasNext(); )
		{
			mapDiscrToFaces.put(it.next(), new LinkedHashSet<BDiscretization>());
		}

		for (Iterator<BCADGraphCell> itp = root.shapesExplorer(CADShapeEnum.FACE); itp.hasNext(); )
		{
			BCADGraphCell pcell = itp.next();
			for (Iterator<BDiscretization> itpd = pcell.discretizationIterator(); itpd.hasNext(); )
			{
				BDiscretization pd = itpd.next();
				for (Iterator<BCADGraphCell> itc = pcell.shapesExplorer(CADShapeEnum.EDGE); itc.hasNext(); )
				{
					BCADGraphCell ccell = itc.next();
					for (Iterator<BDiscretization> itcd = ccell.discretizationIterator(); itcd.hasNext(); )
					{
						BDiscretization cd = itcd.next();
						if (pd.contained(cd))
						{
							// here mapDiscrToFaces maps the parent discretizations on 
							// the faces to the child discretizations on the edge
							LinkedHashSet<BDiscretization> set = mapDiscrToFaces.get(cd);
							set.add(pd);
						}
					}
				}
			}
		}

		assert(isValid());
	}
	
	/**
	 * Returns the topological shape.
	 *
	 * @return the topological shape.
	 */
	public CADShape getGeometry()
	{
		return shape;
	}
	
	/**
	 * Returns the file name containing the topological shape.
	 *
	 * @return the file name containing the topological shape.
	 */
	public String getGeometryFilename()
	{
		return filename;
	}
	
	/**
	 * Returns the set of faces containing this topological edge.
	 *
	 * @return the set of faces containing this topological edge.
	 */
	public Set<CADFace> getAdjacentFaces(CADEdge E)
	{
		Set<CADFace> ret = mapTEdgeToFaces.get(E);
		// May be null for beams
		return ret;
	}
	
	/**
	 * Update node labels.
	 */
	public void updateNodeLabels()
	{
		logger.debug("Update node labels");
		//  Resets all labels
		CADExplorer expE = CADShapeFactory.getFactory().newExplorer();
		for (expE.init(shape, CADShapeEnum.EDGE); expE.more(); expE.next())
		{
			CADEdge E = (CADEdge) expE.current();
			SubMesh1D submesh1d = mapTEdgeToSubMesh1D.get(E);
			for (Iterator<MNode1D> it = submesh1d.getNodesIterator(); it.hasNext(); )
				it.next().setLabel(0);
		}
		int i = 0;
		for (expE.init(shape, CADShapeEnum.EDGE); expE.more(); expE.next())
		{
			CADEdge E = (CADEdge) expE.current();
			SubMesh1D submesh1d = mapTEdgeToSubMesh1D.get(E);
			for (Iterator<MNode1D> it = submesh1d.getNodesIterator(); it.hasNext(); )
			{
				MNode1D n = it.next();
				if (0 == n.getMaster().getLabel())
				{
					i++;
					n.getMaster().setLabel(i);
				}
			}
		}
	}
	
	/**
	 * Duplicates edges so that boundary faces are closed.
	 * This method must be used after all 1D algorithms have been applied,
	 * and before any 2D meshing is performed.
	 *
	 */
	public void duplicateEdges()
	{
		logger.debug("Compute vertex references");
		CADExplorer expV = CADShapeFactory.getFactory().newExplorer();
		//  For each topological vertex, compute the list of
		//  MNode1D objects which are bound to this vertex.
		int nVertex = 0;
		for (expV.init(shape, CADShapeEnum.VERTEX); expV.more(); expV.next())
			nVertex++;
		HashMap<CADShape, ArrayList<MNode1D>> vertex2Ref = new HashMap<CADShape, ArrayList<MNode1D>>(nVertex);
		for (expV.init(shape, CADShapeEnum.VERTEX); expV.more(); expV.next())
			vertex2Ref.put(expV.current(), new ArrayList<MNode1D>());
		
		Iterator<SubMesh1D> ite = mapTEdgeToSubMesh1D.values().iterator();
		while (ite.hasNext())
		{
			SubMesh1D submesh1d = ite.next();
			Iterator<MNode1D> itn = submesh1d.getNodesIterator();
			while (itn.hasNext())
			{
				MNode1D pt = itn.next();
				if (null != pt.getCADVertex())
					vertex2Ref.get(pt.getCADVertex()).add(pt);
			}
		}
		
		LinkedHashSet<CADVertex> seen = new LinkedHashSet<CADVertex>();
		for (expV.init(shape, CADShapeEnum.VERTEX); expV.more(); expV.next())
		{
			CADVertex V = (CADVertex) expV.current();
			if (seen.contains(V))
				continue;
			seen.add(V);
			ArrayList<MNode1D> vnodelist = vertex2Ref.get(V);
			if (vnodelist.size() <= 1)
				continue;
			// Make sure that all MNode1D objects share the same master.
			MNode1D master = vnodelist.get(0);
			for (MNode1D pt: vnodelist)
				pt.setMaster(master);
			master.setMaster(null);
		}
		assert(isValid());
	}
	
	/**
	 * Returns the list of topological edges.
	 *
	 * @return the list of topological edges.
	 */
	public Iterator<CADEdge> getTEdgeIterator()
	{
		return mapTEdgeToSubMesh1D.keySet().iterator();
	}
	
	public Iterator<BDiscretization> getBEdgeIterator()
	{
		return mapDiscrToSubMesh1D.keySet().iterator();
	}
	
	/**
	 * Returns the list of nodes inserted on a given topological edge.
	 *
	 * @param E  a topological edge.
	 * @return the list of nodes inserted on this edge.
	 */
	public ArrayList<MNode1D> getNodelistFromMap(CADEdge E)
		throws NoSuchElementException
	{
		SubMesh1D submesh1d = mapTEdgeToSubMesh1D.get(E);
		if (null == submesh1d)
			throw new NoSuchElementException("TEdge : "+E);
		return submesh1d.getNodes();
	}
	
	/**
	 * Returns the list of edges inserted on a given topological edge.
	 *
	 * @param E  a topological edge.
	 * @return the list of edges inserted on this edge.
	 */
	public ArrayList<MEdge1D> getEdgelistFromMap(CADEdge E)
	       throws NoSuchElementException
	{
		SubMesh1D submesh1d = mapTEdgeToSubMesh1D.get(E);
		if (null == submesh1d)
			throw new NoSuchElementException("TEdge : "+E);
		return submesh1d.getEdges();
	}
	
	/** 
	 * Returns an upper limit of the total number of nodes
	 *
	 * @return an upper limit of the total number of nodes
	 */
	public int maximalNumberOfNodes()
	{
		int result = 0;
		for(Iterator<SubMesh1D> it=mapTEdgeToSubMesh1D.values().iterator();it.hasNext();)
		{
			SubMesh1D submesh1d = it.next();
			if (null != submesh1d)
				result += submesh1d.getNodes().size();
		}
		return result;
	}
	
	/**
	 * Returns the <code>SubMesh1D</code> instance associated with a
	 * CADEdge.
	 *
	 * @param E  a topological edge.
	 * @return the <code>SubMesh1D</code> instance associated with it.
	 */
	public SubMesh1D getSubMesh1DFromMap(CADEdge E)
	       throws NoSuchElementException
	{
		if (! mapTEdgeToSubMesh1D.containsKey(E))
			throw new NoSuchElementException("TEdge : "+E);
		return mapTEdgeToSubMesh1D.get(E);
	}
	
	/**
	 * Returns the <code>SubMesh1D</code> instance associated with a
	 * BDiscretization.
	 *
	 * @param discrE  discretization of an edge.
	 * @return the <code>SubMesh1D</code> instance associated with it.
	 */
	public SubMesh1D getSubMesh1DFromMap(BDiscretization discrE)
	       throws NoSuchElementException
	{
		if (! mapTEdgeToSubMesh1D.containsKey(discrE))
			throw new NoSuchElementException("TEdge : "+discrE);
		return mapTEdgeToSubMesh1D.get(discrE);
	}

	public Vertex2D [] boundaryNodes(CADFace face, MeshParameters mp)
	{
		double epsilon = mp.getEpsilon();
		boolean accumulateEpsilon = mp.hasCumulativeEpsilon();
		double deflection = mp.getDeflection();
		boolean hasRelativeDeflection = mp.hasRelativeDeflection();
		boolean hasDeflection = mp.hasDeflection();

		//  Rough approximation of the final size
		int roughSize = 10*maximalNumberOfNodes();
		ArrayList<Vertex2D> result = new ArrayList<Vertex2D>(roughSize);
		CADExplorer expW = CADShapeFactory.getFactory().newExplorer();
		CADWireExplorer wexp = CADShapeFactory.getFactory().newWireExplorer();
		
		for (expW.init(face, CADShapeEnum.WIRE); expW.more(); expW.next())
		{
			MNode1D p1 = null;
			Vertex2D p20 = null, p2 = null, lastPoint = null;
			double accumulatedLength = 0.0;
			ArrayList<Vertex2D> nodesWire = new ArrayList<Vertex2D>(roughSize);
			for (wexp.init((CADWire) expW.current(), face); wexp.more(); wexp.next())
			{
				CADEdge te = wexp.current();
				CADGeomCurve2D c2d = CADShapeFactory.getFactory().newCurve2D(te, face);
				CADGeomCurve3D c3d = CADShapeFactory.getFactory().newCurve3D(te);

				ArrayList<MNode1D> nodelist = getNodelistFromMap(te);
				Iterator<MNode1D> itn = nodelist.iterator();
				ArrayList<MNode1D> saveList = new ArrayList<MNode1D>();
				while (itn.hasNext())
					saveList.add(itn.next());
				if (!te.isOrientationForward())
				{
					//  Sort in reverse order
					int size = saveList.size();
					for (int i = 0; i < size/2; i++)
					{
						MNode1D o = saveList.get(i);
						saveList.set(i, saveList.get(size - i - 1));
						saveList.set(size - i - 1, o);
					}
				}
				itn = saveList.iterator();
				//  Except for the very first edge, the first
				//  vertex is constrained to be the last one
				//  of the previous edge.
				p1 = itn.next();
				if (null == p2)
				{
					p2 = Vertex2D.valueOf(p1, c2d, face);
					nodesWire.add(p2);
					p20 = p2;
					lastPoint = p2;
				}
				ArrayList<Vertex2D> newNodes = new ArrayList<Vertex2D>(saveList.size());
				while (itn.hasNext())
				{
					p1 = itn.next();
					p2 = Vertex2D.valueOf(p1, c2d, face);
					newNodes.add(p2);
				}
				// An edge is skipped if all the following conditions
				// are met:
				//   1.  It is not degenerated
				//   2.  It has not been discretized in 1D
				//   3.  Edge length is smaller than epsilon
				//   4.  Accumulated points form a curve with a deflection
				//       which meets its criterion
				boolean canSkip = false;
				if (nodelist.size() == 2 && !te.isDegenerated())
				{
					//   3.  Edge length is smaller than epsilon
					double edgelen = c3d.length();
					if (accumulateEpsilon)
						canSkip = edgelen + accumulatedLength < epsilon;
					else
						canSkip = edgelen < epsilon;
					if (canSkip)
						accumulatedLength += edgelen;
					// 4.  Check whether deflection is valid.
					if (canSkip && hasDeflection)
					{
						assert lastPoint != null;
						double [] uv = lastPoint.getUV();
						double [] start = face.getGeomSurface().value(uv[0], uv[1]);
						uv = p2.getUV();
						double [] end = face.getGeomSurface().value(uv[0], uv[1]);
						double dist = Math.sqrt(
						  (start[0] - end[0]) * (start[0] - end[0]) +
						  (start[1] - end[1]) * (start[1] - end[1]) +
						  (start[2] - end[2]) * (start[2] - end[2]));
						double dmax = deflection;
						if (hasRelativeDeflection)
							dmax *= accumulatedLength;
						if (accumulatedLength - dist > dmax)
							canSkip = false;
					}
				}

				if (!canSkip)
				{
					nodesWire.addAll(newNodes);
					accumulatedLength = 0.0;
					lastPoint = p2;
				}
			}
			//  If a wire has less than 3 points, it is discarded
			if (nodesWire.size() > 3)
			{
				//  Overwrite the last value to close the wire
				nodesWire.set(nodesWire.size()-1, p20);
				result.addAll(nodesWire);
			}
		}
		
		return result.toArray(new Vertex2D[result.size()]);
	}
	
	/**
	 * Checks the validity of a <code>MMesh1D</code> instance.
	 * This method is called within assertions, this is why it returns a
	 * <code>boolean</code>.
	 *
	 * @return <code>true</code> if all checks pass.
	 * @throws AssertException if a check fails.
	 */
	public boolean isValid()
	{
		for(Iterator<SubMesh1D> it=mapTEdgeToSubMesh1D.values().iterator();it.hasNext();)
		{
			SubMesh1D submesh1d = it.next();
			if (null != submesh1d)
				assert(submesh1d.isValid());
		}
		return true;
	}
	
	/**
	 * Prints edge lengths of a <code>MMesh1D</code> instance.
	 */
	public void printInfos()
	{
		for(Iterator<SubMesh1D> it=mapTEdgeToSubMesh1D.values().iterator();it.hasNext();)
		{
			SubMesh1D submesh1d = it.next();
			if (null != submesh1d)
				submesh1d.printInfos();
		}
	}
	
	@Override
	public String toString()
	{
		String cr=System.getProperty("line.separator");
		StringBuilder r = new StringBuilder("MMesh1D"+cr);
		logger.debug("Printing "+r.toString());
		for(Iterator<SubMesh1D> it=mapTEdgeToSubMesh1D.values().iterator();it.hasNext();)
		{
			SubMesh1D submesh1d = it.next();
			if (null != submesh1d)
				r.append(submesh1d);
		}
		logger.debug("...done");
		return r.toString();
	}
}

