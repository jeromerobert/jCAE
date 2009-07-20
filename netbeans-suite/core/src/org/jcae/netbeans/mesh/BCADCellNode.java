/*
 * This source code is the property of EADS France. No part of it shall
 * be reproduced or transmitted without the express prior written
 * authorization of EADS France, and its contents shall not be disclosed.
 * Copyright EADS France.
 */

package org.jcae.netbeans.mesh;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.ds.Constraint;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 *
 * @author Gautam Botrel
 *
 */
public class BCADCellNode extends Children.Keys implements Node.Cookie {

	protected final BCADGraphCell cell;
	protected final SubmeshNode.DataModel dataModel;

	public BCADCellNode(BCADGraphCell cell, SubmeshNode.DataModel dataModel) {
		this.cell = cell;
		this.dataModel = dataModel;
	}

	@Override
	protected void addNotify() {
		Iterator<BCADGraphCell> it = cell.uniqueShapesIterator();
		ArrayList<BCADGraphCell> toReturn = new ArrayList<BCADGraphCell>();
		while (it.hasNext()) {
			BCADGraphCell c = it.next();
			if (c != null) {
					BCADGraphCell cReverse = c.getReversed();
					if (cReverse != null && cReverse.getId() < c.getId())
						toReturn.add(cReverse);
					else
						toReturn.add(c);
			}
		}
		setKeys(toReturn.toArray());
	}

	protected Node[] createNodes(Object o) {
		BCADGraphCell obj = (BCADGraphCell) o;
		return new Node[]{new BCADGraphNode(obj, dataModel)};
	}
}
