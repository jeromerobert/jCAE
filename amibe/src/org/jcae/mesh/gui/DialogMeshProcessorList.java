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

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Constructor;
import javax.swing.*;
import org.jcae.mesh.MeshModule;
import org.jcae.mesh.MeshObject;

/**
 * @author  Jerome Robert
 */
public class DialogMeshProcessorList extends JDialog
{
	
	/** Creates new form DialogMeshProcessorList */
	public DialogMeshProcessorList(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = getSize();
		if (frameSize.height > screenSize.height) frameSize.height = screenSize.height;
		if (frameSize.width > screenSize.width) frameSize.width = screenSize.width;
		setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);		
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 */
	private void initComponents()
	{
		panelBottom = new JPanel();
		buttonOK = new JButton();
		buttonCancel = new JButton();
		scrollPane = new JScrollPane();
		list = new JList(MeshProcessorLister.list().toArray());
		
		setTitle("Select a mesh processor");
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
		
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setViewportView(list);
		
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		pack();
	}
	
	private void buttonCancelActionPerformed(ActionEvent evt)
	{
		closeDialog(null);
	}
	
	private void buttonOKActionPerformed(ActionEvent evt)
	{
		try
		{
			String processor=list.getSelectedValue().toString();
			Class guiClass=Class.forName("org.jcae.mesh."+processor+"GUI");
			Constructor c=guiClass.getConstructor(new Class[0]);
			MeshProcessorGUI dialog=(MeshProcessorGUI)c.newInstance(new Object[0]);
			dialog.setMeshObject(meshObject);
			dialog.setMeshModule(meshModule);			
			dialog.show();
			closeDialog(null);
		} catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/** Closes the dialog */
	private void closeDialog(WindowEvent evt)
	{
		setVisible(false);
		dispose();
	}
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[])
	{
		new DialogMeshProcessorList(new JFrame(), true).show();
	}
	
	public void setMeshObject(MeshObject meshObject)
	{
		this.meshObject = meshObject;
	}	
	
	public void setMeshModule(MeshModule meshModule)
	{
		this.meshModule = meshModule;
	}	
	
	private JButton buttonCancel;
	private JButton buttonOK;
	private JList list;
	private JPanel panelBottom;
	private JScrollPane scrollPane;	
	private MeshObject meshObject;	
	private MeshModule meshModule;	
}
