package org.jcae.netbeans.mesh;

import java.io.*;
import java.util.HashSet;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.mesh.xmldata.UNVConverter;
import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.xml.sax.SAXException;

public class ExportGroupAction extends CookieAction
{

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
			HashSet set=new HashSet();
			for(int i=0; i<arg0.length; i++)
			{
				MeshNode n=(MeshNode) arg0[i].getParentNode().getParentNode()
					.getCookie(MeshNode.class);
				set.add(n.getMeshDirectory());
			}
			
			if(set.size()>1)
			{
				JOptionPane.showMessageDialog(null,
					"UNV export can only work with groups from the same mesh");
				return;
			}
			
			String meshDir=set.toArray()[0].toString();
			
			JFileChooser jfc=new JFileChooser();
			if(jfc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
			{
				int[] ids=new int[arg0.length];
				for(int i=0; i<arg0.length; i++)
				{
					GroupNode n=(GroupNode) arg0[i].getCookie(GroupNode.class);
					ids[i]=n.getGroup().getId();
				}			
				PrintStream stream=new PrintStream(new BufferedOutputStream(
					new FileOutputStream(jfc.getSelectedFile())));
				new UNVConverter(new File(meshDir), ids).writeUNV(stream);
				stream.close();
			}
		}
		catch(IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
		catch (ParserConfigurationException e)
		{
			ErrorManager.getDefault().notify(e);
		}
		catch (SAXException e)
		{
			ErrorManager.getDefault().notify(e);
		}
	}

	public String getName()
	{
		return "Export as UNV";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}

}
