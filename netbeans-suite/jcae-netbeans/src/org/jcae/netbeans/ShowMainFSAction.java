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
