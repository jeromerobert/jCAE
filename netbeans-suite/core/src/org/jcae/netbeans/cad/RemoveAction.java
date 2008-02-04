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

import java.io.IOException;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jcae.opencascade.jni.BRep_Builder;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

public class RemoveAction extends CookieAction
{
	private static Class[] COOKIE_CLASSES=new Class[]{ShapeCookie.class};
	protected int mode()
	{
		return CookieAction.MODE_ANY;
	}

	protected Class[] cookieClasses()
	{
		return COOKIE_CLASSES;
	}

	protected void performAction(Node[] arg0)
	{
		for(int i=0; i<arg0.length; i++)
		{
			Node parent=arg0[i].getParentNode();			
			TopoDS_Shape shape=GeomUtils.getShape(arg0[i]);
			TopoDS_Shape parentShape=
				GeomUtils.getParentShape(GeomUtils.getShape(parent), shape);
			
			if(shape!=null && parentShape!=null)
			{
				BRep_Builder bb = new BRep_Builder();				
				bb.remove(parentShape, shape);
			}
			
			GeomUtils.getParentBrep(arg0[i]).getDataObject().setModified(true);
			try
			{
				arg0[i].destroy();
			}
			catch (IOException ex)
			{
				ErrorManager.getDefault().notify(ex);
			}			
		}
	}

	public String getName()
	{
		return "Remove";
	}

	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}
	
	protected boolean asynchronous()
	{
		return false;
	}
}
