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
 * (C) Copyright 2005-2009, by EADS France
 */

package org.jcae.netbeans.mesh;

import java.beans.XMLDecoder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.jcae.netbeans.Utilities;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.FileEntry;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiDataObject.Entry;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.NbBundle;

public class OldAmibeMeshDataLoader extends MultiFileLoader
{
	public static final String REQUIRED_MIME = "text/mesh+xml";
	private static final long serialVersionUID = 1L;
	private static Map<FileObject, Mesh> primaryToMesh = new HashMap<FileObject, Mesh>();
	private static Map<FileObject, FileObject> secToPrimary = new HashMap<FileObject, FileObject>();

	public OldAmibeMeshDataLoader()
	{
		super("org.jcae.netbeans.mesh.OldAmibeMeshDataObject");
	}

	@Override
	protected String defaultDisplayName()
	{
		return NbBundle.getMessage(OldAmibeMeshDataLoader.class, "LBL_Mesh_loader_name");
	}

	@Override
	protected FileObject findPrimaryFile(FileObject arg0) {		
		if(REQUIRED_MIME.equals(arg0.getMIMEType()))
		{
			Mesh m = primaryToMesh.get(arg0);
			if(m == null)
				addMapEntry(arg0);
			return arg0;
		}
		else if(isjCAEDirectory(arg0))
		{
			FileObject p = secToPrimary.get(arg0);
			if(p == null)
				return fillMaps(arg0);
			return p;
		}
		else if("oemm".equals(arg0.getExt()) && arg0.isFolder())
		{
			return arg0.getParent().getFileObject(arg0.getName()+"_mesh.xml");
		}
		return null;
	}

	@Override
	protected MultiDataObject createMultiObject(FileObject primaryFile)
		throws DataObjectExistsException
	{
		return new OldAmibeMeshDataObject(primaryFile, this, primaryToMesh.get(primaryFile));
	}

	@Override
	protected String actionsContext()
	{
		return "Loaders/" + REQUIRED_MIME + "/Actions";
	}

	@Override
	protected Entry createSecondaryEntry(MultiDataObject obj, FileObject secondaryFile) {		
		primaryToMesh.remove(secToPrimary.remove(secondaryFile));
		return new FileEntry(obj, secondaryFile);
	}

	@Override
	protected Entry createPrimaryEntry(MultiDataObject obj, FileObject primaryFile) {
		return new FileEntry(obj, primaryFile);
	}

	private boolean isjCAEDirectory(FileObject arg0) {
		if(arg0.isFolder())
			return arg0.getFileObject("jcae3d") != null;
		else
			return false;
	}

	private FileObject fillMaps(FileObject arg0) {
		for(FileObject f:arg0.getParent().getChildren())
			if(REQUIRED_MIME.equals(f.getMIMEType()))
				if(arg0.equals(addMapEntry(f)))
					return f;
		return null;
	}
	
	/**
	 *
	 * @param arg0 primary file (.xml)
	 * @return secondary file (amibe.dir)
	 */
	private FileObject addMapEntry(FileObject arg0)
	{
		Mesh m = createMesh(arg0);
		primaryToMesh.put(arg0, m);
		String reference = FileUtil.toFile(arg0.getParent()).getPath();
		FileObject sec = FileUtil.toFileObject(
			new File(Utilities.absoluteFileName(m.getMeshFile(), reference)));
		secToPrimary.put(sec, arg0);
		return sec;
	}


	private static Mesh createMesh(FileObject file)
	{
		InputStream in=null;
		Mesh toReturn;
		try
		{
			in=file.getInputStream();
			XMLDecoder decoder=new XMLDecoder(in);
			toReturn=(Mesh)decoder.readObject();
		}
		catch (Exception ex)
		{
			ErrorManager.getDefault().log(ex.getMessage());
			String name=Utilities.getFreeName(file.getParent(), "amibe",".dir");
			toReturn = new Mesh(name);
		}
		finally
		{
			if(in!=null)
				try
				{
					in.close();
				}
				catch (IOException ex)
				{
					ErrorManager.getDefault().notify(ex);
				}
		}
		return toReturn;
	}
}
