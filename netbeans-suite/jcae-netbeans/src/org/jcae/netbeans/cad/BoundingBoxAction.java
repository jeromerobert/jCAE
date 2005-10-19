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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.jcae.opencascade.jni.BRepBndLib;
import org.jcae.opencascade.jni.Bnd_Box;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

public class BoundingBoxAction extends CookieAction
{
	private static final String CR=System.getProperty("line.separator");
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	protected int mode()
	{
		return CookieAction.MODE_ONE;
	}
	
	protected Class[] cookieClasses()
	{
		return new Class[] { ShapeCookie.class } ;
	}
	
	protected void performAction(Node[] arg0)
	{
		Bnd_Box box = new Bnd_Box(); 			
		BRepBndLib.add(GeomUtils.getShape(arg0[0]),box);			
		double[] bbox = box.get();
		String text="Xmin="+bbox[0]+CR;
		text+="Ymin="+bbox[1]+CR;
		text+="Zmin="+bbox[2]+CR;
		
		text+="Xmax="+bbox[3]+CR;
		text+="Ymax="+bbox[4]+CR;
		text+="Zmax="+bbox[5]+CR;
		
		JTextArea textField=new JTextArea(text);
		textField.setEditable(false);
		DialogDescriptor dd=new DialogDescriptor(textField, "Bounding box");
		DialogDisplayer.getDefault().createDialog(dd).setVisible(true);
	}
	
	public String getName()
	{
		return "Bounding Box";
	}
	
	public HelpCtx getHelpCtx()
	{
		return null;
	}
}
