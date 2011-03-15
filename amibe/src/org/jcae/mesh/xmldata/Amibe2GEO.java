/*
 * Project Info:  http://jcae.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2011, by EADS France
 */


package org.jcae.mesh.xmldata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jcae.mesh.xmldata.AmibeReader.Group;
import org.jcae.mesh.xmldata.AmibeReader.SubMesh;
import org.xml.sax.SAXException;

/**
 *
 * @author Jerome Robert
 */
public class Amibe2GEO {	
	private class Node
	{
		public final double x, y, z;
		public int surfaceID = -1;
		public int edgeID = -1;
		private List<Object> elements = new ArrayList<Object>(6);

		private Node(double[] buffer) {
			x = buffer[0];
			y = buffer[1];
			z = buffer[2];
		}

		public void addElement(Object element)
		{
			elements.add(element);
		}

		public void getEdges(Collection<Edge> edges)
		{
			for(Object o:elements)
				if(o instanceof Edge)
					edges.add((Edge) o);
		}

		/**
		 * @param result contains {nb edges elements, nb triangles elements}
		 * at return
		 */
		public void nbElement(int[] result)
		{
			result[0] = 0;
			result[1] = 0;
			for(Object o:elements)
			{
				if(o instanceof Triangle)
					result[1] ++;
				else
					result[0] ++;
			}
		}
	}
	private class Edge
	{
		public final Node n1, n2;
		public String group;
		public int id;

		public Edge(Node n1, Node n2) {
			this.n1 = n1;
			this.n2 = n2;
		}

		@Override
		public boolean equals(Object obj) {
			Edge o = (Edge) obj;
			return (n1 == o.n1 && n2 == o.n2) || (n1 == o.n2 && n2 == o.n1);
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 53 * hash + (this.n1 != null ? this.n1.hashCode() : 0);
			hash = 53 * hash + (this.n2 != null ? this.n2.hashCode() : 0);
			return hash;
		}

		public Collection<Triangle> getTriangles()
		{
			HashSet<Triangle> toReturn = new HashSet<Triangle>(4);
			for(Object e:n1.elements)
			{
				if(e instanceof Triangle)
				{
					Triangle t=(Triangle) e;
					if(equals(t.edge1) || equals(t.edge2) || equals(t.edge3))
						toReturn.add(t);
				}
			}
			for(Object e:n2.elements)
			{
				if(e instanceof Triangle)
				{
					Triangle t=(Triangle) e;
					if(equals(t.edge1) || equals(t.edge2) || equals(t.edge3))
						toReturn.add(t);
				}
			}
			return toReturn;
		}
	}

	private class Triangle
	{
		public String group;
		public final Edge edge1, edge2, edge3;
		public final int id;

		private Triangle(int[] buffer, Map<Edge, Edge> edgesMap, Node[] nodes, int id) {
			edge1 = createEdge(buffer[0], buffer[1], nodes, edgesMap);
			edge2 = createEdge(buffer[1], buffer[2], nodes, edgesMap);
			edge3 = createEdge(buffer[2], buffer[0], nodes, edgesMap);
			nodes[buffer[0]].addElement(this);
			nodes[buffer[1]].addElement(this);
			nodes[buffer[2]].addElement(this);
			this.id = id;
		}

		private Edge createEdge(int i1, int i2, Node[] nodes,
			Map<Edge, Edge> edgesMap) {
			Edge e = new Edge(nodes[i1], nodes[i2]);
			Edge toReturn = edgesMap.get(e);
			if(toReturn == null)
			{
				edgesMap.put(e, e);
				toReturn = e;
			}
			return toReturn;
		}

		public void getNodes(Set<Node> result)
		{
			result.add(edge1.n1);
			result.add(edge1.n2);
			result.add(edge2.n1);
			result.add(edge2.n2);
			result.add(edge3.n1);
			result.add(edge3.n2);
		}
	}
	private final String inputDir;
	
	public Amibe2GEO(String inputDir)
	{
		this.inputDir = inputDir;
	}

