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
 * (C) Copyright 2008, by EADS France
 */
package org.jcae.netbeans.viewer3d.actions;

import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.Storage;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.mesh.AmibeDataObject;
import org.jcae.netbeans.mesh.bora.BoraDataObject;
import org.jcae.vtk.MeshVisuBuilder;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

public final class BuildOEMMVisu extends CookieAction
{

	protected void performAction(Node[] activatedNodes)
	{
		AmibeDataObject c = activatedNodes[0].getCookie(AmibeDataObject.class);
		String reference = FileUtil.toFile(
				c.getPrimaryFile().getParent()).getPath();
		
		OEMM oemm =  Storage.readOEMMStructure(Utilities.absoluteFileName(activatedNodes[0].getName()+".oemm", reference));
		MeshVisuBuilder builder = new MeshVisuBuilder();
		oemm.walk(builder);
		
		/*OEMMParameters bean=new OEMMParameters();
		if(Utilities.showEditBeanDialog(bean))
		{
			MeshDataObject c = activatedNodes[0].getCookie(MeshDataObject.class);

			String reference = FileUtil.toFile(
				c.getPrimaryFile().getParent()).getPath();

			String xmlDir=Utilities.absoluteFileName(
				c.getMesh().getMeshFile(), reference);
			
			String className="org.jcae.mesh.MeshOEMMIndex";
			String[] cmdLinePre=Settings.getDefault().getCommandLineAlgo();
			String[] cmdLine=new String[cmdLinePre.length+5];

			System.arraycopy(cmdLinePre, 0, cmdLine, 0, cmdLinePre.length);
			int i=cmdLinePre.length;

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
		}*/
	}

	protected int mode()
	{
		return CookieAction.MODE_EXACTLY_ONE;
	}

	public String getName()
	{
		return NbBundle.getMessage(BuildOEMMVisu.class, "CTL_BuildOEMMVisu");
	}

	protected Class[] cookieClasses()
	{
		return new Class[] {
			AmibeDataObject.class
		};
	}

	@Override
	protected void initialize()
	{
		super.initialize();
		// see org.openide.util.actions.SystemAction.iconResource() Javadoc for more details
		putValue("noIconInMenu", Boolean.TRUE);
	}

	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}

	@Override
	protected boolean asynchronous()
	{
		return true;
	}
}

