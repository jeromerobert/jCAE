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

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.jcae.mesh.xmldata.Mesh3dToSoupConvert;
import org.jcae.mesh.xmldata.MeshToSoupConvert;
import org.jcae.netbeans.ProcessExecutor;
import org.jcae.netbeans.Utilities;
import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.xml.sax.SAXException;

public final class BuildSoupAction extends CookieAction
{	
	protected void performAction(Node[] activatedNodes)
	{
		MeshDataObject c = (MeshDataObject) activatedNodes[0].getCookie(MeshDataObject.class);
		
		String reference = FileUtil.toFile(
			c.getPrimaryFile().getParent()).getPath();
		
		String xmlDir=Utilities.absoluteFileName(c.getMesh().getMeshFile(), reference);
		try
		{
			Mesh3dToSoupConvert.convert(xmlDir);
		}
		catch (IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
	}
	
	protected int mode()
	{
		return CookieAction.MODE_EXACTLY_ONE;
	}
	
	public String getName()
	{
		return NbBundle.getMessage(BuildSoupAction.class, "CTL_BuildSoupAction");
	}
	
	protected Class[] cookieClasses()
	{
		return new Class[] {
			MeshDataObject.class
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

