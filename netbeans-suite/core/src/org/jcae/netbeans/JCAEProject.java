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

package org.jcae.netbeans;

import java.util.Arrays;
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
	private class ProjectChildren extends Children.Array
	{				
		public ProjectChildren(FileObject directory)
		{
			super(Arrays.asList(new Node[]{
				new ModuleNode(JCAEProject.this),
				new org.jcae.netbeans.mesh.ModuleNode(JCAEProject.this)
			}));
		}
	}	
	
	private final FileObject projectDirectory;
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
		Node n=DataFolder.findFolder(getProjectDirectory()).getNodeDelegate();
		return new FilterNode(n,
			new ProjectChildren(getProjectDirectory()),
			Lookups.singleton(this));		
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
