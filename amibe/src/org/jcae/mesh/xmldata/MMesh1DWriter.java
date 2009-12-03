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
 * (C) Copyright 2009, by EADS France
 */


package org.jcae.mesh.xmldata;

import java.util.logging.Level;
import org.jcae.mesh.amibe.ds.MEdge1D;
import org.jcae.mesh.amibe.ds.MMesh1D;
import org.jcae.mesh.amibe.ds.MNode1D;
import org.jcae.mesh.amibe.ds.SubMesh1D;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeFactory;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADExplorer;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashSet;
import java.util.logging.Logger;


public class MMesh1DWriter
{
	private static final Logger LOGGER=Logger.getLogger(MMesh1DWriter.class.getName());
	
	/**
	 * Used by {@link #writeObject(org.jcae.mesh.amibe.ds.MMesh1D, String, String)}
	 */
	private static void writeObjectNodes(AmibeWriter.Dim1 aw,
		Iterator<MNode1D> nodesIterator, MMesh1D m1d)
		throws IOException
	{
		int i=0, nref=0;
		while(nodesIterator.hasNext())
		{
			MNode1D n=nodesIterator.next();
			aw.addNode(n.getParameter());
			if (null != n.getCADVertex())
			{
				aw.addNodeRef(n.getLabel());
				aw.addNodeRef(m1d.getIndexGeometricalVertex(n.getCADVertex()));
				nref++;
			}
			i++;
		}
	}
	
	/**
	 * Used by {@link #writeObject(org.jcae.mesh.amibe.ds.MMesh1D, String, String)}
	 */
	private static void writeObjectBeams(AmibeWriter.Dim1 aw,
		Iterator<MEdge1D> edgesIterator) throws IOException
	{
		int i=0;
		while(edgesIterator.hasNext())
		{
			MEdge1D e=edgesIterator.next();
			i++;
			MNode1D pt1 = e.getNodes1();
			MNode1D pt2 = e.getNodes2();
			aw.addBeam(pt1.getLabel(), pt2.getLabel());
		}
	}

	/**
	 * Write the current object to a XML file and binary files. The XML file
	 * have links to the binary files.
	 * @param xmlDir       name of the XML file
	 * @param brepFile     brep file
	 */
	public static void writeObject(MMesh1D m1d, String xmlDir, String brepFile)
	{
		try {
			//  Compute node labels
			m1d.updateNodeLabels();
			AmibeWriter.Dim1 amibeWriter = new AmibeWriter.Dim1(xmlDir);
			CADShape shape = m1d.getGeometry();
			amibeWriter.setShape(brepFile);
			// Create and fill the DOM
			int iEdge = 0;
			HashSet<CADEdge> setSeenEdges = new HashSet<CADEdge>();
			CADExplorer expE = CADShapeFactory.getFactory().newExplorer();
			for (expE.init(shape, CADShapeEnum.EDGE); expE.more(); expE.next()) {
				CADEdge E = (CADEdge) expE.current();
				SubMesh1D submesh = m1d.getSubMesh1DFromMap(E);
				if (null == submesh || setSeenEdges.contains(E)) {
					continue;
				}
				setSeenEdges.add(E);
				iEdge++;
				amibeWriter.setSubShape(iEdge);
				writeObjectNodes(amibeWriter, submesh.getNodesIterator(), m1d);
				writeObjectBeams(amibeWriter, submesh.getEdgesIterator());
				if (expE.more()) {
					amibeWriter.nextSubMesh();
				}
			}
			amibeWriter.finish();
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		}
	}
}

