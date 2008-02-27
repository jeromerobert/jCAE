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

package org.jcae.netbeans.mesh;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.LifecycleManager;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.Cancellable;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.openide.windows.WindowManager;

public class ComputeMeshAction extends CookieAction
{
	static private class MeshRun implements Runnable
	{
		private Runnable runnable;
		private Cancellable cancellable;
		private MeshNode node;

		public MeshRun(Runnable r, Cancellable cancellable, MeshNode node)
		{
			runnable=r;
			this.cancellable=cancellable;
			this.node=node;
		}
		
		public void run()
		{
			ProgressHandle ph = ProgressHandleFactory.createHandle(runnable.toString(), cancellable);
			ph.start();
			runnable.run();
			node.refreshGroups();
			ph.finish();
		}
	}  
	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected Class[] cookieClasses()
	{
		return new Class[]{MeshNode.class};
	}

	protected void performAction(Node[] arg0)
	{
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){

				WindowManager.getDefault().findTopComponent("output").open();
			}
		});
		LifecycleManager.getDefault().saveAll();
		for (int i = 0; i < arg0.length; i++)
		{
			MeshNode m = arg0[0].getCookie(MeshNode.class);			
			String ref = FileUtil.toFile(
				m.getDataObject().getPrimaryFile().getParent()).getPath();

			if(m.getMesh().getGeometryFile()!=null)
			{
				JCAEMesher r=new JCAEMesher(ref, m.getMesh());
				new Thread(new MeshRun(r, r, m)).start();
			}
			else
			{
				SwingUtilities.invokeLater(new Runnable(){
					public void run()
					{
						JOptionPane.showMessageDialog(
							WindowManager.getDefault().getMainWindow(),
							"This mesh have no geometry. To set a geometry in "+
							"this mesh copy/past or drag/drop a geomtry node on it.",
							"Undefined geometry file",
							JOptionPane.ERROR_MESSAGE);
					}
				});
			}
		}
	}

	protected boolean asynchronous()
	{
		return true;
	}

	public String getName()
	{
		return "Compute";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}	
}
