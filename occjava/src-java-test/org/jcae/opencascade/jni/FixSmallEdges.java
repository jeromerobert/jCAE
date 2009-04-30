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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.opencascade.jni;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.opencascade.Utilities;

/**
 *
 * @author Jerome Robert
 */
public class FixSmallEdges {

	private static int countSmallEdges(TopoDS_Shape shape, double minLength)
	{
		TopExp_Explorer exp = new TopExp_Explorer(shape, TopAbs_ShapeEnum.EDGE);
		GProp_GProps property=new GProp_GProps();
		
		int count = 0;
		int index = 1;
		while(exp.more())
		{
			TopoDS_Edge edge = (TopoDS_Edge) exp.current();
			BRepGProp.linearProperties(edge, property);
			double l = property.mass();
			if(l < minLength)
			{
				System.out.println(index+" "+l);
				count++;			
			}
			exp.next();
			index++;
		}
		return count;
	}

	public static void main(String[] args)
	{
		try
		{
			if (args.length != 3) displayHelp();
			if (!new File(args[0]).canRead())
			{
				System.out.println("Cannot open " + args[0]);
				return;
			}

			double tolerance;
			try
			{
				tolerance = Double.parseDouble(args[1]);
			}
			catch (NumberFormatException ex)
			{
				displayHelp();
				return;
			}

			if (!new File(args[2]).createNewFile())
			{
				System.out.println("Cannot create " + args[2]);
				return;
			}

			TopoDS_Shape shape = Utilities.readFile(args[0]);
			System.out.println("Number of small edges before: " +
				countSmallEdges(shape, tolerance));
			ShapeFix_Wireframe fix =
				new ShapeFix_Wireframe(shape);
			fix.setPrecision(tolerance);
			fix.fixSmallEdges();
			shape = fix.shape();
			System.out.println("Number of small edges after: " +
				countSmallEdges(shape, tolerance));
			BRepTools.write(shape, args[2]);
		}
		catch (IOException ex)
		{
			Logger.getLogger(FixSmallEdges.class.getName()).
				log(Level.SEVERE, null, ex);
		}
	}

	private static void displayHelp()
	{
		System.out.println("Syntax: fixsmalledge <input file> <tolerance> <output file>");
	}
}
