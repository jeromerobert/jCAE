/*
 * This source code is the property of EADS France. No part of it shall
 * be reproduced or transmitted without the express prior written
 * authorization of EADS France, and its contents shall not be disclosed.
 * Copyright EADS France.
 */

package org.jcae.netbeans.mesh;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.Action;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.ds.Constraint;
import org.jcae.mesh.cad.CADShapeEnum;
import org.openide.actions.DeleteAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author Gautam Botrel
 *
 */
public class SubmeshNode extends AbstractNode implements Node.Cookie {

	private final String boraFileName;

	public SubmeshNode(String objName,final BModel bModel) {
		super(new Children.Keys() {
			@Override
			protected Node[] createNodes(Object arg0) {return null;}
		});
		boraFileName = bModel.getOutputDir() + bModel.getOutputFile();
		final BCADGraphCell rootCell = bModel.getGraph().getRootCell();
		final BSubMesh subMesh = bModel.getSubMeshes().iterator().next();
		AbstractNode entities = new AbstractNode(new EntitieChildrenNode(rootCell, bModel.getConstraints(), subMesh));
		entities.setDisplayName("Entities");

		AbstractNode graph = new AbstractNode(new BCADCellNode(rootCell, bModel.getConstraints(), subMesh));
		graph.setDisplayName("Graph");
		getChildren().add(new Node[] {entities, graph});
		this.setDisplayName(objName);
	}

	@Override
	public void destroy() {
		try {
			super.destroy();
			new File(boraFileName).delete();
		} catch (Exception e) {
			throw new RuntimeException(
					"Could not delete " + boraFileName + ". Cause : " + e);
		}
	}

	@Override
	public Action[] getActions(boolean arg0) {
		ArrayList<Action> l = new ArrayList<Action>();
		l.add(SystemAction.get(DeleteAction.class));
		return l.toArray(new Action[l.size()]);
	}

	@Override
	public boolean canDestroy() {
		return true;
	}



//	@Override
//	public Action[] getActions(boolean arg0)
//	{
//		ArrayList<Action> l=new ArrayList<Action>();
//		l.add(SystemAction.get(RenameAction.class));
//		l.add(SystemAction.get(DeleteAction.class));
//		return l.toArray(new Action[l.size()]);
//	}

	private class EntitieChildrenNode extends Children.Keys {

		protected BCADGraphCell cell;
		protected Collection<Constraint> constraints;
		protected BSubMesh subMesh;
		private final ArrayList<CADShapeEnum> shapesToDisplay;

		public EntitieChildrenNode(BCADGraphCell cell, Collection<Constraint> constraints,BSubMesh subMesh) {
			this.cell = cell;
			this.constraints = constraints;
			this.subMesh = subMesh;
			shapesToDisplay = new ArrayList<CADShapeEnum>(3);
			shapesToDisplay.add(CADShapeEnum.FACE);
			shapesToDisplay.add(CADShapeEnum.EDGE);
			shapesToDisplay.add(CADShapeEnum.VERTEX);

		}

		@Override
		protected void addNotify() {
			setKeys(shapesToDisplay.toArray());
		}

		protected Node[] createNodes(Object key) {
			CADShapeEnum type = (CADShapeEnum)key;
			ArrayList<BCADGraphNode> toReturn = new ArrayList<BCADGraphNode>();
			Iterator<BCADGraphCell> it = cell.uniqueShapesExplorer(type);
			while (it.hasNext()) {
				BCADGraphCell c = it.next();
				if (c != null) {
					String name = c.getType() + "" + c.getId();
					toReturn.add(new BCADGraphNode(name, c, constraints, subMesh));
				}
			}
			Node[] toDisplay = new Node[toReturn.size()];
			for (int i = 0; i < toDisplay.length; i++) {
				toDisplay[i] = toReturn.get(i);
			}
			AbstractNode node = new AbstractNode(new Children.Keys() {
				@Override
				protected Node[] createNodes(Object arg0) {
					return null;
				}
			});
			node.getChildren().add(toDisplay);
			node.setName(type.toString());
			return new Node[] {node};
		}
	}
}