	public void writeProject(String directory, String name) throws SAXException, IOException
	{
		File pd = new File(directory, name);
		pd.mkdirs();
		PrintStream ps = new PrintStream(new FileOutputStream(new File(pd, name+".geo")));
		write(ps);
		ps.close();
	}

	public void write(PrintStream out) throws SAXException, IOException
	{
		writeHeader(out);
		AmibeReader.Dim3 ar = new AmibeReader.Dim3(inputDir);
		SubMesh sm = ar.getSubmeshes().get(0);
		Node[] nodes = readNodes(sm.getNodes());
		Map<Edge, Edge> edgesMap = new HashMap<Edge, Edge>();
		Triangle[] triangles = readTriangles(sm.getTriangles(), edgesMap, nodes);
		Edge[] beams = readBeams(sm.getBeams(), nodes);
		Map<String, Integer> groupIDs = new HashMap<String, Integer>();
		int gid = 1;
		for(Group g:sm.getGroups())
		{
			for(int i:g.readBeamsIds())
				beams[i].group = g.getName();
			for(int i:g.readTria3Ids())
				triangles[i].group = g.getName();
			groupIDs.put(g.getName(), gid++);
		}
		LinkedHashSet<Node> surfaceNodes = new LinkedHashSet<Node>();
		for(Triangle t:triangles)
			t.getNodes(surfaceNodes);
		LinkedHashSet<Node> beamNodes = new LinkedHashSet<Node>();
		for(Edge e:beams)
		{
			beamNodes.add(e.n1);
			beamNodes.add(e.n2);
		}

		HashSet<Node> junctions = new HashSet<Node>(surfaceNodes);
		junctions.retainAll(beamNodes);
		writeDim(out, surfaceNodes.size(), edgesMap.size(), triangles.length,
			beamNodes.size(), beams.length, junctions.size());
		writeSurfNode(out, surfaceNodes);
		writeEdges(out, edgesMap.values());
		writeTriangles(out, triangles, groupIDs);
		writeWireNode(out, beamNodes, groupIDs);
		writeSegments(out, beams, groupIDs);
		writeJunctions(out, nodes);
		writeFreeSpace(out);
		writeProperties(out, sm.getGroups());
		writeObject(out);
		writeSubObject(out, sm.getGroups());
		out.println("[ENDFILE]");
	}

	private Edge[] readBeams(IntFileReader beams, Node[] nodes) 
		throws IOException
	{
		Edge[] toReturn = new Edge[(int)(beams.size()/2)];
		int[] buffer = new int[2];
		for(int i = 0; i < toReturn.length; i++)
		{
			beams.get(buffer);
			Node n1 = nodes[buffer[0]];
			Node n2 = nodes[buffer[1]];
			toReturn[i] = new Edge(n1, n2);
			toReturn[i].id = i+1;
			n1.addElement(toReturn[i]);
			n2.addElement(toReturn[i]);
		}
		return toReturn;
	}
	
	private Triangle[] readTriangles(IntFileReader trias,
		Map<Edge, Edge> edgesMap, Node[] nodes) throws IOException
	{
		Triangle[] toReturn = new Triangle[(int)(trias.size()/3)];
		int[] buffer = new int[3];
		for(int i = 0; i < toReturn.length; i++)
		{
			trias.get(buffer);
			toReturn[i] = new Triangle(buffer, edgesMap, nodes, i+1);
		}
		return toReturn;
	}

	private Node[] readNodes(DoubleFileReader dfr) throws IOException
	{
		Node[] toReturn = new Node[(int)(dfr.size()/3)];
		double[] buffer = new double[3];
		for(int i = 0; i<toReturn.length; i++)
		{
			dfr.get(3*i, buffer);
			toReturn[i] = new Node(buffer);
		}
		return toReturn;
	}

