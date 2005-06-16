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

import java.util.NoSuchElementException;
import org.jcae.opencascade.jni.*;
import org.jcae.opencascade.jni.BRepTools;
import org.jcae.opencascade.jni.IGESControl_Reader;
import org.jcae.opencascade.jni.STEPControl_Reader;
import org.jcae.opencascade.jni.TopoDS_Shape;
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
        this(loadShape(fileName));
	}
	
	
	private static TopoDS_Shape loadShape(String fileName)
	{
        TopoDS_Shape brepShape;
        if (fileName.endsWith(".step"))
        {
            STEPControl_Reader aReader = new STEPControl_Reader();
            aReader.readFile(fileName);
            aReader.nbRootsForTransfer();
            aReader.transferRoots();
            brepShape = aReader.oneShape();
        }
        else if (fileName.endsWith(".igs"))
        {
            IGESControl_Reader aReader = new IGESControl_Reader();
            aReader.readFile(fileName);
            aReader.nbRootsForTransfer();
            aReader.transferRoots();
            brepShape = aReader.oneShape();
        }
        else
            brepShape = BRepTools.read(fileName, new BRep_Builder());
        return brepShape;
	}
	/* (non-Javadoc)
	 * @see jcae.viewer3d.DomainProvider#getDomainIDs()
	 */
	public int[] getDomainIDs()
	{
		return new int[]{EDGE_DOMAIN, FACE_DOMAIN, VERTEX_DOMAIN};
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.DomainProvider#getDomain(int)
	 */
	public Domain getDomain(int id)
	{
		switch(id)
		{
			case EDGE_DOMAIN: return new OCCEdgeDomain(shape);
			case FACE_DOMAIN: return new OCCFaceDomain(shape);
			case VERTEX_DOMAIN: return new OCCVertexDomain(shape);
			default: throw new NoSuchElementException();
		}
	}
}
