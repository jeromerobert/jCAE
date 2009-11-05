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

import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.UniFileLoader;
import org.openide.util.NbBundle;

public class BoraDataLoader extends UniFileLoader
{
	public BoraDataLoader()
	{
		super("org.jcae.netbeans.mesh.BoraDataObject");
	}
	
	@Override
	protected String defaultDisplayName()
	{
		return NbBundle.getMessage(BoraDataLoader.class, "LBL_Mesh_loader_name");
	}

	@Override
	protected FileObject findPrimaryFile(FileObject fo) {
		if (fo.getNameExt().endsWith(".bora"))
			return fo;
		return null;
	}



	protected MultiDataObject createMultiObject(FileObject primaryFile) throws DataObjectExistsException, IOException
	{
		return new BoraDataObject(primaryFile, this);
	}
}
