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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import org.jcae.netbeans.cad.ModuleNode;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.ErrorManager;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataLoader;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;

public class JCAEProject implements Project, LogicalViewProvider, ActionProvider
{
	private static String  MIME_UNKNOWN="content/unknown";
	
	private class ProjectChildren extends Children.Keys implements FileChangeListener
	{		
		private FileObject directory;
		private ModuleNode cadNode=new ModuleNode(JCAEProject.this);
		private org.jcae.netbeans.mesh.ModuleNode meshNode=
			new org.jcae.netbeans.mesh.ModuleNode(JCAEProject.this);
		private java.util.Map loaderToMNode=new HashMap();
		
		public ProjectChildren(FileObject directory)
		{		
			this.directory=directory;
			directory.addFileChangeListener(this);
		}
		
		protected Node[] createNodes(Object arg0)
		{
			return new Node[]{(Node)arg0};
		}
		
		private void clearMNodes()
		{
			Iterator it=loaderToMNode.values().iterator();
			while(it.hasNext())
			{
				Node n=(Node)it.next();
				n.getChildren().remove(n.getChildren().getNodes());
			}
		}
		
		protected void addNotify()
		{			
			FileObject[] os=directory.getChildren();
			HashSet l=new HashSet();
			clearMNodes();
			for(int i=0; i<os.length; i++)
			{
				String s=FileUtil.getMIMEType(os[i]);
				if(s!=null && !MIME_UNKNOWN.equals(s) && 
					!"text/x-unv".equals(s) && !"text/mesh+xml".equals(s))
				{					
					try
					{					
						DataObject dObj = DataObject.find(os[i]);						
						DataLoader loader=dObj.getLoader();
						Node mNode=(Node)loaderToMNode.get(loader.getClass());						
						if(mNode==null)
						{
							mNode=new AbstractNode(new Children.Array());
							mNode.setDisplayName(loader.getDisplayName());
							loaderToMNode.put(loader.getClass(), mNode);							
						}
						l.add(mNode);
						mNode.getChildren().add(new Node[]{dObj.getNodeDelegate()});
					}
					catch (DataObjectNotFoundException ex)
					{
						ErrorManager.getDefault().notify(ex);
					}					
				}
			}				
			Object[] keys=new Object[2+l.size()];
			keys[0]=cadNode;
			keys[1]=meshNode;
			System.arraycopy(l.toArray(), 0, keys, 2, l.size());
			setKeys(keys);				
		}		
		
		public void fileFolderCreated(FileEvent arg0)
		{		
			addNotify();
		}

		public void fileDataCreated(FileEvent arg0)
		{
			RequestProcessor.getDefault().post(new Runnable()
			{
				public void run()
				{
					addNotify();
				}				
			});
		}

		public void fileChanged(FileEvent arg0)
		{		
			addNotify();
		}

		public void fileDeleted(FileEvent arg0)
		{
			addNotify();
		}

		public void fileRenamed(FileRenameEvent arg0)
		{
			addNotify();
		}

		public void fileAttributeChanged(FileAttributeEvent arg0)
		{
			addNotify();
		}		
	}	
	
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
