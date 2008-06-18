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

package org.jcae.vtk.test;

import javax.vecmath.Point3d;
import javax.vecmath.Vector4d;
import org.jcae.geometry.BoundingPolytope;
import org.jcae.vtk.Utils;

/**
 *
 * @author ibarz
 */
public class TestPolytope {
	public static void main(String[] args) {
		BoundingPolytope frustum = new BoundingPolytope();
		Vector4d[] planes = new Vector4d[] {
			new Vector4d(1.,0.,0.,-1.),
			new Vector4d(-1.,0.,0.,-1.),
			new Vector4d(0.,1.,0.,-1.),
			new Vector4d(0,-1.,0.,-1.)
		};
		frustum.setPlanes(planes);
		
		System.out.println("TRUE : " + frustum.intersect(new Point3d(0., 0., 0.)));
		System.out.println("TRUE : " + frustum.intersect(new Point3d(0., .5, 0.)));
		System.out.println("FALSE : " + frustum.intersect(new Point3d(-2., 0., 0.)));
		System.out.println("FALSE : " + frustum.intersect(new Point3d(2., 0., 0.)));
		System.out.println("FALSE : " + frustum.intersect(new Point3d(.5, 2., 0.)));
		
		frustum = Utils.computePolytope(new double[] {
			0.,-10.,10.,1.0,
			0.,-10.,-10.,1.0,
			0.,10.,10.,1.0,
			0.,10.,-10.,1.0,
			10.,-10.,10.,1.0,
			10.,-10.,-10.,1.0,
			10.,10.,10.,1.0,
			10.,10.,-10.,1.0,
		});
		System.out.println("TRUE : " + frustum.intersect(new Point3d(0., 0., 0.)));
		System.out.println("TRUE : " + frustum.intersect(new Point3d(0., 5, 0.)));
		System.out.println("TRUE : " + frustum.intersect(new Point3d(5., 5, 0.)));
		System.out.println("TRUE : " + frustum.intersect(new Point3d(0., 5, 5.)));
		System.out.println("TRUE : " + frustum.intersect(new Point3d(5., 5, 5.)));
		System.out.println("FALSE : " + frustum.intersect(new Point3d(-12., 0., 0.)));
		System.out.println("FALSE : " + frustum.intersect(new Point3d(12., 0., 0.)));
		System.out.println("FALSE : " + frustum.intersect(new Point3d(.5, 12., 0.)));
		System.out.println("FALSE : " + frustum.intersect(new Point3d(.5, 2., 12.)));
		System.out.println("FALSE : " + frustum.intersect(new Point3d(15, 12., 0.)));
		
	}
}
