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

package org.jcae.viewer3d.cad.occ;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import org.jcae.opencascade.jni.*;
import org.jcae.viewer3d.ColoredDomain;
import org.jcae.viewer3d.cad.CADDomainAdapator;

/**
 * @author Jerome Robert
 *
 */
public class OCCEdgeDomain extends CADDomainAdapator implements ColoredDomain
{
	static class MyEdge extends TopoDS_Edge
	{
		protected TopoDS_Edge delegate;
		public MyEdge(TopoDS_Edge e)
		{
			delegate=e;
		}
		/* (non-Javadoc)
		 * @see org.jcae.opencascade.jni.TopoDS_Edge#equals(java.lang.Object)
		 */
		public boolean equals(Object o)
		{
			if(o instanceof MyEdge)
				return delegate.isSame(((MyEdge)o).delegate);
			else return false;
		}
		/* (non-Javadoc)
		 * @see org.jcae.opencascade.jni.TopoDS_Shape#hashCode()
		 */
		public int hashCode()
		{
			return delegate.hashCode();
		}
	}
	
	private TopoDS_Shape shape;
	private ArrayList edges=new ArrayList();
	private Color color=Color.WHITE;
	
	/***
	 * Not used. Just here to keep it somewhere.
	 * @return
	 */
	private double[] computeBoundingBox()
	{
		double[] toReturn=new double[6];
		toReturn[0]=Double.POSITIVE_INFINITY;
		toReturn[1]=Double.POSITIVE_INFINITY;
		toReturn[2]=Double.POSITIVE_INFINITY;
		toReturn[3]=Double.NEGATIVE_INFINITY;
		toReturn[4]=Double.NEGATIVE_INFINITY;
		toReturn[5]=Double.NEGATIVE_INFINITY;

		TopExp_Explorer explorer = new TopExp_Explorer();
		ArrayList points=new ArrayList();
        for (explorer.init(shape, TopAbs_ShapeEnum.VERTEX); explorer.more(); explorer.next())
        {
            TopoDS_Shape s = explorer.current();
            if (!(s instanceof TopoDS_Vertex)) continue; // should not happen!
            double[] pnt = BRep_Tool.pnt((TopoDS_Vertex)s);
            for(int i=0; i<3; i++)
            {
            	if(pnt[i]<toReturn[i])
            		toReturn[i]=pnt[i];
            	if(pnt[i]>toReturn[i+3])
            		toReturn[i+3]=pnt[i];            	
            }
        }
        return toReturn;
	}
	/**
	 * 
	 */
	public OCCEdgeDomain(TopoDS_Shape shape)
	{	
		this.shape=shape;
		TopExp_Explorer explorer = new TopExp_Explorer();
		HashSet alreadyDone=new HashSet();
	    Bnd_Box box = new Bnd_Box(); 
		BRepBndLib.add(shape,box);
		double[] bbox = box.get();
	    //double[] bbox=computeBoundingBox();
		double boundingBoxDeflection=0.0005*
			Math.max(Math.max(bbox[3]-bbox[0], bbox[4]-bbox[1]), bbox[5]-bbox[2]);

		for (explorer.init(shape, TopAbs_ShapeEnum.EDGE); explorer.more(); explorer.next())
		{
		    TopoDS_Shape s = explorer.current();		    
		    if (!(s instanceof TopoDS_Edge)) continue; // should not happen!
		    TopoDS_Edge e = (TopoDS_Edge)s;
		    
		    if(!alreadyDone.add(e))
		    	continue;
						
			double[] range = BRep_Tool.range(e);
		    Geom_Curve gc = BRep_Tool.curve(e, range);
		    float[] array;
		    if(gc!=null)
		    {
			    GeomAdaptor_Curve adaptator = new GeomAdaptor_Curve(gc);
				GCPnts_UniformDeflection deflector = new GCPnts_UniformDeflection();

				deflector.initialize(adaptator, boundingBoxDeflection, range[0], range[1]);
				int npts = deflector.nbPoints();
				
				// Allocate one additional point at each end  = parametric value 0, 1
				array = new float[(npts+2)*3];		    
			    int j=0;
			    double[] values = adaptator.value(range[0]);
			    array[j++] = (float) values[0];
			    array[j++] = (float) values[1];
			    array[j++] = (float) values[2];
			    // All intermediary points
				for (int i=0; i<npts; ++i) {
				    values = adaptator.value(deflector.parameter(i+1));
				    array[j++] = (float) values[0];
				    array[j++] = (float) values[1];
				    array[j++] = (float) values[2];
				}
				// Add last point
			    values = adaptator.value(range[1]);
			    array[j++] = (float) values[0];
			    array[j++] = (float) values[1];
			    array[j++] = (float) values[2];
			    edges.add(array);
		    }
		    else
		    {
		    	if (!BRep_Tool.degenerated(e))
		    	{
				    // So, there is no curve, and the edge is not degenerated?
				    // => draw lines between the vertices and ignore curvature  
				    // best approximation we can do
					ArrayList aa = new ArrayList(); // store points here
					for (TopExp_Explorer explorer2 = new TopExp_Explorer(s, TopAbs_ShapeEnum.VERTEX);
						explorer2.more(); explorer2.next())
					{
					    TopoDS_Shape sv = explorer2.current();
					    if (!(sv instanceof TopoDS_Vertex)) continue; // should not happen!
					    TopoDS_Vertex v = (TopoDS_Vertex)sv;
					    aa.add(BRep_Tool.pnt(v));
					}
					array = new float[aa.size()*3];
					for(int i=0, j=0; i<aa.size(); i++)
					{
						double[] f=(double[])aa.get(i);
						array[j++]=(float) f[0];
						array[j++]=(float) f[1];
						array[j++]=(float) f[2];
					}
					edges.add(array);
				}
		    }
		}		
	}
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.cad.CADDomainAdapator#getEdgeIterator()
	 */
	public Iterator getEdgeIterator()
	{
		return edges.iterator();
	}
	
	public Color getColor()
	{
		return color;
	}
	
	public void setColor(Color color)
	{
		this.color=color;
	}
}
