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

import java.io.IOException;
import org.openide.execution.ExecutionEngine;
import org.openide.execution.ExecutorTask;
import org.openide.execution.NbProcessDescriptor;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.NodeOperation;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.IOProvider;

public class ShowMainFSAction extends CallableSystemAction
{
	public void performAction()
	{
		try
		{
			NodeOperation.getDefault().explore(DataObject.find(Repository.getDefault().getDefaultFileSystem().getRoot()).getNodeDelegate());		
			Runnable r=new Runnable()
			{
				public void run()
				{					
					try
					{
						new NbProcessDescriptor("/usr/bin/find", "/").exec();
						/*Runtime.getRuntime().exec("/usr/bin/find", new String[]{"/"}).waitFor();
						System.out.println("pouet");*/
					}
					catch(IOException ex)
					{
						throw new RuntimeException();
					}
					/*catch (InterruptedException e)
					{
						throw new RuntimeException();
					}*/
				}
			};
			ExecutorTask t = ExecutionEngine.getDefault().execute("ls", r, IOProvider.getDefault().getIO("ls", true));
			t.waitFinished();
			System.out.println(t.isFinished());
		}
		catch (DataObjectNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	public String getName()
	{
		return "Show default filesystem";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}
}
