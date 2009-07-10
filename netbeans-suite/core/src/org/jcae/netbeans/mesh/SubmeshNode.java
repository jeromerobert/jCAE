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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.Action;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.ds.Constraint;
import org.jcae.mesh.bora.ds.Hypothesis;
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
	private final DataModel dataModel = new DataModel();


	public SubmeshNode(String objName,final BModel bModel) {
		super(new Children.Keys() {
			@Override
			protected Node[] createNodes(Object arg0) {return null;}
		});
		boraFileName = bModel.getOutputDir() + bModel.getOutputFile();
		final BCADGraphCell rootCell = bModel.getGraph().getRootCell();
		dataModel.subMesh = bModel.getSubMeshes().iterator().next();

		//now putting cell without constraints as keys
		Collection<BCADGraphCell> allCells = bModel.getGraph().getCellList(CADShapeEnum.COMPOUND);
		allCells.addAll(bModel.getGraph().getCellList(CADShapeEnum.COMPSOLID));
		allCells.addAll(bModel.getGraph().getCellList(CADShapeEnum.EDGE));
		allCells.addAll(bModel.getGraph().getCellList(CADShapeEnum.FACE));
		allCells.addAll(bModel.getGraph().getCellList(CADShapeEnum.SHELL));
		allCells.addAll(bModel.getGraph().getCellList(CADShapeEnum.SOLID));
		allCells.addAll(bModel.getGraph().getCellList(CADShapeEnum.VERTEX));
		allCells.addAll(bModel.getGraph().getCellList(CADShapeEnum.WIRE));
		for (BCADGraphCell cell : allCells) {
				dataModel.constraints.put(cell, null);
		}
		//getting the existing constraints and filling the constraints map
		for (Constraint c : dataModel.subMesh.getConstraints()) {
			dataModel.constraints.put(c.getGraphCell(), c);
		}

		final AbstractNode graph = new AbstractNode(new BCADCellNode(rootCell, dataModel));
		graph.setDisplayName("Graph");

		final AbstractNode entities = new AbstractNode(new EntitieChildrenNode(rootCell));
		entities.setDisplayName("Entities");

		getChildren().add(new Node[] {graph, entities});
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


	/**
	 * Represent the Entitie's representation
	 * (BCADGraphCell are represented by types :
	 * eg : a folder for faces, a folder for edges... etc)
	 */
	private class EntitieChildrenNode extends Children.Keys {

		private final BCADGraphCell root;
		private final ArrayList<CADShapeEnum> shapesToDisplay;

		public EntitieChildrenNode(BCADGraphCell root) {
			this.root = root;
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
			
			AbstractNode node = new AbstractNode(new Children.Keys() {
				@Override
				protected Node[] createNodes(Object arg0) {
					return null;
				}
			});
			ArrayList<BCADGraphNode> toAdd = new ArrayList<BCADGraphNode>();
			Iterator<BCADGraphCell> it = root.shapesExplorer(type);
			while (it.hasNext()) {
				BCADGraphCell cell = it.next();
				toAdd.add(new BCADGraphNode(cell.getType()+""+cell.getId(),cell, dataModel));
			}
			node.getChildren().add(toAdd.toArray(new Node[0]));
			node.setName(type.toString());
			return new Node[] {node};
		}
	}


	/**
	 * This inner class represents the DataModel of a Submesh
	 * It will be used by Children nodes (which doesn't contains data)
	 */
	public class DataModel {
		private BSubMesh subMesh;
		//this constraint map is the data model used by the BCADGraphNode
		private final Map<BCADGraphCell, Constraint> constraints = new HashMap<BCADGraphCell, Constraint>();

		public void addConstraint(BCADGraphCell cell, Hypothesis hyp) {
			if (!constraints.containsKey(cell)) {
				Constraint cons = new Constraint(cell, hyp);
				constraints.put(cell, cons);
				subMesh.add(cons);
			}
			else {
				//the key already exists, but the constraint might not
				if (constraints.get(cell) == null) {
					Constraint cons = new Constraint(cell, hyp);
					constraints.put(cell, cons);
					subMesh.add(cons);
				}
				else {
					constraints.get(cell).setHypothesis(hyp);
				}
			}
		}

		public void removeConstraint(BCADGraphCell cell) {
			if (!constraints.containsKey(cell))
				return;
			Constraint cons = constraints.get(cell);
			if (cons != null) {
				subMesh.remove(cons);
				constraints.put(cell, null);
			}
		}
		public Constraint getConstraint(BCADGraphCell cell) {
			return constraints.get(cell);
		}
	}
}
