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
 * (C) Copyright 2005-2009, by EADS France
 */

package org.jcae.netbeans.mesh;

import org.jcae.netbeans.mesh.bora.BoraNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.jcae.mesh.xmldata.Group;
import org.jcae.mesh.xmldata.Groups;
import org.jcae.netbeans.viewer3d.EntitySelection;
import org.jcae.netbeans.viewer3d.SelectionManager;
import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.xml.sax.SAXException;

public class FuseGroupAction extends CookieAction{

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
			//A set to ensure all nodes are from the same mesh
			HashSet<Groups> set=new HashSet<Groups>();
			ArrayList<Group> list=new ArrayList<Group>();
			for(int i=0; i<arg0.length; i++)
			{
				GroupNode n=arg0[i].getCookie(GroupNode.class);
				set.add(n.getGroups());
				list.add(n.getGroup());
			}
					
			if(set.size()>1)
			{
				JOptionPane.showMessageDialog(null,
					"Fuse can only work with groups from the same mesh");
				return;
			}
			
			Groups groups=(Groups) set.toArray()[0];
			groups.fuse(list);
			EntitySelection meshSelection = SelectionManager.getDefault().getEntitySelection(this);
			if(meshSelection!=null)
				meshSelection.unselectAll();
			BoraNode mn=arg0[0].getParentNode().getParentNode().getCookie(BoraNode.class);
			mn.refreshGroups();			
		}
		catch(IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		} catch (TransformerConfigurationException e) {
			ErrorManager.getDefault().notify(e);
		} catch (TransformerException e) {
			ErrorManager.getDefault().notify(e);
		} catch (ParserConfigurationException e) {
			ErrorManager.getDefault().notify(e);
		} catch (SAXException e) {
			ErrorManager.getDefault().notify(e);
		}
	}

	public String getName()
	{
		return "Fuse";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}

}
