/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jcae.netbeans.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

@OptionsPanelController.TopLevelRegistration(categoryName = "#OptionsCategory_Name_jCAE",
iconBase = "org/jcae/netbeans/options/amibe.png",
keywords = "#OptionsCategory_Keywords_jCAE",
keywordsCategory = "jCAE")
@org.openide.util.NbBundle.Messages({"OptionsCategory_Name_jCAE=jCAE", "OptionsCategory_Keywords_jCAE=jcae"})
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
            panel = new JCAEPanel(this);
        }
        return panel;
    }

    void changed() {
        if (!changed) {
            changed = true;
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }
}
