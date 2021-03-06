/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
    Copyright (C) 2007-2011, by EADS France

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
import org.jcae.mesh.amibe.ds.TriangleHE;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.amibe.util.QSortedTree;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import java.util.Iterator;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.util.HashFactory;

public abstract class AbstractAlgoHalfEdge
{
	Mesh mesh;
	MeshLiaison liaison;
	int nrFinal = 0;
	int nrTriangles = 0;
	double tolerance = 0.0;
	int processed = 0;
	int swapped = 0;
	int notProcessed = 0;
	private int notInTree = 0;
	private int progressBarStatus = 10000;
	private boolean noSwapAfterProcessing = false;
	double minCos = 0.95;
	boolean moreTriangles = false;
	private QSortedTree<HalfEdge> tree = new PAVLSortedTree<HalfEdge>();
	
	private final Collection<HalfEdge> notProcessedObjects = HashFactory.createSet();
	protected abstract void preProcessAllHalfEdges();
	protected void postProcessAllHalfEdges()
	{
		//LOGGER.info("Number of edges which were not in the binary tree before being removed: "+notInTree);
		thisLogger().info("Number of edges still present in the binary tree: "+tree.size());
	}
	protected abstract boolean canProcessEdge(HalfEdge e);
	protected abstract HalfEdge processEdge(HalfEdge e, double cost);
	protected abstract double cost(HalfEdge e);
	protected abstract Logger thisLogger();
	private static final String dumpFile = "/tmp/jcae.dump";

	AbstractAlgoHalfEdge(final Mesh m)
	{
		this(m, null);
	}
	AbstractAlgoHalfEdge(final Mesh m, final MeshLiaison meshLiaison)
	{
		mesh = m;
		liaison = meshLiaison;
	}

	protected void preCheck()
	{
		assert mesh.checkNoDegeneratedTriangles();
		assert mesh.checkNoInvertedTriangles();
	}

	protected void postCheck()
	{
		assert mesh.checkNoDegeneratedTriangles();
		assert mesh.checkNoInvertedTriangles();
	}

	public final void compute()
	{
		long startTime = System.nanoTime();
		preCheck();
		thisLogger().info("Run "+getClass().getName());
		mesh.getTrace().println("# Begin "+getClass().getName());
		preProcessAllHalfEdges();
		thisLogger().info("Compute initial tree");
		computeTree();
		postComputeTree();
		thisLogger().info("Initial number of triangles: "+countInnerTriangles(mesh));
		processAllHalfEdges();
		thisLogger().info("Final number of triangles: "+countInnerTriangles(mesh));
		mesh.getTrace().println("# End "+getClass().getName());
		postCheck();
		long endTime = System.nanoTime();
		thisLogger().log(Level.INFO, "Computation time: {0}ms",
			Double.toString((endTime - startTime)/1E6));
	}

	public void setProgressBarStatus(int n)
	{
		progressBarStatus = n;
	}

	public static int countInnerTriangles(final Mesh mesh)
	{
		int ret = 0;
		for (Triangle af: mesh.getTriangles())
		{
			if (af.isWritable())
				ret++;
		}
		return ret;
	}
	
	/**
	 * Ensure that edge orientation is fixed and does not depend on hashcodes.  This method
	 * must be used when entering canProcessEdge() and processEdge().
	 */
	static HalfEdge uniqueOrientation(HalfEdge current)
	{
		if (current.hasAttributes(AbstractHalfEdge.MARKED) || !current.hasSymmetricEdge())
			return current;
		if (current.sym().hasAttributes(AbstractHalfEdge.MARKED) || current.hasAttributes(AbstractHalfEdge.OUTER))
			return current.sym();
		return current;
	}

