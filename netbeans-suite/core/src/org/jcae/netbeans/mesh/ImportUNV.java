package org.jcae.netbeans.mesh;

import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import org.jcae.mesh.xmldata.UNV2Amibe;
import org.jcae.netbeans.Utilities;
import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

public final class ImportUNV extends CookieAction
{
	
	protected void performAction(Node[] activatedNodes)
	{
		JFileChooser chooser=new JFileChooser();
		if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
		{
			OldAmibeMeshNode meshNode = activatedNodes[0].getCookie(OldAmibeMeshNode.class);
			OldAmibeMeshDataObject c = activatedNodes[0].getCookie(OldAmibeMeshDataObject.class);
			
			String reference = FileUtil.toFile(
				c.getPrimaryFile().getParent()).getPath();

			String xmlDir=Utilities.absoluteFileName(c.getMesh().getMeshFile(), reference);
			
			try
			{	
				File selectedFile = chooser.getSelectedFile();
				String prefix = Utilities.removeExt(selectedFile.getName());				
				String strp = Utilities.getFreeName(
					c.getPrimaryFile().getParent(), prefix, "-strp.unv");
				UNV2Amibe u = new UNV2Amibe();
				u.setStripedUnv(new File(reference, strp).getPath());
				u.importMesh(selectedFile, xmlDir);
				meshNode.refreshGroups();				
			}
			catch (IOException ex)
			{
				ErrorManager.getDefault().notify(ex);
			}
		}
	}
	
	protected int mode()
	{
		return CookieAction.MODE_EXACTLY_ONE;
	}
	
	public String getName()
	{
		return NbBundle.getMessage(ImportUNV.class, "CTL_ImportUNV");
	}
	
	protected Class[] cookieClasses()
	{
		return new Class[] {
			MeshDataObject.class
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

