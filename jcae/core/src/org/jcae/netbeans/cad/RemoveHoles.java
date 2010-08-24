package org.jcae.netbeans.cad;

import java.awt.Component;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jcae.opencascade.jni.ShapeUpgrade_RemoveInternalWires;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.openide.windows.WindowManager;

public final class RemoveHoles extends CookieAction
{
	
	private class RHPanel extends JPanel
	{
		private final JCheckBox checkBox = new JCheckBox("Remove faces");
		private final JTextField minAreaField = new JTextField("1.0");

		public RHPanel()
		{
			 setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			 add(createTolerancePanel());
			 add(checkBox);
			 checkBox.setSelected(true);
		}

		private Component createTolerancePanel()
		{
			Box toReturn=Box.createHorizontalBox();
			toReturn.add(new JLabel("Minimum area"));
			toReturn.add(Box.createHorizontalStrut(5));
			toReturn.add(minAreaField);
			return toReturn;
		}
		
		public boolean isRemoveFace()
		{
			return checkBox.isSelected();
		}
		
		public double getMinArea()
		{
			return Double.parseDouble(minAreaField.getText());
		}
		
		public boolean showDialog()
		{
			boolean valid;
			int r;
			do
			{
				r=JOptionPane.showConfirmDialog(
					WindowManager.getDefault().getMainWindow(),
					this, "Remove holes", JOptionPane.OK_CANCEL_OPTION);
				try
				{
					getMinArea();
					valid=true;
				}
				catch (NumberFormatException ex)
				{
					valid=false;
				}
			}
			while(r==JOptionPane.OK_OPTION && !valid);
			
			return r==JOptionPane.OK_OPTION;
		}
	}
	
	protected void performAction(Node[] activatedNodes)
	{
		NbShape s = GeomUtils.getShape(activatedNodes[0]);
		ShapeUpgrade_RemoveInternalWires riw=new ShapeUpgrade_RemoveInternalWires(s.getImpl());
		RHPanel panel=new RHPanel();
		if(panel.showDialog())
		{
			riw.setMinArea(panel.getMinArea());
			riw.setRemoveFaceMode(panel.isRemoveFace());
			riw.perform();
			GeomUtils.insertShape(riw.getResult(),
				activatedNodes[0].getName()+"_RH",
				activatedNodes[0].getParentNode());
			GeomUtils.getParentBrep(activatedNodes[0]).getDataObject().setModified(true);
		}
	}
	
	protected int mode()
	{
		return CookieAction.MODE_EXACTLY_ONE;
	}
	
	public String getName()
	{
		return NbBundle.getMessage(RemoveHoles.class, "CTL_RemoveHoles");
	}
	
	protected Class[] cookieClasses()
	{
		return new Class[] {
			NbShape.class
		};
	}
	
	protected void initialize()
	{
		super.initialize();
		// see org.openide.util.actions.SystemAction.iconResource() javadoc for more details
		putValue("noIconInMenu", Boolean.TRUE);
		putValue(Action.SHORT_DESCRIPTION,
			"Removes all internal whose area is lesser than this specified area");
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

