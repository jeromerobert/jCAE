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
 * (C) Copyright 2006, by EADS CRC
 */

package org.jcae.netbeans.mesh;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import javax.media.j3d.BranchGroup;
import org.jcae.mesh.oemm.IndexedStorage;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.OEMMViewer;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.viewer3d.SelectViewableAction;
import org.jcae.netbeans.viewer3d.View3DManager;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.bg.ViewableBG;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.openide.util.actions.SystemAction;

public final class OEMMViewAction extends CookieAction
{
	public static class OEMMKeyListener extends KeyAdapter
	{
		private ViewableBG femesh;
		private View bgView;
		private OEMM oemm;
		private ViewableBG fe1;
		
		public OEMMKeyListener(View view, OEMM oemm, ViewableBG viewable)
		{
			bgView=view;
			this.oemm=oemm;
			fe1=viewable;
		}
		
		public void keyPressed(KeyEvent event)
		{
			if(bgView.getCurrentViewable()!=fe1)
			{			
				if(!Arrays.asList(bgView.getViewables()).contains(fe1))
				{
					bgView.removeKeyListener(this);
				}
				return;
			}
			if(event.getKeyChar()=='n')
			{
				if (femesh != null)
					bgView.remove(femesh);
				if(fe1.getResultSet().size()>0)
				{
					BranchGroup mesh = OEMMViewer.meshOEMM(oemm, fe1.getResultSet());
					mesh.setPickable(false);
					femesh=new ViewableBG(mesh);
					fe1.unselectAll();
					bgView.add(femesh);
				}
			}
			else if(event.getKeyChar()=='d')
			{
				if (femesh != null)
					bgView.remove(femesh);
				org.jcae.mesh.amibe.ds.Mesh amesh =
					IndexedStorage.loadNodes(oemm, fe1.getResultSet());
				
				if(fe1.getResultSet().size()>0)
				{
					BranchGroup mesh = OEMMViewer.meshOEMM(oemm, fe1.getResultSet());
					mesh.setPickable(false);
					femesh=new ViewableBG(mesh);
					fe1.unselectAll();
					bgView.add(femesh);
				}
			}
			((SelectViewableAction)SystemAction.get(SelectViewableAction.class)).refresh();
		}
		
	}
	
	protected void performAction(Node[] activatedNodes)
	{
		MeshDataObject c = (MeshDataObject) activatedNodes[0].getCookie(MeshDataObject.class);
		
		String reference = FileUtil.toFile(
			c.getPrimaryFile().getParent()).getPath();
		String oemmDir=Utilities.absoluteFileName(
			activatedNodes[0].getName()+".oemm", reference);
		view(oemmDir, activatedNodes[0].getName()+" OEMM");
	}

	public static void view(String dir, String viewableName)
	{
		final OEMM oemm = IndexedStorage.buildOEMMStructure(dir);
		boolean onlyLeaves = true;
		View bgView=View3DManager.getDefault().getView3D().getView();
		BranchGroup octree = OEMMViewer.bgOEMM(oemm, onlyLeaves);
		ViewableBG fe1 = new ViewableBG(octree);
		fe1.setName(viewableName);
		bgView.add(fe1);
		bgView.addKeyListener(new OEMMKeyListener(bgView, oemm, fe1));
		bgView.setCurrentViewable(fe1);
		((SelectViewableAction)SystemAction.get(SelectViewableAction.class)).refresh();
	}	
	
	protected int mode()
	{
		return CookieAction.MODE_EXACTLY_ONE;
	}
	
	public String getName()
	{
		return NbBundle.getMessage(OEMMViewAction.class, "CTL_OEMMViewAction");
	}
	
	protected Class[] cookieClasses()
	{
		return new Class[] {
			MeshDataObject.class
		};
	}
	
	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}
	
	protected boolean asynchronous()
	{
		return false;
	}
	
}

