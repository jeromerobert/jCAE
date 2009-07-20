package org.jcae.netbeans.mesh;

import gnu.trove.TIntArrayList;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import javax.swing.JFileChooser;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.ds.Constraint;
import org.jcae.mesh.bora.xmldata.BoraToUnvConvert;
import org.jcae.mesh.xmldata.Amibe2UNV;
import org.jcae.mesh.xmldata.MeshExporter;
import org.jcae.netbeans.mesh.ExportGroupAction.ChooseUnitPanel;
import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

public final class ExportBUNV extends CookieAction
{
	
	protected void performAction(Node[] activatedNodes)
	{
		try
		{
			//pannel to choose the unv file
			JFileChooser jfc=new JFileChooser();
			ChooseUnitPanel unitPanel=new ChooseUnitPanel();

			//the subMesh to export
			SubmeshNode meshNode=activatedNodes[0].getCookie(SubmeshNode.class);
			final BSubMesh subMesh = meshNode.getDataModel().getSubMesh();

			jfc.setAccessory(unitPanel);
			jfc.setCurrentDirectory(new File(subMesh.getModel().getOutputDir()));

			if(jfc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
			{
				String unvFile=jfc.getSelectedFile().getPath();
				if(!unvFile.endsWith(".unv"))
					unvFile += ".unv";

				//getting the list of ids
				TIntArrayList listOfShapes = new TIntArrayList();
				ArrayList<BCADGraphCell> toUse = new ArrayList<BCADGraphCell>();
				for (Constraint c : subMesh.getConstraints()) {
					BCADGraphCell cell = c.getGraphCell();
					BDiscretization d = cell.getDiscretizationSubMesh(subMesh);
					if (d != null) {
						toUse.add(cell);
						listOfShapes.add(d.getId());
					}
				}


				BoraToUnvConvert conv = new BoraToUnvConvert(unvFile, subMesh);
				conv.collectBoundaryNodes(listOfShapes.toNativeArray());
				conv.beforeProcessingAllShapes(false);

				//specifying the groups
				int groupId = 0;
				Map<String, HashSet<BCADGraphCell>> groupMap = meshNode.getDataModel().getGroupMap();
				for (String group : groupMap.keySet()) {
					HashSet<BCADGraphCell> belongsToGroup = groupMap.get(group);
					if (belongsToGroup == null)
						continue;
					belongsToGroup.retainAll(toUse);
					if (belongsToGroup.isEmpty())
						continue;
					groupId++;
					for (BCADGraphCell cell : belongsToGroup) {
						int faceId = (cell.getDiscretizationSubMesh(subMesh)).getId();
						conv.processOneShape(groupId, group, faceId);
					}
				}
				conv.afterProcessingAllShapes();
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
		return NbBundle.getMessage(ExportBUNV.class, "CTL_ExportUNV");
	}
	
	protected Class[] cookieClasses()
	{
		return new Class[] {
			SubmeshNode.class
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