	private void computeTree()
	{
		//  Remove all MARKED attributes
		for (Triangle af: mesh.getTriangles())
		{
			TriangleHE f = (TriangleHE) af;
			HalfEdge e = f.getAbstractHalfEdge();
			for (int i = 0; i < 3; i++)
			{
				e = e.next();
				e.clearAttributes(AbstractHalfEdge.MARKED);
			}
		}
		//  Compute edge cost
		nrTriangles = 0;
		for (Triangle af: mesh.getTriangles())
		{
			if (!af.isWritable())
				continue;
			TriangleHE f = (TriangleHE) af;
			nrTriangles++;
			HalfEdge e = f.getAbstractHalfEdge();
			for (int i = 0; i < 3; i++)
			{
				e = e.next();
				HalfEdge h = uniqueOrientation(e);
				if (!tree.contains(h))
					addToTree(h);
			}
		}
	}

	void postComputeTree()
	{
	}

	// This routine is needed by DecimateHalfedge.
	void preProcessEdge()
	{
	}

	protected void addToTreeIfNot(HalfEdge current)
	{
		HalfEdge h = uniqueOrientation(current.next());
		if (!h.hasAttributes(
			AbstractHalfEdge.IMMUTABLE | AbstractHalfEdge.OUTER |
			AbstractHalfEdge.SHARP | AbstractHalfEdge.BOUNDARY |
			AbstractHalfEdge.NONMANIFOLD) && !tree.contains(h))
		{
			double val = cost(h);
			if (val <= tolerance)
			{
				// the edge has changed so we want canProcessEdge to be
				// re-evaluated, so we remove the edge from the
				// notProcessObjects set
				notProcessedObjects.remove(h);
				tree.insert(h, val);
				h.setAttributes(AbstractHalfEdge.MARKED);
			}
		}
	}

	final void addToTree(final HalfEdge e)
	{
		if (!e.origin().isReadable() || !e.destination().isReadable())
			return;
		if (e.hasAttributes(AbstractHalfEdge.IMMUTABLE))
			return;
		double val = cost(e);
		// If an edge will not be processed because of its cost, it is
		// better to not put it in the tree.  One drawback though is
		// that tree.size() is not equal to the total number of edges,
		// and output displayed by postProcessAllHalfEdges() may thus
		// not be very useful.
		if (nrFinal != 0 || val <= tolerance)
		{
			// the edge has changed so we want canProcessEdge to be
			// re-evaluated, so we remove the edge from the notProcessObjects
			// set
			notProcessedObjects.remove(e);
			tree.insert(e, val);
			e.setAttributes(AbstractHalfEdge.MARKED);
		}
	}

	final void removeFromTree(final HalfEdge e)
	{
		for (Iterator<AbstractHalfEdge> it = e.fanIterator(); it.hasNext(); )
		{
			HalfEdge f = (HalfEdge) it.next();
			HalfEdge h = uniqueOrientation(f);
			if(notProcessedObjects.remove(h))
				assert !tree.contains(h);
			if (!tree.remove(h))
				notInTree++;
			h.clearAttributes(AbstractHalfEdge.MARKED);
			assert !tree.contains(h);
		}
	}

