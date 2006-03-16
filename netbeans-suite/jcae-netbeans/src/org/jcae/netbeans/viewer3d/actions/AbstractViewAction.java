package org.jcae.netbeans.viewer3d.actions;

import javax.swing.Action;
import org.jcae.netbeans.viewer3d.View3D;
import org.jcae.netbeans.viewer3d.View3DManager;
import org.jcae.viewer3d.View;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;

public abstract class AbstractViewAction extends CallableSystemAction
{	
	public void performAction()
	{
		View3D v = View3DManager.getDefault().getSelectedView3D();
		if(v!=null)
		{
			actionPerformed(v.getView());
		}
	}

	public String getName()
	{
		return getValue(Action.NAME).toString();
	}

	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}
	
	public abstract void actionPerformed(View view);
}