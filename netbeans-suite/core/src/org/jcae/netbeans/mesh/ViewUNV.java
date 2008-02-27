package org.jcae.netbeans.mesh;

import java.io.File;
import java.io.IOException;
import org.jcae.netbeans.viewer3d.View3D;
import org.jcae.netbeans.viewer3d.View3DManager;
import org.jcae.viewer3d.fe.ViewableFE;
import org.jcae.viewer3d.fe.unv.UNVProvider;
import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

public final class ViewUNV extends CookieAction
{
	
	protected void performAction(Node[] activatedNodes)
	{		
		for(int i=0; i<activatedNodes.length; i++)
		{
			try
			{
				UNVDataObject c = activatedNodes[i].getCookie(UNVDataObject.class);
				File file = FileUtil.toFile(c.getPrimaryFile());
				View3D v=View3DManager.getDefault().getView3D();
				UNVProvider unvp=new UNVProvider(file);
				ViewableFE vfe=new ViewableFE(unvp);
				vfe.setName(file.getName());
				v.add(vfe);
			}
			catch(IOException ex)
			{
				ErrorManager.getDefault().notify(ex);
			}
		}
	}
	
	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}
	
	public String getName()
	{
		return NbBundle.getMessage(ViewUNV.class, "CTL_ViewUNV");
	}
	
	protected Class[] cookieClasses()
	{
		return new Class[] {
			UNVDataObject.class
		};
	}
	
	protected void initialize()
	{
		super.initialize();
		// see org.openide.util.actions.SystemAction.iconResource() javadoc for more details
		putValue("noIconInMenu", Boolean.TRUE);
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

