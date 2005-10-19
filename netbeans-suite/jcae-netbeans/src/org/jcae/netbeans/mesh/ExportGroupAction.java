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

package org.jcae.netbeans.mesh;

import java.io.*;
import java.util.HashSet;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.mesh.xmldata.UNVConverter;
import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.xml.sax.SAXException;

public class ExportGroupAction extends CookieAction
{

	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected Class[] cookieClasses()
	{
		return new Class[]{GroupNode.class};
	}

	protected void performAction(Node[] arg0)
	{
		try
		{
			HashSet set=new HashSet();
			for(int i=0; i<arg0.length; i++)
			{
				MeshNode n=(MeshNode) arg0[i].getParentNode().getParentNode()
					.getCookie(MeshNode.class);
				set.add(n.getMeshDirectory());
			}
			
			if(set.size()>1)
			{
				JOptionPane.showMessageDialog(null,
					"UNV export can only work with groups from the same mesh");
				return;
			}
			
			String meshDir=set.toArray()[0].toString();
			
			JFileChooser jfc=new JFileChooser();
			if(jfc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
			{
				int[] ids=new int[arg0.length];
				for(int i=0; i<arg0.length; i++)
				{
					GroupNode n=(GroupNode) arg0[i].getCookie(GroupNode.class);
					ids[i]=n.getGroup().getId();
				}			
				PrintStream stream=new PrintStream(new BufferedOutputStream(
					new FileOutputStream(jfc.getSelectedFile())));
				new UNVConverter(new File(meshDir), ids).writeUNV(stream);
				stream.close();
			}
		}
		catch(IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
		catch (ParserConfigurationException e)
		{
			ErrorManager.getDefault().notify(e);
		}
		catch (SAXException e)
		{
			ErrorManager.getDefault().notify(e);
		}
	}

	public String getName()
	{
		return "Export as UNV";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}

}
