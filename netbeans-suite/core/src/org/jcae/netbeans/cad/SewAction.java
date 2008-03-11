/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.netbeans.cad;

import java.awt.Component;
import javax.swing.*;
import org.jcae.opencascade.jni.BRepBuilderAPI_Sewing;
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
		private final JCheckBox checkBoxOption =
			new JCheckBox("Analysis of degenerated shapes");
		private final JCheckBox checkBoxCutting =
			new JCheckBox("Cutting or not shared edges");
		private final JCheckBox checkBoxNonManifold =
			new JCheckBox("Non manifold processing");	
		private final JTextField toleranceField = new JTextField("1E-6");
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
		return new Class[]{NbShape.class};
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
			BRepBuilderAPI_Sewing sewer=new BRepBuilderAPI_Sewing();
			sewer.init(spanel.getTolerance(), spanel.getOption(),
				spanel.getCutting(), spanel.getNonManifold());
			
			for(Node n: activatedNodes)
				sewer.add(GeomUtils.getShape(n).getImpl());
			
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

	@Override
	protected boolean asynchronous()
	{
		return false;
	}
}
