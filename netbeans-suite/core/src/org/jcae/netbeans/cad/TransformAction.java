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
 */

package org.jcae.netbeans.cad;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jcae.netbeans.Utilities;
import org.jcae.opencascade.jni.BRepBuilderAPI_Transform;
import org.jcae.opencascade.jni.GP_Trsf;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.awt.Actions;
import org.openide.awt.Mnemonics;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.openide.util.actions.NodeAction;
import org.openide.util.actions.SystemAction;

public abstract class TransformAction extends CookieAction
{
	static public class AllActions extends NodeAction
	{
		protected boolean enable(Node[] arg0)
		{
			return true;
		}

		public HelpCtx getHelpCtx()
		{
			return null;
		}

		public javax.swing.JMenuItem getMenuPresenter()
		{
			JMenu menu = new JMenu();		
			Mnemonics.setLocalizedText(menu, getName());		

			JMenuItem item = new JMenuItem();
			Mnemonics.setLocalizedText(item, item.getText());
			Actions.connect(item, (Action)SystemAction.get(Translate.class), true);
			menu.add(item);

			item = new JMenuItem();
			Mnemonics.setLocalizedText(item, item.getText());
			Actions.connect(item, (Action)SystemAction.get(RotateDialog.RotateAction.class), true);
			menu.add(item);

			item = new JMenuItem();
			Mnemonics.setLocalizedText(item, item.getText());
			Actions.connect(item, (Action)SystemAction.get(Scale.class), true);
			menu.add(item);

			return menu;			
		}

		public String getName()
		{
			return "Transform";
		}

		public javax.swing.JMenuItem getPopupPresenter()
		{
			return getMenuPresenter();
		}

		protected void performAction(Node[] arg0)
		{
		}
	}
	
	static public class Translate extends TransformAction
	{
		public String getName()
		{
			return "Translation";
		}

		protected Object getParameters()
		{
			return new Translation();
		}

		protected GP_Trsf getTrsf(Object parameter)
		{
			Translation r=(Translation) parameter;
			GP_Trsf theTransformation = new GP_Trsf();
			theTransformation.setTranslation(new double[]{r.getX(), r.getY(), r.getZ()});
			return theTransformation;
		}
	}
	
	static public class ScaleFactor
	{
		double factor=1.0;
		
		public double getFactor()
		{
			return factor;
		}
		
		public void setFactor(double factor)
		{
			this.factor=factor;
		}		
	}
	
	static public class Scale extends TransformAction
	{
		
		public String getName()
		{
			return "Scale";
		}

		protected Object getParameters()
		{
			//Use the Action object as a bean to define the parameters
			return new ScaleFactor();
		}

		protected GP_Trsf getTrsf(Object parameter)
		{
			ScaleFactor r=(ScaleFactor) parameter;
			GP_Trsf theTransformation = new GP_Trsf();
			double a=r.getFactor();
			double[] matrix=new double[]{
				a, 0, 0, 0,
				0, a, 0, 0,
				0, 0, a, 0	
			};
			theTransformation.setValues(matrix, 0.0, 0.0);
			return theTransformation;
		}
	}	

	protected Class[] cookieClasses()
	{
		return new Class[]{NbShape.class};
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}

	protected abstract Object getParameters();
	protected abstract GP_Trsf getTrsf(Object parameter);

	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected void performAction(Node[] nodes)
	{
		Object param = getParameters();
		if (Utilities.showEditBeanDialog(param))
		{
			GP_Trsf trsf = getTrsf(param);
			for (Node node: nodes)
			{
				TopoDS_Shape s = GeomUtils.getShape(node).getImpl();
				BRepBuilderAPI_Transform bt = new BRepBuilderAPI_Transform(s,
					trsf, true);
				TopoDS_Shape newShape=bt.shape();
				GeomUtils.insertShape(newShape, getName(), node.getParentNode());
				GeomUtils.getParentBrep(node).getDataObject().setModified(true);
			}
		}
	}
}
