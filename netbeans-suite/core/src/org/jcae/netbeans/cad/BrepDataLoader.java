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

package org.jcae.netbeans.cad;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.FileEntry;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.loaders.MultiDataObject.Entry;

public class BrepDataLoader extends MultiFileLoader
{
	final private static Collection<String> EXTENSION=new HashSet<String>(
		Arrays.asList("brep", "step", "igs", "iges"));
	
	private final static String META_EXTENSION=".xml";
	
	public BrepDataLoader()
	{
		super("org.jcae.netbeans.cad.BrepDataObject");
		setDisplayName("CAD file");
	}

	protected MultiDataObject createMultiObject(FileObject primaryFile)
		throws java.io.IOException
	{
		return new BrepDataObject(primaryFile, this);
	}
	
	protected FileObject findPrimaryFile(FileObject arg0) {
		if(EXTENSION.contains(arg0.getExt()))
			return arg0;
		else if(arg0.getNameExt().endsWith(META_EXTENSION))
		{
			File f=FileUtil.toFile(arg0);
			if(f==null)
				return null;
			String n=f.getPath();			
			n=n.substring(0, n.length()-META_EXTENSION.length());
			return FileUtil.toFileObject(new File(n));
		}
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
