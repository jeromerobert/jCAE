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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jcae.netbeans.viewer3d.actions;

import org.jcae.vtk.ViewableCAD;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

public final class CAOFaceSelection extends CAOShapeSelection
{
	
	@Override
	public void actionPerformed(ViewableCAD interactor)
	{
		interactor.setShapeTypeSelection(ViewableCAD.ShapeType.FACE);
	}
	
	public String getName()
	{
		return NbBundle.getMessage(CAOFaceSelection.class, "CTL_CAOFaceSelection");
	}

	@Override
	protected String iconResource()
	{
		return "org/jcae/netbeans/viewer3d/actions/selectface.png";
	}

	@Override
	protected void updateButton(ViewableCAD viewer)
	{
		setBooleanState(viewer.getShapeTypeSelection() == ViewableCAD.ShapeType.FACE);
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
