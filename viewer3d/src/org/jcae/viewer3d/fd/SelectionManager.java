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
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.viewer3d.fd;

import java.util.*;
import java.util.logging.Logger;
import javax.media.j3d.Geometry;
import javax.media.j3d.LineArray;
import javax.media.j3d.PointArray;
import javax.media.j3d.QuadArray;

/**
 * @author Jerome Robert
 *
 */
public class SelectionManager
{
	private static class IntegerPair
	{
		private int first, second;
		/**
		 * @param first
		 * @param second
		 */
		public IntegerPair(int first, int second)
		{
			super();
			this.first = first;
			this.second = second;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj)
		{
			IntegerPair other=(IntegerPair) obj;
			return other.first==first && other.second==second;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode()
		{
			return first+second;
		}
		public int getFirst()
		{
			return first;
		}
		public int getSecond()
		{
			return second;
		}
	}
	
	/** Same as IntegerPair but with a byte field representing a type */
	private static class IntegerPairTyped
	{
		private int first, second;
		private byte type;
		/**
		 * @param first
		 * @param second
		 */
		public IntegerPairTyped(byte type, int first, int second)
		{
			this.first = first;
			this.second = second;
			this.type=type;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj)
		{
			IntegerPairTyped other=(IntegerPairTyped) obj;
			return other.first==first && other.second==second && other.type==type;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode()
		{
			return first+second+type;
		}
		public int getFirst()
		{
			return first;
		}
		public int getSecond()
		{
			return second;
		}
		
		public int getType()
		{
			return type;
		}
	}
	
	private static class Selection
	{
		private int domainID;
		private Collection xPlates=new HashSet();
		private Collection yPlates=new HashSet();
		private Collection zPlates=new HashSet();

		private Collection xWires=new HashSet();
		private Collection yWires=new HashSet();
		private Collection zWires=new HashSet();
		private Collection[] slots=new Collection[FDDomain.SLOT_LAST+1];
		private Collection solids=new HashSet();
		private Map marks=new HashMap();
		/**
		 * @param domainID
		 */
		public Selection(int domainID)
		{
			this.domainID=domainID;
			for(int i=0; i<slots.length; i++)
			{
				slots[i]=new HashSet();
			}
		}

		/**
		 * @param relativeID
		 * @return
		 */
		public boolean addXPlate(int relativeID)
		{
			return xPlates.add(new Integer(relativeID));
		}
		
		/**
		 * @param relativeID
		 * @return
		 */
		public boolean containsXPlate(int relativeID)
		{
			return xPlates.contains(new Integer(relativeID));
		}

		/**
		 * @param relativeID
		 * @return
		 */
		public boolean addYPlate(int relativeID)
		{
			return yPlates.add(new Integer(relativeID));
		}
		
		/**
		 * @param relativeID
		 * @return
		 */
		public boolean containsYPlate(int relativeID)
		{
			return yPlates.contains(new Integer(relativeID));
		}

		/**
		 * @param relativeID
		 * @return
		 */
		public boolean addZPlate(int relativeID)
		{
			return zPlates.add(new Integer(relativeID));
		}
		
		/**
		 * @param relativeID
		 * @return
		 */
		public boolean containsZPlate(int relativeID)
		{
			return zPlates.contains(new Integer(relativeID));
		}

