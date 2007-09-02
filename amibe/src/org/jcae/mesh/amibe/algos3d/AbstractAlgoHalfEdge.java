/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006 by EADS CRC
    Copyright (C) 2007 by EADS France

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

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.HalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.util.QSortedTree;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import java.util.Stack;
import java.util.Map;
import java.util.Iterator;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.log4j.Logger;

public abstract class AbstractAlgoHalfEdge
{
	protected Mesh mesh;
	protected int nrFinal = 0;
	protected int nrTriangles = 0;
	protected double tolerance = 0.0;
	protected int processed = 0;
	protected int swapped = 0;
	protected int notProcessed = 0;
	protected int notInTree = 0;
	protected QSortedTree tree = new PAVLSortedTree();
	
	protected abstract void preProcessAllHalfEdges();
	protected abstract void postProcessAllHalfEdges();
	protected abstract boolean canProcessEdge(HalfEdge e);
	protected abstract HalfEdge processEdge(HalfEdge e);
	protected abstract double cost(HalfEdge e);
	protected abstract Logger thisLogger();
	private static final String dumpFile = "/tmp/jcae.dump";

	public AbstractAlgoHalfEdge(Mesh m, Map options)
	{
		mesh = m;
	}
	public void compute()
	{
		thisLogger().info("Run "+getClass().getName());
		preProcessAllHalfEdges();
		thisLogger().info("Compute initial tree");
		computeTree();
		postComputeTree();
		thisLogger().info("Initial number of triangles: "+countInnerTriangles(mesh));
		processAllHalfEdges();
		thisLogger().info("Final number of triangles: "+countInnerTriangles(mesh));
	}

