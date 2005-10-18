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
	final private static Collection EXTENSION=new HashSet(Arrays.asList(new String[]{
		"brep", "step", "igs", "iges"  
	}));
	
	private final static String META_EXTENSION=".xml";
	
	public BrepDataLoader()
	{
		super("org.jcae.netbeans.cad.BrepDataLoader");
		setDisplayName("CAD file");
	}

	protected MultiDataObject createMultiObject(FileObject primaryFile)
		throws DataObjectExistsException, java.io.IOException
	{
		return new BrepDataObject(primaryFile, this);
	}

	public static String getMetaFile(FileObject geomFile)
	{
		String n=geomFile.getNameExt();
		return n+META_EXTENSION;		
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
		System.out.println("createSecondaryEntry "+arg1);
		return new FileEntry(arg0, arg1);
	}	
}
