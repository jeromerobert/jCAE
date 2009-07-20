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

	private final BGroupNode groupNode;

	public BGroupsNode(SubmeshNode.DataModel dataModel) {
		super(Children.LEAF);
		groupNode = new BGroupNode(dataModel);
		setChildren(groupNode);
	}

	public void fireModelChanged() {
		groupNode.fireModelChanged();
	}

	public class BGroupNode extends Children.Keys {

		private final SubmeshNode.DataModel dataModel;

		public BGroupNode(DataModel dataModel) {
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
				toCreate.add(new BCADGraphNode(true, cell, dataModel));
			}
			AbstractNode toAdd = new AbstractNode(new Children.Keys() {
				@Override
				protected Node[] createNodes(Object arg0) {
					return null;
				}
			}) {

				@Override
				protected void createPasteTypes(Transferable t,
												List<PasteType> ls) {
					final Node[] ns = NodeTransfer.nodes(t,
							NodeTransfer.COPY | NodeTransfer.MOVE);
					if (ns != null && ns.length == 1) {
						if (ns[0] instanceof BCADGraphNode) {
							final BCADGraphNode n = (BCADGraphNode) ns[0];
							ls.add(new PasteType() {
								public Transferable paste() {
									dataModel.addGroup(group, n.getGraphCell());
									fireModelChanged();
									firePropertyChange(null, null, null);
									return null;
								}
							});
						}
					}
					// Also try superclass, but give it lower priority:
					super.createPasteTypes(t, ls);
				}
			};
			toAdd.getChildren().add(toCreate.toArray(new Node[0]));
			toAdd.setDisplayName(group);

			return new Node[]{toAdd};
		}
	}
}
