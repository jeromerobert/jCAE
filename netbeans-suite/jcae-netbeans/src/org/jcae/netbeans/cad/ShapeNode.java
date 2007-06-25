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

import java.util.ArrayList;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jcae.netbeans.cad.BooleanAction.Common;
import org.jcae.netbeans.cad.BooleanAction.Cut;
import org.jcae.netbeans.cad.BooleanAction.Section;
import org.jcae.opencascade.Utilities;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.actions.CopyAction;
import org.openide.actions.ViewAction;
import org.openide.awt.Actions;
import org.openide.awt.Mnemonics;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Node;
import org.openide.nodes.Node.Cookie;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.NewType;

public class ShapeNode extends AbstractNode implements ShapeCookie
{	
	static public class UpgradeActions extends NodeAction
	{
		public javax.swing.JMenuItem getMenuPresenter() 
		{
			JMenu menu = new JMenu();		
			Mnemonics.setLocalizedText(menu, getName());		

			JMenuItem item = new JMenuItem();
			Mnemonics.setLocalizedText(item, item.getText());
			Actions.connect(item, (Action)SystemAction.get(SewAction.class), true);
			menu.add(item);

			item = new JMenuItem();
			Mnemonics.setLocalizedText(item, item.getText());
			Actions.connect(item, (Action)SystemAction.get(RemoveHoles.class), true);
			menu.add(item);

			item = new JMenuItem();
			Mnemonics.setLocalizedText(item, item.getText());
			Actions.connect(item, (Action)SystemAction.get(SplitFaces.class), true);
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
			return "Upgrade";
		}

		public HelpCtx getHelpCtx()
		{
			return null;
		}
	}
		
	private String name;
	
	protected TopoDS_Shape shape;
	
	public ShapeNode(String name, TopoDS_Shape shape, ShapePool pool)
	{		
		super(new ShapeChildren());
		this.shape=shape;
		getCookieSet().add(this);
		getCookieSet().add(pool);
		getCookieSet().add(new ShapeOperationCookie(this));
		getCookieSet().add((Cookie) getChildren());
		this.name=name;
		pool.putNode(shape, this);
	}
	
	public Action[] getActions(boolean arg0)
	{
		ArrayList toReturn=new ArrayList();		
		toReturn.add(SystemAction.get(ExplodeAction.class));
		toReturn.add(SystemAction.get(ViewAction.class));
		toReturn.add(SystemAction.get(RemoveAction.class));
		toReturn.add(SystemAction.get(BooleanAction.AllActions.class));
		toReturn.add(SystemAction.get(TransformAction.AllActions.class));
		toReturn.add(SystemAction.get(UpgradeActions.class));
		toReturn.add(SystemAction.get(GroupFaceAction.class));
		toReturn.add(SystemAction.get(FreeBoundsAction.class));
		toReturn.add(SystemAction.get(BoundingBoxAction.class));
		if(getShape().shapeType()==TopAbs_ShapeEnum.FACE)
			toReturn.add(SystemAction.get(ReverseAction.class));
		toReturn.add(null);
		toReturn.add(SystemAction.get(CopyAction.class));
		return (Action[]) toReturn.toArray(new Action[0]); 
	}

	public String getDisplayName()
	{
		return name;
	}

	public String getName()
	{
		return name;
	}
	
	public TopoDS_Shape getShape()
	{
		return shape;
	}

	public boolean canDestroy()
	{
		return true;
	}
	
	/*public void destroy() throws IOException
	{
		ShapePool sp=(ShapePool) getCookie(ShapePool.class);
		getParentNode().getChildren().remove(new Node[]{this});
	}*/
	
	public NewType[] getNewTypes()
	{
		return PrimitiveNewType.getNewType(this);		
	}

	public Sheet createSheet()
	{
		Sheet sheet=super.createSheet();
		Sheet.Set set=new Sheet.Set();
		set.put(new PropertySupport.ReadOnly(
			"tolerance", Double.class, "tolerance", "tolerance")
			{	
				public Object getValue()
				{
					return Double.valueOf(Utilities.tolerance(getShape()));
				};
			});
		set.setName("Geometry");
		sheet.put(set);
		return sheet;
	}

	public boolean canCopy()
	{
		return true;
	}
}