		/**
		 * @return
		 */
		public FDSelection toFDSelection()
		{		
			return new FDSelection(domainID)
			{
				/* (non-Javadoc)
				 * @see org.jcae.viewer3d.fd.FDSelection#getXPlates()
				 */
				public int[] getXPlates()
				{
					return Utils.collectionToIntArray(xPlates);
				}
				
				/* (non-Javadoc)
				 * @see org.jcae.viewer3d.fd.FDSelection#getYPlates()
				 */
				public int[] getYPlates()
				{
					return Utils.collectionToIntArray(yPlates);
				}
				
				/* (non-Javadoc)
				 * @see org.jcae.viewer3d.fd.FDSelection#getZPlates()
				 */
				public int[] getZPlates()
				{
					return Utils.collectionToIntArray(zPlates);
				}
				
				/* (non-Javadoc)
				 * @see org.jcae.viewer3d.fd.FDSelection#getXWires()
				 */
				public int[] getXWires()
				{
					return Utils.collectionToIntArray(xWires);
				}

				/* (non-Javadoc)
				 * @see org.jcae.viewer3d.fd.FDSelection#getXWires()
				 */
				public int[] getYWires()
				{
					return Utils.collectionToIntArray(yWires);
				}

				/* (non-Javadoc)
				 * @see org.jcae.viewer3d.fd.FDSelection#getXWires()
				 */
				public int[] getZWires()
				{
					return Utils.collectionToIntArray(zWires);
				}
				
				/* (non-Javadoc)
				 * @see org.jcae.viewer3d.fd.FDSelection#getSlots(byte)
				 */
				public int[] getSlots(byte type)
				{
					return Utils.collectionToIntArray(slots[type]);
				}
				
				public Map getMarks()
				{
					Map toReturn=new HashMap();
					Iterator it=marks.entrySet().iterator();
					while(it.hasNext())				
					{
						Map.Entry e=(Map.Entry)it.next();
						toReturn.put(e.getKey(), Utils.collectionToIntArray((Collection)e.getValue()));
					}				
					return toReturn;
				}
				
				public int[] getSolids()
				{
					return Utils.collectionToIntArray(solids);
				}
			};
		}

		/**
		 * @param relativeID
		 * @return
		 */
		public boolean removeXPlate(int relativeID)
		{
			return xPlates.remove(new Integer(relativeID));
		}

		/**
		 * @param relativeID
		 * @return
		 */
		public boolean removeYPlate(int relativeID)
		{
			return yPlates.remove(new Integer(relativeID));
		}

		/**
		 * @param relativeID
		 * @return
		 */
		public boolean removeZPlate(int relativeID)
		{
			return zPlates.remove(new Integer(relativeID));
		}

		/**
		 * @param value
		 */
		public boolean containsXWire(int value)
		{
			return xWires.contains(new Integer(value));
		}
		
		/**
		 * @param value
		 */
		public void addXWire(int value)
		{
			xWires.add(new Integer(value));
		}

		/**
		 * @param value
		 */
		public boolean containsZWire(int value)
		{
			return zWires.contains(new Integer(value));
		}
		
		/**
		 * @param value
		 */
		public void addZWire(int value)
		{
			zWires.add(new Integer(value));
		}

		/**
		 * @param value
		 */
		public boolean containsYWire(int value)
		{
			return yWires.contains(new Integer(value));
		}
		/**
		 * @param value
		 */
		public void addYWire(int value)
		{
			yWires.add(new Integer(value));
		}

		/**
		 * @param value
		 */
		public void removeXWire(int value)
		{
			xWires.remove(new Integer(value));
		}

		/**
		 * @param value
		 */
		public void removeYWire(int value)
		{
			yWires.remove(new Integer(value));
		}

		/**
		 * @param value
		 */
		public void removeZWire(int value)
		{
			zWires.remove(new Integer(value));
		}

		/**
		 * @param type
		 * @param value
		 */
		public void addSlot(byte type, int value)
		{
			slots[type].add(new Integer(value));
		}
		
		/**
		 * @param type
		 * @param value
		 */
		public boolean containsSlot(byte type, int value)
		{
			return slots[type].contains(new Integer(value));
		}

		/**
		 * @param type
		 * @param value
		 */
		public void removeSlot(byte type, int value)
		{
			slots[type].remove(new Integer(value));
		}

		/**
		 * @param typeId
		 * @param markID
		 */
		public boolean containsMark(int typeId, int markID)
		{
			Integer i=new Integer(typeId);			
			Collection c=(Collection) marks.get(new Integer(typeId));
			if(c==null)
				return false;
			return c.contains(new Integer(markID));
		}
		