	private void writeHeader(PrintStream out)
	{
		out.println("[GENERAL]");
		out.println("3.71 //Version number");
		out.println(new Date()+" //Creation date");
		out.println("jCAE//Origin software");
		out.println("FMM//Analysis type");
		out.println("2 //Precision 1:simple 2:double");
		out.println("[END]");
		out.println();
	}

	private void writeDim(PrintStream out, int nbSurfNod, int nbEdge,
		int nbTriangles, int nbWireNode, int nbWire, int junctionSize)
	{
		out.println("[DIM]");
		out.println(nbSurfNod + " //Number of surface-nod");
		out.println(nbEdge + " 0 //Number and multiplicity of edge");
		out.println(nbTriangles + " //Number of triangle");
		out.println(nbWireNode + " 0 //Number and multiplicity of wire-nod");
		out.println(nbWire + " //Number of wire");
		out.println(junctionSize
			+ " 0 0 //Number of junction and number max of triangle and wire");
		out.println("1 0  //Number and rank of FreeSpace");
		out.println("0  //Number and rank of Image");
		out.println("0  //Number and rank of Lossy plane");
		out.println("0  //Number and rank of Full-Reg");
		out.println("0  //Number and rank of Parallel plate");
		out.println("0  //Number and rank of RectWg");
		out.println("0  //Number and rank of RectCavit");
		out.println("0 //Number and rank of CylWg");
		out.println("0 //Number and rank of Cylcavit");
		out.println("0 //Number and rank of CoaxialWg");
		out.println("0 //Number and rank of CoaxialCavit");
		out.println("1 //Number of Object");
		out.println("5 //Number of Sub-Object");
		out.println("0 //Number of Sources");
		out.println("3 //Number of Properties");
		out.println("0 //Number of Em-Field");
		out.println("0 //Number of Antenna");
		out.println("[END]");
		out.println();
	}

	private void writeSurfNode(PrintStream out, Collection<Node> surfaceNodes) {
		out.println("[SURFACE-NOD]");
		out.println("TNod   Label  X(m)             Y(m)             Z(m)             Region");
		writeUselessLine(out);
		int i = 1;
		for(Node n:surfaceNodes)
		{
			n.surfaceID = i;
			out.println(i+" "+i+" "+n.x+" "+n.y+" "+n.z+" 1");
			i++;
		}
		out.println("[END]");
		out.println();
	}

	private void writeEdges(PrintStream out, Collection<Edge> edges)
	{
		out.println("[EDGE]");
		out.println("Edg    Labl   Np   Ns   Nd1    Nd2    Mult Tg1  Tg2    .....");
		writeUselessLine(out);
		int i = 1;
		for(Edge e:edges)
		{
			e.id = i;
			Collection<Triangle> ts = e.getTriangles();
			out.print(i+" 0 0 0 "+e.n1.surfaceID+" "+e.n2.surfaceID+" "+(ts.size()-1));
			for(Triangle t:ts)
				out.print(" "+t.id);
			out.println();
			i++;
		}
		out.println("[END]");
		out.println();
	}

	private void writeTriangles(PrintStream out, Triangle[] triangles, Map<String, Integer> groupIDs) {
		out.println("[TRIANGLE]");
		out.println("Tgl    Label  Type   Met Reg     IP Edg1   Edg2   Edg3   SubObj Object Nd1    Nd2    Nd3");
		writeUselessLine(out);
		HashSet<Node> nodes = new HashSet<Node>();
		for(Triangle t:triangles)
		{
			int gid = groupIDs.get(t.group);
			out.print(t.id+" "+t.id+" COND   MOM 01 01 "+gid+" "+t.edge1.id+" "+t.edge2.id+" "+t.edge3.id);
			out.print(" "+gid+" 0");
			t.getNodes(nodes);
			for(Node n:nodes)
				out.print(" "+n.surfaceID);
			nodes.clear();
			out.println();
		}
		out.println("[END]");
		out.println();
	}

