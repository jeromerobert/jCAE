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
import javax.swing.*;
import org.jcae.netbeans.viewer3d.View3D;
import org.jcae.netbeans.viewer3d.View3DManager;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 *
 * @author  philavong
 */
public class PanelView implements ActionListener
{
    protected Groups groups = null;
    protected DialogDescriptor descriptor;
    protected JPanel innerPane = new JPanel();
    protected boolean cancel = true;
    protected JLabel labelGroup = new JLabel("select groups to display : ");
    protected JList listGroup = null;
    protected JScrollPane scrollpane = null;
    protected ArrayList selectedGroups = new ArrayList();
    protected JLabel labelView = new JLabel("select a view");
    protected JComboBox comboboxView = null;
    protected View3D selectedView = null;
    protected JButton buttonSelectAllGroups=new JButton("Select All");
    
    /** 
     * Creates a new instance of PanelFuse.
     */
    public PanelView(Groups groups) {
        this.groups = groups;
        
        initPanel();
        
        descriptor = new DialogDescriptor(innerPane, "Select groups to display", true, NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.OK_OPTION, this);
        
        Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.setSize(300,680);
        dialog.setVisible(true);
    }
    
    /**
     *
     */
    public void initPanel()
    {
        innerPane.setLayout(null);
        
        labelGroup.setBounds(10,10,200,20);
        
        listGroup = new JList(groups.getGroups());
        scrollpane = new JScrollPane(listGroup);
        scrollpane.setBounds(10,30,270,460);
        
        labelView.setBounds(10,500,200,20);
        comboboxView = new JComboBox(View3DManager.getDefault().getAllView3D());
		comboboxView.setSelectedItem(View3DManager.getDefault().getSelectedView3D());
        comboboxView.setBounds(10,530,200,20);
        
	buttonSelectAllGroups.setBounds(10,500,100,20);
        buttonSelectAllGroups.addActionListener(this);

        innerPane.add(labelGroup);
        innerPane.add(scrollpane);
        innerPane.add(labelView);
        innerPane.add(comboboxView);
	innerPane.add(buttonSelectAllGroups);
    }
    
    /**
     *
     */
    public ArrayList getSelectedGroups()
    {
        return selectedGroups;
    }
    
    /**
     *
     */
    public View3D getSelectedView()
    {
        return selectedView;
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
	if (e.getSource()==buttonSelectAllGroups)
        {
            int[] all = new int[groups.getGroups().length];
            for (int i=0;i<all.length;i++)
            {
                all[i] = i;
            }
            listGroup.setSelectedIndices(all);
        }
        else if (e.getSource()==NotifyDescriptor.OK_OPTION)
        {
            // Ok was pressed;
            if (!listGroup.isSelectionEmpty())
            {
                for (int i=0;i<listGroup.getSelectedValues().length;i++)
                {
                    selectedGroups.add(listGroup.getSelectedValues()[i]);
                }
                selectedView = (View3D)comboboxView.getSelectedItem();
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
