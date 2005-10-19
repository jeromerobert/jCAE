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

package org.jcae.netbeans;

import org.jcae.netbeans.cad.ModuleNode;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public class JCAEProject implements Project, LogicalViewProvider, ActionProvider
{
	private FileObject projectDirectory;
	public JCAEProject(FileObject projectDirectory, ProjectState state)
	{
		this.projectDirectory=projectDirectory;
	}

	public FileObject getProjectDirectory()
	{
		return projectDirectory;
	}

	public Lookup getLookup()
	{
		return Lookups.fixed(new Object[]{this});
	}

	public Node createLogicalView()
	{		
		Children c=new Children.Array();
		c.add(new Node[]{new ModuleNode(this), new org.jcae.netbeans.mesh.ModuleNode(this)});
		Node n=DataFolder.findFolder(getProjectDirectory()).getNodeDelegate();
		return new FilterNode(n, c, Lookups.singleton(this));
	}
	
	public Node findPath(Node arg0, Object arg1)
	{
		return null;
	}

	public String[] getSupportedActions()
	{
		return new String[0];
	}

	public void invokeAction(String arg0, Lookup arg1) throws IllegalArgumentException
	{
		//nothing
	}

	public boolean isActionEnabled(String arg0, Lookup arg1) throws IllegalArgumentException
	{
		return false;
	}
}
