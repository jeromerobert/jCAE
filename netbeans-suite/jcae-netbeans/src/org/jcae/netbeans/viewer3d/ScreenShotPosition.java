package org.jcae.netbeans.viewer3d;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.media.j3d.Transform3D;
import javax.swing.ImageIcon;
import org.jcae.viewer3d.ScreenshotListener;
import org.jcae.viewer3d.View;

public class ScreenShotPosition implements ScreenshotListener {
	Transform3D position;
	ImageIcon snapShot;
	View view;
	final static int height=100;
	final static int width=100;
	
	public ScreenShotPosition(View view,Transform3D position){
		this.view=view;
		this.position=position;
	}
	
	public Transform3D getPosition() {
		return position;
	}
	public ImageIcon getSnapShot() {
		return snapShot;
	}

	public void shot(BufferedImage aSnapShot) {
		int h=view.getHeight();
		int w=view.getWidth();

		BufferedImage buffer=new BufferedImage(
				width, height, BufferedImage.TYPE_INT_RGB);
		double scaleH=((double)height)/h;
		double scaleW=((double)width)/w;
		double scale=Math.min(scaleH,scaleW);
		
		//scale transform to get always the same image size
		AffineTransform s=new AffineTransform();
		s.scale(scale,scale);
		//translate transform to center the image
		AffineTransform t=new AffineTransform();
		t.setToTranslation(((double)(width-scale*w))/2,((double)(height-scale*h))/2);
		t.concatenate(s);
		
		Graphics2D g2D=buffer.createGraphics();
		g2D.setBackground(view.getBackground());
		g2D.drawRenderedImage(aSnapShot,t);
		snapShot=new ImageIcon(buffer);
	}
	
	public void shot(){
		view.takeScreenshot(this);
	}
}
