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
 * (C) Copyright 2005-2010, by EADS France
 */

package org.jcae.netbeans.mesh;

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.IOException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import org.jcae.mesh.xmldata.Group;
import org.jcae.mesh.xmldata.Groups;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.options.OptionNode;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.AmibeToMesh;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.openide.ErrorManager;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.xml.sax.SAXException;

/**
 *
 * @author Gautam Botrel
 */
public class AmibeDataObject extends MultiDataObject implements SaveCookie
{
	private int threshold = 400000;
	private boolean listenFlag;

	private void refresh()
	{
		final View view = ViewManager.getDefault().getCurrentView();
		for (Viewable v : view.getViewables()) {
			if (v instanceof AmibeNViewable) {
				AmibeNViewable av = (AmibeNViewable) v;
				try {
					if(av.getDataObject().equals(AmibeDataObject.this))
					{
						AmibeToMesh reader = new AmibeToMesh(
							getGroups().getMeshFile(),
							groupsToID(getGroups().getGroups()));

						if(reader.getNumberOfTriangles() > threshold )
							continue;

						av.addTriangles(reader.getTriangles());
						av.addBeams(reader.getBeams());
						view.Render();
					}
				} catch (SAXException ex) {
					Exceptions.printStackTrace(ex);
				} catch (IOException ex) {
					Exceptions.printStackTrace(ex);
				}
			}
		}
	}

	private final FileChangeListener fileListener = new FileChangeListener() {

		public void fileFolderCreated(FileEvent fe) {}

		public void fileDataCreated(FileEvent fe) {}

		public void fileChanged(FileEvent fe) {
			EventQueue.invokeLater(new Runnable(){
				public void run() {
					refresh();
				}
			});
		}

		public void fileDeleted(FileEvent fe) {}

		public void fileRenamed(FileRenameEvent fe) {}

		public void fileAttributeChanged(FileAttributeEvent fe) {}
	};

	public AmibeDataObject(FileObject arg0, MultiFileLoader arg1, Mesh mesh)
		throws DataObjectExistsException
	{
		super(arg0, arg1);
		this.mesh = mesh;
		mesh.addPropertyChangeListener(new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent evt) {
				setModified(true);
			}
		});

		Preferences pref = OptionNode.REFRESH_THRESHOLD.getPreferences();
		pref.addPreferenceChangeListener(new PreferenceChangeListener() {
			public void preferenceChange(PreferenceChangeEvent evt) {
				if (evt.getKey().equals(OptionNode.REFRESH_THRESHOLD.getPreferenceName())) {
					threshold = Integer.parseInt(evt.getNewValue());
				}
			}
		});
	}

	public String getDisplayName() {
		FileObject xmlMesh = getXMLMesh(false);
		if(xmlMesh == null)
			return getPrimaryFile().getName();
		else
		{
			String s = xmlMesh.getName();
			return s.substring(0, s.length()-"_mesh".length());
		}
	}

	/** Return the secondary entry which ends by _mesh.xml */
	private FileObject getXMLMesh(boolean create)
	{
		FileObject toReturn = null;
		for(Entry e:secondaryEntries())
		{
			String s = e.getFile().getName();
			if(s.endsWith("_mesh"))
				toReturn = e.getFile();
		}
		
		if(create && toReturn==null)
		{
			String name = getPrimaryFile().getName();
			try {
				FileObject parent = getPrimaryFile().getParent();
				FileLock l = parent.lock();
				toReturn = parent.createData(name + "_mesh.xml");
				l.releaseLock();
			} catch (IOException ex) {
				Exceptions.printStackTrace(ex);
			}
		}
		return toReturn;
	}
	
	@Override
	protected Node createNodeDelegate()
	{
		AmibeNode r = new AmibeNode(this);
		if(!isTemplate())
		{
			r.updateGeomNode();
			refreshGroups(r);
		}
		return r;
	}

	private Mesh mesh;
	private Groups groups;
	public Mesh getMesh()
	{
		return mesh;
	}
	
	public String getMeshDirectory()
	{
		String ref=FileUtil.toFile(getPrimaryFile().getParent()).getPath();
		return Utilities.absoluteFileName(getMesh().getMeshFile(), ref);
	}
	
	public void save() throws IOException
	{
		FileLock l = null;
		XMLEncoder encoder = null;
		try
		{
			FileObject out = getXMLMesh(true);
			l = out.lock();
			encoder = new XMLEncoder(out.getOutputStream(l));
			encoder.writeObject(mesh);
			setModified(false);
		}
		catch(IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
		finally
		{
			if(encoder!=null)
				encoder.close();
			if(l!=null)
				l.releaseLock();
		}
	}

	@Override
	protected FileObject handleRename(String name) throws IOException {
		FileObject toReturn = super.handleRename(name);
		mesh.setMeshFile(toReturn.getNameExt());
		refreshGroups();
		return toReturn;
	}

	public void refreshGroups()
	{
		refreshGroups((AmibeNode) getNodeDelegate());
		refreshHeader();
	}
	
	/**
	 * private implementation to be called by createNodeDelegate.
	 * Avoid a stack overflow when calling getNodeDelegate.
	 */
	private void refreshGroups(AmibeNode amibeNode)
	{		
		String meshDir=getMeshDirectory();
		File xmlFile=new File(meshDir, "jcae3d");
        if (xmlFile.exists())
            groups = Groups.getGroups(meshDir);
		amibeNode.setGroups(groups);
	}

	public Groups getGroups() {
		return groups;
	}

	public void refreshHeader() {
		FileObject[] amibeChildren = getPrimaryFile().getChildren();
		for (int i = 0; i < amibeChildren.length; i++) {
			if (amibeChildren[i].getName().equalsIgnoreCase("jcae3d")) {
				amibeChildren[i].refresh();
				break;
			}
		}
	}

	public void addListener()
	{
		if(!listenFlag)
		{
			FileObject f = getPrimaryFile().getFileObject("jcae3d");
			if(f != null)
			{
				f.addFileChangeListener(fileListener);
				listenFlag = true;
			}
		}
	}

	public void removeListener()
	{
		FileObject f = getPrimaryFile().getFileObject("jcae3d");
		if(f != null)
			f.removeFileChangeListener(fileListener);
		listenFlag = false;
	}

	private String[] groupsToID(Group[] groupsToDisplay)
	{
		String[] idGroupsDisplayed = new String[groupsToDisplay.length];
		for (int i = 0; i < groupsToDisplay.length; i++)
		{
			idGroupsDisplayed[i] = groupsToDisplay[i].getName();
		}

		return idGroupsDisplayed;
	}
}
