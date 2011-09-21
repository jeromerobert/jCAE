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
import javax.vecmath.Vector4d;
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
	private final Vector4d[] planes = new Vector4d[6];
	private final Point3d frustumLower = new Point3d();
	private final Point3d frustumUpper = new Point3d();
	private final boolean oneCell;
	private final RayPicker rayPicker;
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
		oneCell = false;
		this.firstPoint = firstPoint;
		this.secondPoint = secondPoint;
		canvas.lock();
		double[] verts = Utils.computeVerticesFrustum(firstPoint.x, firstPoint.y,
			secondPoint.x, secondPoint.y, canvas.GetRenderer());
		canvas.unlock();
		computePlanes(verts);
		rayPicker = null;
	}

	/**
	 * Sometimes picked objects are very small and selection
	 * by a ray picker would be uncomfortable.  This constructor transforms
	 * a RayPicker object into a FrustumPicker object by enlarging pick point
	 * by the desired number of pixels in each direction.
	 * The create FrustrumPicker and the RayPicker share the same selected node
	 * list. That is adding a node to the FrustrumPicker will make it available
	 * to the underlying RayPicker.
	 * 
	 * @param picker picker to enlarge
	 * @param tolerance  pixel tolerance
	 */
	public FrustumPicker(RayPicker picker, int tolerance)
	{
		super(picker.getCanvas(), picker.onlyVisible());
		selectionNode = picker.selectionNode;
		oneCell = true;
		int [] pickPosition = picker.getPressPosition();
		this.firstPoint = new Point(pickPosition[0] - tolerance, pickPosition[1] - tolerance);
		this.secondPoint = new Point(pickPosition[0] + tolerance, pickPosition[1] + tolerance);
		Canvas canvas = picker.getCanvas();
		canvas.lock();
		double[] verts = Utils.computeVerticesFrustum(firstPoint.x, firstPoint.y,
			secondPoint.x, secondPoint.y, canvas.GetRenderer());
		canvas.unlock();
		computePlanes(verts);
		rayPicker = picker;
	}

	/**
	 * Return the ray picker used to create this frustrum picker or null if
	 * this frustrum picker as been created without ray picker
	 */
	public RayPicker getRayPicker()
	{
		return rayPicker;
	}

	private final void computePlanes(double[] verts)
	{
		BoundingPolytope frustum = Utils.computePolytope(verts);
		for (int i = 0; i < planes.length; i++)
			planes[i] = new Vector4d();
		frustum.getPlanes(planes);
		frustumLower.x = verts[0];
		frustumLower.y = verts[1];
		frustumLower.z = verts[2];
		for (int i = 1; i < 8; i++)
		{
			if (frustumLower.x > verts[4*i])
				frustumLower.x = verts[4*i];
			if (frustumLower.y > verts[4*i+1])
				frustumLower.y = verts[4*i+1];
			if (frustumLower.z > verts[4*i+2])
				frustumLower.z = verts[4*i+2];
		}
		frustumUpper.x = verts[0];
		frustumUpper.y = verts[1];
		frustumUpper.z = verts[2];
		for (int i = 1; i < 8; i++)
		{
			if (frustumUpper.x < verts[4*i])
				frustumUpper.x = verts[4*i];
			if (frustumUpper.y < verts[4*i+1])
				frustumUpper.y = verts[4*i+1];
			if (frustumUpper.z < verts[4*i+2])
				frustumUpper.z = verts[4*i+2];
		}
	}

	/**
	 * Get the mouse position when button was pressed.
	 * 
	 * @return the position of mouse when button was pressed
	 */
	@Override
	public final int[] getPressPosition()
	{
		return new int[] { firstPoint.x, firstPoint.y };
	}

	/**
	 * Get the mouse position when button was released.
	 * 
	 * @return Get the position of mouse when button was released
	 */
	@Override
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
	@Override
	public boolean intersect(BoundingBox bbox)
	{
		Point3d lower = new Point3d();
		Point3d upper = new Point3d();
		bbox.getLower(lower);
		bbox.getUpper(upper);
		// Fast intersection test with bounding boxes
		if (upper.x < frustumLower.x || lower.x > frustumUpper.x ||
		    upper.y < frustumLower.y || lower.y > frustumUpper.y ||
		    upper.z < frustumLower.z || lower.z > frustumUpper.z)
			return false;

		// Precise intersection test with frustum planes
		double [] x = new double[24];
		double [] extents = new double[] {
			lower.x, upper.x,
			lower.y, upper.y,
			lower.z, upper.z
		};
		int cnt = 0;
		for (int i = 0; i < 2; i++)
			for (int j = 2; j < 4; j++)
				for (int k = 4; k < 6; k++)
				{
					x[cnt++] = extents[i];
					x[cnt++] = extents[j];
					x[cnt++] = extents[k];
				}

		// ignore front and back planes
		for (int j = 0; j < 4; j++)
		{
			Vector4d p = planes[j];
			boolean in = false;
			for (int i = 0; i < 8; i++)
			{
				if (p.x * x[3*i] + p.y * x[3*i+1] + p.z * x[3*i+2] + p.w < 0.0)
				{
					in = true;
					break;
				}
			}
			if (!in)
				return false;
		}
		return true;
	}

	@Override
	public boolean isOneCell()
	{
		return oneCell;
	}
}
