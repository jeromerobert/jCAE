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

package org.jcae.netbeans.mesh;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 *
 * @author  philavong
 */
public class PanelFuse implements ActionListener
{
    protected Groups groups;
    protected DialogDescriptor descriptor;
    protected JPanel innerPane = new JPanel();
    protected boolean cancel = true;
    protected JLabel labelGroup = new JLabel("select groups to fuse : ");
    protected JList listGroup;
    protected JScrollPane scrollpane;
    protected Collection<Group> selectedGroups = new ArrayList<Group>();
    
    /** 
     * Creates a new instance of PanelFuse.
     */
    public PanelFuse(Groups groups) {
        this.groups = groups;
        
        initPanel();
        
        descriptor = new DialogDescriptor(innerPane, "Select groups to fuse", true, NotifyDescriptor.OK_CANCEL_OPTION, DialogDescriptor.OK_OPTION, this);
        
        Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.setSize(300,580);
        dialog.setVisible(true);
    }
    
    /**
     *
     */
    private void initPanel()
    {
        innerPane.setLayout(null);
        
        labelGroup.setBounds(10,10,200,20);
        
        listGroup = new JList(groups.getGroups());
        scrollpane = new JScrollPane(listGroup);
        scrollpane.setBounds(10,30,270,460);
        
        innerPane.add(labelGroup);
        innerPane.add(scrollpane);
    }
    
    /**
     *
     */
    public Collection<Group> getSelectedGroups()
    {
        return selectedGroups;
    }
    
    /**
     * @return true if the button cancel has been clicked, after closing this dialog.
     */
    public boolean cancel()
    {
        return cancel;
    }
    
    /**
     *
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource()==NotifyDescriptor.OK_OPTION)
        {
            // Ok was pressed;
            if (!listGroup.isSelectionEmpty())
            {
                for(Object s:listGroup.getSelectedValues())
					selectedGroups.add((Group)s);

				cancel = false;
            }
        }
        else if (e.getSource()==NotifyDescriptor.CANCEL_OPTION)
        {
            // Cancel was pressed
            cancel = true;
        }
    }
    
}
