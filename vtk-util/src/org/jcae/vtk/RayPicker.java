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
package org.jcae.vtk;

import java.awt.Point;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import org.jcae.geometry.BoundingBox;

/**
 * The RayPicker object defines a picker for point picking.
 * 
 * @author Denis Barbier
 */
public class RayPicker extends PickContext
{
	private final Point pickPoint;
	private final Point3d rayOrigin;
	private final Vector3d rayDirection;

	/**
	 * Constructor.
	 * 
	 * @param canvas  underlying canvas
	 * @param visible  if <code>true</code>, only visible objects are picked,
	 * otherwise any pickable object can be picked
	 * @param pickPoint   mouse position when button was pressed
	 */
	public RayPicker(Canvas canvas, boolean visible, Point pickPoint)
	{
		super(canvas, visible);
		this.pickPoint = pickPoint;
		rayOrigin = new Point3d();
		rayDirection = new Vector3d();

		canvas.lock();
		Utils.computeRay(canvas.GetRenderer(), this.pickPoint, rayOrigin, rayDirection);
		canvas.unlock();
	}

	/**
	 * Get the ray origin.
	 * 
	 * @returnthe ray origin
	 */
	public Point3d getRayOrigin()
	{
		return rayOrigin;
	}

	/**
	 * Get the ray direction.
	 * 
	 * @return the ray direction
	 */
	public Vector3d getRayDirection()
	{
		return rayDirection;
	}

	/**
	 * Get the mouse position when button was pressed.
	 * 
	 * @return the position of mouse when button was pressed
	 */
	public final int[] getPressPosition()
	{
		return new int[] { pickPoint.x, pickPoint.y };
	}

	/**
	 * Get the mouse position when button was released.
	 * 
	 * @return Get the position of mouse when button was released
	 */
	public final int[] getReleasePosition()
	{
		return getPressPosition();
	}

	/**
	 * Tell whether a box intersects this picker.
	 * 
	 * @param bbox  box to be checked for
	 * @return <code>true</code> if this box is intersected by current picker,
	 * <code>false</code> otherwise.
	 */
	public boolean intersect(BoundingBox bbox)
	{
		return bbox.intersect(rayOrigin, rayDirection);
	}

	public Point3d getIntersect(BoundingBox bbox) {
		Point3d p=new Point3d();
		bbox.intersect(rayOrigin, rayDirection,p);
		return p;
	}
}
