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
 * (C) Copyright 2004-2008, by EADS France
 */

package org.jcae.netbeans.viewer3d.actions;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import org.jcae.netbeans.viewer3d.CurrentViewChangeListener;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;

/**
 * @author Jerome Robert
 *
 */
public class SelectViewable extends CallableSystemAction implements CurrentViewChangeListener
{

	static int counter = 0;

	public SelectViewable()
	{
		++counter;
		if (counter != 1)
			throw new RuntimeException("Only one SelectInteractor can be created, if you want more change its conception");
		ViewManager.getDefault().addViewListener(this);
	}

	public static class MyComboBoxModel extends DefaultComboBoxModel
	{
		private static MyComboBoxModel instance = null;

		MyComboBoxModel()
		{
			if (instance != null)
				throw new RuntimeException("Only one instance can be created");

			instance = this;
		}

		public static MyComboBoxModel getDefault()
		{
			return instance;
		}

		/* (non-Javadoc)
		 * @see javax.swing.ComboBoxModel#getSelectedItem()
		 */
		@Override
		public Object getSelectedItem()
		{
			if(ViewManager.getDefault().hasView())
				return ViewManager.getDefault().getCurrentView().getCurrentViewable();
			else return null;
		}

		/* (non-Javadoc)
		 * @see javax.swing.ComboBoxModel#setSelectedItem(java.lang.Object)
		 */
		@Override
		public void setSelectedItem(Object anItem)
		{
			if(ViewManager.getDefault().hasView())
				ViewManager.getDefault().getCurrentView().setCurrentViewable((Viewable) anItem);
		}

		/* (non-Javadoc)
		 * @see javax.swing.ListModel#getSize()
		 */
		@Override
		public int getSize()
		{
			if(ViewManager.getDefault().hasView())
				return ViewManager.getDefault().getCurrentView().getViewables().size();
			else
				return 0;
		}

		/* (non-Javadoc)
		 * @see javax.swing.ListModel#getElementAt(int)
		 */
		@Override
		public Object getElementAt(int index)
		{
			if(ViewManager.getDefault().hasView())
				return ViewManager.getDefault().getCurrentView().getViewables().get(index);
			else
				return "" + index;
		}

		public void refresh()
		{
			super.fireContentsChanged(this, 0, getSize() - 1);
		}
	}
	// Need to be static because 2 instances of this class are created
	// (this is probably a bug of netbeans) and we need to have the "only"
	// comboBoxModel in refresh().
	private static MyComboBoxModel comboBoxModel = new MyComboBoxModel();
	private final JComboBox box = new JComboBox(comboBoxModel);

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

	/* (non-Javadoc)see
	 * @see org.openide.util.actions.CallableSystemAction#getToolbarPresenter()
	 */
	@Override
	public Component getToolbarPresenter()
	{
		box.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
		box.setLightWeightPopupEnabled(false);
		return box;
	}
	
	/* (non-Javadoc)
	 * @see org.openide.util.actions.CallableSystemAction#performAction()
	 */
	public void performAction()
	{
	}

	/**
	 * Refresh the data and also relink the selecting combobox to the
	 * selecting interactor of ViewManager
	 */
	public void refresh()
	{
		comboBoxModel.refresh();
	}

	public void currentViewChanged(View view)
	{
		comboBoxModel.refresh();
	}
}
