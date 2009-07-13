/*
 * This source code is the property of EADS France. No part of it shall
 * be reproduced or transmitted without the express prior written
 * authorization of EADS France, and its contents shall not be disclosed.
 * Copyright EADS France.
 */
package org.jcae.netbeans.mesh;

import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BDiscretization;
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
		for (Node n : arg0) {
			BCADGraphCell cell = (BCADGraphCell)n.getValue("CELL");
			System.out.println("Cell type : " + cell.getType());
			for (BDiscretization discr :cell.getDiscretizations()) {
				try {
				Mesh2D mesh = (Mesh2D)discr.getMesh();
				MeshWriter.writeObject3D(mesh, "/tmp/Bora", "dummy.brep");
				AmibeToMesh toMesh = new AmibeToMesh("/tmp/Bora");
				ViewableMesh vMesh = new ViewableMesh(toMesh.getMesh());
				vMesh.setName(n.getName());
				v.add(vMesh);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
//			view((Mesh) cell.getShape(), cell.getType() + ""+cell.getId());
		}
	}


//	WeakReference<ViewableCAD> viewableRef = new WeakReference<ViewableCAD>(null);

//	private void view(OCCShape shape, String name) {
//		View v = ViewManager.getDefault().getCurrentView();
//		for (Viewable w : v.getViewables()) {
//			if (w instanceof ViewableCAD) {
//				ViewableCAD wCad = (ViewableCAD)w;
//				if (wCad.getName().equals(name)) {
//					wCad.refresh();
//					return;
//				}
//			}
//		}
//
//		ViewableCAD	viewable = new ViewableCAD(shape.getShape());
//		viewable.setName(name);
//
//		// TODO manage selection...
//		// SelectionManager.getDefault().addInteractor(viewable, shape.getShape());
//		v.add(viewable);
//	}
//
//		public void view() {
//		if (subMeshNode != null && getMesh() != null && getMesh().getBoraModel() != null) {
//			try {
//				MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault3D();
//				mtb.addNodeList();
//				org.jcae.mesh.amibe.ds.Mesh m = new org.jcae.mesh.amibe.ds.Mesh(
//						mtb);
//				Storage.readAllFaces(m,
//						getMesh().getBoraModel().getGraph().getRootCell());
//				MeshWriter.writeObject3D(m, getMeshDirectory(), "dummy.brep");
//				View v = ViewManager.getDefault().getCurrentView();
//				AmibeToMesh toMesh = new AmibeToMesh(getMeshDirectory());
//				ViewableMesh vMesh = new ViewableMesh(toMesh.getMesh());
//				vMesh.setName(subMeshNode.getName());
//				v.add(vMesh);
//			} catch (Exception ex) {
//				ex.printStackTrace();
//				return;
//			}
//		}
//	}

	@Override
	protected boolean asynchronous() {
		 // performAction() should run in event thread for actions that need a rendering of canva
		return false;
	}

	@Override
	protected boolean enable(Node[] arg0) {
		for (Node n : arg0) {
			BCADGraphCell cell = (BCADGraphCell)n.getValue("CELL");
			if (cell == null ||cell.getDiscretizations() == null)
				return false;
			if (cell.getDiscretizations().isEmpty())
				return false;
			for (BDiscretization disc : cell.getDiscretizations()) {
				if (disc.getMesh() == null)
					return false;
			}
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
