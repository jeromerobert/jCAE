package org.jcae.netbeans.cad;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import javax.swing.JOptionPane;
import org.openide.ErrorManager;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

public class GroupFaceAction extends CookieAction
{
	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected Class[] cookieClasses()
	{
		return new Class[]{ShapeCookie.class};
	}

	protected void performAction(Node[] arg0)
	{
		try
		{
			//check the all nodes are under the same brep
			HashSet set=new HashSet();
			for(int i=0; i<arg0.length; i++)
			{
				set.add(GeomUtils.getParentBrep(arg0[i]));
			}
			
			if(set.size()>1)
			{
				JOptionPane.showMessageDialog(null, "Can only group shapes from a lonely geometry");
			}
			
			BrepNode brepNode=(BrepNode) set.toArray()[0];
			//get the xml Node related to the .brep
			MetaNode mn=brepNode.getMetaNode();
			if(mn==null)
			{
				FileObject fo=brepNode.getDataObject().getPrimaryFile();
				String name=BrepDataLoader.getMetaFile(fo);
				FileObject newFo = fo.getParent().createData(name);
				FileLock lock = newFo.lock();
				PrintStream ps=new PrintStream(newFo.getOutputStream(lock));
				ps.println("<jcae/>");
				ps.close();
				lock.releaseLock();
				brepNode.updateChildren();
				mn=brepNode.getMetaNode();
			}
			//add a new group
			BrepDataObject bdo=(BrepDataObject)brepNode.getDataObject();
			GeomMeta gm=new GeomMeta(bdo.getMetaDocument(), bdo.getPrimaryFile().getNameExt());
			gm.addFaceGroup(GeomUtils.nodeToIDs(arg0));
			mn.refresh();
			bdo.setModified(true);
		}
		catch(IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
	}

	public String getName()
	{
		return "group faces";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}
}
