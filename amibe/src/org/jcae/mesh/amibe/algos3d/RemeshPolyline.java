/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2010-2011, by EADS France
 
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.metrics.MetricSupport.AnalyticMetricInterface;


/**
 * Remesh polyline
 */

public class RemeshPolyline
{
	private final static Logger LOGGER = Logger.getLogger(RemeshPolyline.class.getName());

	// Background mesh, it is needed only as a factory to build vertices
	private final Mesh mesh;
	// Sorted list of vertices
	private final List<Vertex> bgWire = new ArrayList<Vertex>();
	// Map containing the metrics at each input point
	private final Map<Vertex, EuclidianMetric3D> metricsMap = new LinkedHashMap<Vertex, EuclidianMetric3D>();

	public RemeshPolyline(Mesh m, List<Vertex> vertices, double size)
	{
		this(m, vertices,
			Collections.nCopies(vertices.size(), new EuclidianMetric3D(size)));
	}
	/**
	 * Constructor.
	 *
	 * @param m Mesh
	 * @param vertices Sorted list of vertices
	 * @param metrics List of metrics at those points
	 */
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
		LOGGER.log(Level.FINE, "Polyline approximate length: {0}", abscissa);
	}

	public RemeshPolyline(Mesh m, List<Vertex> vertices, AnalyticMetricInterface analytic)
	{
		mesh = m;
		double abscissa = 0;
		Vertex last = null;
		for (Vertex v : vertices)
		{
			bgWire.add(v);
			double [] pos = v.getUV();
			metricsMap.put(v, new EuclidianMetric3D(analytic.getTargetSize(pos[0], pos[1], pos[2])));
			if (last != null)
				abscissa += v.distance3D(last);
			last = v;
		}
		LOGGER.log(Level.FINE, "Polyline approximate length: {0}", abscissa);
	}

	/**
	 * Discretize the polyline and return the sorted list of vertices.
	 *
	 * @return sorted list of vertices
	 */

	public List<Vertex> compute()
	{
		List<Vertex> newWire = new ArrayList<Vertex>();
		Vertex last = bgWire.get(bgWire.size() -1);
		newWire.add(bgWire.get(0));
		newWire.add(last);
		// Target size in the unit mesh.  This value is adjusted so
		// that the last length is similar to others.
		double target = 1.0;
		// Maximal error
		double maxError = 1.e-3;
		double curError = Double.MAX_VALUE;
		while(true)
		{
			List<Vertex> saveList = new ArrayList<Vertex>(newWire);
			// lastLength is the distance between the last inserted
			// point and the last point of the polyline.
			double lastLength = compute(newWire, target, maxError);
			if (lastLength < maxError)
			{
				// We found a good discretization, replace the
				// last point by the real destination point.
				newWire.set(newWire.size() - 1, last);
				break;
			}
			else if (lastLength > target - maxError || 1 == newWire.size())
			{
				// In the first case, discretization is also good.
				// If only one vertex has been inserted, this means that
				// the polyline is too small and does not have to
				// be discretized.
				newWire.add(last);
				break;
			}
			else if (lastLength < 0.5 * target)
			{
				// Avoid infinite loops
				if (lastLength > curError)
				{
					LOGGER.warning("Beam discretization may be of poor quality");
					return saveList;
				}
				curError = lastLength;
				// The last subsegment is small, increase target size
				// so that when all subsegments have the same size.
				// Use a relaxation factor of 0.6
				target += 0.6 * lastLength / (newWire.size() - 1);
				newWire.set(newWire.size() - 1, last);
			}
			else
			{
				// Avoid infinite loops
				if (target - lastLength > curError)
				{
					LOGGER.warning("Beam discretization may be of poor quality");
					break;
				}
				curError = target - lastLength;
				// The last subsegment is large, we will add another
				// point, but target size must be decreased.
				// Use a relaxation factor of 0.6
				target -= 0.6 * (target - lastLength) / (newWire.size());
				newWire.add(last);
			}

			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.log(Level.FINE, "Length of last segment: {0} number of vertices: {1} -> new target: {2}", new Object[]{lastLength, newWire.size(), target});
		}
		LOGGER.log(Level.CONFIG, "Number of segments: {0} mean target: {1}", new Object[]{newWire.size() - 1, target});
		return newWire;
	}

	// Discretization points are inserted into the newWire list. Distances are
	// computed according to metrics at given points, we try to insert points
	// at distance 1.  Return the fraction of segment which could not be
	// discretized.
	private double compute(List<Vertex> newWire, double targetSize, double maxError)
	{
		// FIXME: In order to control the global error, we fix error
		// at each point, maybe this is not needed.
		if (!newWire.isEmpty())
			maxError /= newWire.size();

		// Start a new wire, add the first point
		newWire.clear();
		int segment = 0;
		Vertex vS = bgWire.get(0);
		EuclidianMetric3D mS = metricsMap.get(vS);
		Vertex vE = bgWire.get(1);
		EuclidianMetric3D mE = metricsMap.get(vE);
		newWire.add(vS);

		// Metrics are interpolated geometrically.
		double hS = mS.getUnitBallBBox()[0];
		double hE = mE.getUnitBallBBox()[0];
		double logRatio = Math.log(hE/hS);

		// The best candidate is found by dichotomy.
		// Allocate lower and upper bounds.
		double [] lower = new double[3];
		double [] upper = new double[3];
		double target = targetSize;
		double accumulated = 0;
		int nrDichotomy = - 2 * (int) (Math.log(maxError) / Math.log(2.0));
		if (LOGGER.isLoggable(Level.FINEST))
			LOGGER.log(Level.FINEST, "Dichotomy: MaxError={0} max nr. of dichotomy: {1}", new Object[]{maxError, nrDichotomy});
		while (true)
		{
			double edgeLength = interpolatedDistance(vS, mS, vE, mE);
			if (edgeLength < target)
			{
				accumulated += edgeLength;
				target -= edgeLength;
				if (LOGGER.isLoggable(Level.FINE))
					LOGGER.log(Level.FINE, "End of segment {0} found, edgeLength={1} target set to {2}", new Object[]{segment, edgeLength, target});
				segment++;
				if (segment >= bgWire.size() - 1)
					return accumulated;
				vS = bgWire.get(segment);
				mS = metricsMap.get(vS);
				vE = bgWire.get(segment+1);
				mE = metricsMap.get(vE);
				hS = mS.getUnitBallBBox()[0];
				hE = mE.getUnitBallBBox()[0];
				logRatio = Math.log(hE/hS);
				continue;
			}
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.log(Level.FINE, "Length segment={0} target={1} maxError={2}", new Object[]{edgeLength, target, maxError});

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
			if (edgeLength > 1.0)
				cnt *= (int) edgeLength;
			while(cnt >= 0)
			{
				cnt--;
				double [] pos = np.getUV();
	
				// Compute metrics at this position
				EuclidianMetric3D m = new EuclidianMetric3D(hS*Math.exp(alpha*logRatio));
				double l = interpolatedDistance(vS, mS, np, m);
				if (Math.abs(l - target) < maxError)
				{
					if (LOGGER.isLoggable(Level.FINER))
						LOGGER.log(Level.FINER, "Add point: {0} =~ {1} {2}", new Object[]{l, target, np});
					vS = np;
					mS = m;
					newWire.add(np);
					target = targetSize;
					accumulated = 0;
					break;
				}
				else if (l > target)
				{
					delta *= 0.5;
					alpha -= delta;
					if (LOGGER.isLoggable(Level.FINEST))
						LOGGER.log(Level.FINEST, "{0} > {1} {2} {3} {4}", new Object[]{l, target, cnt, delta, alpha});
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
					if (LOGGER.isLoggable(Level.FINEST))
						LOGGER.log(Level.FINEST, "{0} < {1} {2} {3} {4}", new Object[]{l, target, cnt, delta, alpha});
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
				return -1.0;
			}
		}
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
