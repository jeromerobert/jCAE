package org.jcae.netbeans.mesh;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import javax.swing.JFileChooser;
import org.jcae.mesh.xmldata.Amibe2UNV;
import org.jcae.mesh.xmldata.MeshExporter;
import org.jcae.netbeans.mesh.ExportGroupAction.ChooseUnitPanel;
import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

public final class ExportUNV extends CookieAction
{
	
	protected void performAction(Node[] activatedNodes)
	{
		try
		{
			JFileChooser jfc=new JFileChooser();
			ChooseUnitPanel unitPanel=new ChooseUnitPanel();
						
			AmibeDataObject meshNode=activatedNodes[0].getLookup().lookup(AmibeDataObject.class);
			File meshDir=new File(meshNode.getMeshDirectory());
			
			jfc.setAccessory(unitPanel);
			jfc.setCurrentDirectory(meshDir.getParentFile());
			if(jfc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
			{
				String unvFile=jfc.getSelectedFile().getPath();

				if(!unvFile.endsWith(".unv"))
					unvFile+=".unv";

				PrintStream stream=new PrintStream(new BufferedOutputStream(
					new FileOutputStream(unvFile)));				
				Amibe2UNV amibe2unv=new Amibe2UNV(meshDir);

				if(unitPanel.isMeters())
					amibe2unv.setUnit(MeshExporter.UNV.Unit.METER);
				else
					amibe2unv.setUnit(MeshExporter.UNV.Unit.MM);
				amibe2unv.write(stream);				
				stream.close();				
			}
		}
		catch(Exception ex)
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
		return NbBundle.getMessage(ExportUNV.class, "CTL_ExportUNV");
	}
	
	protected Class[] cookieClasses()
	{
		return new Class[] {
			AmibeDataObject.class
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

