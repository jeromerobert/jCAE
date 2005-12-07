package org.jcae.viewer3d.fd;

import java.util.*;
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
	private Map selMap=new HashMap();

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
		return (FDSelection[])selMap.values().toArray(new FDSelection[selMap.size()]);
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
		switch(type){
		case FDDomain.XY_SLOT:
		case FDDomain.XZ_SLOT:
			return selectXPlaneSlot(type,domainId,value,cds,point); 
		case FDDomain.YX_SLOT:
		case FDDomain.YZ_SLOT:
			return selectYPlaneSlot(type,domainId,value,cds,point);
		case FDDomain.ZX_SLOT:
		case FDDomain.ZY_SLOT:
			return selectZPlaneSlot(type,domainId,value,cds,point);
		default : return null;
		}
	}
	
	private LineArray selectXPlaneSlot(byte type, int domainId, int value, Point3d[] cds, Point3d point)
	{
//		Compute the cell number
		GridUtil aGridUtil=new GridUtil(provider);
		double p=point.x;
		double start=Math.min(cds[0].x,cds[1].x);
		int pIndex=aGridUtil.getXCellIndex(p);
		int startIndex=aGridUtil.getXCellIndex(start);
		
		int cell=pIndex-startIndex+1;
		FDSelection aFDSelection=getSelectedDomain(domainId);
		if(isSlotCellSelected(aFDSelection,type,value,cell)) return null;
		addSlotCell2Selection(aFDSelection,type,value,cell);
		
		//Compute the selection line arrays
		Point3d[] selection = new Point3d[2];
		selection[0]=new Point3d(cds[0]);
		selection[0].x=provider.getXGrid(pIndex);
		selection[1]=new Point3d(cds[1]);
		selection[1].x=provider.getXGrid(pIndex+1);
		
		
		LineArray la=new LineArray(selection.length, GeometryArray.COORDINATES);
		la.setCoordinates(0,selection);
		return la;
	}
	
	private LineArray selectYPlaneSlot(byte type, int domainId, int value, Point3d[] cds, Point3d point)
	{
//		Compute the cell number
		GridUtil aGridUtil=new GridUtil(provider);
		double p=point.y;
		double start=Math.min(cds[0].y,cds[1].y);
		int pIndex=aGridUtil.getYCellIndex(p);
		int startIndex=aGridUtil.getYCellIndex(start);
		
		int cell=pIndex-startIndex+1;
		FDSelection aFDSelection=getSelectedDomain(domainId);
		if(isSlotCellSelected(aFDSelection,type,value,cell)) return null;
		addSlotCell2Selection(aFDSelection,type,value,cell);
		
		//Compute the selection line arrays
		Point3d[] selection = new Point3d[2];
		selection[0]=new Point3d(cds[0]);
		selection[0].y=provider.getYGrid(pIndex);
		selection[1]=new Point3d(cds[1]);
		selection[1].y=provider.getYGrid(pIndex+1);
		
		
		LineArray la=new LineArray(selection.length, GeometryArray.COORDINATES);
		la.setCoordinates(0,selection);
		return la;
	}
	
	private LineArray selectZPlaneSlot(byte type, int domainId, int value, Point3d[] cds, Point3d point)
	{
//		Compute the cell number
		GridUtil aGridUtil=new GridUtil(provider);
		double p=point.z;
		double start=Math.min(cds[0].z,cds[1].z);
		int pIndex=aGridUtil.getZCellIndex(p);
		int startIndex=aGridUtil.getZCellIndex(start);
		
		int cell=pIndex-startIndex+1;
		FDSelection aFDSelection=getSelectedDomain(domainId);
		if(isSlotCellSelected(aFDSelection,type,value,cell)) return null;
		addSlotCell2Selection(aFDSelection,type,value,cell);
		
		//Compute the selection line arrays
		Point3d[] selection = new Point3d[2];
		selection[0]=new Point3d(cds[0]);
		selection[0].z=provider.getZGrid(pIndex);
		selection[1]=new Point3d(cds[1]);
		selection[1].z=provider.getZGrid(pIndex+1);
		
		
		LineArray la=new LineArray(selection.length, GeometryArray.COORDINATES);
		la.setCoordinates(0,selection);
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
		//Compute the cell number
		GridUtil aGridUtil=new GridUtil(provider);
		double p=point.x;
		double start=Math.min(cds[0].x,cds[1].x);
		int pIndex=aGridUtil.getXCellIndex(p);
		int startIndex=aGridUtil.getXCellIndex(start);
		
		int cell=pIndex-startIndex+1;
		FDSelection aFDSelection=getSelectedDomain(domainId);
		if(isXWireCellSelected(aFDSelection,value,cell)) return null;
		addXWireCell2Selection(aFDSelection,value,cell);
		
		//Compute the selection line arrays
		Point3d[] selection = new Point3d[2];
		selection[0]=new Point3d(cds[0]);
		selection[0].x=provider.getXGrid(pIndex);
		selection[1]=new Point3d(cds[1]);
		selection[1].x=provider.getXGrid(pIndex+1);
		
		
		LineArray la=new LineArray(selection.length, GeometryArray.COORDINATES);
		la.setCoordinates(0,selection);
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
//		Compute the cell number
		GridUtil aGridUtil=new GridUtil(provider);
		double p=point.y;
		double start=Math.min(cds[0].y,cds[1].y);
		int pIndex=aGridUtil.getYCellIndex(p);
		int startIndex=aGridUtil.getYCellIndex(start);
		
		int cell=pIndex-startIndex+1;
		FDSelection aFDSelection=getSelectedDomain(domainId);
		if(isYWireCellSelected(aFDSelection,value,cell)) return null;
		addYWireCell2Selection(aFDSelection,value,cell);
		
		//Compute the selection line arrays
		Point3d[] selection = new Point3d[2];
		selection[0]=new Point3d(cds[0]);
		selection[0].y=provider.getYGrid(pIndex);
		selection[1]=new Point3d(cds[1]);
		selection[1].y=provider.getYGrid(pIndex+1);
		
		
		LineArray la=new LineArray(selection.length, GeometryArray.COORDINATES);
		la.setCoordinates(0,selection);
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
//		Compute the cell number
		GridUtil aGridUtil=new GridUtil(provider);
		double p=point.z;
		double start=Math.min(cds[0].z,cds[1].z);
		int pIndex=aGridUtil.getZCellIndex(p);
		int startIndex=aGridUtil.getZCellIndex(start);
		
		int cell=pIndex-startIndex+1;
		FDSelection aFDSelection=getSelectedDomain(domainId);
		if(isZWireCellSelected(aFDSelection,value,cell)) return null;
		addZWireCell2Selection(aFDSelection,value,cell);
		
		//Compute the selection line arrays
		Point3d[] selection = new Point3d[2];
		selection[0]=new Point3d(cds[0]);
		selection[0].z=provider.getZGrid(pIndex);
		selection[1]=new Point3d(cds[1]);
		selection[1].z=provider.getZGrid(pIndex+1);
		
		
		LineArray la=new LineArray(selection.length, GeometryArray.COORDINATES);
		la.setCoordinates(0,selection);
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

	public void unselectAll(){
		selMap.clear();
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
	
	private FDSelection getSelectedDomain(int domainId){
		Integer key=new Integer(domainId);
		if(selMap.containsKey(key)) return (FDSelection)selMap.get(key);
		else {
			FDSelection aFDSelection=new FDSelection(domainId);
			selMap.put(key,aFDSelection);
			return aFDSelection;
		}
	}
	
	private boolean isXWireCellSelected(FDSelection aFDSelection,int value,int cell){
		int[] selectedCells=aFDSelection.getXWireCells();
		if(selectedCells==null) return false;
		for(int i=0;i<(selectedCells.length/2);i++) 
			if((selectedCells[2*i]==value)&(selectedCells[2*i+1]==cell)) return true;
		return false;
	}
	
	private boolean isYWireCellSelected(FDSelection aFDSelection,int value,int cell){
		int[] selectedCells=aFDSelection.getYWireCells();
		if(selectedCells==null) return false;
		for(int i=0;i<(selectedCells.length/2);i++) 
			if((selectedCells[2*i]==value)&(selectedCells[2*i+1]==cell)) return true;
		return false;
	}
	
	private boolean isZWireCellSelected(FDSelection aFDSelection,int value,int cell){
		int[] selectedCells=aFDSelection.getZWireCells();
		if(selectedCells==null) return false;
		for(int i=0;i<(selectedCells.length/2);i++) 
			if((selectedCells[2*i]==value)&(selectedCells[2*i+1]==cell)) return true;
		return false;
	}
	
	private void addXWireCell2Selection(FDSelection aFDSelection,int value,int cell){
		int[] selectedCells=aFDSelection.getXWireCells();
		if(selectedCells==null) selectedCells=new int[0];
		int[] buffer=new int[selectedCells.length+2];
		buffer[0]=value;
		buffer[1]=cell;
		System.arraycopy(selectedCells,0,buffer,2,selectedCells.length);
		aFDSelection.setXWireCells(buffer);
	}
	
	private void addYWireCell2Selection(FDSelection aFDSelection,int value,int cell){
		int[] selectedCells=aFDSelection.getYWireCells();
		if(selectedCells==null) selectedCells=new int[0];
		int[] buffer=new int[selectedCells.length+2];
		buffer[0]=value;
		buffer[1]=cell;
		System.arraycopy(selectedCells,0,buffer,2,selectedCells.length);
		aFDSelection.setYWireCells(buffer);
	}
	
	private void addZWireCell2Selection(FDSelection aFDSelection,int value,int cell){
		int[] selectedCells=aFDSelection.getZWireCells();
		if(selectedCells==null) selectedCells=new int[0];
		int[] buffer=new int[selectedCells.length+2];
		buffer[0]=value;
		buffer[1]=cell;
		System.arraycopy(selectedCells,0,buffer,2,selectedCells.length);
		aFDSelection.setZWireCells(buffer);
	}
	
	private boolean isSlotCellSelected(FDSelection aFDSelection,byte type, int value,int cell){
		int[] selectedCells=aFDSelection.getSlotCells(type);
		if(selectedCells==null) return false;
		for(int i=0;i<(selectedCells.length/2);i++) 
			if((selectedCells[2*i]==value)&(selectedCells[2*i+1]==cell)) return true;
		return false;
	}
	
	private void addSlotCell2Selection(FDSelection aFDSelection,byte type, int value,int cell){
		int[] selectedCells=aFDSelection.getSlotCells(type);
		if(selectedCells==null) selectedCells=new int[0];
		int[] buffer=new int[selectedCells.length+2];
		buffer[0]=value;
		buffer[1]=cell;
		System.arraycopy(selectedCells,0,buffer,2,selectedCells.length);
		aFDSelection.setSlotCells(type,buffer);
	}
}
