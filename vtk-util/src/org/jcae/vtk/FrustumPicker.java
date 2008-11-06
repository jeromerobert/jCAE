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
import org.jcae.geometry.BoundingBox;
import org.jcae.geometry.BoundingPolytope;

/**
 * The FrustumPicker object defines a picker for rubber band selection.
 * 
 * @author Denis Barbier
 */
public class FrustumPicker extends PickContext
{
	private final Point firstPoint;
	private final Point secondPoint;
	private BoundingPolytope frustum;

	/**
	 * Constructor.
	 * 
	 * @param canvas  underlying canvas
	 * @param visible  if <code>true</code>, only visible objects are picked,
	 * otherwise any pickable object can be picked
	 * @param firstPoint   mouse position when button was pressed
	 * @param secondPoint  mouse position when button was released
	 */
	public FrustumPicker(Canvas canvas, boolean visible,
		Point firstPoint, Point secondPoint)
	{
		super(canvas, visible);
		this.firstPoint = firstPoint;
		this.secondPoint = secondPoint;
		canvas.lock();
		double[] verts = Utils.computeVerticesFrustum(firstPoint.x, firstPoint.y,
			secondPoint.x, secondPoint.y, canvas.GetRenderer());
		canvas.unlock();
		frustum = Utils.computePolytope(verts);
	}

	/**
	 * Constructor.  Sometimes picked objects are very small and selection
	 * by a ray picker would be uncomfortable.  This constructor transforms
	 * a RayPicker object into a FrustumPicker object by enlarging pick point
	 * by the desired number of pixels in each direction.
	 * 
	 * @param picker picker to enlarge
	 * @param tolerance  pixel tolerance
	 */
	public FrustumPicker(RayPicker picker, int tolerance)
	{
		super(picker.getCanvas(), picker.onlyVisible());
		int [] pickPosition = picker.getPressPosition();
		this.firstPoint = new Point(pickPosition[0] - tolerance, pickPosition[1] - tolerance);
		this.secondPoint = new Point(pickPosition[0] + tolerance, pickPosition[1] + tolerance);
		Canvas canvas = picker.getCanvas();
		canvas.lock();
		double[] verts = Utils.computeVerticesFrustum(firstPoint.x, firstPoint.y,
			secondPoint.x, secondPoint.y, canvas.GetRenderer());
		canvas.unlock();
		frustum = Utils.computePolytope(verts);
	}

	/**
	 * Get the mouse position when button was pressed.
	 * 
	 * @return the position of mouse when button was pressed
	 */
	public final int[] getPressPosition()
	{
		return new int[] { firstPoint.x, firstPoint.y };
	}

	/**
	 * Get the mouse position when button was released.
	 * 
	 * @return Get the position of mouse when button was released
	 */
	public final int[] getReleasePosition()
	{
		return new int[] { secondPoint.x, secondPoint.y };
	}

	/**
	 * Tell whether a box intersects this picker.
	 * 
	 * @param bbox  box to be checked for
	 * @return <code>true</code> if this box is intersected by the frustum,
	 * <code>false</code> otherwise.
	 */
	public boolean intersect(BoundingBox bbox)
	{
		return frustum.intersect(bbox);
	}
}
