/*
 * This source code is the property of EADS France. No part of it shall
 * be reproduced or transmitted without the express prior written
 * authorization of EADS France, and its contents shall not be disclosed.
 * Copyright EADS France.
 */
package org.jcae.netbeans.mesh;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BSubMesh;
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
		String name = "";
		ArrayList<BDiscretization> discrs = new ArrayList<BDiscretization>();
		for (Node n : arg0) {
			BCADGraphCell cell = (BCADGraphCell) n.getValue("CELL");
			BSubMesh subMesh = (BSubMesh) n.getValue("SUBMESH");
			discrs.add(cell.getDiscretizationSubMesh(subMesh));
			name += cell.getType() + "" + cell.getId() + " ";
		}
		Map<String, Collection<BDiscretization>> meshData = new HashMap<String, Collection<BDiscretization>>();
		meshData.put(name, discrs);

		MeshNode.view(name, meshData, null);
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
			BSubMesh subMesh = (BSubMesh)n.getValue("SUBMESH");
			if (subMesh == null)
				return false;
			if (cell.getDiscretizationSubMesh(subMesh) == null)
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
