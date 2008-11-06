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
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.viewer3d.ScreenshotListener;
import org.jcae.viewer3d.SelectionListener;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;
import org.jcae.viewer3d.fd.DefaultFDProvider;
import org.jcae.viewer3d.fd.ViewableFD;
import org.jcae.viewer3d.fe.ViewableFE;
import org.jcae.viewer3d.fe.amibe.AmibeNodeSelection;
import org.jcae.viewer3d.fe.amibe.AmibeOverlayProvider;
import org.jcae.viewer3d.fe.amibe.AmibeProvider;
import org.jcae.viewer3d.fe.unv.UNVProvider;
import org.xml.sax.SAXException;

/**
 * @author Jerome Robert
 *
 */
public class Main
{
	private final static Logger LOGGER=Logger.getLogger(Main.class.getName());
	static
	{
		LOGGER.setLevel(Level.ALL);
		ConsoleHandler cd=new ConsoleHandler();
		cd.setLevel(Level.ALL);
		LOGGER.addHandler(cd);
	}
	
	public static void testCAD()
	{
		//Test CAD visu in 1 view
		JFrame cadFrame=new JFrame("jcae-viewer3d-cad demo");			
		cadFrame.setSize(800,600);
		cadFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		final View cadView=new View(cadFrame);					
		
		ViewableCAD fcad=new ViewableCAD(new OCCProvider("/home/jerome/Models/F1.brep"));
		cadView.add(fcad);
		cadView.fitAll();
		cadFrame.getContentPane().add(cadView);
		cadFrame.getContentPane().add(new JButton(new AbstractAction(){

			public void actionPerformed(ActionEvent e)
			{
				cadView.fitAll();
			}}), BorderLayout.NORTH);
		
		cadFrame.setVisible(true);
		cadView.setOriginAxisVisible(true);
		fcad.domainsChanged(null);
		cadView.setOriginAxisVisible(false);
		cadView.setOriginAxisVisible(true);
		//cadView.showFloatAxis(true);
	}

	public static void testUNV() throws IOException
	{
		//Test UNV Loader
		JFrame feFrame=new JFrame();			
		feFrame.setSize(800,600);
		feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		View feView=new View(feFrame);
		UNVProvider unvProvider=new UNVProvider();
		unvProvider.load(new FileInputStream("/home/jerome/Models/unv/flight_solid.unv"));
		
		ViewableFE vfe=new ViewableFE(unvProvider);
		vfe.setPickingMode(ViewableFE.PICK_DOMAIN);
		feView.add(vfe);			
		feFrame.getContentPane().add(feView);
		feView.setOriginAxisVisible(false);
		feView.setFixedAxisVisible(false);
		feView.fitAll();
		feFrame.setVisible(true);
		/*feView.setOriginAxisVisible(true);
		feView.setFixedAxisVisible(true);*/		
	}
	
	public static void testAmibe()
		throws ParserConfigurationException, SAXException, IOException
	{
		//Test FE visu in 1 view
		JFrame feFrame=new JFrame("jcae-viewer3d-fe demo");			
		feFrame.setSize(800,600);
		feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		View feView=new View(feFrame);			
		final AmibeProvider ap=new AmibeProvider(new File("/var/tmp/mesh"));
		final ViewableFE fev=new ViewableFE(ap);			
		ViewableFE ffe=new ViewableFE(new AmibeOverlayProvider(new File("/var/tmp/mesh"), AmibeOverlayProvider.FREE_EDGE));			
		
		feView.add(ffe);
		feView.add(fev);			
		feView.fitAll();
		fev.setPickingMode(ViewableFE.PICK_NODE);
		feFrame.getContentPane().add(feView);
		feFrame.setVisible(true);
		feView.setOriginAxisVisible(true);
		feView.setCurrentViewable(fev);
		fev.addSelectionListener(new SelectionListener()
		{
			public void selectionChanged()
			{
				try
				{
					System.out.println(new AmibeNodeSelection(ap, fev.getSelectedNodes()));
				} catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
	
	static void testOffscreen() throws ParserConfigurationException, SAXException, IOException
	{
		//Test offscreen screenshot
		View feView2=new View(null, true);			
		ViewableFE fev2=new ViewableFE(new AmibeProvider(new File("/var/tmp/mesh")));			
		//ViewableFE fev=new ViewableFE(new AmibeProvider(new File("/tmp")));
		feView2.add(fev2);
		feView2.fitAll();
		feView2.setOriginAxisVisible(true);
		ImageIO.write(feView2.takeSnapshot(4096,4096), "png",
			File.createTempFile("jcae-viewer3d-snap",".png"));
	}
	
	static public void testFD() throws InterruptedException
	{
		JFrame f=new JFrame("jcae-viewer3d-fd demo");
		JSplitPane splitPane=new JSplitPane();
		f.setSize(800,600);
		f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		f.setContentPane(splitPane);
		
		//Test visualisation of FD in 2 different view
		final View view=new View(f);
		final ViewableFD fdv=new ViewableFD(new DefaultFDProvider());
		fdv.addSelectionListener(new SelectionListener()
		{
			public void selectionChanged()
			{
				System.out.println("new selection: "+Arrays.asList(fdv.getSelection()));
			}
		});
		view.add(fdv);
		view.setOriginAxisVisible(true);
		view.fitAll();	
		splitPane.add(view, JSplitPane.RIGHT);
		View view2=new View(f);
		view2.add(fdv);
		view2.setOriginAxisVisible(true);
		view2.setFixedAxisVisible(true);
		view2.fitAll();		
		view.setNavigationMaster(view2);
		splitPane.add(view2, JSplitPane.LEFT);
		f.setVisible(true);
		splitPane.setDividerLocation(0.5);
		fdv.domainsChanged(fdv.getDomainProvider().getDomainIDs());

		//Test view can be removed and added.
		view.remove(fdv);
		view.add(fdv);
		view.add(fdv);
		view.setOrientation(View.LEFT);
		
		fdv.setPlatePicking(false);
		fdv.setWirePicking(false);
		//fdv.setPlatePicking(true);
		//fdv.setWirePicking(true);
		fdv.setMarkPicking(true);
		//take a snaphshot of the view
		Thread.sleep(1000);
		view.takeScreenshot(new ScreenshotListener()
		{
			public void shot(BufferedImage snapshot)
			{
				try
				{
					ImageIO.write(snapshot, "png",
						File.createTempFile("jcae-viewer3d-snap",".png"));
				} catch (IOException e)
				{
					e.printStackTrace();
				}		
			}
		});
	}
	
	public static void main(String[] args)
	{
		try
		{						
			//testCAD();
			testUNV();
			//testAmibe();
			//testOffscreen();
			//testFD();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
