/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004 Jerome Robert <jeromerobert@users.sourceforge.net>

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

import org.jcae.mesh.mesher.ds.MNode3D;
import org.jcae.opencascade.jni.Adaptor3d_Curve;
import java.util.ArrayList;

public class OCCDiscretizeCurve3D
{
	private Adaptor3d_Curve curve = null;
	private int nr = 0;
	private double length = -1.0;
	private double [] a;
	
	public void initialize(Adaptor3d_Curve myCurve, double len, double start, double end)
	{
		curve = myCurve;
		int nsegments = 10;
		double [] xyz;
		while (true)
		{
			nsegments *= 10;
			a = new double[nsegments+1];
			xyz = new double[3*(nsegments+1)];
			double [] oldXYZ = curve.value(start);
			double [] newXYZ;
			double abscissa, dist;
			nr = 1;
			a[0] = start;
			for (int i = 0; i < 3; i++)
				xyz[i] = oldXYZ[i];
			for (int ns = 1; ns < nsegments; ns++)
			{
				abscissa = start + ns * (end - start) / ((double) nsegments);
				newXYZ = curve.value(abscissa);
				dist = Math.sqrt(
				  (oldXYZ[0] - newXYZ[0]) * (oldXYZ[0] - newXYZ[0]) +
				  (oldXYZ[1] - newXYZ[1]) * (oldXYZ[1] - newXYZ[1]) +
				  (oldXYZ[2] - newXYZ[2]) * (oldXYZ[2] - newXYZ[2]));
				if (dist > len)
				{
					a[nr] = abscissa;
					oldXYZ = newXYZ;
					xyz[3*nr]   = oldXYZ[0];
					xyz[3*nr+1] = oldXYZ[1];
					xyz[3*nr+2] = oldXYZ[2];
					nr++;
				}
			}
			a[nr] = end;
			oldXYZ = curve.value(end);
			xyz[3*nr]   = oldXYZ[0];
			xyz[3*nr+1] = oldXYZ[1];
			xyz[3*nr+2] = oldXYZ[2];
			nr++;
			//  Stop when there are at least 10 points per segments
			if (nr * 10 < nsegments)
				break;
		}
		length = -1.0;
		adjustAbscissas(xyz);
	}
	
	public void initialize(Adaptor3d_Curve myCurve, int n, double start, double end)
	{
		curve = myCurve;
		nr = n;
		int nsegments = 10 * n;
		double [] p = new double[nsegments+1];
		MNode3D [] v = new MNode3D[nsegments+1];
		for (int ns = 0; ns <= nsegments; ns++)
		{
			p[ns] = start + ns * (end - start) / ((double) nsegments);
			double [] xyz = myCurve.value(p[ns]);
			v[ns] = new MNode3D(xyz, -1);
		}
		double length = 0.0;
		for (int ns = 1; ns <= nsegments; ns++)
			length += v[ns].distance(v[ns-1]);
		double lmax = 2.0 * length / ((double) nr);
		double lmin = 0.0;
		double maxlen;
		ArrayList abscissa = new ArrayList(nsegments);
		while (true)
		{
			maxlen = 0.5 * (lmin + lmax);
			int lastIndex = 0;
			abscissa.clear();
			nr = 1;
			abscissa.add(new Integer(0));
			for (int ns = 1; ns < nsegments; ns++)
			{
				if (v[lastIndex].distance(v[ns]) > maxlen)
				{
					lastIndex = ns;
					nr++;
					abscissa.add(new Integer(ns));
				}
			}
			nr++;
			abscissa.add(new Integer(nsegments));
			if (n == nr)
				break;
			else if (nr < n)
				lmax = lmax - 0.5 * (lmax - lmin);
			else
				lmin = lmin + 0.5 * (lmax - lmin);
		}
		a = new double[nr];
		double [] xyz = new double[3*nr];
		for (int i = 0; i < nr; i++)
		{
			a[i] = p[((Integer) abscissa.get(i)).intValue()];
			double [] newXYZ = curve.value(a[i]);
			xyz[3*i]   = newXYZ[0];
			xyz[3*i+1] = newXYZ[1];
			xyz[3*i+2] = newXYZ[2];
		}
		length = -1.0;
		adjustAbscissas(xyz);
	}
	
