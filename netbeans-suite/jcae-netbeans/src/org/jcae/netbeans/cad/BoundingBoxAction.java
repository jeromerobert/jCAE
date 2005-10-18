package org.jcae.netbeans.cad;

import java.util.ArrayList;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.jcae.opencascade.jni.BRepBndLib;
import org.jcae.opencascade.jni.Bnd_Box;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

public class BoundingBoxAction extends CookieAction
{
	private static final String CR=System.getProperty("line.separator");
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	protected int mode()
	{
		return CookieAction.MODE_ONE;
	}
	
	protected Class[] cookieClasses()
	{
		return new Class[] { ShapeCookie.class } ;
	}
	
	protected void performAction(Node[] arg0)
	{
		Bnd_Box box = new Bnd_Box(); 			
		BRepBndLib.add(GeomUtils.getShape(arg0[0]),box);			
		double[] bbox = box.get();
		String text="Xmin="+bbox[0]+CR;
		text+="Ymin="+bbox[1]+CR;
		text+="Zmin="+bbox[2]+CR;
		
		text+="Xmax="+bbox[3]+CR;
		text+="Ymax="+bbox[4]+CR;
		text+="Zmax="+bbox[5]+CR;
		
		JTextArea textField=new JTextArea(text);
		textField.setEditable(false);
		DialogDescriptor dd=new DialogDescriptor(textField, "Bounding box");
		DialogDisplayer.getDefault().createDialog(dd).setVisible(true);
	}
	
	public String getName()
	{
		return "Bounding Box";
	}
	
	public HelpCtx getHelpCtx()
	{
		return null;
	}
}
