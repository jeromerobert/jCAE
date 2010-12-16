/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2010, by EADS France
 
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
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.EuclidianMetric3D;
import org.jcae.mesh.amibe.metrics.Metric;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Remesh polyline
 */

public class RemeshPolyline
{
	private final static Logger LOGGER = Logger.getLogger(RemeshPolyline.class.getName());

	private final Mesh mesh;
	private final Map<Vertex, EuclidianMetric3D> metricsMap = new LinkedHashMap<Vertex, EuclidianMetric3D>();
	private final List<Vertex> bgWire = new ArrayList<Vertex>();
	private static final class VertexOnWire
	{
		private final Vertex vertex;
		private final double abscissa;
		public VertexOnWire(Vertex v, double a)
		{
			this.vertex = v;
			this.abscissa = a;
		}
	}

	public RemeshPolyline(Mesh m, List<Vertex> vertices, List<EuclidianMetric3D> metrics)
	{
		if (vertices.size() != metrics.size())
			LOGGER.severe("Argument mismatch");
		mesh = m;
		double abscissa = 0;
		Vertex last = null;
		for (int i = 0, n = metrics.size(); i < n; i++)
		{
			Vertex v = vertices.get(i);
			bgWire.add(v);
			metricsMap.put(v, metrics.get(i));
			if (i > 0)
				abscissa += v.distance3D(last);
			last = v;
		}
		LOGGER.fine("Polyline length: "+abscissa);
	}

	public List<Vertex> compute()
	{
		List<Vertex> newWire = new ArrayList<Vertex>();

		int segment = 0;
		Vertex vS = bgWire.get(0);
		EuclidianMetric3D mS = metricsMap.get(vS);
		Vertex vE = bgWire.get(1);
		EuclidianMetric3D mE = metricsMap.get(vE);

		double hS = mS.getUnitBallBBox()[0];
		double hE = mE.getUnitBallBBox()[0];
		double logRatio = Math.log(hE/hS);
		double [] lower = new double[3];
		double [] upper = new double[3];
		double maxError = 0.001;
		double target = 1.0;
		int nrDichotomy = 20;
		while (true)
		{
			double edgeLength = interpolatedDistance(vS, mS, vE, mE);
			if (edgeLength < 1.0 - maxError)
			{
				target = 1.0 - edgeLength;
				segment++;
				if (segment >= bgWire.size() - 1)
					break;
				vS = bgWire.get(segment);
				mS = metricsMap.get(vS);
				vE = bgWire.get(segment+1);
				mE = metricsMap.get(vE);
				hS = mS.getUnitBallBBox()[0];
				hE = mE.getUnitBallBBox()[0];
				logRatio = Math.log(hE/hS);
				continue;
			}
			else
				target = 1.0;

			// One could take nrDichotomy = 1-log(maxError)/log(2), but this
			// value may not work when surface parameters have a large
			// gradient, so take a larger value to be safe.
	
			System.arraycopy(vS.getUV(), 0, lower, 0, 3);
			System.arraycopy(vE.getUV(), 0, upper, 0, 3);
			// 1-d coordinate between lower and upper points
			double alpha = 0.5;
			double delta = 0.5;
			Vertex np = mesh.createVertex(
				0.5*(lower[0]+upper[0]),
				0.5*(lower[1]+upper[1]),
				0.5*(lower[2]+upper[2]));
			int cnt = nrDichotomy;
			while(cnt >= 0)
			{
				cnt--;
				double [] pos = np.getUV();
	
				// Compute metrics at this position
				EuclidianMetric3D m = new EuclidianMetric3D(hS*Math.exp(alpha*logRatio));
				double l = interpolatedDistance(vS, mS, np, m);
				if (Math.abs(l - target) < maxError)
				{
					LOGGER.finest("Add point: "+l+" =~ "+target);
					vS = np;
					mS = m;
					newWire.add(np);
					break;
				}
				else if (l > target)
				{
					delta *= 0.5;
					alpha -= delta;
					LOGGER.finest(l+" > "+target+" "+cnt+" "+delta+" "+alpha);
					System.arraycopy(pos, 0, upper, 0, 3);
					np.moveTo(
						0.5*(lower[0] + pos[0]),
						0.5*(lower[1] + pos[1]),
						0.5*(lower[2] + pos[2]));
				}
				else
				{
					delta *= 0.5;
					alpha += delta;
					LOGGER.finest(l+" < "+target+" "+cnt+" "+delta+" "+alpha);
					System.arraycopy(pos, 0, lower, 0, 3);
					np.moveTo(
						0.5*(upper[0] + pos[0]),
						0.5*(upper[1] + pos[1]),
						0.5*(upper[2] + pos[2]));
				}
			}
			if (cnt < 0)
			{
				LOGGER.severe("Dichotomy failed");
				assert false;
			}
		}

		return newWire;
	}

	private static double interpolatedDistance(Vertex pt1, Metric m1, Vertex pt2, Metric m2)
	{
		assert m1 != null : "Metric null at point "+pt1;
		assert m2 != null : "Metric null at point "+pt2;
		double[] p1 = pt1.getUV();
		double[] p2 = pt2.getUV();
		double a = Math.sqrt(m1.distance2(p1, p2));
		double b = Math.sqrt(m2.distance2(p1, p2));
		// Linear interpolation:
		//double l = (2.0/3.0) * (a*a + a*b + b*b) / (a + b);
		// Geometric interpolation
		double l = Math.abs(a-b) < 1.e-6*(a+b) ? 0.5*(a+b) : (a - b)/Math.log(a/b);

		return l;
	}

}
