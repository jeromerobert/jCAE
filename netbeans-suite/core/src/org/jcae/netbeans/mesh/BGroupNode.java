/*
 * This source code is the property of EADS France. No part of it shall
 * be reproduced or transmitted without the express prior written
 * authorization of EADS France, and its contents shall not be disclosed.
 * Copyright EADS France.
 */

package org.jcae.netbeans.mesh;

import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.Action;
import org.openide.actions.DeleteAction;
import org.openide.actions.RenameAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.PasteType;

/**
 *
 * @author Gautam Botrel
 *
 */
public class BGroupNode extends AbstractNode {

	private final SubmeshNode.DataModel dataModel;

	public BGroupNode(String displayName, Collection<Node> children,
					  final SubmeshNode.DataModel dataModel) {
		super(new Children.Keys() {

			@Override
			protected Node[] createNodes(Object arg0) {
				return null;
			}
		});
		this.dataModel = dataModel;
		setName(displayName);
		getChildren().add(children.toArray(new Node[0]));
		
	}

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
						dataModel.addGroup(getName(), n.getGraphCell());
						firePropertyChange(null, null, null);
						return null;
					}
				});
			}
		}
		// Also try superclass, but give it lower priority:
		super.createPasteTypes(t, ls);
	}

	@Override
	public Action[] getActions(boolean arg0) {
		ArrayList<Action> l = new ArrayList<Action>();
		l.add(SystemAction.get(DeleteAction.class));
		l.add(SystemAction.get(RenameAction.class));
		return l.toArray(new Action[l.size()]);
	}

	@Override
	public boolean canDestroy() {
		return dataModel.getAllGroups().size() > 1;
	}

	@Override
	public boolean canRename() {
		return true;
	}

	@Override
	public String getDisplayName() {
		return getName();
	}


	@Override
	public void setName(String newName) {
		String old = getName();
		if (old != null)
			dataModel.renameGroup(old, newName);
		super.setName(newName);
		fireDisplayNameChange(old, newName);
		fireNameChange(old, newName);
	}

	@Override
	public void destroy() throws IOException {
		dataModel.destroyGroup(getName());
		super.destroy();
	}






}
