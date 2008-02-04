package org.jcae.netbeans.mesh;

import java.io.File;
import java.io.IOException;
import org.jcae.mesh.amibe.util.UNVReader;
import org.jcae.mesh.xmldata.MeshExporter;
import org.jcae.mesh.xmldata.MeshWriter;
import org.openide.ErrorManager;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

public final class UNVToUNVAction extends CookieAction
{	
	protected void performAction(Node[] activatedNodes)
	{		
		FileLock newFileLock=null;
		try
		{
			UNVDataObject c = (UNVDataObject) activatedNodes[0].getCookie(
				UNVDataObject.class);
			FileObject oldUnv=c.getPrimaryFile();
			// Read the mesh
			org.jcae.mesh.amibe.ds.Mesh m=new org.jcae.mesh.amibe.ds.Mesh();
			UNVReader.readMesh(m, FileUtil.toFile(oldUnv).getPath());

			// Write the mesh as a jCAE mesh
			File tmpDir=File.createTempFile("jcae", null);
			tmpDir.delete();
			tmpDir.mkdirs();
			MeshWriter.writeObject3D(m, tmpDir.getPath(), null);			
			
			// Write the new UNV to a temporary place
			File newUNVFile=File.createTempFile("jcae", null);
			new MeshExporter.UNV(tmpDir.getPath()).write(newUNVFile.getPath());
			
			// Overwrite the old unv with the new one
			FileObject newUnv=FileUtil.toFileObject(newUNVFile);
			newFileLock=newUnv.lock();	
			oldUnv.delete();
			newUnv.move(newFileLock, oldUnv.getParent(), oldUnv.getName(), oldUnv.getExt());
						
			FileUtil.toFileObject(tmpDir).delete();
		}
		catch(IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
		finally
		{
			if(newFileLock!=null)
				newFileLock.releaseLock();
		}
	}
	
	protected int mode()
	{
		return CookieAction.MODE_EXACTLY_ONE;
	}
	
	public String getName()
	{
		return NbBundle.getMessage(UNVToUNVAction.class, "CTL_UNVToUNVAction");
	}
	
	protected Class[] cookieClasses()
	{
		return new Class[] {
			UNVDataObject.class
		};
	}
	
	protected void initialize()
	{
		super.initialize();
		// see org.openide.util.actions.SystemAction.iconResource() javadoc for more details
		putValue("noIconInMenu", Boolean.TRUE);
	}
	
	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}
	
	protected boolean asynchronous()
	{
		return false;
	}
	
}

