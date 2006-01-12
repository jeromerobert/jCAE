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

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

public class ExplodeAction extends CookieAction
{
	private static Class[] COOKIE_CLASSES=new Class[]{ShapeOperationCookie.class};
	protected int mode()
	{
		return CookieAction.MODE_ONE;
	}

	protected Class[] cookieClasses()
	{
		return COOKIE_CLASSES;
	}

	protected void performAction(Node[] arg0)
	{
		JPanel panel=new JPanel();
		panel.add(new JLabel("Shape type"));
		int maxType=getMaxType(arg0);
		JComboBox box=createCombo(maxType);
		panel.add(box);
		
		DialogDescriptor dd=new DialogDescriptor(panel, "Explode");
		DialogDisplayer.getDefault().createDialog(dd).setVisible(true);
		
		if(dd.getValue()==NotifyDescriptor.OK_OPTION)
		{			
			for(int i=0; i<arg0.length; i++)
			{
				ShapeOperationCookie sc=(ShapeOperationCookie)
					arg0[i].getCookie(ShapeOperationCookie.class);
				sc.explode(box.getSelectedIndex()+maxType);
			}
		}
	}
	
	private static JComboBox createCombo(int type)
	{
		Object[] toReturn=new Object[TopAbs_ShapeEnum.SHAPE-type+1];
		for(int i=type; i<=TopAbs_ShapeEnum.SHAPE; i++)
		{
			toReturn[i-type]=ShapePool.SHAPE_LABEL[i];
		}
		return new JComboBox(toReturn);
	}
	
	private static int getMaxType(Node[] node)
	{
		int maxType=0;
		for(int i=0; i<node.length; i++)
		{
			int type=GeomUtils.getShape(node[i]).shapeType();
			if(type>maxType)
				maxType=type;
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
	
	protected boolean asynchronous()
	{
		return false;
	}
}
