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
 * (C) Copyright 2012, by EADS France
 */

package org.jcae.netbeans.options;

import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.explorer.propertysheet.PropertySheet;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

/**
 *
 * @author Mohit Garg
 */
@OptionsPanelController.TopLevelRegistration(categoryName = "jCAE",
iconBase = "org/jcae/netbeans/options/frame32.gif")
public final class JCAEOptionsPanelController extends OptionsPanelController {

	public void update() {
	}

	public void applyChanges() {
	}

	public void cancel() {
		// need not do anything special, if no changes have been persisted yet
	}

	public boolean isValid() {
		return true;
	}

	public boolean isChanged() {
		return false;
	}

	public HelpCtx getHelpCtx() {
		return null; // new HelpCtx("...ID") if you have a help set
	}

	public JComponent getComponent(Lookup masterLookup) {
		PropertySheet ps = new PropertySheet();
		ps.setNodes(new Node[]{new OptionNode()});
		return ps;
	}

	public void addPropertyChangeListener(PropertyChangeListener l) {
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
	}
}
