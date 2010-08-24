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

package org.jcae.netbeans.viewer3d;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import org.jcae.vtk.CameraManager;
import org.openide.windows.WindowManager;

/**
 * Class to display View Position
 * @author Erwann Feat and Julian Ibarz porting to VTK
 */
public  class ViewCameraList extends JDialog {
	
	class ViewRenderer extends JLabel implements ListCellRenderer {

	     public Component getListCellRendererComponent(
	       JList list,
	       Object value,            // value to display
	       int index,               // cell index
	       boolean isSelected,      // is the cell selected
	       boolean cellHasFocus)    // the list and the cell have the focus
	     {
	         setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	    	 setIcon((ImageIcon )value);
	         
	           if (isSelected) {
	        	   setBackground(list.getSelectionBackground());
	               setForeground(list.getSelectionForeground());
	           }
	         else {
	               setBackground(list.getBackground());
	               setForeground(list.getForeground());
	           }
	           setEnabled(list.isEnabled());
	           setFont(list.getFont());
	         setOpaque(true);
	         return this;
	     }
	 }
	
	 private CameraManager mgr;
	private JButton goButton;
	private JList viewList;
	private ListModel model;
	
	private static ImageIcon goIcon=new ImageIcon(ViewCameraList.class.getResource("1rightarrow.png"));
	private static ImageIcon removeIcon=new ImageIcon(ViewCameraList.class.getResource("button_cancel.png"));
	
	public ViewCameraList(CameraManager cameraManager)
	{
		super(WindowManager.getDefault().getMainWindow(),"Go To ...", true);
		this.mgr = cameraManager;
		
		JPanel mainPanel=new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(createlist(),BorderLayout.CENTER);
		
		JPanel buttonPanel=new JPanel();
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		buttonPanel.add(createButtons());
		mainPanel.add(buttonPanel,BorderLayout.EAST);
		getContentPane().add(mainPanel);
		pack();
	}

	private JPanel createlist(){
		JPanel toReturn=new JPanel();
		model = new AbstractListModel()
		{
			public int getSize()
			{
				return mgr.getNumberOfCameras();
			}
			
			public Object getElementAt(int index)
			{
				return mgr.getScreenshotCamera(index);
			}
		};
		
		viewList = new JList(model);
		viewList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		viewList.setCellRenderer(new ViewRenderer());
		viewList.setVisibleRowCount(5);
		//viewList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		
		if( mgr.getNumberOfCameras() > 0)
			viewList.setSelectedIndex(0);
		
		JScrollPane scroll=new JScrollPane(viewList);
		toReturn.add(scroll);
		return toReturn;
	}
	
	private Container createButtons() {
		
		JPanel panel=new JPanel();
		panel.setLayout(new GridLayout(2,1));
		
		JButton button = new JButton();
		button.setToolTipText("Remove");
		button.setIcon(removeIcon);
		button.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e)
				{
					removePerformed();
				}
			});
		panel.add(button);
		
		goButton = new JButton();
		goButton.setToolTipText("Go");
		goButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e)	{goPerformed();}
			});
		
		goButton.setIcon(goIcon);
		panel.add(goButton);
		
		Container buttonContainer = javax.swing.Box.createVerticalBox();

		buttonContainer.add(javax.swing.Box.createVerticalStrut(10));
		buttonContainer.add(javax.swing.Box.createVerticalGlue());
		buttonContainer.add(panel);

		buttonContainer.add(javax.swing.Box.createVerticalStrut(10));
		
		return buttonContainer;
	}
	
	
	protected void goPerformed()
	{
		int index=viewList.getSelectedIndex();
		if(index!=-1 && model.getSize()>0)
		{
			mgr.setCamera(index);
			setVisible(false);
		}
	}
	
	protected void removePerformed()
	{
		int index=viewList.getSelectedIndex();		
		if(index!=-1 && model.getSize()>0)
		{
			mgr.removeCamera(index);
			viewList.repaint();
			if(mgr.getNumberOfCameras()>0)
				viewList.setSelectedIndex(0);
		}
	}	
}
