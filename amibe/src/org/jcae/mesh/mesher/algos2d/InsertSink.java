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

package org.jcae.mesh.mesher.algos2d;

import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.mesher.metrics.*;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import org.apache.log4j.Logger;

/**
 * Cuts long edges and removes small edges.
 *
 * @see CollapseEdges
 * @see CutEdges
 */

public class InsertSink
{
	private static Logger logger=Logger.getLogger(InsertSink.class);
	private SubMesh2D submesh2d;
	
	private HashMap map;
	private int maxiter = 100;
	
	private class MFace2DQuality implements Comparable
	{
		public MFace2D element;
		public MEdge2D edge;
		public double quality;
		public MFace2DQuality(MFace2D f)
		{
			element = f;
			double l = -1;
			for (Iterator ite = f.getEdgesIterator(); ite.hasNext(); )
			{
				MEdge2D e2 = (MEdge2D) ite.next();
				double l2 = submesh2d.compGeom().length(e2);
				if (l2 > l)
				{
					l = l2;
					edge = e2;
				}
			}
			quality = submesh2d.compGeom().quality(element);
		}
	
		public int compareTo(Object o)
		{
			MFace2DQuality t = (MFace2DQuality) o;
			if (quality < t.quality)
				return -1;
			else if (quality > t.quality)
				return 0;
			return  0;
		}
		
		public String toString()
		{
			return element+" "+edge+" "+quality;
		}
	}
	
	/**
	 * Creates a <code>InsertSink</code> instance.
	 *
	 * @param m  the <code>SubMesh2D</code> instance to refine.
	 */
	public InsertSink(SubMesh2D m)
	{
		submesh2d = m;
	}
	public InsertSink(SubMesh2D m, int iter)
	{
		submesh2d = m;
		maxiter = iter;
	}
	
	/**
	 * Iteratively collapses and refines edges.
	 *
	 * @see CollapseEdges
	 * @see CutEdges
	 */
	public void compute()
	{
		boolean redo = false;
		CADGeomSurface surf = submesh2d.getGeomSurface();
		assert(submesh2d.isValid());
		assert(null == submesh2d.checkDelaunay());
		int ns = submesh2d.getFaces().size();
		MFace2DQuality [] obj = new MFace2DQuality[ns];
		MFace2DQuality [] newObj = null;
		int i = 0;
		map = new HashMap();
		for (Iterator itf = submesh2d.getFacesIterator(); itf.hasNext(); i++)
		{
			obj[i] = new MFace2DQuality((MFace2D) itf.next());
			map.put(obj[i].element, obj[i]);
		}
		
		int ne = 0;
		int niter = maxiter;
		if (maxiter <= 0)
			niter = 1;
		do {
			redo = false;
			niter--;
			if (logger.isDebugEnabled())
				logger.debug("Current number of nodes: "+submesh2d.getNodes().size());
			Arrays.sort(obj);
			HashSet edgesToCheck = new HashSet();
			HashSet markedEdges = new HashSet();
			HashSet splitFaces = new HashSet();
			for (i = 0; i < obj.length; i++)
			{
				if (submesh2d.compGeom().length(obj[i].edge) < 2.0)
					continue;
				try
				{
					MFace2D f = getAcuteTriangle(obj[i]);
					boolean isNeighbor = false;
					for (Iterator ite = f.getEdgesIterator(); ite.hasNext(); )
					{
						if (markedEdges.contains(ite.next()))
						{
							isNeighbor = true;
							break;
						}
					}
					if (isNeighbor)
						continue;
					splitFaces.add(f);
					markedEdges.addAll(f.getEdges());
					redo = true;
					if (maxiter <= 0)
						break;
				}
				catch (RuntimeException e)
				{
					logger.debug("boundary found");
				}
			}
			newObj = new MFace2DQuality[obj.length+3*splitFaces.size()];
			HashSet boundSplitFaces = new HashSet();
			System.arraycopy(obj, 0, newObj, 0, obj.length);
			int tail = obj.length;
			for (i = 0; i < obj.length; i++)
			{
				MFace2D f = obj[i].element;
				if (boundSplitFaces.contains(f))
					continue;
				map.remove(f);
				assert (submesh2d.getFaces().contains(f));
				if (splitFaces.contains(f))
				{
					MNode2D c = f.circumcenter(surf);
					//MNode2D c = f.centroid();
					MFace2D [] newT = submesh2d.explodeTriangle(f, c);
					submesh2d.rmFace(f);
					newObj[i] = new MFace2DQuality(newT[0]);
					map.put(newT[0], newObj[i]);
					edgesToCheck.addAll(newT[0].getEdges());
					for (int n = 1; n < newT.length && n < 4; n++)
					{
						newObj[tail] = new MFace2DQuality(newT[n]);
						map.put(newT[n], newObj[tail]);
						edgesToCheck.addAll(newT[n].getEdges());
						tail++;
					}
					if (5 == newT.length)
					{
						submesh2d.rmFace(newT[4]);
						splitFaces.remove(newT[4]);
						boundSplitFaces.add(newT[4]);
						map.remove(newT[4]);
					}
				}
				else
					map.put(f, newObj[i]);
			}
			//assert tail == obj.length+2*splitFaces.size();

			obj = new MFace2DQuality[tail];
			System.arraycopy(newObj, 0, obj, 0, tail);
			if (!edgesToCheck.isEmpty())
			{
				HashSet newFaces = submesh2d.checkAndSwapDelaunay(edgesToCheck);
				if (null != newFaces)
				{
					Iterator it = newFaces.iterator();
					for (int n = 0; n < obj.length; n++)
					{
						if (!submesh2d.getFaces().contains(obj[n].element))
						{
							map.remove(obj[n].element);
							MFace2D f = null;
							while (it.hasNext())
							{
								f = (MFace2D) it.next();
								if (submesh2d.getFaces().contains(f))
									break;
							}
							assert null != f;
							obj[n] = new MFace2DQuality(f);
							map.put(f, obj[n]);
						}
					}
/*
					while (it.hasNext())
					{
						if (submesh2d.getFaces().contains((MFace2D) it.next()))
							break;
					}
					assert !it.hasNext();
*/
				}
			}
		} while (redo && niter > 0);
		assert(submesh2d.isValid());
	}
	
	private MFace2D getAcuteTriangle(MFace2DQuality obj)
		throws RuntimeException
	{
		MFace2DQuality curr = obj;
		CADGeomSurface surf = submesh2d.getGeomSurface();
		MNode2D ap = null;
		MEdge2D e = null;
		HashSet seen = new HashSet();
		double [] xapex, xe1, xe2;
		do {
			if (!curr.edge.isMutable())
				throw new RuntimeException("Boundary");
			ap = curr.element.apex(curr.edge);
			xapex = ap.getUV();
			xe1 = curr.edge.getNodes1().getUV();
			xe2 = curr.edge.getNodes2().getUV();
			if ((xe1[0] - xapex[0])*(xe2[0] - xapex[0]) +
			    (xe1[1] - xapex[1])*(xe2[1] - xapex[1]) >= 0.0)
				return curr.element;
			Iterator it = curr.edge.getFacesIterator();
			MFace2D ngh = (MFace2D) it.next();
			if (ngh == curr.element)
				ngh = (MFace2D) it.next();
			curr = (MFace2DQuality) map.get(ngh);
			assert curr != null : ngh;
			if (seen.contains(curr.element))
				throw new RuntimeException("Boundary");
			seen.add(curr.element);
		} while (true);
	}
	
}
