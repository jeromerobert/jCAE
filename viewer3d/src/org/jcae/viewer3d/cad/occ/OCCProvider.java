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
import java.util.NoSuchElementException;
import org.jcae.opencascade.Utilities;
import org.jcae.opencascade.jni.*;
import org.jcae.viewer3d.Domain;
import org.jcae.viewer3d.cad.CADProvider;

/**
 * @author Jerome Robert
 * @todo all methods must be implemented
 */
public class OCCProvider implements CADProvider
{
	public static final int EDGE_DOMAIN=0;
	public static final int FACE_DOMAIN=1;
	public static final int VERTEX_DOMAIN=2;
	private TopoDS_Shape shape;
	private Color[] facesColors;
	private Color edgeColor=Color.WHITE;
	
	private boolean provideEdge=true;
	private boolean provideFace=true;
	private boolean provideVertex=true;

	/**
	 * Create an empty OCCProvider
	 */
	public OCCProvider(){}
	
	/**
	 * Create an OCCProvider from a TopoDS_Shape object
	 */
	public OCCProvider(TopoDS_Shape shape)
	{
		this.shape=shape;
	}
	
	/**
	 * Create an OCCProvider from a BREP, STEP or IGES file.
	 * @param fileName
	 */
	public OCCProvider(String fileName)
	{
        this(Utilities.readFile(fileName));
	}
	
	/**
	 * Set the OCCProvider shape
	 * @param shape
	 */
	public void setShape(TopoDS_Shape shape)
	{
		this.shape=shape;
		facesColors=null;
	}
	public void setShape(TopoDS_Shape shape,Color[] facesColors)
	{
		this.shape=shape;
		this.facesColors=facesColors;
	}
	
	/**
	 * Allow to provide Edge of the Shape
	 * @param provide
	 */
	public void provideEdge(boolean provide){
		provideEdge=provide;
	}
	
	/**
	 * Allow to provide Face of the Shape
	 * @param provide
	 */
	public void provideFace(boolean provide){
		provideFace=provide;
	}
	
	/**
	 * Allow to provide Vertex of the Shape
	 * @param provide
	 */
	public void provideVertex(boolean provide){
		provideVertex=provide;
	}
	
	/* (non-Javadoc)
	 * @see jcae.viewer3d.DomainProvider#getDomainIDs()
	 */
	public int[] getDomainIDs()
	{
		if(shape==null) return new int[0];
		int DomainCount=0;
		if(provideEdge) DomainCount++;
		if(provideFace) DomainCount++;
		if(provideVertex) DomainCount++;
		
		int[] toReturn=new int[DomainCount];
		DomainCount=0;
		if(provideEdge){
			toReturn[DomainCount]=EDGE_DOMAIN;
			DomainCount++;
		} 
		if(provideFace){
			toReturn[DomainCount]=FACE_DOMAIN;
			DomainCount++;
		}  
		if(provideVertex) {
			toReturn[DomainCount]=VERTEX_DOMAIN;
			DomainCount++;
		} 
		
		return toReturn;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.DomainProvider#getDomain(int)
	 */
	public Domain getDomain(int id)
	{
		if(shape==null) throw new NoSuchElementException();
		switch(id)
		{
			case EDGE_DOMAIN:
			{	OCCEdgeDomain oed=new OCCEdgeDomain(shape);
				oed.setColor(edgeColor);
				return oed;
			}
			case FACE_DOMAIN: return new OCCFaceDomain(shape,facesColors);
			case VERTEX_DOMAIN: return new OCCVertexDomain(shape);
			default: throw new NoSuchElementException();
		}
	}

	public Color getEdgeColor()
	{
		return edgeColor;
	}

	public void setEdgeColor(Color edgeColor)
	{
		this.edgeColor = edgeColor;
	}
}
