/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
	Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>
 
	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.
 
	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.
 
	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.*;
import javax.swing.*;


/**
 *
 * @author Jerome Robert
 */
public class DialogGroupRename extends javax.swing.JDialog
{
	private JPanel panelCenter;
	private JLabel labelName;
	private JButton buttonOK;
	private JPanel panelBottom;
	private JButton buttonCancel;
	private JTextField textFieldName;
	private boolean returnOK;
	
	/** Creates new form DialogGroupRename */
	public DialogGroupRename(Frame parent)
	{
		super(parent, true);		
		initComponents();
		setLocation((parent.getWidth()-getWidth())/2,
			(parent.getHeight()-getHeight())/2);
	}

	/**
	 * @param parent
	 * @return The new name of the group or null if the dialog was canceled
	 * unchanged.
	 */
	public static String execute(Frame parent)
	{
		DialogGroupRename d=new DialogGroupRename(parent);
		d.show();
		if(d.returnOK) return d.textFieldName.getText();
		else return null;
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 */
	private void initComponents()
	{
		panelBottom = new JPanel();
		buttonOK = new JButton();
		buttonCancel = new JButton();
		panelCenter = new JPanel();
		labelName = new JLabel();
		textFieldName = new JTextField();
		
		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Rename group");
		setModal(true);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt)
			{
				closeDialog(evt);
			}
		});
		
		buttonOK.setText("OK");
		buttonOK.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				buttonOKActionPerformed(evt);
			}
		});
		
		panelBottom.add(buttonOK);
		
		buttonCancel.setText("Cancel");
		buttonCancel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				buttonCancelActionPerformed(evt);
			}
		});
		
		panelBottom.add(buttonCancel);
		
		getContentPane().add(panelBottom, BorderLayout.SOUTH);
		
		panelCenter.setLayout(new BorderLayout(10, 0));
		
		panelCenter.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(5, 5, 0, 5)));
		labelName.setText("Group name");
		panelCenter.add(labelName, BorderLayout.WEST);
		
		textFieldName.setPreferredSize(new java.awt.Dimension(100, 0));
		panelCenter.add(textFieldName, BorderLayout.CENTER);
		
		getContentPane().add(panelCenter, BorderLayout.CENTER);
		
		pack();
	}
	
	private void buttonCancelActionPerformed(ActionEvent evt)
	{
		returnOK=false;
		dispose();
	}
	
	private void buttonOKActionPerformed(ActionEvent evt)
	{
		returnOK=true;
		dispose();
	}
	
	/** Closes the dialog */
	private void closeDialog(WindowEvent evt)
	{
		setVisible(false);
		dispose();
	}
}
