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

package org.jcae.vtk;

import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.ds.Constraint;
import org.jcae.mesh.bora.xmldata.BModelReader;
import org.jcae.mesh.bora.xmldata.Storage;
import org.jcae.mesh.cad.CADShapeEnum;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Converts a list of discretization to vtk mesh object
 * @see ViewableMesh
 * @author Gautam Botrel
 *
 */
public class BoraToMesh {
	private final static Logger LOGGER=Logger.getLogger(BoraToMesh.class.getName());
	private final  Map<String, LeafNode.DataProvider> mesh;

	public BoraToMesh(String xmlBoraDir) {
		this(buildMeshData(xmlBoraDir));
	}

	public BoraToMesh(Map<String, Collection<BDiscretization>> meshData) {
		mesh = new  HashMap<String, LeafNode.DataProvider>(meshData.keySet().size());
		for (String group : meshData.keySet()) {
			mesh.put(group, new GroupData(meshData.get(group)));
		}
	}

	private static Map<String, Collection<BDiscretization>> buildMeshData(String xmlBoraDir) {
		BModel bModel = BModelReader.readObject(xmlBoraDir);
		Map<String, Collection<BDiscretization>> meshData = new HashMap<String, Collection<BDiscretization>>();
		for (BSubMesh subMesh : bModel.getSubMeshes()){
			for (Constraint cons : subMesh.getConstraints()) {
				BDiscretization discr = cons.getGraphCell().getDiscretizationSubMesh(subMesh);
				if (discr == null)
					continue;
				String group = cons.getGroup();
				if (group == null)
					group = "DEFAULT_GROUP";
				Collection<BDiscretization> gDiscr = meshData.get(group);
				if (gDiscr == null)
				{
					gDiscr = new ArrayList<BDiscretization>();
					meshData.put(group, gDiscr);
				}
				gDiscr.add(discr);
			}
		}
		return meshData;
	}

	private static class GroupData extends LeafNode.DataProvider
	{
		private Collection<BDiscretization> meshData;

		GroupData(Collection<BDiscretization> meshData)
		{
			this.meshData = meshData;
		}

		/**
		 * Completes the nodes and polys infos of the LeafNode.DataProvider obj
		 * @param discr
		 */
		private void processOneDiscr(BDiscretization d) {
			int offset = lNodes.length / 3 -1;
			int[] connectivity = null;

			try {
				appendNodes(convertToFloat(Storage.readNodeCoordinates(d)));
				connectivity = Storage.readConnectivity(d);
			}
			catch (IOException ex) {
				LOGGER.severe("Unable to read mesh data : " + ex);
				return;
			}
			if (d.getGraphCell().getType().equals(CADShapeEnum.EDGE)) {
				// beams
				for (int i = 0; i < connectivity.length; i++)
					connectivity[i] += offset;
			
				appendLines(Utils.createBeamCells(connectivity));
				return;
			}
			else if (d.getGraphCell().getType().equals(CADShapeEnum.FACE)) {
				//triangles
				lNbPolys += connectivity.length / 3;
				appendPolys(Utils.createTriangleCells(connectivity, offset));
			}
			else if (d.getGraphCell().getType().equals(CADShapeEnum.SOLID)) {
				// quads
				lNbPolys += connectivity.length / 4;
				appendPolys(Utils.createQuadsCells(connectivity, offset));
			}
		}

		private int [] lPolys = new int[0];
		private int [] lLines = new int[0];
		private float [] lNodes = new float[0];
		private int lNbPolys = 0;
		private void appendPolys(int[] toAppend) {
			lPolys = Arrays.copyOf(lPolys, lPolys.length + toAppend.length);
			System.arraycopy(toAppend, 0, lPolys, lPolys.length - toAppend.length, toAppend.length);
		}

		private void appendLines(int[] toAppend) {
			lLines = Arrays.copyOf(lLines, lLines.length + toAppend.length);
			System.arraycopy(toAppend, 0, lLines, lLines.length - toAppend.length, toAppend.length);
		}

		private void appendNodes(float[] toAppend) {
			lNodes = Arrays.copyOf(lNodes, lNodes.length + toAppend.length);
			System.arraycopy(toAppend, 0, lNodes, lNodes.length - toAppend.length, toAppend.length);
		}

		@Override
		public void load()
		{
			for (BDiscretization discr : meshData) {
				processOneDiscr(discr);
			}
			setNodes(lNodes);
			setLines(lLines);
			setPolys(lNbPolys, lPolys);
		}

		private static void printNodes(float[] tab) {
			System.out.println("Nodes : ");
			for (int i = 0; i < tab.length;) {
				System.out.println("Node " + (i/3 + 1) + " {" + tab[i++] + ","
						+ tab[i++] + ","
						+ tab[i++] + "}");
			}
		}

		private static float[] convertToFloat(double[] tab) {
			float[] res = new float[tab.length];
			for (int i = 0; i < res.length; i++) {
				res[i] = (float) tab[i];
			}
			return res;
		}
	}


	public Map<String, LeafNode.DataProvider> getMesh()
	{
		return mesh;
	}

}
