/*
 * This source code is the property of EADS France. No part of it shall
 * be reproduced or transmitted without the express prior written
 * authorization of EADS France, and its contents shall not be disclosed.
 * Copyright EADS France.
 */
package org.jcae.netbeans.mesh.bora;

import org.jcae.netbeans.mesh.bora.BCADGraphNode;
import java.util.HashMap;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.cad.occ.OCCShape;
import org.jcae.netbeans.cad.NbShape;
import org.jcae.netbeans.viewer3d.SelectionManager;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.jcae.vtk.ViewableCAD;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

/**
 *
 * @author Gautam Botrel
 *
 */
public final class ViewBCellGeometryAction extends CookieAction {

	@Override
	public String getName() {
		return "View Geometry";
	}

	private static Node getParentSubmeshNode(Node n) {
//		Node parent = n;
//		int iter = 0;
//		while (iter < 10) {
//			parent = parent.getParentNode();
//			SubmeshNode toReturn = parent.getCookie(SubmeshNode.class);
//			if (toReturn != null) {
//				Children c = toReturn.getChildren();
//				for (Node no : c.getNodes()) {
//					if (no.getDisplayName().equals("Graph")) {
//						return no;
//					}
//				}
//			}
//		}
		return n.getParentNode();
	}

	@Override
	protected void performAction(Node[] arg0) {
		String name = "";
		NbBShape toDisplay = new NbBShape((TopoDS_Shape)null, getParentSubmeshNode(arg0[0]));
		for (Node n : arg0) {
			BCADGraphCell cell = (BCADGraphCell)n.getValue("CELL");
			TopoDS_Shape shape = ((OCCShape)cell.getShape()).getShape();
			name+=n.getDisplayName() + " ";
			NbShape toAdd = new NbShape(shape);
			toAdd.setNode(n);
			toDisplay.add(toAdd);
		}
		view(toDisplay, name);
	}

	private void view(NbBShape shape, String name) {
		View v = ViewManager.getDefault().getCurrentView();
		for (Viewable w : v.getViewables()) {
			if (w instanceof ViewableCAD) {
				ViewableCAD wCad = (ViewableCAD)w;
				if (wCad.getName().equals(name)) {
					wCad.refresh();
					return;
				}
			}
		}

		ViewableCAD viewable = new ViewableCAD(shape.getImpl());
		viewable.setName(name);

		SelectionManager.getDefault().addInteractor(viewable, shape);
		v.add(viewable);
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
			if (cell == null ||cell.getShape() == null)
				return false;
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


	/**
	 * Decoy class, to use a BCADSelection in Selection Manager
	 */
	public static class NbBShape extends NbShape {
		public NbBShape(TopoDS_Shape shape, Node node) {
			super(shape, new HashMap<TopoDS_Shape, NbShape>(), new NbShape[0]);
			ref = node;
		}
		//ref to the node to explore (e.g the Graph of the Geometry)
		private final Node ref;
		public Node getRefToExploreNode() {
			return ref;
		}
	}

}
