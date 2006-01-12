package org.jcae.netbeans.mesh;

import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.UniFileLoader;
import org.openide.util.NbBundle;

public class UNVDataLoader extends UniFileLoader
{
	
	public static final String REQUIRED_MIME = "text/x-unv";
	
	private static final long serialVersionUID = 1L;
	
	public UNVDataLoader()
	{
		super("org.jcae.netbeans.mesh.UNVDataObject");
	}
	
	protected String defaultDisplayName()
	{
		return NbBundle.getMessage(UNVDataLoader.class, "LBL_UNV_loader_name");
	}
	
	protected void initialize()
	{
		super.initialize();
		getExtensions().addMimeType(REQUIRED_MIME);
	}
	
	protected MultiDataObject createMultiObject(FileObject primaryFile) throws DataObjectExistsException, IOException
	{
		return new UNVDataObject(primaryFile, this);
	}
	
	protected String actionsContext()
	{
		return "Loaders/" + REQUIRED_MIME + "/Actions";
	}
	
}
