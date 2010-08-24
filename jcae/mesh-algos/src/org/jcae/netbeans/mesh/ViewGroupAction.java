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
 * (C) Copyright 2005-2010, by EADS France
 */

package org.jcae.netbeans.mesh;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeSet;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.mesh.xmldata.Group;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.AmibeToMesh;
import org.jcae.vtk.View;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.openide.windows.TopComponent;
import org.xml.sax.SAXException;

public class ViewGroupAction extends CookieAction
{

	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected Class<?>[] cookieClasses()
	{
		return new Class<?>[]{GroupNode.class, AmibeNode.class};
	}

	private TreeSet<Group> createTreeSet(Group ... groups)
	{
		TreeSet<Group> r = new TreeSet<Group>(new Comparator<Group>(){
			public int compare(Group t, Group t1) {
				return t.getName().compareTo(t1.getName());
			}
		});
		r.addAll(Arrays.asList(groups));
		return r;
	}

	protected void performAction(Node[] arg0)
	{
		ExplorerManager em = ((ExplorerManager.Provider)
			TopComponent.getRegistry().getActivated()).getExplorerManager();
		try
		{
			HashMap<Node, Collection<Group>> groups2Group =
				new HashMap<Node, Collection<Group>>();

			//AmibeNode
			for(Node n:arg0)
			{
				AmibeDataObject ado = n.getLookup().lookup(AmibeDataObject.class);
				if(ado != null)
					groups2Group.put(n, createTreeSet(ado.getGroups().getGroups()));
			}

			//GroupNode
			for(Node n:arg0)
			{
				GroupNode gn = n.getLookup().lookup(GroupNode.class);
				if(gn != null)
				{
					Node amibeNode=n.getParentNode().getParentNode();
					Collection<Group> c=groups2Group.get(amibeNode);
					if(c==null)
					{
						c=createTreeSet(gn.getGroup());
						groups2Group.put(amibeNode, c);
					}
					else
						c.add(gn.getGroup());
				}
			}

			View v = ViewManager.getDefault().getCurrentView();
			for(Entry<Node, Collection<Group>> e:groups2Group.entrySet())
				displayGroups(e.getValue(), v, e.getKey(), em);
		}
		catch(IOException ex)
		{
			Exceptions.printStackTrace(ex);
		}
		catch(ParserConfigurationException ex)
		{
			Exceptions.printStackTrace(ex);
		}
		catch(SAXException ex)
		{
			Exceptions.printStackTrace(ex);
		}			
	}

	public String getName()
	{
		return "View";
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

	/**
	 * Display in a View3D a list of groups.
	 * The xml Directory xmlDir must be setted. After having load a project, xmlDir may be null the first time.
	 *
	 * @param the list of groups to display.
	 * @param the View3D in which the Groups are displayed.
	 */
	private static void displayGroups(Collection<Group> groupsToDisplay,
		View view, Node node, ExplorerManager em)
		throws ParserConfigurationException, SAXException, IOException
	{
		String[] idGroupsDisplayed = new String[groupsToDisplay.size()];
		Iterator<Group> iter = groupsToDisplay.iterator();
		String sb="";
		boolean full=false;
		for(int i = 0 ; i < idGroupsDisplayed.length ; ++i)
		{
			Group g = iter.next();

			idGroupsDisplayed[i] = g.getName();
			if(sb.length()<20)
				sb=sb+" "+g.getName();
			else
				full=true;
		}

		if(full)
			sb=sb+"...";

		AmibeDataObject ado = node.getLookup().lookup(AmibeDataObject.class);
		AmibeToMesh reader = new AmibeToMesh(ado.getGroups().getMeshFile(), idGroupsDisplayed);
		AmibeNViewable interactor = new AmibeNViewable(reader.getTriangles(), node, em);
		interactor.addBeams(reader.getBeams());
		interactor.setName(ado.getName()+" ["+sb+"]");
		//SelectionManager.getDefault().addInteractor(interactor, groups);
		view.add(interactor);
	}
}