		/**
		 * @param typeId
		 * @param markID
		 */
		public void addMark(int typeId, int markID)
		{
			Integer i=new Integer(typeId);			
			Collection c=(Collection) marks.get(i);
			if(c==null)
			{
				c=new ArrayList();
				marks.put(i,c);
			}
			c.add(new Integer(markID));
		}

		/**
		 * @param typeId
		 * @param markID
		 */
		public void removeMark(int typeId, int markID)
		{
			((Collection)marks.get(new Integer(typeId))).remove(new Integer(markID));
		}

		
		public boolean containsSolid(int solidID)
		{
			return solids.contains(new Integer(solidID));
		}
		
		public void addSolid(int solidID)
		{
			solids.add(new Integer(solidID));
		}

		public void removeSolid(int solidID)
		{
			solids.remove(new Integer(solidID));
		}				
	}
		
	private Map domainToSelection=new HashMap();
	private FDProvider provider;
	private Map plateIDToQuadArray=new HashMap();
	private Map xWireToLineArray=new HashMap();
	private Map yWireToLineArray=new HashMap();
	private Map zWireToLineArray=new HashMap();
	private Map slotToLineArray=new HashMap();
	private Map solidIDToQuadArray=new HashMap();
	private Map markToGeometryArray=new HashMap();
	
	/**
	 * 
	 */
	public SelectionManager(FDProvider provider)
	{
		this.provider=provider;
	}
	
	public FDSelection[] getSelection()
	{
		Collection ss = domainToSelection.values();
		FDSelection[] toReturn=new FDSelection[ss.size()];
		Iterator it=ss.iterator();
		int i=0;
		while(it.hasNext())
		{
			Selection sct=(Selection)it.next();
			toReturn[i++]=sct.toFDSelection();
		}
		return toReturn;
	}
	
	private Selection getSelection(int domainID)
	{
		Selection s=(Selection) domainToSelection.get(new Integer(domainID));
		if(s==null)
		{
			s=new Selection(domainID);
			domainToSelection.put(new Integer(domainID), s);
		}
		return s;
	}
	
	
	public boolean isPlateSelected(int plateID, int domainID)
	{
		FDDomain d=(FDDomain) provider.getDomain(domainID);
		Selection s=getSelection(domainID);
		int relativeID;
		boolean selected=false;
		if(plateID<d.getNumberOfXPlate())
		{		
			relativeID=plateID;
			selected=s.containsXPlate(relativeID);
		}
		else if(plateID<d.getNumberOfYPlate()+d.getNumberOfXPlate())
		{		
			relativeID=plateID-d.getNumberOfXPlate();
			selected=s.containsYPlate(relativeID);
		}
		else
		{		
			relativeID=plateID-d.getNumberOfXPlate()-d.getNumberOfYPlate();
			selected=s.containsZPlate(relativeID);
		}
		return selected;
	}
	
	public boolean selectPlate(int plateID, int domainID, QuadArray qa)
	{
		FDDomain d=(FDDomain) provider.getDomain(domainID);
		Selection s=getSelection(domainID);
		int relativeID;
		boolean added=false;
		if(plateID<d.getNumberOfXPlate())
		{		
			relativeID=plateID;
			added=s.addXPlate(relativeID);
		}
		else if(plateID<d.getNumberOfYPlate()+d.getNumberOfXPlate())
		{		
			relativeID=plateID-d.getNumberOfXPlate();
			added=s.addYPlate(relativeID);
		}
		else
		{		
			relativeID=plateID-d.getNumberOfXPlate()-d.getNumberOfYPlate();
			added=s.addZPlate(relativeID);
		}
		plateIDToQuadArray.put(new IntegerPair(domainID, plateID), qa);
		return added;
	}

