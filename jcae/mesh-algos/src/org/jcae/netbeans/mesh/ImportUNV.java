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
 * (C) Copyright 2012, by EADS France
 */

package org.jcae.netbeans.mesh;

import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import org.jcae.mesh.xmldata.UNV2Amibe;
import org.jcae.netbeans.Utilities;
import org.openide.ErrorManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CookieAction;

@ActionID(id = "org.jcae.netbeans.mesh.ImportUNV", category = "jCAE")
@ActionRegistration(displayName = "#CTL_ImportUNV")
@ActionReference(path = "Loaders/text/mesh+xml/Actions", position = 120)
@Messages("CTL_ImportUNV=Import UNV")
public final class ImportUNV extends CookieAction
{

	protected void performAction(Node[] activatedNodes)
	{
		JFileChooser chooser=new JFileChooser();
		if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
		{
			AmibeDataObject c = activatedNodes[0].getCookie(AmibeDataObject.class);

			String reference = FileUtil.toFile(
				c.getPrimaryFile().getParent()).getPath();

			String xmlDir=Utilities.absoluteFileName(c.getMesh().getMeshFile(), reference);

			try
			{
				File selectedFile = chooser.getSelectedFile();
				String prefix = Utilities.removeExt(selectedFile.getName());
				String strp = Utilities.getFreeName(
					c.getPrimaryFile().getParent(), prefix, "-strp.unv");
				UNV2Amibe u = new UNV2Amibe();
				u.setStripedUnv(new File(reference, strp).getPath());
				u.importMesh(selectedFile, xmlDir);
				c.refreshGroups();
			}
			catch (IOException ex)
			{
				ErrorManager.getDefault().notify(ex);
			}
		}
	}

	@Override
	protected int mode()
	{
		return CookieAction.MODE_EXACTLY_ONE;
	}

	@Override
	public String getName()
	{
		return NbBundle.getMessage(ImportUNV.class, "CTL_ImportUNV");
	}

	@Override
	protected Class[] cookieClasses()
	{
		return new Class<?>[] { AmibeDataObject.class };
	}

	@Override
	protected void initialize()
	{
		super.initialize();
		// see org.openide.util.actions.SystemAction.iconResource() javadoc for more details
		putValue("noIconInMenu", Boolean.TRUE);
	}

	@Override
	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}

	@Override
	protected boolean asynchronous()
	{
		return false;
	}
}