	private boolean processAllHalfEdges()
	{
		double cost = -1.0;
		while (!tree.isEmpty() && (nrFinal == 0 || (moreTriangles && nrTriangles < nrFinal) || (!moreTriangles && nrTriangles > nrFinal)))
		{
			preProcessEdge();
			HalfEdge current = null;
			Iterator<QSortedTree.Node<HalfEdge>> itt = tree.iterator();
			if (processed > 0 && (processed % progressBarStatus) == 0)
				thisLogger().info("Edges processed: "+processed);
			while (itt.hasNext())
			{
				QSortedTree.Node<HalfEdge> q = itt.next();
				current = q.getData();
				assert current == uniqueOrientation(current);
				cost = q.getValue();
				if (nrFinal == 0 && cost > tolerance)
					break;
				if (canProcessEdge(current))
					break;
				if (thisLogger().isLoggable(Level.FINE))
					thisLogger().fine("Edge not processed: "+current);
				notProcessed++;
				tree.remove(current);
				notProcessedObjects.add(current);
				current = null;
			}
			if ((nrFinal == 0 && cost > tolerance) || current == null)
				break;
			// Update costs for edges which were not contracted because
			// canProcessEdge returned false
			if(tree.isEmpty())
			{
				for(HalfEdge e: notProcessedObjects)
					tree.insert(e, cost(e));
				notProcessedObjects.clear();
			}
			current = processEdge(current, cost);
			afterProcessHook();
			processed++;

			if (noSwapAfterProcessing || minCos < -1.0)
				continue;
			
			// Loop around current.apex with
			//   current = current.nextApexLoop();
			// to check all edges which have current.apex
			// as apical vertex and swap them if this improves
			// mesh quality.
			Vertex o = current.origin();
			boolean redo = true;
			while(redo)
			{
				redo = false;
				while(true)
				{
					if (current.checkSwap3D(mesh, minCos) >= 0.0 && current.canSwapTopology())
					{
						// Swap edge
						for (int i = 0; i < 3; i++)
						{
							current = current.next();
							removeFromTree(current);
						}
						HalfEdge sym = current.sym();
						for (int i = 0; i < 2; i++)
						{
							sym = sym.next();
							removeFromTree(sym);
						}
						Vertex a = current.apex();
						current = (HalfEdge) mesh.edgeSwap(current);
						swapped++;
						redo = true;
						// Now current = (ona)
						assert a == current.apex();
						for (int i = 0; i < 3; i++)
						{
							current = current.next();
							for (Iterator<AbstractHalfEdge> it = current.fanIterator(); it.hasNext(); )
							{
								HalfEdge e = uniqueOrientation((HalfEdge) it.next());
								addToTree(e);
							}
						}
						sym = current.next().sym();
						for (int i = 0; i < 2; i++)
						{
							sym = sym.next();
							for (Iterator<AbstractHalfEdge> it = sym.fanIterator(); it.hasNext(); )
							{
								HalfEdge e = uniqueOrientation((HalfEdge) it.next());
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
			afterSwapHook();
		}
		postProcessAllHalfEdges();
		return processed > 0;
	}

	public void setNoSwapAfterProcessing(boolean noSwapAfterProcessing)
	{
		this.noSwapAfterProcessing = noSwapAfterProcessing;
	}

	final void dumpState()
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

	void appendDumpState(ObjectOutputStream out)
		throws IOException
	{
	}

	@SuppressWarnings("unchecked")
	final boolean restoreState()
	{
		try
		{
			FileInputStream istream = new FileInputStream(dumpFile);
			ObjectInputStream q = new ObjectInputStream(istream);
			System.out.println("Loading restored state");
			mesh = (Mesh) q.readObject();
			tree = (QSortedTree<HalfEdge>) q.readObject();
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

	void appendRestoreState(ObjectInputStream q)
		throws IOException
	{
	}

	protected void afterProcessHook()
	{
	}

	protected void afterSwapHook()
	{
	}

	protected void updateCost(HalfEdge f)
	{
		updateCost(f, cost(f));
	}

	protected void updateCost(HalfEdge f, double newCost)
	{
		HalfEdge h = uniqueOrientation(f);
		// the edge has changed so we want canProcessEdge to be re-evaluated, so
		// we remove the edge from the notProcessObjects set
		notProcessedObjects.remove(h);
		if (tree.contains(h))
			tree.update(h, newCost);
		else
		{
			tree.insert(h, newCost);
			h.setAttributes(AbstractHalfEdge.MARKED);
		}
	}

	protected HalfEdge removeOneFromTree(HalfEdge e)
	{
		HalfEdge h = uniqueOrientation(e);
		if(notProcessedObjects.remove(h))
		{
			// and edge cannot be in tree and in notProcessedObjects at the same
			// time
			assert !tree.contains(h);
			return h;
		}
		if (!tree.remove(h))
			notInTree++;
		assert !tree.contains(h);
		return h;
	}
}
