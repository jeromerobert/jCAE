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
 * (C) Copyright 2010 by EADS France
 */
package org.jcae.netbeans.mesh;

import org.jcae.mesh.xmldata.Groups;
import org.jcae.netbeans.viewer3d.EntitySelection;
import org.jcae.netbeans.viewer3d.SelectionManager;
import org.jcae.vtk.Viewable;
import org.openide.modules.ModuleInstall;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

	@Override
	public void restored() {
		SelectionManager.getDefault().addSelectionFactory(
			new SelectionManager.SelectionFactory()
		{
			public EntitySelection create(Object entity) {
				return new AmibeSelection((Groups)entity);
			}

			public boolean canCreate(Viewable viewable, Object entity) {
				return viewable instanceof AmibeNViewable && entity instanceof Groups;
			}
		});
	}
}
