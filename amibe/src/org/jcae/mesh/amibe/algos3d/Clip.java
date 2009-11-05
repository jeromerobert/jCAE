/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2009, by EADS France

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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

public class Clip
{
	private static final Logger LOGGER=Logger.getLogger(Clip.class.getName());
	private final Mesh mesh;
	private final double [] lower = new double[3];
	private final double [] upper = new double[3];
	
	/**
	 * Creates a <code>Clip</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to clip.
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  Valid keys are <code>lower</code> and
	 *        <code>upper</code>.
	 */
	public Clip(final Mesh m, final Map<String, String> options)
	{
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("lower"))
			{
				String[] values = val.split(",");
				if (values.length != 3)
					throw new RuntimeException("Wrong value: --lower "+val);
				for (int i = 0; i < 3; i++)
					lower[i] = Double.valueOf(values[i]).doubleValue();
			}
			else if (key.equals("upper"))
			{
				String[] values = val.split(",");
				if (values.length != 3)
					throw new RuntimeException("Wrong value: --upper "+val);
				for (int i = 0; i < 3; i++)
					upper[i] = Double.valueOf(values[i]).doubleValue();
			}
			else
				throw new RuntimeException("Unknown option: "+key);
		}
		mesh = m;
	}

        public void compute()
	{
		int inner = 0;
		int outer = 0;
		ArrayList<Triangle> out = new ArrayList<Triangle>();
		for (Triangle t : mesh.getTriangles())
		{
			if (!t.isWritable())
			{
				out.add(t);
				continue;
			}
			boolean inside = true;
			for (int i = 0; inside && i < 3; i++)
			{
				double[] pos = t.vertex[i].getUV();
				for (int j = 0; inside && j < 3; j++)
				{
					if (pos[j] > upper[j] || pos[j] < lower[j])
						inside = false;
				}
			}
			if (inside)
			{
				inner++;
			}
			else
			{
				out.add(t);
				outer++;
			}
		}
		for (Triangle t : out)
			mesh.remove(t);

		LOGGER.info("Number of triangles inside of the clip box: "+inner);
		LOGGER.info("Number of triangles outside of the clip box: "+outer);
	}
	
	private static void usage(int rc)
	{
		System.out.println("Usage: Clip [options] xmlDir outDir");
		System.out.println("Options:");
		System.out.println(" -h, --help    Display this message and exit");
		System.out.println(" --lower <c>   Coordinates of the lower left corner");
		System.out.println(" --upper <c>   Coordinates of the upper right corner");
		System.exit(rc);
	}

	public static void main(String[] args)
	{
		org.jcae.mesh.amibe.traits.MeshTraitsBuilder mtb = new org.jcae.mesh.amibe.traits.MeshTraitsBuilder();
		mtb.addTriangleSet();
		Mesh mesh = new Mesh(mtb);
		Map<String, String> opts = new HashMap<String, String>();
		int argc = 0;
		for (String arg: args)
			if (arg.equals("--help") || arg.equals("-h"))
				usage(0);
		while (argc < args.length-1)
		{
			if (args[argc].length() < 2 || args[argc].charAt(0) != '-' || args[argc].charAt(1) != '-')
				break;
			opts.put(args[argc].substring(2), args[argc+1]);
			argc += 2;
		}
		if (argc + 2 != args.length)
			usage(1);
		try
		{
			MeshReader.readObject3D(mesh, args[argc]);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		Clip algo = new Clip(mesh, opts);
		algo.compute();
		try
		{
			MeshWriter.writeObject3D(mesh, args[argc+1], null);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
 }