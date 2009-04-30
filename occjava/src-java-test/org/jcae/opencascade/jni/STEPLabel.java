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

package org.jcae.opencascade.jni;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Example which dump the label from the edges of a STEP file
 * @author Jerome Robert
 */
public class STEPLabel
{
	@Test public void reader()
	{
		STEPControl_Reader aReader = new STEPControl_Reader();
		aReader.readFile("/tmp/Un_autre_essai.stp".getBytes());
		aReader.nbRootsForTransfer();
		aReader.transferRoots();
		TopoDS_Shape s = aReader.oneShape();
		TopExp_Explorer ex = new TopExp_Explorer(s, TopAbs_ShapeEnum.EDGE);
		int i = 0;
		while(ex.more())
		{
			i++;
			System.out.println(i+ " " + aReader.getLabel(ex.current()));
			ex.next();
		}
	}
}
