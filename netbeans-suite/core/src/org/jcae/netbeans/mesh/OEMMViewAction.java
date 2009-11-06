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
 * (C) Copyright 2008, by EADS France
 */

package org.jcae.netbeans.mesh;

import org.jcae.mesh.oemm.Storage;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.viewer3d.SelectionManager;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.SelectionListener;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.jcae.vtk.ViewableOEMM;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

public final class OEMMViewAction extends CookieAction implements SelectionListener
{
	ViewableOEMM viewable = null;

	public void selectionChanged(Viewable viewable)
	{
		viewable.highlight();
	}
	
	protected void performAction(Node[] activatedNodes)
	{
		BoraDataObject c = activatedNodes[0].getCookie(BoraDataObject.class);
		
		String reference = FileUtil.toFile(
			c.getPrimaryFile().getParent()).getPath();
		String oemmDir=Utilities.absoluteFileName(
			activatedNodes[0].getName()+".oemm", reference);
		view(c,oemmDir, activatedNodes[0].getName()+" OEMM");
	}

	public void view(BoraDataObject c, String dir, String viewableName)
	{
		OEMM oemm = Storage.readOEMMStructure(dir);
		
		View v = ViewManager.getDefault().getCurrentView();

		if (viewable == null)
		{
			viewable = new ViewableOEMM(oemm);
			viewable.setName(viewableName);
			viewable.addSelectionListener(this);
			SelectionManager.getDefault().addInteractor(viewable, c);

			// Create a OEMMSelection ?
			/*if (SelectionManager.getDefault().getEntitySelection(c) == null)
			{
				OEMMSelection oemmSelection = new OEMMSelection(c);
				SelectionManager.getDefault().addEntitySelection(
						c, oemmSelection);
			}*/
		}
		v.add(viewable);	
	}	
	
	protected int mode()
	{
		return CookieAction.MODE_EXACTLY_ONE;
	}
	
	public String getName()
	{
		return NbBundle.getMessage(OEMMViewAction.class, "CTL_OEMMViewAction");
	}
	
	protected Class[] cookieClasses()
	{
		return new Class[] {
			AmibeDataObject.class
		};
	}
	
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

