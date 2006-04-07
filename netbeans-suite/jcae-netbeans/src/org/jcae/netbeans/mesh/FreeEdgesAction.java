package org.jcae.netbeans.mesh;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.media.j3d.BranchGroup;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.jcae.mesh.java3d.ComputeEdgesConnectivity;
import org.jcae.mesh.java3d.XMLBranchGroup;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.viewer3d.View3DManager;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.bg.ViewableBG;
import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.xml.sax.SAXException;

public final class FreeEdgesAction extends AbstractEdgesAction
{
	public String getBranchGroupLabel()
	{
		return "FreeEdges";
	}

	public String getActionLabel()
	{
		return "CTL_FreeEdgesAction";
	}	
}

