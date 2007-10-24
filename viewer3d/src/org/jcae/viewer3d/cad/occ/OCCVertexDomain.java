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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d.cad.occ;

import java.util.ArrayList;
import org.jcae.opencascade.jni.*;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.jcae.opencascade.jni.TopExp_Explorer;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.opencascade.jni.TopoDS_Vertex;
import org.jcae.viewer3d.cad.CADDomainAdapator;

/**
 * @author Jerome Robert
 *
 */
public class OCCVertexDomain extends CADDomainAdapator
{
	float[] marks;
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.cad.CADDomainAdapator#getMarksTypes()
	 */
	@Override
	public Object[] getMarksTypes()
	{
		return new Object[]{new Float(3.0)};
	}
	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.cad.CADDomainAdapator#getMarks(java.lang.Object)
	 */
	@Override
	public float[] getMarks(Object type)
	{
		return marks;
	}
	
	public OCCVertexDomain(TopoDS_Shape shape)
	{
		TopExp_Explorer explorer = new TopExp_Explorer();
		ArrayList<double[]> points=new ArrayList<double[]>();
        for (explorer.init(shape, TopAbs_ShapeEnum.VERTEX); explorer.more(); explorer.next())
        {
            TopoDS_Shape s = explorer.current();
            if (!(s instanceof TopoDS_Vertex)) continue; // should not happen!
            double[] pnt = BRep_Tool.pnt((TopoDS_Vertex)s);            
            points.add(pnt);
        }
        marks=new float[points.size()*3];
        for(int i=0, j=0; i<points.size(); i++, j+=3)
        {
        	double[] pnt=points.get(i);
        	marks[j]=(float) pnt[0];
        	marks[j+1]=(float) pnt[1];
        	marks[j+2]=(float) pnt[2];
        }
	}
}
