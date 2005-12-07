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
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.netbeans.mesh;

import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.FileEntry;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.loaders.MultiDataObject.Entry;

public class MeshDataLoader extends MultiFileLoader
{	
	private final static String EXTENSION="_mesh.xml";
	
	public MeshDataLoader()
	{
		super("org.jcae.netbeans.mesh.MeshDataLoader");
		setDisplayName("Mesh file");
	}

	protected MultiDataObject createMultiObject(FileObject primaryFile)
		throws DataObjectExistsException, java.io.IOException
	{
		return new MeshDataObject(primaryFile, this);
	}


	protected FileObject findPrimaryFile(FileObject arg0) {
		if(arg0.getNameExt().endsWith(EXTENSION))
			return arg0;
		else return null;
	}

	protected Entry createPrimaryEntry(MultiDataObject arg0, FileObject arg1)
	{
		return new FileEntry(arg0, arg1);
	}

	protected Entry createSecondaryEntry(MultiDataObject arg0, FileObject arg1)
	{
		return new FileEntry(arg0, arg1);
	}	
}
