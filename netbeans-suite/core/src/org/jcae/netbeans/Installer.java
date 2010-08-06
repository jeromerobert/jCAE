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
package org.jcae.netbeans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.prefs.Preferences;
import org.jcae.opencascade.jni.BRep_Builder;
import javax.swing.filechooser.FileSystemView;
import org.jcae.netbeans.cad.BCADSelection;
import org.jcae.netbeans.cad.CADSelection;
import org.jcae.netbeans.cad.NbShape;
import org.jcae.netbeans.mesh.bora.BGroupsNode;
import org.jcae.netbeans.mesh.bora.BoraSelection;
import org.jcae.netbeans.mesh.bora.BoraViewable;
import org.jcae.netbeans.mesh.bora.ViewBCellGeometryAction.NbBShape;
import org.jcae.netbeans.viewer3d.EntitySelection;
import org.jcae.netbeans.viewer3d.SelectionManager;
import org.jcae.vtk.Viewable;
import org.jcae.vtk.ViewableCAD;
import org.jcae.vtk.ViewableOEMM;
import org.openide.explorer.ExplorerManager;
import org.openide.modules.InstalledFileLocator;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall
{
	@Override
	public void restored()
	{
		//load opencascade libraries
		new BRep_Builder();
		//Load default log configuration
		String logp = System.getProperty("java.util.logging.config.file");
		if(logp == null || logp.isEmpty())
		{
			String logPath =
				InstalledFileLocator.getDefault().
				locate("etc/logging.properties", "org.jcae.netbeans", false).
				getAbsolutePath();
			System.setProperty("java.util.logging.config.file", logPath);
			// We are using VTK
			//System.setProperty("org.jcae.vtk.enable","true");
		}
		
		//Set default project directory
		File defaultDir = FileSystemView.getFileSystemView().getDefaultDirectory();
		if (defaultDir != null && defaultDir.exists() && defaultDir.isDirectory()) {
			System.setProperty("netbeans.projects.dir", defaultDir.getPath());
		}
		
		WindowManager.getDefault().addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				for(ExplorerManager em:Utilities.getExplorerManagers())
				{
					em.removePropertyChangeListener(NodeSelectionManager.getDefault());
					em.addPropertyChangeListener(NodeSelectionManager.getDefault());
				}				
			}
		});
		
		//hide the property panel description area by default
		Preferences p = NbPreferences.root().node("org/openide/explorer");
		if(p.get("showDescriptionArea", null) == null)
			p.putBoolean("showDescriptionArea", false);

		SelectionManager.getDefault().addSelectionFactory(
			new SelectionManager.SelectionFactory()
		{
			public EntitySelection create(Object entity) {
				return null;
			}

			public boolean canCreate(Viewable viewable, Object entity) {
				return viewable instanceof ViewableOEMM;
			}
		});
		SelectionManager.getDefault().addSelectionFactory(
			new SelectionManager.SelectionFactory()
		{
			public EntitySelection create(Object entity) {
				return new BoraSelection((BGroupsNode)entity);
			}

			public boolean canCreate(Viewable viewable, Object entity) {
				return viewable instanceof BoraViewable &&
					entity instanceof BGroupsNode;
			}
		});
		SelectionManager.getDefault().addSelectionFactory(
			new SelectionManager.SelectionFactory()
		{
			public EntitySelection create(Object entity) {
				if (entity instanceof NbBShape)
					return new BCADSelection((NbBShape)entity);
				else if(entity instanceof NbShape)
					return new CADSelection((NbShape)entity);
				else
					throw new IllegalArgumentException("The entity associated wit ha ViewableCAD has to be a NbShape");
			}

			public boolean canCreate(Viewable viewable, Object entity) {
				return viewable instanceof ViewableCAD;
			}
		});
	}
}
