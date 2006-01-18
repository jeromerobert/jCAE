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
 * (C) Copyright 2006, by EADS CRC
 */

package org.jcae.netbeans.mesh;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import org.openide.awt.Actions;
import org.openide.awt.Mnemonics;
import org.openide.util.HelpCtx;
import org.openide.util.actions.Presenter;
import org.openide.util.actions.SystemAction;


public class ExpertMenu extends SystemAction implements Presenter.Popup
{	public ExpertMenu()
	{			
	}
	
	public String getName()
	{
		return "Expert";
	}

	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}

	public void actionPerformed(ActionEvent actionEvent)
	{
		// this item does nothing, it displays a popup
	}

	public JMenuItem getPopupPresenter()
	{
		JMenu menu = new JMenu();		
		Mnemonics.setLocalizedText(menu, getName());		
		
		JMenuItem item = new JMenuItem();
		Mnemonics.setLocalizedText(item, item.getText());
		Actions.connect(item, (Action)SystemAction.get(BuildSoupAction.class), true);
		menu.add(item);
		
		item = new JMenuItem();
		Mnemonics.setLocalizedText(item, item.getText());
		Actions.connect(item, (Action)SystemAction.get(BuidOEMMAction.class), true);
		menu.add(item);

		item = new JMenuItem();
		Mnemonics.setLocalizedText(item, item.getText());
		Actions.connect(item, (Action)SystemAction.get(OEMMViewAction.class), true);
		menu.add(item);
		
		menu.add(new JSeparator());
		
		item = new JMenuItem();
		Mnemonics.setLocalizedText(item, item.getText());
		Actions.connect(item, (Action)SystemAction.get(DecimateAction.class), true);
		menu.add(item);
		
		return menu;			
	}		
}