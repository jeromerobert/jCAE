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

import org.jcae.netbeans.cad.modeler.PrimitiveNewType;
import java.util.ArrayList;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.openide.actions.CopyAction;
import org.openide.actions.ViewAction;
import org.openide.awt.Actions;
import org.openide.awt.Mnemonics;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Node;
import org.openide.nodes.Node.Cookie;
import org.openide.nodes.Sheet;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.NewType;

public class ShapeNode extends AbstractNode
{	
	static public class UpgradeActions extends NodeAction
	{
		@Override
		public JMenuItem getMenuPresenter() 
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

		@Override
	    public JMenuItem getPopupPresenter() {
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
	
	public ShapeNode(NbShape shape)
	{		
		super(new ShapeChildren());
		shape.setNode(this);
		setIconBaseWithExtension("org/jcae/netbeans/cad/ShapeNode.png");
		getCookieSet().add(new ViewShapeCookie(this));
		getCookieSet().add((Cookie) getChildren());
		getCookieSet().add(shape);
	}
	
	@Override
	public Action[] getActions(boolean arg0)
	{
		ArrayList<Action> toReturn=new ArrayList<Action>();		
		toReturn.add(SystemAction.get(ExplodeAction.class));
		toReturn.add(SystemAction.get(ViewAction.class));
		toReturn.add(SystemAction.get(RemoveAction.class));
		toReturn.add(SystemAction.get(BooleanAction.AllActions.class));
		toReturn.add(SystemAction.get(TransformAction.AllActions.class));
		toReturn.add(SystemAction.get(UpgradeActions.class));
		toReturn.add(SystemAction.get(FreeBoundsAction.class));
		toReturn.add(SystemAction.get(BoundingBoxAction.class));
		if(GeomUtils.getShape(this).getType()==TopAbs_ShapeEnum.FACE)
			toReturn.add(SystemAction.get(ReverseAction.class));
		toReturn.add(null);
		toReturn.add(SystemAction.get(CopyAction.class));
		return toReturn.toArray(new Action[toReturn.size()]); 
	}

	@Override
	public String getDisplayName()
	{
		return GeomUtils.getShape(this).getName();
	}

	@Override
	public String getName()
	{
		return GeomUtils.getShape(this).getName();
	}

	@Override
	public boolean canDestroy()
	{
		return true;
	}
	
	@Override
	public NewType[] getNewTypes()
	{
		return PrimitiveNewType.getNewType(this);		
	}

	@Override
	public Sheet createSheet()
	{
		Sheet sheet=super.createSheet();
		sheet.put(GeomUtils.createSheetSet(this));		
		return sheet;
	}

	@Override
	public boolean canCopy()
	{
		return true;
	}
}
