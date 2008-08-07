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

import org.jcae.vtk.ViewableOEMM;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

public final class OctreeVisible extends OEMMButton
{
	@Override
	public void actionPerformed(ViewableOEMM viewable)
	{
		viewable.setOctreeVisible(!viewable.isOctreeVisible());
		viewable.render();
	}
	
	protected void updateButton(ViewableOEMM viewer)
	{
		setBooleanState(viewer.isOctreeVisible());
	}
	

	public String getName()
	{
		return NbBundle.getMessage(OctreeVisible.class, "CTL_OctreeVisible");
	}

	@Override
	protected String iconResource()
	{
		return "org/jcae/netbeans/viewer3d/actions/octreeVisible.png";
	}

	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}

}