	private void adjustAbscissas(double [] xyz)
	{
		boolean backward = false;
		int niter = 2*nr;
		while (niter > 0)
		{
			niter--;
			backward = ! backward;
			boolean redo = false;
			if (backward)
			{
				for (int i = nr - 2; i > 0; i--)
				{
					double l1 = Math.sqrt(
					  (xyz[3*i] - xyz[3*i-3]) * (xyz[3*i] - xyz[3*i-3]) +
					  (xyz[3*i+1] - xyz[3*i-2]) * (xyz[3*i+1] - xyz[3*i-2]) +
					  (xyz[3*i+2] - xyz[3*i-1]) * (xyz[3*i+2] - xyz[3*i-1]));
					double l2 = Math.sqrt(
					  (xyz[3*i] - xyz[3*i+3]) * (xyz[3*i] - xyz[3*i+3]) +
					  (xyz[3*i+1] - xyz[3*i+4]) * (xyz[3*i+1] - xyz[3*i+4]) +
					  (xyz[3*i+2] - xyz[3*i+5]) * (xyz[3*i+2] - xyz[3*i+5]));
					if (Math.abs(l2 - l1) > 0.01 * 0.5 * (l1+l2))
					{
						redo = true;
						double newA = a[i] + 0.8 * (a[i+1] - a[i-1]) * (l2 - l1) / (l1 + l2);
						if (newA > a[i-1] && newA < a[i+1])
							a[i] = newA;
						
						double [] newXYZ = curve.value(a[i]);
						xyz[3*i]   = newXYZ[0];
						xyz[3*i+1] = newXYZ[1];
						xyz[3*i+2] = newXYZ[2];
					}
				}
			}
			else
			{
				for (int i = 1; i < nr - 1; i++)
				{
					double l1 = Math.sqrt(
					  (xyz[3*i] - xyz[3*i-3]) * (xyz[3*i] - xyz[3*i-3]) +
					  (xyz[3*i+1] - xyz[3*i-2]) * (xyz[3*i+1] - xyz[3*i-2]) +
					  (xyz[3*i+2] - xyz[3*i-1]) * (xyz[3*i+2] - xyz[3*i-1]));
					double l2 = Math.sqrt(
					  (xyz[3*i] - xyz[3*i+3]) * (xyz[3*i] - xyz[3*i+3]) +
					  (xyz[3*i+1] - xyz[3*i+4]) * (xyz[3*i+1] - xyz[3*i+4]) +
					  (xyz[3*i+2] - xyz[3*i+5]) * (xyz[3*i+2] - xyz[3*i+5]));
					if (Math.abs(l2 - l1) > 0.01 * 0.5 * (l1+l2))
					{
						redo = true;
						double newA = a[i] + 0.8 * (a[i+1] - a[i-1]) * (l2 - l1) / (l1 + l2);
						if (newA > a[i-1] && newA < a[i+1])
							a[i] = newA;
						double [] newXYZ = curve.value(a[i]);
						xyz[3*i]   = newXYZ[0];
						xyz[3*i+1] = newXYZ[1];
						xyz[3*i+2] = newXYZ[2];
					}
				}
			}
			if (!redo)
				break;
		}
	}
	
	public int nbPoints()
	{
		return nr;
	}
	
	public double parameter(int index)
	{
		return a[index-1];
	}
	
	public double length()
	{
		if (length >= 0.0)
			return length;
		
		assert nr > 0;
		double [] oldXYZ;
		double [] newXYZ = curve.value(a[0]);
		length = 0.0;
		for (int i = 1; i < nr; i++)
		{
			oldXYZ = newXYZ;
			newXYZ = curve.value(a[i]);
			length += Math.sqrt(
			  (oldXYZ[0] - newXYZ[0]) * (oldXYZ[0] - newXYZ[0]) +
			  (oldXYZ[1] - newXYZ[1]) * (oldXYZ[1] - newXYZ[1]) +
			  (oldXYZ[2] - newXYZ[2]) * (oldXYZ[2] - newXYZ[2]));
		}
		return length;
	}
}
