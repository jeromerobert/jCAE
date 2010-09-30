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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import org.jcae.mesh.xmldata.Groups;
import org.jcae.mesh.xmldata.GroupsReader;
import org.jcae.netbeans.Utilities;
import org.openide.ErrorManager;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Node;

/**
 *
 * @author Gautam Botrel
 */
public class AmibeDataObject extends MultiDataObject implements SaveCookie
{
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
	}

	@Override
	public String getName() {
		Set<Entry> se = secondaryEntries();
		if(se.isEmpty())
			return getPrimaryFile().getName();
		else
		{
			String s = null;
			for(Entry e:se)
			{
				s = e.getFile().getName();
				if(s.endsWith("_mesh"))
					break;
			}
			System.out.println("dn: "+s);
			return s.substring(0, s.length()-"_mesh".length());
		}
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
			FileObject out = getPrimaryFile();
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

	public void refreshGroups()
	{
		refreshGroups((AmibeNode) getNodeDelegate());
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
            groups = GroupsReader.getGroups(meshDir);
		amibeNode.setGroups(groups);
	}

	public Groups getGroups() {
		return groups;
	}
}
