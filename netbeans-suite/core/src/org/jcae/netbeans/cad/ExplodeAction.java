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
 * (C) Copyright 2009, by EADS France
 */

package org.jcae.netbeans.cad;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jcae.opencascade.Shape;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

public class ExplodeAction extends CookieAction
{
	protected int mode()
	{
		return CookieAction.MODE_ONE;
	}

	protected Class[] cookieClasses()
	{
		return new Class[] { ViewShapeCookie.class };
	}

	protected void performAction(Node[] nodes)
	{
		JPanel panel=new JPanel();
		panel.add(new JLabel("Shape type"));
		int maxType=getMaxType(nodes);
		JComboBox box=createCombo(maxType);
		panel.add(box);
		
		DialogDescriptor dd=new DialogDescriptor(panel, "Explode");
		DialogDisplayer.getDefault().createDialog(dd).setVisible(true);
				
		if(dd.getValue()==NotifyDescriptor.OK_OPTION)
		{			
			int type = box.getSelectedIndex()+maxType;
			for(Node n:nodes)
			{				
				NbShape shape = GeomUtils.getShape(n);
				n.getLookup().lookup(ShapeChildren.class).addShapes(
					shape.explore(type));
			}
		}
	}
	
	private static JComboBox createCombo(int type)
	{
		Object[] toReturn=new Object[TopAbs_ShapeEnum.values().length - type - 1];
		System.arraycopy(Shape.TYPE_LABEL, type, toReturn, 0, toReturn.length);
		return new JComboBox(toReturn);
	}
	
	private static int getMaxType(Node[] node)
	{
		int maxType=0;
		for (Node aNode : node)
		{
			int type = GeomUtils.getShape(aNode).getType().ordinal();
			if (type > maxType)
				maxType = type;
		}
		return maxType;
	}
	
	public String getName()
	{
		return "Explode";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}
	
	@Override
	protected boolean asynchronous()
	{
		return false;
	}
}
