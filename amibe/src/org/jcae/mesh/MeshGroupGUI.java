/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh;
import org.jcae.gui.AbstractModuleGUI;
import javax.swing.*;
import java.awt.event.*;
import org.jcae.util.FileFilterExtension;
import javax.swing.filechooser.FileFilter;
import org.jcae.util.NamingServiceResolver;
import org.jcae.view3d.viewer.ViewPanel;

/**
 * @author  Jerome Robert
 */

public class MeshGroupGUI extends AbstractModuleGUI
{
	private MeshGroup group;	
	
	class ActionRename extends AbstractAction
	{
		public ActionRename()
		{
			super("Rename");
		}
		
		public void actionPerformed(ActionEvent e)
		{
			String newName=org.jcae.mesh.gui.DialogGroupRename.execute(gui);
			if(newName!=null)
			{	
				group.name(newName);
				gui.refreshTree();
			}			
		}		
	}
	
	class ActionMerge extends AbstractAction
	{
		private org.omg.CORBA.Object[] selectedObjects;
		public ActionMerge(org.omg.CORBA.Object[] selectedObjects)
		{
			super("Merge");
			this.selectedObjects=selectedObjects;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			MeshGroup newGroup=((MeshGroup)selectedObjects[0])._clone();			
			for(int i=0;i<selectedObjects.length;i++)
			{
				MeshGroup g=(MeshGroup)(selectedObjects[i]);
				newGroup.add(g);
				
				//delete the merged group
				MeshObject mesh=(MeshObject)gui.getParentInTree(g);
				mesh.deleteGroup(g);				
			}
			gui.refreshTree();
		}
	}
	
	class ActionUnion extends AbstractAction
	{
		private org.omg.CORBA.Object[] selectedObjects;
		public ActionUnion(org.omg.CORBA.Object[] selectedObjects)
		{
			super("Union");
			this.selectedObjects=selectedObjects;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			MeshGroup newGroup=((MeshGroup)selectedObjects[0])._clone();
			for(int i=0;i<selectedObjects.length;i++)
				newGroup.add((MeshGroup)selectedObjects[i]);
			gui.refreshTree();
		}
	}

	class ActionIntersection extends AbstractAction
	{
		private org.omg.CORBA.Object[] selectedObjects;
		public ActionIntersection(org.omg.CORBA.Object[] selectedObjects)
		{
			super("Intersection");
			this.selectedObjects=selectedObjects;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			MeshGroup newGroup=((MeshGroup)selectedObjects[0])._clone();
			for(int i=0;i<selectedObjects.length;i++)
				newGroup.intersect((MeshGroup)selectedObjects[i]);
			gui.refreshTree();
		}
	}
	
	class ActionDisplay extends AbstractAction
	{
		private org.omg.CORBA.Object[] selectedObjects;
		public ActionDisplay(org.omg.CORBA.Object[] selectedObjects)
		{
			super("Display");
			this.selectedObjects=selectedObjects;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			ViewPanel view=gui.newView("Mesh view");
			MeshModule meshModule=(MeshModule)(new NamingServiceResolver(gui.getORB()).
				getObject("MESH.org\\.jcae\\.mesh\\.MeshModule"));
			
			MeshScene meshScene=meshModule.createMeshScene();
			meshScene.name("Mesh Scene");
			view.addScene(meshScene);

			for(int i=0;i<selectedObjects.length;i++)
			{
				ViewableMesh vm=meshModule.createViewableMesh((MeshGroup)selectedObjects[i]);
				meshScene.addObject(vm);
				vm.scene(meshScene);
				meshScene.showFreeEdges(vm,true);
				meshScene.showTEdges(vm,true);
				meshScene.showWorstTriangle(vm, 0.2f);				
				view.refresh();
			}
			view.zoomTo();
		}		
	}
	
	class ActionCreateMesh extends AbstractAction
	{
		private org.omg.CORBA.Object[] selectedObjects;
		public ActionCreateMesh(org.omg.CORBA.Object[] selectedObjects)
		{
			super("Insert into a new mesh");
			this.selectedObjects=selectedObjects;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			MeshModule meshModule=(MeshModule)(new NamingServiceResolver(gui.getORB()).
				getObject("MESH.org\\.jcae\\.mesh\\.MeshModule"));
			MeshObject o=meshModule.createMeshObject(null);
			for(int i=0;i<selectedObjects.length;i++)
			{
				o.insertGroup((MeshGroup)selectedObjects[i]);
			}
			gui.refreshTree();
		}		
	}

	class ActionDelete extends AbstractAction
	{
		private org.omg.CORBA.Object[] selectedObjects;
		public ActionDelete(org.omg.CORBA.Object[] selectedObjects)
		{
			super("Delete");
			this.selectedObjects=selectedObjects;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			for(int i=0;i<selectedObjects.length;i++)
			{
				MeshGroup g=(MeshGroup)(selectedObjects[i]);
				MeshObject mesh=(MeshObject)gui.getParentInTree(g);
				mesh.deleteGroup(g);
			}
			gui.refreshTree();
		}		
	}
	
	public MeshGroupGUI(org.omg.CORBA.Object group)
	{
		super(group);
		this.group=(MeshGroup)group;
	}
	
	public JPopupMenu getPopupMenu()
	{
		JPopupMenu menu=new JPopupMenu();
		menu.add(new JMenuItem(new ActionRename()));
		menu.add(new JMenuItem(new ActionUnion(selectedObjects)));
		menu.add(new JMenuItem(new ActionMerge(selectedObjects)));
		menu.add(new JMenuItem(new ActionIntersection(selectedObjects)));
		menu.add(new JMenuItem(new ActionDisplay(selectedObjects)));
		menu.add(new JMenuItem(new ActionCreateMesh(selectedObjects)));
		menu.add(new JMenuItem(new ActionDelete(selectedObjects)));
		return menu;
	}
}
