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

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jcae.mesh.JCAEFormatter;
import org.jcae.mesh.bora.ds.BModel;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.LifecycleManager;
import org.openide.execution.ExecutionEngine;
import org.openide.execution.ExecutorTask;
import org.openide.nodes.Node;
import org.openide.util.Cancellable;
import org.openide.util.HelpCtx;
import org.openide.util.Task;
import org.openide.util.TaskListener;
import org.openide.util.actions.CookieAction;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.WindowManager;

public class ComputeMeshAction extends CookieAction
{
	static private class MeshRun implements Runnable
	{
		private BModel model;
		private MeshNode node;

		public MeshRun(BModel model, MeshNode node)
		{
			this.model = model;
			this.node=node;
		}
		
		public void run()
		{
			final Formatter jcaeFormatter = new JCAEFormatter();

			//getting and redirecting logs from Bora mesher
			Logger root = Logger.getLogger("org.jcae.mesh.bora");
			root.setLevel(Level.INFO);
			Handler redirector = new Handler() {
				@Override
				public void publish(LogRecord record) {
					System.out.println(jcaeFormatter.format(record));
				}
				@Override
				public void close() throws SecurityException {
				}
				@Override
				public void flush() {
				}
			};
			root.addHandler(redirector);
			for (Handler h : root.getHandlers()) {
				h.setFormatter(jcaeFormatter);
			}

			//computing the bora model
			model.cleanWorkDirectory();
			model.compute();
			node.refreshGroups();

			root.removeHandler(redirector);
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

	private static transient int ioProviderCounter=1;

	protected void performAction(Node[] arg0)
	{
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				WindowManager.getDefault().findTopComponent("output").open();
			}
		});

		LifecycleManager.getDefault().saveAll();
		InputOutput io=IOProvider.getDefault().getIO("jCAE Mesher "+ioProviderCounter, true);
		ioProviderCounter++;
		for (int i = 0; i < arg0.length; i++) {
			final MeshNode m = arg0[i].getCookie(MeshNode.class);
			if (m.getBModel() != null) {
				final ExecutorTask task = ExecutionEngine.getDefault().execute("Bora Mesher",new MeshRun(m.getBModel(), m), io);
				final ProgressHandle ph = ProgressHandleFactory.createHandle("Bora Mesher", new Cancellable() {
					public boolean cancel() {
						task.stop();
						return true;
					}
				});
				ph.start();
				task.addTaskListener(new TaskListener() {
					public void taskFinished(Task arg0) {
						ph.finish();
					}
				});
			}
		}
	}

	@Override
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
