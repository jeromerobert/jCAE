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

import org.jcae.netbeans.mesh.AmibeNViewable;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

public final class MeshViewMode extends MeshButton
{
@Override
	public void actionPerformed(AmibeNViewable viewable)
	{
		switch(viewable.getViewMode())
		{
			case FILLED:
				viewable.setViewMode(AmibeNViewable.ViewMode.WIRED);
				break;
			case WIRED:
				viewable.setViewMode(AmibeNViewable.ViewMode.FILLED);
				break;
			default:
				throw new IllegalArgumentException("Mesh view mode not supported !");
		}
		viewable.render();
	}
	
	protected void updateButton(AmibeNViewable viewer)
	{
		setBooleanState(viewer.getViewMode() == AmibeNViewable.ViewMode.WIRED);
	}

	public String getName()
	{
		return NbBundle.getMessage(MeshViewMode.class, "CTL_MeshWired");
	}

	@Override
	protected String iconResource()
	{
		return "org/jcae/netbeans/viewer3d/actions/wired.png";
	}

	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}
	/**
	 * By default the action is not enabled
	 */
	@Override
	protected void initialize()
	{
		setEnabled(false);
		
		super.initialize();
	}
	
}
