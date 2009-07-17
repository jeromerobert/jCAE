/*
 * This source code is the property of EADS France. No part of it shall
 * be reproduced or transmitted without the express prior written
 * authorization of EADS France, and its contents shall not be disclosed.
 * Copyright EADS France.
 */
package org.jcae.netbeans.mesh;

import gnu.trove.TIntObjectHashMap;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.xmldata.Storage;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.AmibeToMesh;
import org.jcae.vtk.View;
import org.jcae.vtk.ViewableMesh;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

/**
 *
 * @author Gautam Botrel
 *
 */
public final class ViewBCellMeshAction extends CookieAction {

	@Override
	public String getName() {
		return "View Mesh";
	}

	@Override
	protected void performAction(Node[] arg0) {
		View v = ViewManager.getDefault().getCurrentView();
		TIntObjectHashMap<Vertex> refMap = new TIntObjectHashMap<Vertex>();
		MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault3D();
		mtb.addNodeList();
		org.jcae.mesh.amibe.ds.Mesh m = new org.jcae.mesh.amibe.ds.Mesh(mtb);
		BSubMesh subMesh = null;
		String name = "";
		for (Node n : arg0) {
			BCADGraphCell cell = (BCADGraphCell) n.getValue("CELL");
			subMesh = (BSubMesh) n.getValue("SUBMESH");
			Storage.readFace(m, cell, subMesh, refMap);
			name += cell.getType() + "" + cell.getId() + " ";
		}

		try {
			MeshWriter.writeObject3D(m, subMesh.getModel().getOutputDir(),
					"dummy.brep");
			AmibeToMesh toMesh = new AmibeToMesh(
					subMesh.getModel().getOutputDir());
			ViewableMesh vMesh = new ViewableMesh(toMesh.getMesh());
			vMesh.setName(name);
			v.add(vMesh);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	protected boolean asynchronous() {
		 // performAction() should run in event thread for actions that need a rendering of canva
		return false;
	}

	@Override
	protected boolean enable(Node[] arg0) {
		for (Node n : arg0) {
			BCADGraphCell cell = (BCADGraphCell)n.getValue("CELL");
			if (cell == null)
				return false;
			if (!cell.getType().equals(CADShapeEnum.FACE))
				return false;
//			if (cell == null ||cell.getDiscretizations() == null)
//				return false;
//			if (cell.getDiscretizations().isEmpty())
//				return false;
//			for (BDiscretization disc : cell.getDiscretizations()) {
//				if (disc.getMesh() == null)
//					return false;
		}
		return true;
	}

	@Override
	protected int mode() {
		return MODE_SOME;
	}

	@Override
	protected Class<?>[] cookieClasses() {
		return new Class[] {
			BCADGraphNode.class
		};
	}

	@Override
	public HelpCtx getHelpCtx() {
		return HelpCtx.DEFAULT_HELP;
	}



}
