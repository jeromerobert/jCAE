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

public class MeshDataLoader extends UniFileLoader
{
	
	public static final String REQUIRED_MIME = "text/meshBora+xml";
	
	private static final long serialVersionUID = 1L;
	
	public MeshDataLoader()
	{
		super("org.jcae.netbeans.mesh.MeshDataObject");
	}
	
	protected String defaultDisplayName()
	{
		return NbBundle.getMessage(MeshDataLoader.class, "LBL_Mesh_loader_name");
	}
	
	protected void initialize()
	{
		super.initialize();
		getExtensions().addMimeType(REQUIRED_MIME);
	}
	
	protected MultiDataObject createMultiObject(FileObject primaryFile) throws DataObjectExistsException, IOException
	{
		return new MeshDataObject(primaryFile, this);
	}
	
	protected String actionsContext()
	{
		return "Loaders/" + REQUIRED_MIME + "/Actions";
	}
	
}
