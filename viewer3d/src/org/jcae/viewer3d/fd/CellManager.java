package org.jcae.viewer3d.fd;

import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.vecmath.Point3d;

/**
 * Keep track of selected cell and of geometry overlay used to represent
 * the selection.
 * The main algorithmic task of this class is to convert floating coordinates
 * to a cell id.
 * @author Jerome Robert
 */
public class CellManager
{
	private FDProvider provider;

	/**
	 * @param provider a FDProvider used to get FD grid
	 */
	public CellManager(FDProvider provider)
	{
		this.provider=provider;
	}

	/**
	 * return the current selection. 1 FDSelection for each Domain.
	 */
	public FDSelection[] getSelection()
	{
		return new FDSelection[0];
	}

	/**
	 * @param a FDDomain.XX_SLOT value
	 * @param domainId
	 * @param value The ID of the slot ownin the selected cell
	 * @param cds The coordinates of the selected slot
	 * @param point The coordinates of the picked point
	 * @return a LineArray representing the selected Cell.
	 */
	public LineArray selectSlot(byte type, int domainId, int value, Point3d[] cds, Point3d point)
	{
		LineArray la=new LineArray(cds.length, GeometryArray.COORDINATES);
		la.setCoordinates(0, cds);
		return la;
	}

	/**
	 * @param domainId
	 * @param value The ID of the wire owning the selected cell
	 * @param cds The coordinates of the selected slot
	 * @param point The coordinates of the picked point
	 * @return a LineArray representing the selected Cell.
	 */
	public Geometry selectXWire(int domainId, int value, Point3d[] cds, Point3d point)
	{
		LineArray la=new LineArray(cds.length, GeometryArray.COORDINATES);
		la.setCoordinates(0, cds);
		return la;
	}

	/**
	 * @param domainId
	 * @param value The ID of the wire owning the selected cell
	 * @param cds The coordinates of the selected slot
	 * @param point The coordinates of the picked point
	 * @return a LineArray representing the selected Cell.
	 */
	public Geometry selectYWire(int domainId, int value, Point3d[] cds, Point3d point)
	{
		LineArray la=new LineArray(cds.length, GeometryArray.COORDINATES);
		la.setCoordinates(0, cds);
		return la;
	}

	/**
	 * @param domainId
	 * @param value The ID of the wire owning the selected cell
	 * @param cds The coordinates of the selected slot
	 * @param point The coordinates of the picked point
	 * @return a LineArray representing the selected Cell.
	 */
	public Geometry selectZWire(int domainId, int value, Point3d[] cds, Point3d point)
	{
		LineArray la=new LineArray(cds.length, GeometryArray.COORDINATES);
		la.setCoordinates(0, cds);
		return la;
	}

	/**
	 * @param a FDDomain.XX_SLOT value
	 * @param domainId
	 * @param value The ID of the wire owning the selected cell
	 * @param cds The coordinates of the selected slot
	 * @param point The coordinates of the picked point
	 * @return The LineArray previously returned by selectSlot
	 */
	public LineArray unselectSlot(byte type, int domainId, int value, Point3d[] cds, Point3d point)
	{
		return null;
	}

	/**
	 * @param domainId
	 * @param value The ID of the wire owning the selected cell
	 * @param cds The coordinates of the selected wire
	 * @param point The coordinates of the picked point
	 * @return a LineArray representing the selected Cell.
	 */
	public Geometry unselectXWire(int domainId, int value, Point3d[] cds, Point3d point)
	{
		return null;
	}

	/**
	 * @param domainId
	 * @param value The ID of the wire owning the selected cell
	 * @param cds The coordinates of the selected wire
	 * @param point The coordinates of the picked point
	 * @return a LineArray representing the selected Cell.
	 */
	public Geometry unselectYWire(int domainId, int value, Point3d[] cds, Point3d point)
	{
		return null;
	}

	/**
	 * @param domainId
	 * @param value The ID of the wire owning the selected cell
	 * @param cds The coordinates of the selected wire
	 * @param point The coordinates of the picked point
	 * @return a LineArray representing the selected Cell.
	 */
	public Geometry unselectZWire(int domainId, int value, Point3d[] cds, Point3d point)
	{
		return null;
	}
}
