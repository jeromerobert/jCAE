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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.amibe.ds;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TraceRecord implements TraceInterface
{
	private static final int NR_METHODS = 80;
	private static final int NR_LINES_BY_METHOD = 40;

        private static final Logger LOGGER=Logger.getLogger(TraceRecord.class.getName());
	private static final String MANY_SPACES = "                                                                                  ";
	private String logName;
	private PrintStream out = System.out;
	private boolean disabled;
	private int indentLevel;
	private String tab = "";
	private int cntLines;
	private int cntMethods;
	private int cntClasses;

	private int labelVertex;
	private final TObjectIntHashMap<Vertex> mapVertexId = new TObjectIntHashMap<Vertex>();
	private final TIntObjectHashMap<Vertex> mapIdVertex = new TIntObjectHashMap<Vertex>();

	private int labelTriangle;
	private final TObjectIntHashMap<Triangle> mapTriangleId = new TObjectIntHashMap<Triangle>();
	private final TIntObjectHashMap<Triangle> mapIdTriangle = new TIntObjectHashMap<Triangle>();

	public void setLogFile(String logName)
	{
		this.logName = logName;
		try {
			out = new PrintStream(new FileOutputStream(logName+".py"));
		} catch (FileNotFoundException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	public void createMesh(String meshName, Mesh mesh)
	{
		String meshVariable = meshName;
		println("import org.jcae.mesh.amibe.traits.MeshTraitsBuilder");
		println("import org.jcae.mesh.xmldata.MeshWriter");
		println("import org.jcae.mesh.amibe.ds.Mesh");

		println("cntModule = 0");
		println("mods = []");
		println("while True:");
		startScope();
		println("try:");
		startScope();
		println("mods.append(__import__(\""+logName+"_cl%d\" % cntModule))");
		println("cntModule += 1");
		endScope();
		println("except ImportError:");
		startScope();
		println("break");
		endScope();
		endScope();

		println("mtb = org.jcae.mesh.amibe.traits.MeshTraitsBuilder.getDefault3D()");
		println("mtb.addTraceReplay()");
		if (mesh.getNodes() instanceof Set)
			println("mtb.addNodeSet()");
		else
			println("mtb.addNodeList()");
		println(meshVariable+" = org.jcae.mesh.amibe.ds.Mesh(mtb)");
		println("[m.c("+meshVariable+") for m in mods]");
		println("org.jcae.mesh.xmldata.MeshWriter.writeObject3D("+meshVariable+
			", \""+logName+"-out\""+", None)");
		out.close();

		try {
			out = new PrintStream(new FileOutputStream(logName+"_cl"+cntClasses+".py"));
		} catch (FileNotFoundException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

		println("class c():");
		startScope();
		println("def startMethodHook(self):");
		println("    pass");
		println("def __init__(self, m):");
		startScope();
		println("self.m = m");

		mapIdVertex.put(-1, mesh.outerVertex);
		mapVertexId.put(mesh.outerVertex, -1);
		println("self.m.getTrace().add(self.m.outerVertex, -1)");
		for (Vertex v : mesh.getNodes())
		{
			createAndAdd(v);
			println("self.m.add(v)");
			checkLines();
		}
		for (Triangle t : mesh.getTriangles())
		{
			if (!t.hasAttributes(AbstractHalfEdge.OUTER))
			{
				createAndAdd(t);
				println("self.m.add(t)");
				checkLines();
			}
		}
		if (mesh.hasAdjacency())
		{
			println("self.m.buildAdjacency()");
			addAdjacentTriangles(mesh);
		}
		checkLines();
	}

	public void setDisabled(boolean b)
	{
		disabled = b;
	}

	public boolean getDisabled()
	{
		return disabled;
	}

	private void createAndAdd(Vertex v)
	{
		add(v);
		double[] pos = v.getUV();
		println("v = self.m.createVertex("+pos[0]+","+pos[1]+","+pos[2]+")");
		println("self.m.getTrace().add(v, "+labelVertex+")");
	}

	public void add(Vertex v)
	{
		labelVertex++;
		mapIdVertex.put(labelVertex, v);
		mapVertexId.put(v, labelVertex);
	}

	public void add(Vertex v, int id)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void remove(Vertex v)
	{
		int id = mapVertexId.get(v);
		println("self.m.getTrace().remove(self.m.getTrace().getVertex("+id+"))");
		mapIdVertex.remove(id);
		mapVertexId.remove(v);
	}

	public Vertex getVertex(int id)
	{
		return mapIdVertex.get(id);
	}

	public int getVertexId(Vertex v)
	{
		return mapVertexId.get(v);
	}

	private void createAndAdd(Triangle t)
	{
		add(t);
		println("vTemp0 = self.m.getTrace().getVertex("+mapVertexId.get(t.vertex[0])+")");
		println("vTemp1 = self.m.getTrace().getVertex("+mapVertexId.get(t.vertex[1])+")");
		println("vTemp2 = self.m.getTrace().getVertex("+mapVertexId.get(t.vertex[2])+")");
		println("t = self.m.createTriangle(vTemp0, vTemp1, vTemp2)");
		println("t.setGroupId("+t.getGroupId()+")");
		if (!t.isReadable())
			println("t.setReadable(False)");
		if (!t.isWritable())
			println("t.setWritable(False)");
		println("self.m.getTrace().add(t, "+labelTriangle+")");
	}

	public void add(Triangle t)
	{
		labelTriangle++;
		mapIdTriangle.put(labelTriangle, t);
		mapTriangleId.put(t, labelTriangle);
	}

	public void add(Triangle t, int id)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void remove(Triangle t)
	{
		int id = mapTriangleId.get(t);
		println("self.m.getTrace().remove(self.m.getTrace().getTriangle("+id+"))");
		mapIdTriangle.remove(id);
		mapTriangleId.remove(t);
	}

	public Triangle getTriangle(int id)
	{
		return mapIdTriangle.get(id);
	}

	public int getTriangleId(Triangle t)
	{
		return mapTriangleId.get(t);
	}

	public void addAdjacentTriangles(Mesh m)
	{
		if (disabled)
			return;
		for (Triangle t : m.getTriangles())
		{
			if (!t.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				continue;
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			AbstractHalfEdge ot = t.getAbstractHalfEdge();
			println("t = self.m.getTrace().getTriangle("+mapTriangleId.get(t)+")");
			println("ot = t.getAbstractHalfEdge()");
			println("for i in xrange(3):");
			startScope();
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				println("ot = ot.next()");
				if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				{
					Triangle s = ot.sym().getTri();
					if (!mapTriangleId.contains(s))
					{
						add(s);
						println("self.m.getTrace().add(ot.sym().getTri(), "+mapTriangleId.get(s)+")");
					}
				}
			}
			endScope();
			checkLines();
		}
	}

	private void startScope()
	{
		indentLevel += 1;
		tab = MANY_SPACES.substring(0, 4*indentLevel);
	}

	private void endScope()
	{
		indentLevel -= 1;
		tab = MANY_SPACES.substring(0, 4*indentLevel);
	}

	public void edgeSwap(AbstractHalfEdge h)
	{
		if (disabled)
			return;
		println("t = self.m.getTrace().getTriangle("+mapTriangleId.get(h.getTri())+")");
		println("ot = t.getAbstractHalfEdge()");
		if (h.getLocalNumber() == 1)
			println("ot = ot.next()");
		else if(h.getLocalNumber() == 2)
			println("ot = ot.prev()");
		println("self.m.edgeSwap(ot)");
		checkLines();
	}

	public void edgeCollapse(AbstractHalfEdge h, Vertex v)
	{
		if (disabled)
			return;
		createAndAdd(v);
		remove(h.origin());
		remove(h.destination());
		println("t = self.m.getTrace().getTriangle("+mapTriangleId.get(h.getTri())+")");
		println("ot = t.getAbstractHalfEdge()");
		if (h.getLocalNumber() == 1)
			println("ot = ot.next()");
		else if(h.getLocalNumber() == 2)
			println("ot = ot.prev()");
		println("self.m.edgeCollapse(ot, self.m.getTrace().getVertex("+mapVertexId.get(v)+"))");
		checkLines();
	}

	public void vertexSplitBefore(AbstractHalfEdge h, Vertex v)
	{
		if (disabled)
			return;
		createAndAdd(v);
		if (h.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
			println("# NONMANIFOLD vertexSplit");
		println("t = self.m.getTrace().getTriangle("+mapTriangleId.get(h.getTri())+")");
		println("ot = t.getAbstractHalfEdge()");
		if (h.getLocalNumber() == 1)
			println("ot = ot.next()");
		else if(h.getLocalNumber() == 2)
			println("ot = ot.prev()");
		println("ot = self.m.vertexSplit(ot, self.m.getTrace().getVertex("+mapVertexId.get(v)+"))");
	}

	public void vertexSplitAfter(AbstractHalfEdge h, Vertex v)
	{
		if (disabled)
			return;
		if (h.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			println("fanIt = ot.fanIterator()");
			for (Iterator<AbstractHalfEdge> fanIt = h.fanIterator(); fanIt.hasNext();)
			{
				println("ot = fanIt.next()");
				traceSplitTriangle(fanIt.next());
			}
		}
		else
			traceSplitTriangle(h);
		checkLines();
	}

	/*
	 * When edge (od) is manifold, vertex split adds two triangles
	 * and returned edge is (on) on t1.
	 * When edge (od) is non-manifold, returned edge is either (on)
	 * or (dn).  By convention, t1 cannot be outer (and thus t3).
	 * Returned edge either belongs to t1 or t3.
	 *            V1                             V1
	 *            /'\                            /|\
	 *          /     \                        /  |  \
	 *        /         \                    /    |    \
	 *      /             \                /      |      \
	 *    /       t1        \            /   t1  v|  t3    \
	 * o +-------------------+ d ---> o +---------+---------+ d
	 *    \       t2        /            \   t4   |  t2    /
	 *      \             /                \      |      /
	 *        \         /                    \    |    /
	 *          \     /                        \  |  /
	 *            \,/                            \|/
	 *            V2                             V2
	 */
	private void traceSplitTriangle(AbstractHalfEdge h)
	{
		if(mapTriangleId.contains(h.getTri()))
		{
			// h is on t1
			// Add t4
			add(h.sym().getTri());
			println("self.m.getTrace().add(ot.sym().getTri(), "+mapTriangleId.get(h.sym().getTri())+")");
			// Add t3
			add(h.next().sym().getTri());
			println("self.m.getTrace().add(ot.next().sym().getTri(), " + mapTriangleId.get(h.next().sym().getTri()) + ")");
		}
		else
		{
			// h is on t3
			// Add t3
			add(h.getTri());
			println("self.m.getTrace().add(ot.getTri(), "+mapTriangleId.get(h.getTri())+")");
			// Add t4.  WARNING: t2 and t4 are outer and are not connected!
			add(h.prev().sym().prev().sym().getTri());
			println("self.m.getTrace().add(ot.prev().sym().prev().sym().getTri(), " + mapTriangleId.get(h.prev().sym().prev().sym().getTri()) + ")");
		}
	}

	public void moveVertex(Vertex v)
	{
		if (!disabled)
		{
			double[] pos = v.getUV();
			println("# Vertex "+v);
			println("v = self.m.getTrace().getVertex("+mapVertexId.get(v)+")");
			println("v.moveTo("+pos[0]+", "+pos[1]+", "+pos[2]+")");
		}
		checkLines();
	}

	private void checkLines()
	{
		if (cntLines > NR_LINES_BY_METHOD)
		{
			out.println(tab+"self.cont"+cntMethods+"()\n");
			if (indentLevel > 0)
				endScope();
			out.println(tab+"def cont"+cntMethods+"(self):");
			startScope();
			out.println(tab+"self.startMethodHook()");
			cntLines = 0;
			cntMethods++;
			checkMethods();
		}
	}

	private void checkMethods()
	{
		if (cntMethods > NR_METHODS)
		{
			out.println(tab+"pass");
			cntClasses++;
			endScope();
			endScope();
			out.close();

			try {
				out = new PrintStream(new FileOutputStream(logName+"_cl"+cntClasses+".py"));
			} catch (FileNotFoundException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}

			out.println(tab+"class c():");
			startScope();
			out.println(tab+"def startMethodHook(self):");
			out.println(tab+"    pass");
			out.println(tab+"def __init__(self, m):");
			startScope();
			out.println(tab+"self.m = m");
			cntMethods = 0;
		}
	}

	public void println(String x)
	{
		if (!disabled)
		{
			out.println(tab+x);
			cntLines++;
		}
	}

	public void finish()
	{
		cntClasses++;
		endScope();
		endScope();
		out.close();
	}

}
