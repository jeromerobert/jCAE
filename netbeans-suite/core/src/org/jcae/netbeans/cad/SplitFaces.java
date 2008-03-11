package org.jcae.netbeans.cad;

import java.awt.Component;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jcae.opencascade.jni.ShapeUpgrade_ShapeDivideArea;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.openide.windows.WindowManager;

public final class SplitFaces extends CookieAction
{
	private class RHPanel extends JPanel
	{
		private final JTextField minAreaField = new JTextField("100.0");

		public RHPanel()
		{
			 setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			 add(createTolerancePanel());
		}

		private Component createTolerancePanel()
		{
			Box toReturn=Box.createHorizontalBox();
			toReturn.add(new JLabel("Maximum area"));
			toReturn.add(Box.createHorizontalStrut(5));
			toReturn.add(minAreaField);
			return toReturn;
		}
		
		public double getMaxArea()
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
					this, "Split faces", JOptionPane.OK_CANCEL_OPTION);
				try
				{
					getMaxArea();
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
		NbShape c = GeomUtils.getShape(activatedNodes[0]);
		ShapeUpgrade_ShapeDivideArea riw=new ShapeUpgrade_ShapeDivideArea(c.getImpl());
		RHPanel panel=new RHPanel();
		if(panel.showDialog())
		{
			riw.setMaxArea(panel.getMaxArea());
			riw.perform();
			GeomUtils.insertShape(riw.getResult(),
				activatedNodes[0].getName()+"_SF",
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
		return NbBundle.getMessage(RemoveHoles.class, "CTL_SplitFaces");
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
			"Split faces whose area is greater than a given value");
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

