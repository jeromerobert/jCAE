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
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

/**
 *
 * @author Mohit Garg
 *
 */
@OptionsPanelController.TopLevelRegistration(categoryName = "jCAE",
iconBase = "org/jcae/netbeans/options/frame32.gif")
public final class JCAEOptionsPanelController extends OptionsPanelController {

	private JCAEPanel panel;
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private boolean changed;

	public void update() {
		changed = false;
	}

	public void applyChanges() {
		changed = false;
	}

	public void cancel() {
		// need not do anything special, if no changes have been persisted yet
	}

	public boolean isValid() {
		return true;
	}

	public boolean isChanged() {
		return changed;
	}

	public HelpCtx getHelpCtx() {
		return null; // new HelpCtx("...ID") if you have a help set
	}

	public JComponent getComponent(Lookup masterLookup) {
		return getPanel();
	}

	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}

	private JCAEPanel getPanel() {
		if (panel == null) {
			panel = new JCAEPanel();
		}
		return panel;
	}

	void changed() {
		if (!changed) {
			changed = true;
			pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false,
				true);
		}
		pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
	}
}