	public boolean unselectPlate(int plateID, int domainID)
	{
		FDDomain d=(FDDomain) provider.getDomain(domainID);
		Selection s=(Selection) domainToSelection.get(new Integer(domainID));
		boolean removed=false;
		if(s!=null)
		{
			int relativeID;
			
			if(plateID<d.getNumberOfXPlate())
			{		
				relativeID=plateID;
				removed=s.removeXPlate(relativeID);
			}
			else if(plateID<d.getNumberOfYPlate()+d.getNumberOfXPlate())
			{		
				relativeID=plateID-d.getNumberOfXPlate();
				removed=s.removeYPlate(relativeID);
			}
			else
			{		
				relativeID=plateID-d.getNumberOfXPlate()-d.getNumberOfYPlate();
				removed=s.removeZPlate(relativeID);
			}
		}
		
		plateIDToQuadArray.remove(new IntegerPair(domainID, plateID));
		return removed;
	}
	
	
	/**
	 * 
	 */
	public void unselectAll()
	{
		plateIDToQuadArray.clear();
		xWireToLineArray.clear();
		yWireToLineArray.clear();
		zWireToLineArray.clear();
		domainToSelection.clear();
		markToGeometryArray.clear();
	}

	/**
	 * @param plateID
	 * @return
	 */
	public Geometry getGeometryForPlate(int domainID, int plateID)
	{
		return (Geometry) plateIDToQuadArray.get(
			new IntegerPair(domainID, plateID));
	}

	
	/**
	 * @param domainId
	 * @param value
	 * @param la
	 */
	public boolean isXWireSelected(int domainId, int value)
	{
		return getSelection(domainId).containsXWire(value);
	}
	/**
	 * @param domainId
	 * @param value
	 * @param la
	 */
	public void selectXWire(int domainId, int value, LineArray la)
	{
		getSelection(domainId).addXWire(value);
		xWireToLineArray.put(new IntegerPair(domainId, value), la);
	}

	/**
	 * @param domainId
	 * @param value
	 * @param la
	 */
	public boolean isYWireSelected(int domainId, int value)
	{
		return getSelection(domainId).containsYWire(value);
	}
	/**
	 * @param domainId
	 * @param value
	 * @param la
	 */
	public void selectYWire(int domainId, int value, LineArray la)
	{
		getSelection(domainId).addYWire(value);
		yWireToLineArray.put(new IntegerPair(domainId, value), la);
	}

	/**
	 * @param domainId
	 * @param value
	 * @param la
	 */
	public boolean isZWireSelected(int domainId, int value)
	{
		return getSelection(domainId).containsZWire(value);
	}
	/**
	 * @param domainId
	 * @param value
	 * @param la
	 */
	public void selectZWire(int domainId, int value, LineArray la)
	{
		getSelection(domainId).addZWire(value);
		zWireToLineArray.put(new IntegerPair(domainId, value), la);
	}

	/**
	 * @param domainId
	 * @param value
	 */
	public void unselectXWire(int domainId, int value)
	{
		getSelection(domainId).removeXWire(value);
		xWireToLineArray.remove(new IntegerPair(domainId, value));
	}

	/**
	 * @param value
	 * @return
	 */
	public Geometry getGeometryForXWire(int domainId, int value)
	{
		return (Geometry) xWireToLineArray.get(new IntegerPair(domainId, value));
	}

	/**
	 * @param domainId
	 * @param value
	 */
	public void unselectYWire(int domainId, int value)
	{
		getSelection(domainId).removeYWire(value);
		yWireToLineArray.remove(new IntegerPair(domainId, value));
	}

	/**
	 * @param value
	 * @return
	 */
	public Geometry getGeometryForYWire(int domainId, int value)
	{
		return (Geometry) yWireToLineArray.get(new IntegerPair(domainId, value));
	}

	/**
	 * @param domainId
	 * @param value
	 */
	public void unselectZWire(int domainId, int value)
	{
		getSelection(domainId).removeZWire(value);
		zWireToLineArray.remove(new IntegerPair(domainId, value));
	}

