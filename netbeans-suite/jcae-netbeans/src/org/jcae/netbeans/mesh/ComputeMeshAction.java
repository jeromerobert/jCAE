package org.jcae.netbeans.mesh;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.LifecycleManager;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.Cancellable;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

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
		LifecycleManager.getDefault().saveAll();
		for (int i = 0; i < arg0.length; i++)
		{
			MeshNode m = (MeshNode) arg0[0].getCookie(MeshNode.class);
			String ref = FileUtil.toFile(
				m.getDataObject().getPrimaryFile().getParent()).getPath();

			JCAEMesher r=new JCAEMesher(ref, m.getMesh());
			new Thread(new MeshRun(r, r, m)).start();
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
