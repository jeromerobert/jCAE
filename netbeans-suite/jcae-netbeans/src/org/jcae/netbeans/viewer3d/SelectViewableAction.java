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
 * (C) Copyright 2004, by EADS CRC
 */

package org.jcae.netbeans.viewer3d;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.Viewable;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;

/**
 * @author Jerome Robert
 *
 */
public class SelectViewableAction extends CallableSystemAction
{	
	private static class MyComboBoxModel extends DefaultComboBoxModel
	{

		private boolean contentChangedLock;

		/* (non-Javadoc)
		 * @see javax.swing.ComboBoxModel#getSelectedItem()
		 */
		public Object getSelectedItem()
		{
			View3D v3d=View3D.getSelectedView3D();
			if(v3d!=null)
			{
				return v3d.getView().getCurrentViewable();
			}
			else return null;
		}

		/* (non-Javadoc)
		 * @see javax.swing.ComboBoxModel#setSelectedItem(java.lang.Object)
		 */
		public void setSelectedItem(Object anItem)
		{
			View3D v3d=View3D.getSelectedView3D();
			if(v3d!=null)
			{
				v3d.getView().setCurrentViewable((Viewable) anItem);
			}
		}

		/* (non-Javadoc)
		 * @see javax.swing.ListModel#getSize()
		 */
		public int getSize()
		{
			View3D v3d=View3D.getSelectedView3D();
			if(v3d!=null)
			{				
				return v3d.getView().getViewables().length;
			}
			else return 0;
		}

		/* (non-Javadoc)
		 * @see javax.swing.ListModel#getElementAt(int)
		 */
		public Object getElementAt(int index)
		{
			if(index==0 && !contentChangedLock)
			{
				contentChangedLock=true;
				fireContentsChanged(this, 0, getSize()-1);
				contentChangedLock=false;
			}
			View3D v3d=View3D.getSelectedView3D();
			if(v3d!=null)
			{				
				return v3d.getView().getViewables()[index];
			}
			else return ""+index;
		}
	}
	
	
	private class ActionRemoveCurent extends AbstractAction
	{
		private View view;
		/**
		 * @param view
		 * 
		 */
		public ActionRemoveCurent(View view)
		{
			super("remove current");
			this.view=view;
		}
		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent e)
		{
			view.remove(view.getCurrentViewable());
		}
	}
	
	/**
	 * @author Jerome Robert
	 *
	 */
	private class ActionViewable extends AbstractAction
	{
		private View view;
		private Viewable viewable;
		/**
		 * @param viewable
		 */
		public ActionViewable(Viewable viewable, View view)
		{
			super(viewable.toString());
			this.viewable=viewable;
			this.view=view;
		}

		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent e)
		{
			AbstractButton button=(AbstractButton) e.getSource();
			if(button.isSelected())
				view.setCurrentViewable(viewable);
		}
	}	

	/* (non-Javadoc)
	 * @see org.openide.util.HelpCtx.Provider#getHelpCtx()
	 */
	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}

	/* (non-Javadoc)
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	public String getName()
	{
		return "Select";
	}

	/* (non-Javadoc)
	 * @see org.openide.util.actions.CallableSystemAction#getToolbarPresenter()
	 */
	public Component getToolbarPresenter()
	{				
		JComboBox box=new JComboBox(new MyComboBoxModel());
		box.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
		return box;
	}
	/* (non-Javadoc)
	 * @see org.openide.util.actions.CallableSystemAction#performAction()
	 */
	public void performAction()
	{
	}
}
