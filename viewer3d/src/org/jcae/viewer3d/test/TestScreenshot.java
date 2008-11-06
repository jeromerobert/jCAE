package org.jcae.viewer3d.test;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.jcae.viewer3d.ScreenshotListener;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.post.Legend;

public class TestScreenshot extends JFrame
	implements KeyListener, ScreenshotListener
{
	private Legend legend=new Legend();
	private View view=new View(this);	

	/** Create frame with a view and a legend */
	public TestScreenshot()
	{
		setSize(800,600);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		view.setOriginAxisVisible(true);
		view.setFixedAxisVisible(true);
		getContentPane().add(view, BorderLayout.CENTER);
		getContentPane().add(legend, BorderLayout.WEST);
		view.addKeyListener(this);		
	}

	public static void main(String[] args)
	{
		TestScreenshot t=new TestScreenshot();
		t.setVisible(true);	
	}
	
	public void keyTyped(KeyEvent e)
	{
	}
	
	public void keyPressed(KeyEvent e)
	{
		System.out.println(e);
		if(e.getKeyChar()=='s')
		{
			view.takeScreenshot(this);
		}
	}

	public void keyReleased(KeyEvent e)
	{
	}
	
	
	public void shot(BufferedImage snapshot)
	{
		try
		{
			int width=legend.getWidth()+view.getWidth();
			int height=view.getHeight();
			
			BufferedImage screenshot = new BufferedImage(
				width, height, BufferedImage.TYPE_INT_RGB);
			
			BufferedImage legendIm = screenshot.getSubimage(
				0, 0, legend.getWidth(), height);
			
			BufferedImage viewIm = screenshot.getSubimage(				
				legend.getWidth(), 0, view.getWidth(), height);
			
			legend.paint(legendIm.createGraphics());
			System.out.println("take view snapshot");											
			System.out.println("take view snapshot finished");
			
			viewIm.createGraphics().drawRenderedImage(
				snapshot, new AffineTransform());
			
			ImageIO.write(screenshot, "png",
				File.createTempFile("jcae-viewer3d-snap",".png"));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
