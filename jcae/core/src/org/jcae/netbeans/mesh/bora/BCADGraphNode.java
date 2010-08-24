/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2005, by EADS CRC
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.netbeans.mesh.bora;

import org.jcae.netbeans.mesh.*;
import java.util.ArrayList;
import javax.swing.Action;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.Hypothesis;
import org.jcae.mesh.cad.CADShapeEnum;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.actions.SystemAction;

public class BCADGraphNode extends AbstractNode implements Node.Cookie
{
	private final Attributes attributes = new Attributes();
	//tells if this node is displayed in the graph view
	//or in the Entities view
	private final boolean isEntitie;

	public BCADGraphNode(BCADGraphCell cell, SubmeshNode.DataModel dataModel) {
		super(new BCADCellNode(cell, dataModel));
		init(cell, dataModel);
		isEntitie = false; //graph view
	}

	/**
	 * Use this constructor if you wish the node not to have children
	 * @param isLeaf
	 * @param cell
	 * @param dataModel
	 */
	public BCADGraphNode(boolean isLeaf, BCADGraphCell cell, SubmeshNode.DataModel dataModel) {
		super(Children.LEAF);
		init(cell, dataModel);
		isEntitie = true; //entitie view
	}

	/**
	 * Initialize name and attributes of this object
	 * @param cell
	 * @param dataModel
	 */
	private void init(BCADGraphCell cell, SubmeshNode.DataModel dataModel) {
		setDisplayName(cell.getType() + "" + cell.getId());
		attributes.initialize(cell, dataModel);
		setIconBaseWithExtension(getIconLocation(cell));
	}

	private static String getIconLocation(BCADGraphCell cell) {
		CADShapeEnum type = cell.getType();
		if (type.equals(CADShapeEnum.FACE)) {
			return "org/jcae/netbeans/mesh/bora/FaceNode.png";
		}
		else if (type.equals(CADShapeEnum.EDGE)) {
			return "org/jcae/netbeans/mesh/bora/EdgeNode.png";
		}
		else if (type.equals(CADShapeEnum.VERTEX)) {
			return "org/jcae/netbeans/mesh/bora/VertexNode.png";
		}
		else if (type.equals(CADShapeEnum.WIRE)) {
			return "org/jcae/netbeans/mesh/bora/WireNode.png";
		}
		return "org/jcae/netbeans/mesh/bora/UndefShapeNode.png";

	}

	@Override
	public String getHtmlDisplayName() {
		CADShapeEnum type = attributes.cell.getType();
		if (type != CADShapeEnum.FACE && type != CADShapeEnum.EDGE)
			return getDisplayName();
		String color = "006900"; //green
		String txt = " - constraint defined";
		if (!attributes.hasConstraint) {
			color = "FF0000";//red
			txt = " - constraint undefined!";
		}
		return  getDisplayName()+"<font color='" + color + "'>" + txt +"</font>";
	}



