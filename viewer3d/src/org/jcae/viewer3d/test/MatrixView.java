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
 * (C) Copyright 2008, by EADS France
 */
package org.jcae.viewer3d.test;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;
import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Node;
import javax.media.j3d.PointArray;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector4d;
import org.jcae.viewer3d.DomainProvider;
import org.jcae.viewer3d.PickViewable;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.ViewBehavior;
import org.jcae.viewer3d.Viewable;
import org.jcae.viewer3d.ViewableAdaptor;

/**
 *
 * @author ibarz
 */
public class MatrixView implements KeyListener
{

	private static class ViewMatrix extends View
	{

		ViewMatrix(JFrame frame)
		{
			super(frame);
		}

		public Viewable drawPoint(final double x, final double y, final double z)
		{
			Viewable toReturn = new ViewableAdaptor()
			{
				//TODO so many empty methods probably means that ViewableAdaptor sucks
				public void domainsChangedPerform(int[] domainId)
				{
				}

				public DomainProvider getDomainProvider()
				{
					return null;
				}

				public void setDomainVisible(Map<Integer, Boolean> map)
				{
				}

				public void pick(PickViewable result)
				{
				}

				public void unselectAll()
				{
				}

				public Node getJ3DNode()
				{
					ColoringAttributes ca = new ColoringAttributes();
					ca.setColor(new Color3f(Color.YELLOW));
					PointAttributes pat = new PointAttributes(5f, false);
					PointArray pa = new PointArray(1, GeometryArray.COORDINATES);
					pa.setCoordinates(0, new double[]
							{
								x, y, z
							});
					Appearance a = new Appearance();
					a.setPointAttributes(pat);
					a.setColoringAttributes(ca);
					Shape3D s3d = new Shape3D(pa, a);
					return s3d;
				}
			};
			add(toReturn);
			return toReturn;
		}
	}
	private static ViewMatrix view;

	public static void main(String[] args)
	{
		MatrixView test = new MatrixView();

		JFrame frame = new JFrame();
		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		view = new MatrixView.ViewMatrix(frame);

		//Fit the view to the geometry
		view.setOriginAxisVisible(true);
		view.drawPoint(0, 0, 0);
		view.drawPoint(2, 2, 2);
		view.addKeyListener(test);
		view.setMouseMode(ViewBehavior.RECTANGLE_MODE);
		//view.getView().setCompatibilityModeEnable(true);
		frame.getContentPane().add(view);
		/*		final ViewableCAD fcad=				
		new ViewableCAD(new OCCProvider("/home/ibarz/models/axe.brep"));				
		view.add(fcad);*/
		view.fitAll();
		//view.setFrontClipDistance(1.);

		frame.setVisible(true);
	}

	public void keyTyped(KeyEvent e)
	{
	}

	public void keyPressed(KeyEvent e)
	{
	}

	public void testProjection()
	{
		Transform3D leftProjection = new Transform3D();
		Transform3D rightProjection = new Transform3D();
		
		double n = view.getFrontClipDistance(), f = view.getBackClipDistance();
		double a = (f*n)/(f-n);
		double b = (f+n)/(2*(f-n)) + .5;

		System.out.println("n : " + n);
		System.out.println("f : " + f);
		
		view.getVworldProjection(leftProjection, rightProjection);

		System.out.println("left : " + leftProjection);
		System.out.println("right : " + rightProjection);

		Vector4d vec = new Vector4d(0., 0., 0., 1.);
		leftProjection.transform(vec);

		System.out.println("vec : " + vec);

		vec.scale(1. / vec.getW());

		System.out.println("vec scaled : " + vec);

		Point3d point = new Point3d();

		view.normalizedEyeCoordinateToViewportCoordinate(vec, point);

		float[] zbuffer = view.getDepthBuffer((int)point.x, (int)point.y, 1, 1);
		
		System.out.println("point : " + point);

		System.out.println("ZBUFFER : " + zbuffer[0]);
		System.out.println("Z : " + point.z);
		
		double ZEyeBuffer = a / (zbuffer[0] - b);	
		double ZEyeCenter = a / (point.z - b);
		
		System.out.println("ZBUFFER : " + ZEyeBuffer);
		System.out.println("Z : " + ZEyeCenter);
	}

	public void keyReleased(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_A:
				System.out.println("PWET !");
				testProjection();
				break;
			case KeyEvent.VK_R:
				view.setMouseMode(ViewBehavior.RECTANGLE_MODE);
				break;
			case KeyEvent.VK_T:
				view.setMouseMode(ViewBehavior.DEFAULT_MODE);
				break;
		}
	}
}
