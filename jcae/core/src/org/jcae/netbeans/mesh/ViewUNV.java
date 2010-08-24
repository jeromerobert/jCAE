package org.jcae.netbeans.mesh;

import java.io.File;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.View;
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
				UNVDataObject c = activatedNodes[i].getCookie(UNVDataObject.class);
				File file = FileUtil.toFile(c.getPrimaryFile());
				View v=ViewManager.getDefault().getCurrentView();
				

				throw new RuntimeException("J3D possibilty not VTK implemented");
			/*		InteractorMesh interactor = new InteractorMesh(file.getAbsolutePath());
					v.getCanvas().GetRenderer().ResetCamera();
					System.out.println("TEST : " + file.getAbsolutePath());*/
				/*} else
				{
					UNVProvider unvp=new UNVProvider(file);
					ViewableFE vfe=new ViewableFE(unvp);
					vfe.setName(file.getName());
					v.add(vfe);
				}*/
				
				
			
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

