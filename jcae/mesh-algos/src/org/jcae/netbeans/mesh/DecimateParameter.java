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
 * (C) Copyright 2010, by EADS France
 */

package org.jcae.netbeans.mesh;

import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.openide.explorer.propertysheet.PropertySheet;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Node.PropertySet;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;

public class DecimateParameter extends PropertySheet
{
	private int triangles=10000;
	private double tolerance = 0.01;
	private boolean useTolerance=true;
	private boolean preserveGroups = true;
	private boolean useMaxLength;
	private double maxLength = 1.0;
	
	private class MyProperty<T> extends PropertySupport.Reflection<T>
	{
		public MyProperty(Class<T> type, String property, String name)
			throws NoSuchMethodException
		{
			this(type, property, name, name);
		}
		public MyProperty(Class<T> type, String property, String name, String description)
			throws NoSuchMethodException
		{
			super(DecimateParameter.this, type, property);
			setName(name);
			setShortDescription(description);
		}
	}
	/** Creates a new instance of SmoothParameters */
	public DecimateParameter()
	{
		AbstractNode node = new AbstractNode(Children.LEAF)
		{
			@Override
			public PropertySet[] getPropertySets() {
				return new PropertySet[]{createPropertySet()};
			}
		};
		setNodes(new Node[]{node});
		setDescriptionAreaVisible(true);
		setPreferredSize(new Dimension(0, 250));
	}

	private Sheet.Set createPropertySet()
	{
		Sheet.Set r = new Sheet.Set();
		r.setName("Parameters");
		try {
			r.put(new MyProperty<Boolean>(Boolean.TYPE, "useTolerance", "Use tolerance",
				"Use tolerance parameter instead of triangle number parameter."));
			r.put(new MyProperty<Double>(Double.TYPE, "tolerance",
				"Tolerance", "Geometry absolute distance error allowed when decimating."){
				@Override
				public boolean canWrite() {
					return useTolerance;
				}
			});
			r.put(new MyProperty<Integer>(Integer.TYPE, "triangles",
				"Target triangles", "Stops iterations when mesh contains this number of triangles."){
				@Override
				public boolean canWrite() {
					return !useTolerance;
				}
			});
			r.put(new MyProperty<Boolean>(Boolean.TYPE, "preserveGroups",
				"Preserve groups",
				"Edges adjacent to two different groups are handled like free edges."));
			r.put(new MyProperty<Boolean>(Boolean.TYPE, "useMaxLength",
				"Use max length",
				"Enable max length parameter."));

			r.put(new MyProperty<Double>(Double.TYPE, "maxLength",
				"Maximum length", "No edges longer than this value are created."){
				@Override
				public boolean canWrite() {
					return useMaxLength;
				}
			});

		} catch (NoSuchMethodException ex) {
			Exceptions.printStackTrace(ex);
		}
		return r;
	}

	public boolean showDialog()
	{
        JOptionPane jp = new JOptionPane(this,
             JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog d = jp.createDialog("Decimate parameters");
		d.setResizable(true);
		d.setVisible(true);
        return Integer.valueOf(JOptionPane.OK_OPTION).equals(jp.getValue());
	}

	public boolean isUseMaxLength() {
		return useMaxLength;
	}

	public void setUseMaxLength(boolean useMaxLength) {
		this.useMaxLength = useMaxLength;
	}

	public double getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(double maxLength) {
		this.maxLength = maxLength;
	}

	public boolean isPreserveGroups() {
		return preserveGroups;
	}

	public void setPreserveGroups(boolean preserveGroups) {
		this.preserveGroups = preserveGroups;
	}

	public double getTolerance() {
		return tolerance;
	}

	public void setTolerance(double tolerance) {
		this.tolerance = tolerance;
	}

	public int getTriangles() {
		return triangles;
	}

	public void setTriangles(int triangle) {
		this.triangles = triangle;
	}

	public boolean isUseTolerance() {
		return useTolerance;
	}

	public void setUseTolerance(boolean useTolerance) {
		this.useTolerance = useTolerance;
		repaint();
	}


}