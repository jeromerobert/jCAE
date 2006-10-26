/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004,2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

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

package org.jcae.mesh.cad.occ;

import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.opencascade.jni.BRep_Tool;
import org.jcae.opencascade.jni.BRepTools;
import org.jcae.opencascade.jni.TopoDS_Vertex;
import org.jcae.opencascade.jni.TopoDS_Edge;
import org.jcae.opencascade.jni.TopoDS_Face;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.opencascade.jni.TopoDS_Solid;
import org.jcae.opencascade.jni.TopAbs_Orientation;
import org.jcae.opencascade.jni.Bnd_Box;
import org.jcae.opencascade.jni.BRepBndLib;
import gnu.trove.TObjectIntHashMap;

public class OCCShape implements CADShape
{
	protected TopoDS_Shape myShape = null;
	protected int id = -1;
	private static TObjectIntHashMap imap = new TObjectIntHashMap();
	
	public OCCShape()
	{
	}
	
	public void setShape(Object o)
	{
		myShape = (TopoDS_Shape) o;
	}
	
	public Object getShape()
	{
		return myShape;
	}
	
	public CADGeomSurface getGeomSurface()
	{
		assert myShape instanceof TopoDS_Face;
		OCCGeomSurface surface = new OCCGeomSurface();
		surface.setSurface(BRep_Tool.surface((TopoDS_Face) myShape));
		return (CADGeomSurface) surface;
	}
	
	public double [] boundingBox()
	{
		Bnd_Box box = new Bnd_Box();
		BRepBndLib.add(myShape, box);
		return box.get();
	}
	
	public CADShape reversed()
	{
		OCCShape s;
		if (myShape instanceof TopoDS_Vertex)
			s = new OCCVertex();
		else if (myShape instanceof TopoDS_Edge)
			s = new OCCEdge();
		else if (myShape instanceof TopoDS_Face)
			s = new OCCFace();
		else if (myShape instanceof TopoDS_Solid)
			s = new OCCSolid();
		else
			s = new OCCShape();
		s.setShape(myShape.reversed());
		return (CADShape) s;
	}
	
	public int orientation()
	{
		return myShape.orientation();
	}
	
	public boolean isOrientationForward()
	{
		return myShape.orientation() == TopAbs_Orientation.FORWARD;
	}
	
	public boolean equals(Object o)
	{
		OCCShape that = (OCCShape) o;
		return myShape.equals(that.myShape);
	}
	
	public boolean isSame(Object o)
	{
		OCCShape that = (OCCShape) o;
		return myShape.isSame(that.myShape);
	}
	
	public void writeNative(String filename)
	{
  		BRepTools.write(myShape, filename);
	}
	
	public int getId()
	{
		return imap.get(this);
	}

	public void setIds()
	{
		int i = 1;
		OCCExplorer exp = new OCCExplorer();
		for (int t = 0; t <= OCCExplorer.VERTEX; t++)
		{
			for (exp.init(this, t); exp.more(); exp.next())
			{
				OCCShape s = (OCCShape) exp.current();
				if (imap.get(s) == 0)
				{
					imap.put(s, i);
					i++;
				}
			}
		}
	}
	
	public int hashCode()
	{
		return myShape.hashCode();
	}
	
}
