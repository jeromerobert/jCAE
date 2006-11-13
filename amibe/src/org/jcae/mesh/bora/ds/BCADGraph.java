/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

   (C) Copyright 2006, by EADS CRC

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */


package org.jcae.mesh.bora.ds;

import org.jcae.mesh.cad.*;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Iterator;
import gnu.trove.THashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectHashingStrategy;

import org.apache.log4j.Logger;

/**
 * Mesh graph.
 */
public class BCADGraph
{
	private static Logger logger=Logger.getLogger(BCADGraph.class);
	// Backward link to the model
	private BModel model;
	// Cell root
	private BCADGraphCell root;
	// Map between topological elements and graph cells
	private THashMap cadShapeToGraphCell = new THashMap(keepOrientation);
	// Map between indices and graph cells or user-defined groups
	private TIntObjectHashMap indexToCell = new TIntObjectHashMap();
	// First free index
	private int freeIndex;

	public static int shapeTypeArray[] = { CADExplorer.VERTEX, CADExplorer.EDGE, CADExplorer.WIRE, CADExplorer.FACE, CADExplorer.SHELL, CADExplorer.SOLID, CADExplorer.COMPSOLID, CADExplorer.COMPOUND};
	public static Class classTypeArray[] = { CADVertex.class, CADEdge.class, CADWire.class, CADFace.class, CADShell.class,  CADSolid.class, CADCompSolid.class, CADCompound.class};
	public static String nameTypeArray[] = { "vertex", "edge", "wire", "face", "shell", "solid", "compsolid", "compound"};

	// In OccJava, 2 CADShape instances can be equal with different orientations.
	// We sometimes need to keep track of shape orientation in our graph, hash
	// sets and maps can then use the keepOrientation instance as hashing
	// strategy.
	private static TObjectHashingStrategy keepOrientation = new TObjectHashingStrategy()
	{
		public int computeHashCode(Object o)
		{
			return o.hashCode();
		}
		public boolean equals(Object o1, Object o2)               
		{
			CADShape s1 = (CADShape) o1;
			CADShape s2 = (CADShape) o2;
			return s1 != null && s1.equals(s2) && s1.orientation() == s2.orientation();
		}
	};

	/**
	 * Creates a root mesh.
	 */
	public BCADGraph (BModel m, CADShape shape)
	{
		model = m;
		root = new BCADGraphCell(this, shape);

		// Build the whole graph
		THashMap seen = new THashMap();
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADExplorer exp = factory.newExplorer();
		for (int t = 0; t < shapeTypeArray.length; t++)
		{
			for (exp.init(shape, shapeTypeArray[t]); exp.more(); exp.next())
			{
				CADShape sub = exp.current();
				if (cadShapeToGraphCell.contains(sub))
					continue;
				BCADGraphCell cell = new BCADGraphCell(this, sub);
				cadShapeToGraphCell.put(sub, cell);
				CADShape r = (CADShape) seen.get(sub);
				if (r != null)
				{
					BCADGraphCell rev = (BCADGraphCell) cadShapeToGraphCell.get(r);
					rev.setReversed(cell);
					cell.setReversed(rev);
				}
				seen.put(sub, sub);
				logger.debug("  Add submesh: "+sub+" "+cell);
			}
		}
		// Add indices
		int i = 1;
		CADExplorer exp2 = factory.newExplorer();
		for (int t = 0; t <= CADExplorer.VERTEX; t++)
		{
			for (exp.init(shape, t); exp.more(); exp.next())
			{
				CADShape s = (CADShape) exp.current();
				BCADGraphCell c = (BCADGraphCell) cadShapeToGraphCell.get(s);
				if (c != null && c.getId() <= 0)
				{
					c.setId(i);
					indexToCell.put(i, c);
					i++;
				}
			}
		}
		freeIndex = i;

		// Add backward links
		for (int t = 0; t <= CADExplorer.VERTEX; t++)
		{
			for (exp.init(shape, t); exp.more(); exp.next())
			{
				CADShape s = (CADShape) exp.current();
				BCADGraphCell c = (BCADGraphCell) cadShapeToGraphCell.get(s);
				if (c == null)
					continue;
				for (int sub = t+1; sub <= CADExplorer.VERTEX; sub++)
				{
					for (exp2.init(s, sub); exp2.more(); exp2.next())
					{
						CADShape s2 = (CADShape) exp2.current();
						BCADGraphCell c2 = (BCADGraphCell) cadShapeToGraphCell.get(s2);
						if (c2 != null)
							c2.addParent(c);
					}
				}
			}
		}
	}

	public BModel getModel()
	{
		return model;
	}

	public BCADGraphCell getRootCell()
	{
		return root;
	}

	public int getFreeIndex()
	{
		return freeIndex;
	}

	/**
	 * Gets the submesh of a shape.
	 *
	 * @return  the submesh
	 */
	public BCADGraphCell cadToGraphCell(CADShape s)
	{
		return (BCADGraphCell) cadShapeToGraphCell.get(s);
	}

	/**
	 * Returns the list of cells for a given dimension.
	 */
	public Collection getCellList(int t)
	{
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADExplorer exp = factory.newExplorer();
		Collection ret = new LinkedHashSet();
		for (exp.init(root.getShape(), shapeTypeArray[t]); exp.more(); exp.next())
		{
			CADShape s = exp.current();
			BCADGraphCell c = (BCADGraphCell) cadShapeToGraphCell.get(s);
			if (ret.contains(c))
				continue;
			ret.add(c);
		}
		return ret;
	}

	/**
	 * Prints the list of geometrical elements.
	 */
	public void printShapes()
	{
		System.out.println("List of geometrical entities");
		for (int t = classTypeArray.length - 1; t >= 0; t--)
			printShapes(t, root.shapesExplorer(t));
		System.out.println("End list");
	}

	public static void printShapes(int t, Iterator it)
	{
		while (it.hasNext())
		{
			BCADGraphCell s = (BCADGraphCell) it.next();
			if (t == 0)
			{
				CADVertex v = (CADVertex) s.getShape();
				double [] coord = v.pnt();
				System.out.println("Shape "+s+ " ("+coord[0]+", "+coord[1]+", "+coord[2]+")");
			}
			else
			{
				System.out.println("Shape "+s+" ("+Integer.toHexString(s.hashCode())+"):");
				for (Iterator it2 = s.allShapesIterator(); it2.hasNext(); )
				{
					BCADGraphCell c = (BCADGraphCell) it2.next();
					System.out.println(" +> shape "+c+" ("+Integer.toHexString(c.hashCode())+")");
				}
			}
		}
	}

}
