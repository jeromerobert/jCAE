/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2011, by EADS France

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
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.amibe.util.QSortedTree;
import org.jcae.mesh.amibe.util.PAVLSortedTree;

import java.util.Stack;
import java.util.Iterator;
import java.util.Collection;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.util.HashFactory;

public abstract class AbstractAlgoVertex
{
	Mesh mesh;
	MeshLiaison liaison;
	double tolerance = 0.0;
	double maxEdgeLength = -1.0;
	int processed = 0;
	int notProcessed = 0;
	int notInTree = 0;
	private int progressBarStatus = 10000;
	double minCos = 0.95;
	QSortedTree<Vertex> tree = new PAVLSortedTree<Vertex>();
	private Collection<Vertex> nodeset;
	
	protected abstract void preProcessAllVertices();
	protected abstract void postProcessAllVertices();
	protected abstract boolean canProcessVertex(Vertex v);
	protected abstract boolean processVertex(Vertex v, double cost);
	protected abstract double cost(Vertex v);
	protected abstract Logger thisLogger();
	private static final String dumpFile = "/tmp/jcae.dump";

	AbstractAlgoVertex(final Mesh m)
	{
		this(m, null);
	}
	AbstractAlgoVertex(final Mesh m, final MeshLiaison meshLiaison)
	{
		mesh = m;
		liaison = meshLiaison;
	}
	public final void compute()
	{
		long startTime = System.nanoTime();
		thisLogger().info("Run "+getClass().getName());
		mesh.getTrace().println("# Begin "+getClass().getName());
		processed = 0;
		notProcessed = 0;
		notInTree = 0;
		preProcessAllVertices();
		thisLogger().info("Compute initial tree");
		computeTree();
		postComputeTree();
		processAllVertices();
		thisLogger().info("Number of processed points: "+processed);
		thisLogger().info("Total number of points which could not be processed: "+notProcessed);
		mesh.getTrace().println("# End "+getClass().getName());
		assert mesh.checkNoDegeneratedTriangles();
		assert mesh.checkNoInvertedTriangles();
		long endTime = System.nanoTime();
		thisLogger().log(Level.INFO, "Computation time: {0}ms",
			Double.toString((endTime - startTime)/1E6));
	}

	public void setProgressBarStatus(int n)
	{
		progressBarStatus = n;
	}

	private void computeTree()
	{
		if (nodeset == null)
			nodeset = mesh.getNodes();
		if (nodeset == null)
		{
			nodeset = HashFactory.<Vertex>createSet(mesh.getTriangles().size() / 2);
			for (Triangle f: mesh.getTriangles())
			{
				if (f.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				f.addVertexTo(nodeset);
			}
		}
		//  Compute vertex cost
		for (Vertex v: nodeset)
		{
			if (!canProcessVertex(v))
				continue;
			if (!tree.contains(v))
			{
				double val = cost(v);
				if (val <= tolerance)
					tree.insert(v, val);
			}
		}
	}

	void postComputeTree()
	{
	}

	// This routine is needed by DecimateHalfedge.
	void preProcessVertex()
	{
	}

	private boolean processAllVertices()
	{
		Stack<Vertex> stackNotProcessedObject = new Stack<Vertex>();
		Stack<Double> stackNotProcessedValue = new Stack<Double>();
		double cost = -1.0;
		while (!tree.isEmpty())
		{
			preProcessVertex();
			Vertex current = null;
			Iterator<QSortedTree.Node<Vertex>> itt = tree.iterator();
			if (processed > 0 && (processed % progressBarStatus) == 0)
				thisLogger().info("Vertices processed: "+processed);
			while (itt.hasNext())
			{
				QSortedTree.Node<Vertex> q = itt.next();
				current = q.getData();
				cost = q.getValue();
				if (cost > tolerance)
					break;
				if (canProcessVertex(current))
					break;
				if (thisLogger().isLoggable(Level.FINE))
					thisLogger().fine("Vertex not processed: "+current);
				notProcessed++;
				// Add a penalty to vertices which could not have been
				// processed.  This has to be done outside this loop,
				// because PAVLSortedTree instances must not be modified
				// when walked through.
				stackNotProcessedObject.push(current);
				if (tolerance != 0.0)
					stackNotProcessedValue.push(Double.valueOf(cost+0.7*(tolerance - cost)));
				else
					// tolerance = cost = 0
					stackNotProcessedValue.push(Double.valueOf(1.0));
				current = null;
			}
			if (cost > tolerance || current == null)
				break;
			// Update costs for edges which were not contracted
			while (stackNotProcessedObject.size() > 0)
			{
				double newCost = stackNotProcessedValue.pop().doubleValue();
				Vertex f = stackNotProcessedObject.pop();
				tree.update(f, newCost);
			}
			tree.remove(current);
			processVertex(current, cost);
			afterProcessHook();
			processed++;
		}
		postProcessAllVertices();
		return processed > 0;
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
			tree = (QSortedTree<Vertex>) q.readObject();
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

}
