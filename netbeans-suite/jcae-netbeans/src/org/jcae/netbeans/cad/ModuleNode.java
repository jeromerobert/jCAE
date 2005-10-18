package org.jcae.netbeans.cad;

import java.io.IOException;
import java.util.ArrayList;
import javax.swing.Action;
import org.jcae.netbeans.Utilities;
import org.netbeans.api.project.Project;
import org.openide.actions.NewAction;
import org.openide.filesystems.*;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.NewType;
import org.openide.util.lookup.Lookups;

public class ModuleNode extends AbstractNode
{	
	private static class ModuleChildren extends Children.Keys implements FileChangeListener
	{
		private FileObject directory;

		public ModuleChildren(FileObject directory)
		{		
			this.directory=directory;
			directory.addFileChangeListener(this);
		}

		protected Node[] createNodes(Object arg0)
		{
			return new Node[]{((DataObject) arg0).getNodeDelegate()};
		}
		
		protected void addNotify()
		{
			try
			{
				FileObject[] os=directory.getChildren();
				ArrayList l=new ArrayList();
				for(int i=0; i<os.length; i++)
				{
					if(os[i].getExt().equalsIgnoreCase("brep"))
						l.add(DataObject.find(os[i]));						
				}
				setKeys(l);
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
	
	public ModuleNode(Project project)
	{
		super(new ModuleChildren(project.getProjectDirectory()), Lookups.singleton(project));		
	}
	
	public String getName()
	{
		return "Geometries";
	}
	
	public Action[] getActions(boolean arg0)
	{
		return new Action[]{SystemAction.get(NewAction.class)};
	}
	
	public NewType[] getNewTypes()
	{
		return new NewType[]{new NewType()
		{
			public void create() throws IOException
			{
				Project p=(Project) getLookup().lookup(Project.class);
				FileObject fo=p.getProjectDirectory();
				fo.createData(Utilities.getFreeName(fo,"Geometry",".brep"));
			}
			
			public String getName()
			{
				return "Geometry";
			}
		}};
	}
}
