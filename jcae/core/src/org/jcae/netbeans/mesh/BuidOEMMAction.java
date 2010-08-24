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

import java.beans.*;
import org.jcae.netbeans.ProcessExecutor;
import org.jcae.netbeans.Utilities;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

public final class BuidOEMMAction extends CookieAction
{
	protected void performAction(Node[] activatedNodes)
	{
		OEMMParameters bean=new OEMMParameters();
		if(Utilities.showEditBeanDialog(bean))
		{
			AmibeDataObject c = activatedNodes[0].getCookie(AmibeDataObject.class);

			String reference = FileUtil.toFile(
				c.getPrimaryFile().getParent()).getPath();

			String xmlDir=Utilities.absoluteFileName(
				c.getMesh().getMeshFile(), reference);

			String classpath = InstalledFileLocator.getDefault().
				locate("modules/ext/amibe.jar", "org.jcae.netbeans.mesh", false).
				getAbsolutePath();

			String className="org.jcae.mesh.MeshOEMMIndex";
			String[] cmdLinePre=Settings.getDefault().getCommandLineAlgo();
			String[] cmdLine=new String[cmdLinePre.length+7];

			System.arraycopy(cmdLinePre, 0, cmdLine, 0, cmdLinePre.length);
			int i=cmdLinePre.length;

			cmdLine[i++]="-classpath";
			cmdLine[i++]=classpath;
			cmdLine[i++]=className;
			cmdLine[i++]=xmlDir;
			cmdLine[i++]=Utilities.absoluteFileName(
				activatedNodes[0].getName()+".oemm", reference);
			cmdLine[i++]=Integer.toString(bean.getLevel());
			cmdLine[i++]=Integer.toString(bean.getTriangle());

			// level_max tri_max outDir brep soupDir
			ProcessExecutor pe=new ProcessExecutor(cmdLine);
			pe.setName("Build OEMM Index");
			pe.start();
		}
	}
	
	protected int mode()
	{
		return CookieAction.MODE_EXACTLY_ONE;
	}
	
	public String getName()
	{
		return NbBundle.getMessage(BuidOEMMAction.class, "CTL_BuidOEMMAction");
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

