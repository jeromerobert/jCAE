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
import java.beans.IntrospectionException;
import java.util.Collection;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.ds.Constraint;
import org.jcae.mesh.bora.ds.Hypothesis;
import org.jcae.netbeans.BeanProperty;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Utilities;

public class BCADGraphNode extends AbstractNode implements Node.Cookie
{

	public class Attributes {
		private Collection<Constraint> allConstraints;
		private Constraint myConstraint;
		private BCADGraphCell cell;
		private Hypothesis hyp = new Hypothesis();
		private BSubMesh subMesh;


		public void initialize(Collection<Constraint> allConstraints, BCADGraphCell cell, BSubMesh subMesh) {
			setAllConstraints(allConstraints);
			setCell(cell);
			this.subMesh = subMesh;
			for (Constraint c : allConstraints) {
				if (c.getGraphCell().equals(cell)) {
					hyp = c.getHypothesis();
					myConstraint = c;
					break;
				}
			}
		}

		private void updateConstraints() {
			if (myConstraint == null) {
				myConstraint = new Constraint(cell, hyp);
				subMesh.add(myConstraint);
//				allConstraints.add(myConstraint);
			}
			Node node = getParentNode();
			while (node != null) {
				if (node instanceof MeshNode) {
					MeshNode mNode = (MeshNode)node;
					mNode.getMesh().refresh();
					break;
				}
				node = node.getParentNode();
			}
			assert (allConstraints.contains(myConstraint));
		}

		/**
		 * @param allConstraints the allConstraints to set
		 */
		private void setAllConstraints(Collection<Constraint> allConstraints) {
			this.allConstraints = allConstraints;
		}

		/**
		 * @return the myConstraint
		 */
		public Constraint getMyConstraint() {
			return myConstraint;
		}

		/**
		 * @param myConstraint the myConstraint to set
		 */
		public void setMyConstraint(Constraint myConstraint) {
			this.myConstraint = myConstraint;
		}

		/**
		 * @param cell the cell to set
		 */
		private void setCell(BCADGraphCell cell) {
			this.cell = cell;
		}

		/**
		 * @return the hyp
		 */
		public Hypothesis getHyp() {
			return hyp;
		}

		/**
		 * @param hyp the hyp to set
		 */
		public void setHyp(Hypothesis hyp) {
			this.hyp = hyp;
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

	public BCADGraphNode(BCADGraphCell cell, Collection<Constraint> constraints, BSubMesh subMesh) {
		super(new BCADCellNode(cell, constraints, subMesh));
		setDisplayName(cell.getType() + "" + cell.getId());
		attributes.initialize(constraints, cell, subMesh);
	}

	public BCADGraphNode(String name, BCADGraphCell cell, Collection<Constraint> constraints, BSubMesh subMesh) {
		super(Children.LEAF);
		setDisplayName(name);
	}

	@Override
	public Image getIcon(int type) {
		return Utilities.loadImage("org/jcae/netbeans/cad/link.png");
	}

	@Override
	public Image getOpenedIcon(int arg0) {
		return getIcon(arg0);
	}

	private Attributes getAttributes() {
		return attributes;
	}

	protected Property[] getMeshProperties() {
		try {
			return new Property[]{
						new BeanProperty(getAttributes(), "deflection"),
						new BeanProperty(getAttributes(), "edgeLength"),
						new BeanProperty(getAttributes(), "element")};
		} catch (NoSuchMethodException e) {
			ErrorManager.getDefault().notify(e);
			return new Property[0];
		} catch (IntrospectionException e) {
			ErrorManager.getDefault().notify(e);
			return new Property[0];
		}
	}

	@Override
	public PropertySet[] getPropertySets() {
		return new PropertySet[]{
					new PropertySet() {
						public Property[] getProperties() {
							return BCADGraphNode.this.getMeshProperties();
						}

						public String getName() {
							return "Mesh";
						}
					}
				};
	}

}
