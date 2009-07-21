/*
 * This source code is the property of EADS France. No part of it shall
 * be reproduced or transmitted without the express prior written
 * authorization of EADS France, and its contents shall not be disclosed.
 * Copyright EADS France.
 */
package org.jcae.netbeans.mesh;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.netbeans.mesh.SubmeshNode.DataModel;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.datatransfer.PasteType;

/**
 *
 * @author Gautam Botrel
 *
 */
public class BGroupsNode extends AbstractNode {

	private final BGroupsNodeKeys groupNode;

	public BGroupsNode(SubmeshNode.DataModel dataModel) {
		super(Children.LEAF);
		groupNode = new BGroupsNodeKeys(dataModel);
		setChildren(groupNode);
	}

	public void fireModelChanged() {
		groupNode.fireModelChanged();
	}

	public class BGroupsNodeKeys extends Children.Keys {

		private final SubmeshNode.DataModel dataModel;

		public BGroupsNodeKeys(DataModel dataModel) {
			this.dataModel = dataModel;
		}

		@Override
		protected void addNotify() {
			fireModelChanged();
		}

		public void fireModelChanged() {
			setKeys(dataModel.getAllGroups());
			for (String group : dataModel.getAllGroups()) {
				refreshKey(group);
			}
		}

		@Override
		protected Node[] createNodes(Object arg0) {
			ArrayList<Node> toCreate = new ArrayList<Node>();
			final String group = arg0.toString();
			for (BCADGraphCell cell : dataModel.getCellsInGroup(group)) {
				BCADGraphNode newNode = new BCADGraphNode(true, cell, dataModel);
				dataModel.addListener(newNode);
				toCreate.add(newNode);
			}
			BGroupNode toAdd = new BGroupNode(group, toCreate, dataModel);
			return new Node[]{toAdd};
		}
	}
}
