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

package org.jcae.netbeans.mesh;

import java.awt.Image;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.Action;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.ds.Constraint;
import org.jcae.mesh.bora.ds.Hypothesis;
import org.jcae.netbeans.cad.GeomUtils;
import org.jcae.netbeans.cad.NbShape;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;

public class BCADGraphNode extends AbstractNode implements Node.Cookie
{

	private class Attributes {
		private BCADGraphCell cell;
		private Hypothesis hyp;
		private SubmeshNode.DataModel dataModel;

		public void initialize(BCADGraphCell cell, SubmeshNode.DataModel dataModel) {
			this.cell = cell;
			this.dataModel = dataModel;
			refresh();
		}

		/**
		 * Synchronizes the attributes with the SubmeshNode DataModel
		 */
		public void refresh() {
			if (dataModel.getConstraint(cell) != null) {
				hyp = dataModel.getConstraint(cell).getHypothesis();
			}
			else if (hyp == null)
				hyp = new Hypothesis();
		}

		private void updateConstraints() {
			dataModel.addConstraint(cell, hyp);
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
		public void setDeflection(double deflection) {
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
		public void setEdgeLength(double edgeLength) {
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

	private final Attributes attributes = new Attributes();

	public BCADGraphCell getGraphCell() {
		return attributes.cell;
	}

	public BCADGraphNode(BCADGraphCell cell, SubmeshNode.DataModel dataModel) {
		super(new BCADCellNode(cell, dataModel));
		setDisplayName(cell.getType() + "" + cell.getId());
		attributes.initialize(cell, dataModel);
		createSheetSet();
	}

	public BCADGraphNode(String name, BCADGraphCell cell, SubmeshNode.DataModel dataModel) {
		super(Children.LEAF);
		setDisplayName(name);
		attributes.initialize(cell, dataModel);
		createSheetSet();
	}

	@Override
	public Image getIcon(int type) {
		return Utilities.loadImage("org/jcae/netbeans/cad/link.png");
	}

	@Override
	public Image getOpenedIcon(int arg0) {
		return getIcon(arg0);
	}

	private void createSheetSet()
	{
		Sheet.Set set=new Sheet.Set();
		set.setName("Mesh constraints");

		set.put(new PropertySupport.ReadWrite<Double>(
				"deflection", Double.class, "Deflection", "Deflection") {

			public Double getValue() {
				return attributes.getDeflection();
			}

			@Override
			public void setValue(Double arg0) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
				attributes.setDeflection(arg0);
			}
		});

		set.put(new PropertySupport.ReadWrite<Double>(
				"edgeLength", Double.class, "Edge Length", "Edge Length") {

			public Double getValue() {
				return attributes.getEdgeLength();
			}

			@Override
			public void setValue(Double arg0) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
				attributes.setEdgeLength(arg0);
			}
		});


		set.put(new PropertySupport.ReadWrite<String> (
			"element", String.class, "Element", "Element")
		{
			@Override
			public String getValue() throws IllegalAccessException,
				InvocationTargetException
			{
				return attributes.getElement();
			}

			@Override
			public void setValue(String val) throws IllegalAccessException,
				IllegalArgumentException, InvocationTargetException
			{
				attributes.setElement(val);
			}
		});
		getSheet().put(set);
	}

	@Override
	public Action[] getActions(boolean arg0) {
		ArrayList<Action> l = new ArrayList<Action>();
		l.add(SystemAction.get(ViewBCellGeometryAction.class));
		l.add(SystemAction.get(ViewBCellMeshAction.class));
		return l.toArray(new Action[l.size()]);
	}

	@Override
	public Object getValue(String attributeName) {
		if (attributeName.equals("CELL"))
			return attributes.cell;
		return super.getValue(attributeName);
	}



}
