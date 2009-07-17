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
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.ds.Constraint;
import org.jcae.mesh.bora.ds.Hypothesis;
import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.cad.occ.OCCShape;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.actions.DeleteAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.actions.SystemAction;

/**
 * Note : this class is not fused in MeshNode because we may have
 * more than one subMeshes in a MeshNode in the future
 * @author Gautam Botrel
 *
 */
public class SubmeshNode extends AbstractNode implements Node.Cookie {

	private final String boraFileName; //needed for delete operation
	private final DataModel dataModel = new DataModel();


	public SubmeshNode(String objName,final BModel bModel) {
		super(new Children.Keys() {
			@Override
			protected Node[] createNodes(Object arg0) {return null;}
		});
		if (!bModel.getOutputDir().endsWith(File.separator))
			boraFileName = bModel.getOutputDir() + File.separator + bModel.getOutputFile();
		else
			boraFileName = bModel.getOutputDir() + bModel.getOutputFile();
		
		final BCADGraphCell rootCell = bModel.getGraph().getRootCell();
		//submeshes can be empty if the user created a meshnode and did not
		//specify any constraints, so we must "re"create the submesh
		if (bModel.getSubMeshes().isEmpty())
			bModel.newMesh();
		dataModel.subMesh = bModel.getSubMeshes().iterator().next();

		//now putting cell without constraints as keys
		Collection<BCADGraphCell> allCells = new ArrayList<BCADGraphCell>();
		for (CADShapeEnum type : CADShapeEnum.iterable(CADShapeEnum.VERTEX, CADShapeEnum.COMPOUND)) {
			allCells.addAll(bModel.getGraph().getCellList(type));
		}
		for (BCADGraphCell cell : allCells) {
			dataModel.constraints.put(getShapeFromCell(cell), null);
		}
		//getting the existing constraints and filling the constraints map
		for (Constraint c : dataModel.subMesh.getConstraints()) {
			dataModel.constraints.put(getShapeFromCell(c.getGraphCell()), c);
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
			dataModel.subMesh.getModel().cleanWorkDirectory();
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
				toAdd.add(new BCADGraphNode(true,cell, dataModel));
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
		public BSubMesh getSubMesh() {
			return subMesh;
		}
		/** Constraint Section **/
		//this constraint map is the data model used by the BCADGraphNode
		//actually, this map maps TopoDS_SHape and constraint, 'cause constraint are applied
		//to topods_shape, not BCADGraphCell, and this is the only way to detect multiple values in the GUI
		//for example, the edge between 2 faces of a cube is defined by 2 BCADGraphCell, and only 1 TopoDS_Shape
		private final Map<TopoDS_Shape, Constraint> constraints = new HashMap<TopoDS_Shape, Constraint>();
		
		public void addConstraint(BCADGraphCell cell, Hypothesis hyp, String group) {
			subMesh.getModel().resetConstraints();
			TopoDS_Shape shape = getShapeFromCell(cell);
			if (!constraints.containsKey(shape)) {
				Constraint cons = new Constraint(cell, hyp);
				cons.setGroup(group);
				constraints.put(shape, cons);
				subMesh.add(cons);
			}
			else {
				//the key already exists, but the constraint might not
				if (constraints.get(shape) == null) {
					Constraint cons = new Constraint(cell, hyp);
					cons.setGroup(group);
					constraints.put(shape, cons);
					subMesh.add(cons);
				}
				else {
					constraints.get(shape).setHypothesis(hyp);
					constraints.get(shape).setGroup(group);
				}
			}
			fireModelChanged();
		}

		public void removeConstraint(BCADGraphCell cell) {
			subMesh.getModel().resetConstraints();
			TopoDS_Shape shape = getShapeFromCell(cell);
			if (!constraints.containsKey(shape))
				return;
			Constraint cons = constraints.get(shape);
			if (cons != null) {
				subMesh.remove(cons);
				constraints.put(shape, null);
			}
			fireModelChanged();
		}

		public Constraint getConstraint(BCADGraphCell cell) {
			return constraints.get(getShapeFromCell(cell));
		}
		/** End Constraint section **/

		private ArrayList<BCADGraphNode> listeners = new ArrayList<BCADGraphNode>();
		private void fireModelChanged() {
			for (BCADGraphNode node : listeners)
				node.refresh();
		}

		public void addListener(BCADGraphNode node) {
			if (!listeners.contains(node))
				listeners.add(node);
		}
	}

	private static TopoDS_Shape getShapeFromCell(BCADGraphCell cell) {
		return ((OCCShape)cell.getShape()).getShape();
	}
}