	@Override
	protected Sheet createSheet() {
		Sheet sheet = Sheet.createDefault();
		Sheet.Set meshSet = new Sheet.Set();
		Sheet.Set groupsSet = new Sheet.Set();

		meshSet.setName("Mesh constraints");
		meshSet.setShortDescription("Mesh constraints");

		groupsSet.setName("Group constraint");
		groupsSet.setShortDescription("Group constraint");
		
		try {
			//mesh constraints
			Property<Double> deflectionProp = new PropertySupport.Reflection<Double>(
					this.attributes,
					Double.class, "getDeflection", "setDeflection");
			deflectionProp.setName("Deflection");

			Property<Double> edgeProp = new PropertySupport.Reflection<Double>(
				this.attributes, Double.class, "getEdgeLength", "setEdgeLength");
			edgeProp.setName("Edge length");

			Property<String> elementProp = new PropertySupport.Reflection<String>(
				this.attributes, String.class, "getElement", "setElement");
			elementProp.setName("Element");
			elementProp.setValue("suppressCustomEditor", true);

			meshSet.put(deflectionProp);
			meshSet.put(edgeProp);
//			meshSet.put(elementProp);

			//group constraint
			Property<String> groupProp = new PropertySupport.Reflection<String>(
				this.attributes, String.class, "getGroup", "setGroup");
			groupProp.setName("Group name");
			groupProp.setValue("suppressCustomEditor", true);
			groupsSet.put(groupProp);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		sheet.put(meshSet);
		sheet.put(groupsSet);
		return sheet;
	}
	
	public BCADGraphCell getGraphCell() {
		return attributes.cell;
	}

	@Override
	public Action[] getActions(boolean arg0) {
		ArrayList<Action> l = new ArrayList<Action>();
		l.add(SystemAction.get(ViewBCellGeometryAction.class));
		l.add(SystemAction.get(ViewBCellMeshAction.class));
		l.add(SystemAction.get(RemoveConstraintAction.class));
		return l.toArray(new Action[l.size()]);
	}

	@Override
	public Object getValue(String attributeName) {
		if (attributeName.equals("CELL"))
			return attributes.cell;
		else if (attributeName.equals("ATTRIBUTES"))
			return attributes;
		else if (attributeName.equals("ENTITIE"))
			return isEntitie;
		else if (attributeName.equals("SUBMESH"))
			return attributes.dataModel.getSubMesh();

		return super.getValue(attributeName);
	}

	public void refresh() {
		attributes.refresh();
		fireDisplayNameChange(null, getDisplayName());
	}

	@Override
	public boolean canCopy() {
		return true;
	}


	public final static String DEFAULT_GROUP_NAME = "DEFAULT_GROUP";

	public class Attributes {
		private BCADGraphCell cell;
		private Hypothesis hyp;
		private SubmeshNode.DataModel dataModel;
		private boolean hasConstraint = false;
		private String group; // user's tag (group)

		public void initialize(BCADGraphCell cell, SubmeshNode.DataModel dataModel) {
			this.cell = cell;
			this.dataModel = dataModel;
			dataModel.addListener(BCADGraphNode.this);
			refresh();
		}

		public String getGroup() {
			return group;
		}

		public void setGroup(String group) {
			this.group = group;
			dataModel.addGroup(group, cell);
			if (hasConstraint)
				updateConstraints();
		}

		public boolean hasConstraint() {
			return hasConstraint;
		}

		public void deleteAttachedConstraint() {
			assert hasConstraint;
			dataModel.removeConstraint(cell);
			BCADGraphNode.this.fireCookieChange();
		}

		/**
		 * Synchronizes the attributes with the SubmeshNode DataModel
		 */
		public void refresh() {
			if (dataModel.getConstraint(cell) != null) {
				hyp = dataModel.getConstraint(cell).getHypothesis();
				group = dataModel.getConstraint(cell).getGroup();
				if (group == null)
					group = DEFAULT_GROUP_NAME;
				hasConstraint = true;
			}
			else {
				hyp = new Hypothesis();
				hasConstraint = false;
				group = DEFAULT_GROUP_NAME;
			}
		}

		private void updateConstraints() {
			dataModel.addConstraint(cell, hyp, group);
			fireCookieChange();
		}


		/**
		 * @return the deflection
		 */
		public double getDeflection() {
			return hyp.getDeflection();
		}

		/**
		 * @param deflection the deflection to set
		 */
		public void setDeflection(Double deflection) {
			this.hyp.setDeflection(deflection);
			updateConstraints();
		}

		/**
		 * @return the edgeLength
		 */
		public double getEdgeLength() {
			return hyp.getLength();
		}

		/**
		 * @param edgeLength the edgeLength to set
		 */
		public void setEdgeLength(Double edgeLength) {
			this.hyp.setLength(edgeLength);
			updateConstraints();
		}

		/**
		 * @return the element
		 */
		public String getElement() {
			return hyp.getElement();
		}

		/**
		 * @param element the element to set
		 */
		public void setElement(String element) {
			this.hyp.setElement(element);
			updateConstraints();
		}
	}
}
