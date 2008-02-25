package org.jcae.netbeans.mesh;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.jcae.mesh.java3d.ComputeEdgesConnectivity;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.viewer3d.View3DManager;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.fe.ViewableFE;
import org.jcae.viewer3d.fe.amibe.AmibeOverlayProvider;
import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.xml.sax.SAXException;

public abstract class AbstractEdgesAction extends CookieAction
{
	public abstract String getBranchGroupLabel();
	public abstract String getActionLabel();
	public abstract String getViewSuffix();
	
	protected void performAction(Node[] activatedNodes)
	{
		try
		{
			MeshDataObject c = (MeshDataObject) activatedNodes[0].getCookie(MeshDataObject.class);
			
			String reference = FileUtil.toFile(
				c.getPrimaryFile().getParent()).getPath();
			String xmlDir=Utilities.absoluteFileName(
				c.getMesh().getMeshFile(), reference);

			String xmlFile = "jcae3d";
			ComputeEdgesConnectivity computeEdgesConnectivity =
				new ComputeEdgesConnectivity(xmlDir, xmlFile);

			computeEdgesConnectivity.compute();			
			View bgView=View3DManager.getDefault().getView3D().getView();			
			ViewableFE fe1 = new ViewableFE(
				new AmibeOverlayProvider(new File(xmlDir), getBranchGroupLabel()));
			fe1.setName(activatedNodes[0].getName()+" "+getViewSuffix());
			bgView.add(fe1);			
			bgView.setCurrentViewable(fe1);
			
		}
		catch (XPathExpressionException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
		catch (IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
		catch (ParserConfigurationException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
		catch (SAXException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}						
	}
	
	protected int mode()
	{
		return CookieAction.MODE_EXACTLY_ONE;
	}
	
	public String getName()
	{
		return NbBundle.getMessage(getClass(), getActionLabel());
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