	/**
	 * @param value
	 * @return
	 */
	public Geometry getGeometryForZWire(int domainId, int value)
	{
		return (Geometry) zWireToLineArray.get(new IntegerPair(domainId, value));
	}

	
	/**
	 * @param type
	 * @param domainId
	 * @param value
	 * @param la
	 */
	public boolean isSlotSelected(byte type, int domainId, int value)
	{
		return getSelection(domainId).containsSlot(type, value);
	}
	/**
	 * @param type
	 * @param domainId
	 * @param value
	 * @param la
	 */
	public void selectSlot(byte type, int domainId, int value, LineArray la)
	{
		Logger.global.finest("type="+type+" domainId="+domainId+" value="+value);
		getSelection(domainId).addSlot(type, value);
		slotToLineArray.put(new IntegerPairTyped(type, domainId, value), la);
	}

	/**
	 * @param type
	 * @param domainId
	 * @param value
	 */
	public void unselectSlot(byte type, int domainId, int value)
	{
		getSelection(domainId).removeSlot(type, value);
		slotToLineArray.remove(new IntegerPairTyped(type, domainId, value));
	}

	/**
	 * @param type
	 * @param domainId
	 * @param value
	 * @return
	 */
	public Geometry getGeometryForSlot(byte type, int domainId, int value)
	{
		return (Geometry) slotToLineArray.get(new IntegerPairTyped(type, domainId, value));
	}

	class MarkInfo {
		int domainId;
		int typeId;
		int markID;
		public MarkInfo(int id, int markid, int id2) {
			super();
			domainId = id;
			markID = markid;
			typeId = id2;
		}
		
		public boolean equals(Object obj){
			MarkInfo mi=(MarkInfo)obj;
			return (domainId==mi.domainId)&(markID==mi.markID)&(typeId==mi.typeId);
		}
		
		public int hashCode(){
			return domainId+typeId+markID;
		}
	}
	/**
	 * @param domainId
	 * @param typeId
	 * @param markID
	 * @param pa
	 */
	public boolean isMarkSelected(int domainId, int typeId, int markID)
	{
		return getSelection(domainId).containsMark(typeId, markID);
	}
	/**
	 * @param domainId
	 * @param typeId
	 * @param markID
	 * @param pa
	 */
	public void selectMark(int domainId, int typeId, int markID, PointArray pa)
	{
		getSelection(domainId).addMark(typeId, markID);
		markToGeometryArray.put(new MarkInfo(domainId,typeId,markID),pa);
	}
	
	/**
	 * @param domainId
	 * @param typeId
	 * @param markID
	 * @return
	 */
	public Geometry getGeometryForMark(int domainId, int typeId, int markID)
	{
		return (Geometry) markToGeometryArray.get(new MarkInfo(domainId,typeId,markID));
	}

	/**
	 * @param domainId
	 * @param typeId
	 * @param markID
	 */
	public void unselectMark(int domainId, int typeId, int markID)
	{
		getSelection(domainId).removeMark(typeId, markID);
	}

	
	public boolean isSolidSelected(int solidID, int domainID)
	{
		FDDomain d=(FDDomain) provider.getDomain(domainID);
		Selection s=getSelection(domainID);
		return s.containsSolid(solidID);
	}
	
	public void selectSolid(int solidID, int domainID, QuadArray qa)
	{
		FDDomain d=(FDDomain) provider.getDomain(domainID);
		Selection s=getSelection(domainID);
		s.addSolid(solidID);
		solidIDToQuadArray.put(new IntegerPair(domainID, solidID), qa);
	}

	public Geometry getGeometryForSolid(int domainID, int solidID)
	{
		return (Geometry) solidIDToQuadArray.get(
			new IntegerPair(domainID, solidID));
	}

	public void unselectSolid(int solidID, int domainID)
	{
		FDDomain d=(FDDomain) provider.getDomain(domainID);
		Selection s=(Selection) domainToSelection.get(new Integer(domainID));
		s.removeSolid(solidID);		
		plateIDToQuadArray.remove(new IntegerPair(domainID, solidID));		
	}		
}
