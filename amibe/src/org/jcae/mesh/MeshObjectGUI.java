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

import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import org.jcae.gui.AbstractModuleGUI;
import org.jcae.mesh.gui.DialogMeshProcessorList;
import org.jcae.util.FileFilterExtension;
import org.jcae.util.NamingServiceResolver;
import org.jcae.view3d.viewer.ViewPanel;

/**
 * @author  Jerome Robert
 */

public class MeshObjectGUI extends AbstractModuleGUI
{
	private MeshObject meshObject;	
	
	class ActionGetFileGroups extends AbstractAction
	{
		public ActionGetFileGroups()
		{
			super("Explode groups");
		}
		
		public void actionPerformed(ActionEvent e)
		{
			int[] ids=meshObject.getGroupIDs();
			if(ids.length==0) meshObject.getAsGroup();			
			else for(int i=0; i<ids.length; i++)
			{
				meshObject.getGroup(ids[i]);
			}
			gui.refreshTree();
		}
	}
	
	class ActionSave extends AbstractAction
	{
		private FileFilter filterUNV=new FileFilterExtension("UNV");
		private FileFilter filterXML=new FileFilterExtension("XML");
		public ActionSave()
		{
			super("Save");	
		}
		
		public void actionPerformed(ActionEvent e)
		{
			JFileChooser fileChooser=new JFileChooser();			
			fileChooser.addChoosableFileFilter(filterXML);
			fileChooser.addChoosableFileFilter(filterUNV);
			if(fileChooser.showSaveDialog(gui)!=JFileChooser.APPROVE_OPTION)
				return;
			
			if(filterXML.accept(fileChooser.getSelectedFile()))
			{
				meshObject.saveXML(fileChooser.getSelectedFile().getPath());			
			}
			else
			{
				meshObject.saveUNV(fileChooser.getSelectedFile().getPath());
			}
			
		}		
	}

	class ActionDisplay extends AbstractAction
	{	
		public ActionDisplay()
		{
			super("Display");	
		}
		
		public void actionPerformed(ActionEvent e)
		{
			ViewPanel view=gui.newView("Mesh view");
			MeshModule meshModule=(MeshModule)(new NamingServiceResolver(gui.getORB()).
				getObject("MESH.org\\.jcae\\.mesh\\.MeshModule"));
			
			MeshScene meshScene=meshModule.createMeshScene();
			meshScene.name("Mesh Scene");
			view.addScene(meshScene);

			int[] ids=meshObject.getGroupIDs();
			if(ids.length==0) 
			{
				ViewableMesh vm=meshModule.createViewableMesh(meshObject.getAsGroup());
				meshScene.addObject(vm);
				vm.scene(meshScene);
				meshScene.showFreeEdges(vm,true);
				meshScene.showTEdges(vm,true);
				meshScene.showWorstTriangle(vm, 0.2f);
			}
			else 
			for(int i=0;i<ids.length;i++)
			{
				ViewableMesh vm=meshModule.createViewableMesh(meshObject.getGroup(ids[i]));
				meshScene.addObject(vm);
				vm.scene(meshScene);
				meshScene.showFreeEdges(vm,true);
				meshScene.showTEdges(vm,true);
				meshScene.showWorstTriangle(vm, 0.2f);
			}
			view.refresh();
			view.zoomTo();
		}		
	}
	
	class ActionApplyProcessor extends AbstractAction
	{
		
		public ActionApplyProcessor()
		{
			super("Apply a processor");
		}
		
		public void actionPerformed(ActionEvent e)
		{
			DialogMeshProcessorList dialog=new DialogMeshProcessorList(null,true);
			dialog.setMeshObject(meshObject);
			MeshModule meshModule=(MeshModule)(new NamingServiceResolver(gui.getORB()).
				getObject("MESH.org\\.jcae\\.mesh\\.MeshModule"));
			dialog.setMeshModule(meshModule);
			dialog.show();
		}		
	}
	
	public MeshObjectGUI(org.omg.CORBA.Object meshObject)	
	{
		super(meshObject);
		this.meshObject=(MeshObject)meshObject;
	}
	
	public JPopupMenu getPopupMenu()
	{
		JPopupMenu menu=new JPopupMenu();
		menu.add(new JMenuItem(new ActionApplyProcessor()));
		menu.add(new JMenuItem(new ActionSave()));
		menu.add(new JMenuItem(new ActionDisplay()));
		menu.add(new JMenuItem(new ActionGetFileGroups()));		
		return menu;
	}	
}

