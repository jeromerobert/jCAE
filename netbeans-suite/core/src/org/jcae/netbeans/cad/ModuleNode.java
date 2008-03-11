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

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.swing.Action;
import org.jcae.netbeans.Utilities;
import org.netbeans.api.project.Project;
import org.openide.actions.NewAction;
import org.openide.actions.PasteAction;
import org.openide.filesystems.*;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.NewType;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public class ModuleNode extends AbstractNode
{	
	private static class ModuleChildren extends Children.Keys implements FileChangeListener
	{
		private final FileObject directory;

		private final static Comparator DATAOBJECT_COMPARATOR=new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				DataObject d1=(DataObject) o1;
				DataObject d2=(DataObject) o2;
				return d1.getName().compareTo(d2.getName());
			}
		};

		public ModuleChildren(FileObject directory)
		{		
			this.directory=directory;
			directory.addFileChangeListener(this);
		}

		protected Node[] createNodes(Object arg0)
		{
			return new Node[]{((DataObject) arg0).getNodeDelegate()};
		}
		
		@Override
		protected void addNotify()
		{
			try
			{
				FileObject[] os=directory.getChildren();
				ArrayList<DataObject> l=new ArrayList<DataObject>();
				for (FileObject o1 : os)
				{
					if(BrepDataLoader.EXTENSION.contains(o1.getExt()))					
						l.add(DataObject.find(o1));
				}
				Object[] o=l.toArray();
				Arrays.sort(o, DATAOBJECT_COMPARATOR);
				setKeys(o);
			}
			catch (DataObjectNotFoundException e) {
				org.openide.ErrorManager.getDefault().notify(e);
			}
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
	
	private static class MyLookup extends ProxyLookup
	{		
		public void setDelegates(Lookup... lookups) 
		{
			setLookups(lookups);
		}
	}
	
	public ModuleNode(Project project)
	{
		super(new ModuleChildren(project.getProjectDirectory()), new MyLookup());
		((MyLookup)getLookup()).setDelegates(Lookups.fixed(project, this));
	}
	
	@Override
	public String getName()
	{
		return "Geometries";
	}
	
	@Override
	public Action[] getActions(boolean arg0)
	{
		return new Action[]{
			SystemAction.get(NewAction.class),
			SystemAction.get(PasteAction.class)};
	}
	
	@Override
	public NewType[] getNewTypes()
	{
		return new NewType[]{new NewType()
		{
			public void create() throws IOException
			{
				Project p=getLookup().lookup(Project.class);
				FileObject fo=p.getProjectDirectory();
				fo.createData(Utilities.getFreeName(fo,"Geometry",".brep"));
			}
			
			@Override
			public String getName()
			{
				return "Geometry";
			}
		}};
	}

	@Override
	protected void createPasteTypes(Transferable t, List<PasteType> ls)
	{
		Node[] ns = NodeTransfer.nodes(t, NodeTransfer.COPY|NodeTransfer.MOVE);
		if (ns != null && ns.length==1)
		{
			final NbShape shape = GeomUtils.getShape(ns[0]);
			if(shape!=null) ls.add(new PasteType()
			{
				public Transferable paste()
				{
					Project p=getLookup().lookup(Project.class);
					FileObject fo=p.getProjectDirectory();
					String s = Utilities.getFreeName(fo, shape.getName(), ".brep");					
					String fileName=new File(FileUtil.toFile(fo), s).getPath();
					shape.saveImpl(fileName);
					return null;
				}
			});
		}
		// Also try superclass, but give it lower priority:
		super.createPasteTypes(t, ls);
	}
}
