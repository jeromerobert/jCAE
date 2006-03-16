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
import org.jcae.viewer3d.View;
import org.openide.windows.WindowManager;


/**
 * Class to display View Position
 * @author userloc
 *
 */
public  class ViewList extends JDialog {
	
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
	
	PositionManager mgr;
	JButton goButton;
	JList viewList;
	ListModel model;
	
	private static ImageIcon goIcon=new ImageIcon(ViewList.class.getResource("1rightarrow.png"));
	private static ImageIcon removeIcon=new ImageIcon(ViewList.class.getResource("button_cancel.png"));
	private View view;
	
	public ViewList(PositionManager mgr, View view)
	{
		super(WindowManager.getDefault().getMainWindow(),"Go To ...", true);
		this.mgr=mgr;
		this.view=view;
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
		model = new AbstractListModel() {
			public int getSize() { return mgr.getPositionCount(); }
			public Object getElementAt(int index) { return mgr.getPosition(index).getSnapShot(); }
		};
		
		viewList = new JList(model);
		viewList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		viewList.setCellRenderer(new ViewRenderer());
		viewList.setVisibleRowCount(5);
		//viewList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		
		if( mgr.getPositionCount() > 0)
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
				public void actionPerformed(ActionEvent e)	{removePerformed();}
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
	
	
	private void goPerformed(){
		int index=viewList.getSelectedIndex();
		if(index!=-1){
			mgr.goToPosition(view, index);
			setVisible(false);
		}
	}
	
	private void removePerformed(){
		int index=viewList.getSelectedIndex();
		if(index!=-1){
			mgr.removePosition(index);
			viewList.repaint();
			if(mgr.getPositionCount()>0)
				viewList.setSelectedIndex(0);
		}
	}
	
}
