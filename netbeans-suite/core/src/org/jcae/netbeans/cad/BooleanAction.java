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
import org.jcae.opencascade.jni.BRepAlgoAPI_BooleanOperation;
import org.jcae.opencascade.jni.BRepAlgoAPI_Common;
import org.jcae.opencascade.jni.BRepAlgoAPI_Cut;
import org.jcae.opencascade.jni.BRepAlgoAPI_Fuse;
import org.jcae.opencascade.jni.BRepAlgoAPI_Section;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.awt.Actions;
import org.openide.awt.Mnemonics;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.openide.util.actions.NodeAction;
import org.openide.util.actions.Presenter;
import org.openide.util.actions.SystemAction;

public abstract class BooleanAction extends CookieAction
{
	static public class AllActions extends NodeAction
	{
		public javax.swing.JMenuItem getMenuPresenter() 
		{
			JMenu menu = new JMenu();		
			Mnemonics.setLocalizedText(menu, getName());		

			JMenuItem item = new JMenuItem();
			Mnemonics.setLocalizedText(item, item.getText());
			Actions.connect(item, (Action)SystemAction.get(Common.class), true);
			menu.add(item);

			item = new JMenuItem();
			Mnemonics.setLocalizedText(item, item.getText());
			Actions.connect(item, (Action)SystemAction.get(Fuse.class), true);
			menu.add(item);

			item = new JMenuItem();
			Mnemonics.setLocalizedText(item, item.getText());
			Actions.connect(item, (Action)SystemAction.get(Cut.class), true);
			menu.add(item);

			item = new JMenuItem();
			Mnemonics.setLocalizedText(item, item.getText());
			Actions.connect(item, (Action)SystemAction.get(Section.class), true);
			menu.add(item);

			return menu;
		}

	    public javax.swing.JMenuItem getPopupPresenter() {
	    	return getMenuPresenter();
	    }

		protected void performAction(Node[] arg0)
		{
		}

		protected boolean enable(Node[] arg0)
		{
			return true;
		}

		public String getName()
		{
			return "Boolean";
		}

		public HelpCtx getHelpCtx()
		{
			return null;
		}
	}
	
	static public class Common extends BooleanAction
	{

		public String getName()
		{
			return "Common";
		}

		protected BRepAlgoAPI_BooleanOperation getTransformation(TopoDS_Shape s1, TopoDS_Shape s2)
		{
			return new BRepAlgoAPI_Common(s1, s2);
		}
	}

	static public class Cut extends BooleanAction
	{

		public String getName()
		{
			return "Cut";
		}

		protected BRepAlgoAPI_BooleanOperation getTransformation(TopoDS_Shape s1, TopoDS_Shape s2)
		{
			return new BRepAlgoAPI_Cut(s1, s2);
		}
	}

	static public class Fuse extends BooleanAction
	{

		public String getName()
		{
			return "Fuse";
		}

		protected BRepAlgoAPI_BooleanOperation getTransformation(TopoDS_Shape s1, TopoDS_Shape s2)
		{
			return new BRepAlgoAPI_Fuse(s1, s2);
		}
	}

	static public class Section extends BooleanAction
	{

		public String getName()
		{
			return "Section";
		}

		protected BRepAlgoAPI_BooleanOperation getTransformation(TopoDS_Shape s1, TopoDS_Shape s2)
		{
			return new BRepAlgoAPI_Section(s1, s2);
		}
	}
	

	protected Class[] cookieClasses()
	{
		return new Class[]{ShapeCookie.class};
	}
	
	protected boolean enable(Node[] arg0)
	{
		return arg0.length==2;
	}
	
	public HelpCtx getHelpCtx()
	{
		return null;
	}
	
	abstract protected  BRepAlgoAPI_BooleanOperation getTransformation(TopoDS_Shape s1, TopoDS_Shape s2);

	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected void performAction(Node[] arg0)
	{
		if(arg0.length==2)
		{
			TopoDS_Shape shape1 = GeomUtils.getShape(arg0[0]);
			TopoDS_Shape shape2 = GeomUtils.getShape(arg0[1]);
			BRepAlgoAPI_BooleanOperation bt=getTransformation(shape1, shape2);			
			GeomUtils.insertShape(bt.shape(), getName(),
				arg0[0].getParentNode());
			GeomUtils.getParentBrep(arg0[0]).getDataObject().setModified(true);
		}
	}
	
	
}
