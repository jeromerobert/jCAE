/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2009, by EADS France
 
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

package org.jcae.mesh.bora.xmldata;

import java.util.logging.Level;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.Constraint;
import org.jcae.mesh.xmldata.FilterInterface;
import org.jcae.mesh.xmldata.UNVGenericWriter;
import org.jcae.mesh.cad.CADShapeEnum;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.IOException;
import java.util.logging.Logger;
import org.xml.sax.SAXException;

public class BoraToUnvConvert implements FilterInterface
{
	private static final Logger LOGGER=Logger.getLogger(BoraToUnvConvert.class.getName());

	private final UNVGenericWriter unvWriter;
	private final TIntObjectHashMap<BDiscretization> mapDiscretizations = new TIntObjectHashMap<BDiscretization>();
	private TIntIntHashMap mapReferences;
	private double [] coordRefs;

	private int nrBoundaryNodes = 0;
	private int nrInnerNodes = 0;
	private int indexBoundaryNodes = 0;
	private int indexInnerNodes = 0;
	private int indexElements = 0;

	public BoraToUnvConvert (String unvFile, BSubMesh submesh)
	{
		this.unvWriter = new UNVGenericWriter(unvFile);
		for (Constraint cons : submesh.getConstraints())
		{
			BCADGraphCell cell = cons.getGraphCell();
			BDiscretization d = cell.getDiscretizationSubMesh(submesh);
			mapDiscretizations.put(d.getId(), d);
		}
	}

	public final void collectBoundaryNodes(int[] shapes)
	{
		for (int id : shapes)
		{
			BDiscretization d = mapDiscretizations.get(id);
			int [] refs = null;
			try {
				refs = Storage.readNodeReferences(d);
			}
			catch (IOException ex)
			{
				LOGGER.warning("Error when reading references");
			}
			if (refs == null)
				continue;
			int numberOfNodes = Storage.getNumberOfNodes(d);
			nrBoundaryNodes += refs.length / 2;
			nrInnerNodes    += numberOfNodes - refs.length / 2;
		}
	}

	public final void beforeProcessingAllShapes(boolean writeNormal)
	{
		coordRefs = new double[3*nrBoundaryNodes];
		mapReferences = new TIntIntHashMap(nrBoundaryNodes);
	}

	public final void afterProcessingAllShapes()
	{
		unvWriter.finish(nrBoundaryNodes, nrInnerNodes, indexElements, coordRefs);
	}

	public final void processOneShape(int groupId, String groupName, int iFace)
	{
		BDiscretization d = mapDiscretizations.get(iFace);
		int[] refs = null;
		double[] coord = null;
		int[] connectivity = null;
		try {
			refs = Storage.readNodeReferences(d);
			coord = Storage.readNodeCoordinates(d);
			connectivity = Storage.readConnectivity(d);
		}
		catch (IOException ex)
		{
			LOGGER.warning("Error when reading references");
			return;
		}
		TIntIntHashMap labels = new TIntIntHashMap(coord.length / 3);
		for (int i = 0; i < refs.length; i+= 2)
		{
			if (!mapReferences.containsValue(refs[i+1]))
			{
				indexBoundaryNodes++;
				mapReferences.put(refs[i+1], indexBoundaryNodes + nrInnerNodes);
			}
			labels.put(refs[i], mapReferences.get(refs[i+1]));
		}
		// Convert inner nodes and store boundary nodes
		double [] c = new double[3];
		int idx = 0;
		for (int i = 0; i < coord.length; i+= 3)
		{
			idx++;
			if (mapReferences.contains(idx))
			{
				int label = mapReferences.get(idx) - nrInnerNodes;
				coordRefs[3*label-3] = coord[i];
				coordRefs[3*label-2] = coord[i+1];
				coordRefs[3*label-1] = coord[i+2];
			}
			else
			{
				c[0] = coord[i];
				c[1] = coord[i+1];
				c[2] = coord[i+2];
				indexInnerNodes++;
				labels.put(idx, indexInnerNodes);
				unvWriter.writeNode(indexInnerNodes, c);
			}
		}
		// Convert elements
		TIntArrayList group = new TIntArrayList();
		if (d.getGraphCell().getType().equals(CADShapeEnum.EDGE))
		{
			int [] localIndex = new int[3];
			localIndex[0] = 2;
			for (int i = 0; i < connectivity.length; i += localIndex.length - 1)
			{
				if (connectivity[i] < 0 || connectivity[i+1] < 0)
				{
					// Skip outer beams
					continue;
				}
				localIndex[1] = labels.get(connectivity[i]);
				localIndex[2] = labels.get(connectivity[i+1]);
				indexElements++;
				unvWriter.writeElement(indexElements, localIndex);
				group.add(indexElements);
			}
		}
		else if (d.getGraphCell().getType().equals(CADShapeEnum.FACE))
		{
			int [] localIndex = new int[4];
			localIndex[0] = 3;
			for (int i = 0; i < connectivity.length; i += localIndex.length - 1)
			{
				if (connectivity[i] < 0 || connectivity[i+1] < 0 || connectivity[i+2] < 0)
				{
					// Skip outer triangles
					continue;
				}
				localIndex[1] = labels.get(connectivity[i]);
				localIndex[2] = labels.get(connectivity[i+1]);
				localIndex[3] = labels.get(connectivity[i+2]);
				indexElements++;
				unvWriter.writeElement(indexElements, localIndex);
				group.add(indexElements);
			}
		}
		else if (d.getGraphCell().getType().equals(CADShapeEnum.SOLID))
		{
			int [] localIndex = new int[5];
			localIndex[0] = 4;
			for (int i = 0; i < connectivity.length; i += localIndex.length - 1)
			{
				localIndex[1] = labels.get(connectivity[i]);
				localIndex[2] = labels.get(connectivity[i+1]);
				localIndex[3] = labels.get(connectivity[i+2]);
				localIndex[4] = labels.get(connectivity[i+3]);
				indexElements++;
				unvWriter.writeElement(indexElements, localIndex);
				group.add(indexElements);
			}
		}
		unvWriter.writeGroup(groupId, groupName, group.toArray());
	}

	public static void main(String[] args)
	{
		try {
			org.jcae.mesh.bora.ds.BModel model = BModelReader.readObject(args[0],
				args[1]);
			BSubMesh s = model.getSubMeshes().iterator().next();
			BoraToUnvConvert conv = new BoraToUnvConvert(args[2], s);
			TIntArrayList listOfFaces = new TIntArrayList();
			for (java.util.Iterator<BCADGraphCell> its = model.getGraph().getRootCell().shapesExplorer(CADShapeEnum.EDGE); its.hasNext();) {
				BCADGraphCell cell = its.next();
				BDiscretization d = cell.getDiscretizationSubMesh(s);
				if (d != null) {
					listOfFaces.add(d.getId());
				}
			}
			for (java.util.Iterator<BCADGraphCell> its = model.getGraph().getRootCell().shapesExplorer(CADShapeEnum.FACE); its.hasNext();) {
				BCADGraphCell cell = its.next();
				BDiscretization d = cell.getDiscretizationSubMesh(s);
				if (d != null) {
					listOfFaces.add(d.getId());
				}
			}
			conv.collectBoundaryNodes(listOfFaces.toArray());
			conv.beforeProcessingAllShapes(false);
			int groupId = 0;
			for (int iFace : listOfFaces.toArray()) {
				groupId++;
				conv.processOneShape(groupId, "test" + groupId, iFace);
			}
			conv.afterProcessingAllShapes();
		} catch (SAXException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

}

