package org.jcae.netbeans.mesh.smooth;

import org.jcae.netbeans.ProcessExecutor;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.mesh.AmibeDataObject;
import org.jcae.netbeans.mesh.Settings;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

public final class Smooth extends CookieAction
{
	
	protected void performAction(Node[] activatedNodes)
	{
		SmoothParameters bean=new SmoothParameters();
		if(Utilities.showEditBeanDialog(bean))
		{
			AmibeDataObject c = activatedNodes[0].getCookie(AmibeDataObject.class);

			String reference = FileUtil.toFile(
				c.getPrimaryFile().getParent()).getPath();

			String xmlDir=Utilities.absoluteFileName(
				c.getMesh().getMeshFile(), reference);
			
			String className="org.jcae.mesh.amibe.algos3d.SmoothNodes3DBg";
			String[] cmdLinePre=Settings.getDefault().getCommandLineAlgo();
			String[] cmdLine=new String[cmdLinePre.length+7];

			System.arraycopy(cmdLinePre, 0, cmdLine, 0, cmdLinePre.length);
			int i=cmdLinePre.length;

			cmdLine[i++]=className;

			cmdLine[i++]="--iterations";
			cmdLine[i++]=Integer.toString(bean.getIterationNumber());

			cmdLine[i++]="--size";
			cmdLine[i++]=Double.toString(bean.getElementSize());

			cmdLine[i++]=xmlDir;
			// Override current directory
			cmdLine[i++]=xmlDir;
			
			// level_max tri_max outDir brep soupDir
			ProcessExecutor pe=new ProcessExecutor(cmdLine);
			pe.setName("Smooth");
			pe.start();
		}		
	}
	
	protected int mode()
	{
		return CookieAction.MODE_EXACTLY_ONE;
	}
	
	public String getName()
	{
		return NbBundle.getMessage(Smooth.class, "CTL_Smooth");
	}
	
	protected Class[] cookieClasses()
	{
		return new Class[] {
			AmibeDataObject.class
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