	public static int countInnerTriangles(Mesh mesh)
	{
		int ret = 0;
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (!isSkippedTriangle(f))
				ret++;
		}
		return ret;
	}
	
	private void computeTree()
	{
		//  Compute edge cost
		nrTriangles = 0;
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (isSkippedTriangle(f))
				continue;
			nrTriangles++;
			HalfEdge e = (HalfEdge) f.getAbstractHalfEdge();
			for (int i = 0; i < 3; i++)
			{
				e = (HalfEdge) e.next();
				if (tree.contains(e.notOriented()))
					continue;
				addToTree(e);
			}
		}
	}

	/**
	 * Tells whether to skip outer triangle or non-writable triangle.
	 * @param f triangle to test.
	 * @return <code>true</code> if this triangle has to be skipped,
	 * <code>false</code> otherwise.
	 */
	protected static boolean isSkippedTriangle(Triangle f)
	{
		return f.isOuter() || !f.isWritable();
	}
	
	protected void postComputeTree()
	{
	}

	// This routine is needed by DecimateHalfedge.
	protected void preProcessEdge()
	{
	}

	protected void addToTree(HalfEdge e)
	{
		if (!e.origin().isReadable() || !e.destination().isReadable())
			return;
		double val = cost(e);
		// If an edge will not be processed because of its cost, it is
		// better to not put it in the tree.  One drawback though is
		// that tree.size() is not equal to the total number of edges,
		// and output displayed by postProcessAllHalfEdges() may thus
		// not be very useful.
		if (nrFinal != 0 || val <= tolerance)
			tree.insert(e.notOriented(), val);
	}

	private boolean processAllHalfEdges()
	{
		boolean noSwap = false;
		Stack stackNotProcessed = new Stack();
		double cost = -1.0;
		while (!tree.isEmpty() && nrTriangles > nrFinal)
		{
			preProcessEdge();
			HalfEdge current = null;
			Iterator itt = tree.iterator();
			if ((processed % 10000) == 0)
				thisLogger().info("Edges processed: "+processed);
			while (itt.hasNext())
			{
				QSortedTree.Node q = (QSortedTree.Node) itt.next();
				current = (HalfEdge) q.getData();
				cost = q.getValue();
				if (nrFinal == 0 && cost > tolerance)
					break;
				if (canProcessEdge(current))
					break;
				if (thisLogger().isDebugEnabled())
					thisLogger().debug("Edge not processed: "+current);
				notProcessed++;
				// Add a penalty to edges which could not have been
				// processed.  This has to be done outside this loop,
				// because PAVLSortedTree instances must not be modified
				// when walked through.
				if (nrFinal == 0)
				{
					stackNotProcessed.push(current);
					if (tolerance > 0.0)
						stackNotProcessed.push(new Double(cost+0.7*(tolerance - cost)));
					else
						// tolerance = cost = 0
						stackNotProcessed.push(new Double(1.0));
				}
				else
				{
					stackNotProcessed.push(current);
					double penalty = tree.getRootValue()*0.7;
					if (penalty == 0.0)
						penalty = 1.0;
					stackNotProcessed.push(new Double(cost+penalty));
				}
				current = null;
			}
			if ((nrFinal == 0 && cost > tolerance) || current == null)
				break;
			// Update costs for edges which were not contracted
			while (stackNotProcessed.size() > 0)
			{
				double newCost = ((Double) stackNotProcessed.pop()).doubleValue();
				HalfEdge f = (HalfEdge) stackNotProcessed.pop();
				assert f == f.notOriented();
				tree.update(f, newCost);
			}
			current = processEdge(current);
			processed++;

			if (noSwap)
				continue;
			
			if (current.hasAttributes(AbstractHalfEdge.OUTER | AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				continue;
			// Check if edges can be swapped
			Vertex o = current.origin();
			while(true)
			{
				if (current.checkSwap3D(0.95) >= 0.0)
				{
					// Swap edge
					for (int i = 0; i < 3; i++)
					{
						current = (HalfEdge) current.next();
						if (!tree.remove(current.notOriented()))
							notInTree++;
						assert !tree.contains(current.notOriented());
					}
					HalfEdge sym = (HalfEdge) current.sym();
					for (int i = 0; i < 2; i++)
					{
						sym = (HalfEdge) sym.next();
						if (!tree.remove(sym.notOriented()))
							notInTree++;
						assert !tree.contains(sym.notOriented());
					}
					Vertex a = current.apex();
					current = (HalfEdge) current.swap();
					swapped++;
					// Now current = (ona)
					assert a == current.apex();
					for (int i = 0; i < 3; i++)
					{
						current = (HalfEdge) current.next();
						addToTree(current);
					}
					sym = (HalfEdge) ((HalfEdge) current.next()).sym();
					for (int i = 0; i < 2; i++)
					{
						sym = (HalfEdge) sym.next();
						addToTree(sym);
					}
				}
				else
				{
					current = current.nextApexLoop();
					if (current.origin() == o)
						break;
				}
			}
		}
		postProcessAllHalfEdges();
		return processed > 0;
	}

	protected final void dumpState()
	{
		ObjectOutputStream out = null;
		try
		{
			out = new ObjectOutputStream(new FileOutputStream(dumpFile));
			out.writeObject(mesh);
			out.writeObject(tree);
			appendDumpState(out);
			out.close();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(-1);
		}
	}

	protected void appendDumpState(ObjectOutputStream out)
		throws IOException
	{
	}

	protected final boolean restoreState()
	{
		try
		{
			FileInputStream istream = new FileInputStream(dumpFile);
			ObjectInputStream q = new ObjectInputStream(istream);
			System.out.println("Loading restored state");
			mesh = (Mesh) q.readObject();
			tree = (QSortedTree) q.readObject();
			appendRestoreState(q);
			System.out.println("... Done.");
			q.close();
			assert mesh.isValid();
		}
		catch (FileNotFoundException ex)
		{
			return false;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(-1);
		}
		return true;
	}

	protected void appendRestoreState(ObjectInputStream q)
		throws IOException
	{
	}

}
