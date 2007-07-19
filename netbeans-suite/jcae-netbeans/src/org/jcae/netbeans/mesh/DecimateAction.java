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

import java.io.File;
import org.jcae.netbeans.ProcessExecutor;
import org.jcae.netbeans.Utilities;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

public final class DecimateAction extends CookieAction
{
	
	protected void performAction(Node[] activatedNodes)
	{
		DecimateParameter bean=new DecimateParameter();
		if(Utilities.showEditBeanDialog(bean))
		{
			MeshDataObject c = (MeshDataObject)
				activatedNodes[0].getCookie(MeshDataObject.class);

			String reference = FileUtil.toFile(
				c.getPrimaryFile().getParent()).getPath();

			String xmlDir=Utilities.absoluteFileName(
				c.getMesh().getMeshFile(), reference);
			
			File brepFile=new File(Utilities.absoluteFileName(
				c.getMesh().getGeometryFile(), reference));
			
			String className="org.jcae.mesh.amibe.algos3d.DecimateHalfEdge";
			String[] cmdLinePre=Settings.getDefault().getCommandLineAlgo();
			String[] cmdLine=new String[cmdLinePre.length+7];

			System.arraycopy(cmdLinePre, 0, cmdLine, 0, cmdLinePre.length);
			int i=cmdLinePre.length;

			cmdLine[i++]=className;
			cmdLine[i++]=xmlDir;
			cmdLine[i++]="jcae3d";
			
			if(bean.isUseTolerance())
			{
				cmdLine[i++]="-t";
				cmdLine[i++]=Double.toString(bean.getTolerance());
			}
			else
			{
				cmdLine[i++]="-n";
				cmdLine[i++]=Integer.toString(bean.getTriangle());
			}
			
			cmdLine[i++]=brepFile.getParent();
			cmdLine[i++]=brepFile.getName();
				
			// level_max tri_max outDir brep soupDir
			ProcessExecutor pe=new ProcessExecutor(cmdLine);
			pe.setName("Decimate");
			pe.start();
		}

	}
	
	protected int mode()
	{
		return CookieAction.MODE_EXACTLY_ONE;
	}
	
	public String getName()
	{
		return NbBundle.getMessage(DecimateAction.class, "CTL_DecimateAction");
	}
	
	protected Class[] cookieClasses()
	{
		return new Class[] {
			MeshDataObject.class
		};
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

