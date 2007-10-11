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
import org.jcae.mesh.amibe.ds.AbstractTriangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.util.QSortedTree;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import java.util.Stack;
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
	protected abstract HalfEdge processEdge(HalfEdge e, double cost);
	protected abstract double cost(HalfEdge e);
	protected abstract Logger thisLogger();
	private static final String dumpFile = "/tmp/jcae.dump";

	public AbstractAlgoHalfEdge(final Mesh m)
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

	public static int countInnerTriangles(final Mesh mesh)
	{
		int ret = 0;
		for (AbstractTriangle af: mesh.getTriangles())
		{
			if (af.isWritable())
				ret++;
		}
		return ret;
	}
	
	private void computeTree()
	{
		//  Compute edge cost
		nrTriangles = 0;
		for (AbstractTriangle af: mesh.getTriangles())
		{
			if (!af.isWritable())
				continue;
			Triangle f = (Triangle) af;
			nrTriangles++;
			HalfEdge e = (HalfEdge) f.getAbstractHalfEdge();
			for (int i = 0; i < 3; i++)
			{
				e = (HalfEdge) e.next();
				if (!tree.contains(e.notOriented()))
					addToTree(e);
			}
		}
	}

	protected void postComputeTree()
	{
	}

	// This routine is needed by DecimateHalfedge.
	protected void preProcessEdge()
	{
	}

	protected void addToTree(final HalfEdge e)
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

	protected void removeFromTree(final HalfEdge e)
	{
		for (Iterator<AbstractHalfEdge> it = e.fanIterator(); it.hasNext(); )
		{
			HalfEdge f = (HalfEdge) it.next();
			if (!tree.remove(f.notOriented()))
				notInTree++;
			assert !tree.contains(f.notOriented());
		}
	}

	private boolean processAllHalfEdges()
	{
		boolean noSwap = false;
		Stack<HalfEdge> stackNotProcessedObject = new Stack<HalfEdge>();
		Stack<Double> stackNotProcessedValue = new Stack<Double>();
		double cost = -1.0;
		while (!tree.isEmpty() && nrTriangles > nrFinal)
		{
			preProcessEdge();
			HalfEdge current = null;
			Iterator<QSortedTree.Node> itt = tree.iterator();
			if (processed > 0 && (processed % 10000) == 0)
				thisLogger().info("Edges processed: "+processed);
			while (itt.hasNext())
			{
				QSortedTree.Node q = itt.next();
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
					stackNotProcessedObject.push(current);
					if (tolerance > 0.0)
						stackNotProcessedValue.push(Double.valueOf(cost+0.7*(tolerance - cost)));
					else
						// tolerance = cost = 0
						stackNotProcessedValue.push(Double.valueOf(1.0));
				}
				else
				{
					stackNotProcessedObject.push(current);
					double penalty = tree.getRootValue()*0.7;
					if (penalty == 0.0)
						penalty = 1.0;
					stackNotProcessedValue.push(Double.valueOf(cost+penalty));
				}
				current = null;
			}
			if ((nrFinal == 0 && cost > tolerance) || current == null)
				break;
			// Update costs for edges which were not contracted
			while (stackNotProcessedObject.size() > 0)
			{
				double newCost = stackNotProcessedValue.pop().doubleValue();
				HalfEdge f = stackNotProcessedObject.pop();
				assert f == f.notOriented();
				tree.update(f, newCost);
			}
			current = processEdge(current, cost);
			processed++;

			if (noSwap)
				continue;
			
			// Check if edges can be swapped
			Vertex o = current.origin();
			while(true)
			{
				if (!current.hasAttributes(AbstractHalfEdge.OUTER | AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD) && current.checkSwap3D(0.95) >= 0.0)
				{
					// Swap edge
					for (int i = 0; i < 3; i++)
					{
						current = (HalfEdge) current.next();
						removeFromTree(current);
					}
					HalfEdge sym = (HalfEdge) current.sym();
					for (int i = 0; i < 2; i++)
					{
						sym = (HalfEdge) sym.next();
						removeFromTree(sym);
					}
					Vertex a = current.apex();
					current = (HalfEdge) mesh.edgeSwap(current);
					swapped++;
					// Now current = (ona)
					assert a == current.apex();
					for (int i = 0; i < 3; i++)
					{
						current = (HalfEdge) current.next();
						for (Iterator<AbstractHalfEdge> it = current.fanIterator(); it.hasNext(); )
						{
							HalfEdge e = (HalfEdge) it.next();
							addToTree(e);
						}
					}
					sym = (HalfEdge) ((HalfEdge) current.next()).sym();
					for (int i = 0; i < 2; i++)
					{
						sym = (HalfEdge) sym.next();
						for (Iterator<AbstractHalfEdge> it = sym.fanIterator(); it.hasNext(); )
						{
							HalfEdge e = (HalfEdge) it.next();
							addToTree(e);
						}
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
