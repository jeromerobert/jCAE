/*
 * This source code is the property of EADS France. No part of it shall
 * be reproduced or transmitted without the express prior written
 * authorization of EADS France, and its contents shall not be disclosed.
 * Copyright EADS France.
 */
package org.jcae.netbeans.mesh;

import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.cad.occ.OCCShape;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.jcae.vtk.ViewableCAD;
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

	@Override
	protected void performAction(Node[] arg0) {
		for (Node n : arg0) {
			BCADGraphCell cell = (BCADGraphCell)n.getValue("CELL");
			view((OCCShape) cell.getShape(), cell.getType() + ""+cell.getId());
		}
	}


//	WeakReference<ViewableCAD> viewableRef = new WeakReference<ViewableCAD>(null);

	private void view(OCCShape shape, String name) {
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
		
		ViewableCAD	viewable = new ViewableCAD(shape.getShape());
		viewable.setName(name);

		// TODO manage selection...
		// SelectionManager.getDefault().addInteractor(viewable, shape.getShape());
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



}
