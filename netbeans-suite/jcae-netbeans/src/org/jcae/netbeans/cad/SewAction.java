package org.jcae.netbeans.cad;

import java.awt.Component;
import javax.swing.*;
import org.jcae.opencascade.jni.BRepOffsetAPI_Sewing;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

/**
 * @author Jerome Robert
 *
 */
public class SewAction extends CookieAction
{
	private static class SewingPanel extends JPanel
	{
		private JCheckBox checkBoxOption= new JCheckBox("Analysis of degenerated shapes");
		private JCheckBox checkBoxCutting= new JCheckBox("Cutting or not shared edges");
		private JCheckBox checkBoxNonManifold= new JCheckBox("Non manifold processing");	
		private JTextField toleranceField = new JTextField("1E-6");
		/**
		 * 
		 */
		public SewingPanel()
		{
			 setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			 add(createTolerancePanel());
			 add(checkBoxOption);
			 add(checkBoxCutting);
			 add(checkBoxNonManifold);
			 checkBoxOption.setSelected(true);
			 checkBoxCutting.setSelected(true);
			 checkBoxNonManifold.setSelected(false);
		}

		/**
		 * @return
		 */
		private Component createTolerancePanel()
		{
			Box toReturn=Box.createHorizontalBox();
			toReturn.add(new JLabel("Tolerance"));
			toReturn.add(Box.createHorizontalStrut(5));
			toReturn.add(toleranceField);
			return toReturn;
		}
		
		public boolean getOption()
		{
			return checkBoxOption.isSelected();
		}
		
		public boolean getCutting()
		{
			return checkBoxCutting.isSelected();
		}
		
		public boolean getNonManifold()
		{
			return checkBoxNonManifold.isSelected();
		}
		
		public double getTolerance()
		{
			return Double.parseDouble(toleranceField.getText());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.openide.util.actions.CookieAction#mode()
	 */
	protected int mode()
	{
		return MODE_ALL;
	}

	/* (non-Javadoc)
	 * @see org.openide.util.actions.CookieAction#cookieClasses()
	 */
	protected Class[] cookieClasses()
	{
		return new Class[]{ShapeCookie.class};
	}

	/* (non-Javadoc)
	 * @see org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 */
	protected void performAction(Node[] activatedNodes)
	{	
		SewingPanel spanel=new SewingPanel();
		if(JOptionPane.showOptionDialog(null, spanel, "Sewing",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
			null, null)==JOptionPane.OK_OPTION)
		{		
			BRepOffsetAPI_Sewing sewer=new BRepOffsetAPI_Sewing();
			sewer.init(spanel.getTolerance(), spanel.getOption(),
				spanel.getCutting(), spanel.getNonManifold());
			
			for(int i=0; i<activatedNodes.length; i++)
			{				
				sewer.add(GeomUtils.getShape(activatedNodes[i]));
			}
			
			sewer.perform();
			GeomUtils.insertShape(sewer.sewedShape(),
				"Sewed", activatedNodes[0].getParentNode());
			GeomUtils.getParentBrep(activatedNodes[0]).getDataObject().setModified(true);
		}
	}

	/* (non-Javadoc)
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	public String getName()
	{
		return "Sew";
	}

	/* (non-Javadoc)
	 * @see org.openide.util.HelpCtx.Provider#getHelpCtx()
	 */
	public HelpCtx getHelpCtx()
	{
        // Update with real help when ready:
        return HelpCtx.DEFAULT_HELP;
	}
}
