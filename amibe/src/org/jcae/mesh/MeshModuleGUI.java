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
/**
 * @author  Jerome Robert
 */

public class MeshModuleGUI extends AbstractModuleGUI
{
	private MeshModule meshModule;
	private FileFilter filterUNV=new FileFilterExtension("UNV");
	private FileFilter filterSFM=new FileFilterExtension("SFM");
	private FileFilter filterMESH=new FileFilterExtension("MESH");
	
	class ActionOpen extends AbstractAction
	{	
		public ActionOpen()
		{
			super("Open");
		}
		
		public void actionPerformed(ActionEvent e)
		{
			JFileChooser fileChooser=new JFileChooser();			
			fileChooser.addChoosableFileFilter(filterSFM);
			fileChooser.addChoosableFileFilter(filterMESH);
			fileChooser.addChoosableFileFilter(filterUNV);			
			if(fileChooser.showOpenDialog(gui)!=JFileChooser.APPROVE_OPTION)
				return;			
			
			MeshObject meshObject;
			if(filterSFM.accept(fileChooser.getSelectedFile()))
			{
				meshObject=meshModule.importSFM(fileChooser.getSelectedFile().getPath());
			}
			else if(filterMESH.accept(fileChooser.getSelectedFile()))
			{
				meshObject=meshModule.importMESH(fileChooser.getSelectedFile().getPath());
			}
			else
			{
				meshObject=meshModule.importUNV(fileChooser.getSelectedFile().getPath());				
			}			
	
			int[] ids=meshObject.getGroupIDs();
			if(ids.length==0) meshObject.getAsGroup();			
			else for(int i=0; i<ids.length; i++)
			{
				meshObject.getGroup(ids[i]);
			}

			gui.refreshTree();
		}
	}
	
	public MeshModuleGUI(org.omg.CORBA.Object meshObject)
	{
		super(meshObject);
		this.meshModule=(MeshModule)meshObject;
	}
	
	public JPopupMenu getPopupMenu()
	{
		JPopupMenu menu=new JPopupMenu();
		menu.add(new JMenuItem(new ActionOpen()));
		return menu;
	}	
}