	private void writeWireNode(PrintStream out, Collection<Node> wireNodes,
		Map<String, Integer> groupIDs)
	{
		out.println("[WIRE-NOD]");
		out.println("WNod   Label  X(m) Y(m) Z(m) Re Np   Ns   Mult Sg1  Sg2 ....");
		writeUselessLine(out);
		int id = 1;
		ArrayList<Edge> edges = new ArrayList<Edge>(3);
		for(Node n: wireNodes)
		{
			n.edgeID = id;
			int propertyID = 0;
			n.getEdges(edges);
			if(edges.size() == 2)
			{
				String g = edges.get(0).group;
				if(g.equals(edges.get(1).group) && isNodalResistance(g))
					propertyID = groupIDs.get(edges.get(0).group);
			}
			out.print(id+" "+id+" "+n.x+" "+n.y+" "+n.z+" 01 "+propertyID+" 0 "+(edges.size()-1));
			for(Edge e: edges)
				out.print(" "+e.id);
			out.println();
			edges.clear();
			id ++;
		}
		out.println("[END]");
		out.println();
	}


	private void writeSegments(PrintStream out, Edge[] beams,
		Map<String, Integer> groupIDs) {
		out.println("[SEGMENT]");
		out.println("Sgl    Labl   Typ    Met Regi. Radius       Np   Elm1   Elm2  SubObj Object");
		writeUselessLine(out);
		for(Edge e: beams)
		{
			int subObject = groupIDs.get(e.group);
			int propertyID = isNodalResistance(e.group) ? 0 : subObject;
			out.println(e.id+" "+e.id+" COND   MOM 01 01 "+getWireRadius(e.group)+
				" "+propertyID+" "+e.n1.edgeID+" "+e.n2.edgeID+" "+subObject+" 0");
		}
		out.println("[END]");
		out.println();
	}

	private void writeJunctions(PrintStream out, Node[] nodes)
	{
		out.println("[JUNCTION]");
		out.println("jun JWmult JTMult WElm   JTNd");
		writeUselessLine(out);
		writeUselessLine(out);
		writeUselessLine(out);
		writeUselessLine(out);
		int k = 1;
		int[] nbElement = new int[2];
		for(Node node:nodes)
		{
			if(node.edgeID >= 0 && node.surfaceID >=0)
			{
				writeUselessLine(out);
				node.nbElement(nbElement);
				out.println(k+" "+nbElement[0]+" "+nbElement[1]+" "+node.edgeID+" "+node.surfaceID);
				for(int j = 0; j<nbElement[1]+1; j++)
					writeUselessLine(out);
				k++;
			}
		}
		out.println("[END]");
		out.println();
	}
	private void writeUselessLine(PrintStream out)
	{
		out.println("- \\_o< -- COIN COIN -- PAN -- \\_x<");
	}

	private void writeFreeSpace(PrintStream out)
	{
		out.println("[FREESPACE]");
		out.println("Description for Reg Zone number 1");
		out.println("1.0000E+00 0.0000E+00 1.0000E+00 0.0000E+00  //Epsilon,Mu for these zone and Name");
		out.println("[END]");
		out.println();
	}

	private void writeProperties(PrintStream out, List<Group> groups)
	{
		out.println("[PROPERTY]");
		int i = 1;
		for(Group g:groups)
			out.println((i++)+" 0 "+getResistivity(g.getName())+" "+g.getName());
		out.println("[END]");
		out.println();
	}

	private void writeObject(PrintStream out)
	{
		out.println("[OBJECT]");
		out.println("00000 not defined");
		out.println("[END]");
		out.println();
	}

	private void writeSubObject(PrintStream out, List<Group> groups)
	{
		out.println("[SUB-OBJECT]");
		out.println("0 not defined");
		int i = 1;
		for(Group g:groups)
			out.println((i++)+" "+g.getName());
		out.println("[END]");
		out.println();
	}

	/** Tell if resistances must be on beams or on nodes in a group */
	protected boolean isNodalResistance(String groupName)
	{
		return false;
	}

	protected double getWireRadius(String groupName)
	{
		return 0.1;
	}

	protected double getResistivity(String name) {
		return 0;
	}
}
