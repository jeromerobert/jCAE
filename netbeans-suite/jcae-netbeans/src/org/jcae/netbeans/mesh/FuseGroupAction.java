package org.jcae.netbeans.mesh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.xml.sax.SAXException;

public class FuseGroupAction extends CookieAction{

	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected Class[] cookieClasses()
	{
		return new Class[]{GroupNode.class};
	}

	protected void performAction(Node[] arg0)
	{
		try
		{
			//A set to ensure all nodes are from the same mesh
			HashSet set=new HashSet();
			ArrayList list=new ArrayList();
			for(int i=0; i<arg0.length; i++)
			{
				GroupNode n=(GroupNode) arg0[i]
					.getCookie(GroupNode.class);
				set.add(n.getGroups());
				list.add(n.getGroup());
			}
					
			if(set.size()>1)
			{
				JOptionPane.showMessageDialog(null,
					"Fuse can only work with groups from the same mesh");
				return;
			}
			
			Groups groups=(Groups) set.toArray()[0];
			groups.fuse(list);
			MeshNode mn=(MeshNode) arg0[0].getParentNode().getParentNode().getCookie(MeshNode.class);
			mn.refreshGroups();			
		}
		catch(IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		} catch (TransformerConfigurationException e) {
			ErrorManager.getDefault().notify(e);
		} catch (TransformerException e) {
			ErrorManager.getDefault().notify(e);
		} catch (ParserConfigurationException e) {
			ErrorManager.getDefault().notify(e);
		} catch (SAXException e) {
			ErrorManager.getDefault().notify(e);
		}
	}

	public String getName()
	{
		return "Fuse";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}

}
