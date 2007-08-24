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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d.test;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Point3d;
import org.jcae.viewer3d.FPSBehavior;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.ViewBehavior;
import org.jcae.viewer3d.fe.ViewableFE;

/**
 * Test FPSBehavior and SquareMeshProvider class
 * @author Jerome Robert
 */
public class FPSCounterTest extends JFrame
{
	private static final long serialVersionUID = -798859792379899301L;
	private FPSBehavior fpsCounter=new FPSBehavior();
	private NumberFormat nf = NumberFormat.getNumberInstance();
	private ViewableFE viewable;
	private SquareMeshProvider model=new SquareMeshProvider(1);
	private View view;
	
	public FPSCounterTest()
	{		
		setSize(800,600);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		view = new View(this);
		viewable = new ViewableFE(model);
		viewable.setPickingMode(ViewableFE.PICK_DOMAIN);
		view.add(viewable);
		
		fpsCounter.setSchedulingBounds(new BoundingSphere(new Point3d(), 1000));
		BranchGroup bgfps=new BranchGroup();
		bgfps.addChild(fpsCounter);
		view.addBranchGroup(bgfps);		
					
		getContentPane().add(view);

		final JSlider slider=new JSlider(1000, 100000);
		final JLabel nbLabel=new JLabel();
		
		slider.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{				
				updateMesgSize(slider.getValue());
				nbLabel.setText("number of tria: "+model.getCellNumber());
			}
		});
		slider.setValue(1000);
		
		final JLabel fpsLabel=new JLabel();
		
		JPanel bottom=new JPanel();
		bottom.setLayout(new BorderLayout());
		bottom.add(slider, BorderLayout.CENTER);
		bottom.add(fpsLabel, BorderLayout.EAST);
		bottom.add(nbLabel, BorderLayout.WEST);
		getContentPane().add(bottom, BorderLayout.SOUTH);
		fpsCounter.addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				fpsLabel.setText("FPS: "+nf.format(fpsCounter.getFPS()));
			}
		});
		
		view.setOriginAxisVisible(true);
		view.setFixedAxisVisible(true);
		updateMesgSize(slider.getValue());
		view.addKeyListener(new KeyAdapter()
		{
			public void keyTyped(KeyEvent e)
			{
				if(e.getKeyChar()==' ')
					view.setMouseMode(ViewBehavior.CLIP_RECTANGLE_MODE);
				else
					view.setMouseMode(ViewBehavior.DEFAULT_MODE);
			}
		});
	}
	
	protected void updateMesgSize(int value)
	{
		model.setCellNumber(value);
		viewable.domainsChanged(null);
		view.fitAll();
	}
	
	public static void main(String[] args)
	{
		FPSCounterTest m=new FPSCounterTest();
		m.setVisible(true);
	}
}
