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
 * (C) Copyright 2012, by EADS France
 */

package org.jcae.mesh.amibe.metrics;

import gnu.trove.TIntObjectHashMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.xmldata.DoubleFileReader;
import org.jcae.mesh.xmldata.PrimitiveFileReaderFactory;

/**
 *
 * @author Jerome Robert
 */
public class MetricSupport {
	public final static Collection<String> KNOWN_OPTIONS = new HashSet<String>(
		Arrays.asList("size", "metricsFile"));
	private final static Logger LOGGER = Logger.getLogger(MetricSupport.class.getName());
	private DoubleFileReader dfrMetrics;
	private AnalyticMetricInterface analyticMetric;
	private final Map<Vertex, EuclidianMetric3D> metrics;
	private final Mesh mesh;
	private final TIntObjectHashMap<AnalyticMetricInterface> metricsPartitionMap =
		new TIntObjectHashMap<AnalyticMetricInterface>();

	public interface AnalyticMetricInterface
	{
		double getTargetSize(double x, double y, double z);
	}

	public MetricSupport(Mesh mesh, Map<String, String> options) {
		this.mesh = mesh;
		Collection<Vertex> nodeset = mesh.getNodes();
		double size = 0.0;
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if ("size".equals(key))
			{
				size = Double.parseDouble(val);
				LOGGER.log(Level.FINE, "Size: {0}", size);
				analyticMetric = null;
				dfrMetrics = null;
			}
			else if ("metricsFile".equals(key))
			{
				PrimitiveFileReaderFactory pfrf = new PrimitiveFileReaderFactory();
				try {
					dfrMetrics = pfrf.getDoubleReader(new File(val));
				} catch (FileNotFoundException ex) {
					LOGGER.log(Level.SEVERE, null, ex);
				} catch (IOException ex) {
					LOGGER.log(Level.SEVERE, null, ex);
				}
				analyticMetric = null;
			}
		}

		// Arbitrary size: 2*initial number of nodes
		metrics = new HashMap<Vertex, EuclidianMetric3D>(2*nodeset.size());
		if (dfrMetrics != null)
		{
			try {
				for (Vertex v : nodeset)
					metrics.put(v, new EuclidianMetric3D(dfrMetrics.get(v.getLabel() - 1)));
			} catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
				throw new RuntimeException("Error when loading metrics map file", ex);
			}
		}
		else if (size > 0.0)
		{
			// If targetSize is 0.0, metrics will be set by calling setAnalyticMetric()
			// below.
			for (Vertex v : nodeset)
				metrics.put(v, new EuclidianMetric3D(size));
		}
	}
	public void setAnalyticMetric(AnalyticMetricInterface m)
	{
		analyticMetric = m;
	}

	public void setAnalyticMetric(int groupId, AnalyticMetricInterface m)
	{
		metricsPartitionMap.put(groupId, m);
	}

	public void compute()
	{
		if (analyticMetric != null || !metricsPartitionMap.isEmpty())
		{
			for (Triangle t : mesh.getTriangles())
			{
				if (!t.isReadable())
					continue;
				AnalyticMetricInterface metric = metricsPartitionMap.get(t.getGroupId());
				if (metric == null)
					metric = analyticMetric;
				if (metric == null)
					throw new NullPointerException("Cannot determine metrics, either set 'size' or 'metricsMap' arguments, or call Remesh.setAnalyticMetric()");
				for (Vertex v : t.vertex)
				{
					double[] pos = v.getUV();
					EuclidianMetric3D curMetric = metrics.get(v);
					EuclidianMetric3D newMetric = new EuclidianMetric3D(
						metric.getTargetSize(pos[0], pos[1], pos[2]));
					if (curMetric == null ||
						curMetric.getUnitBallBBox()[0] > newMetric.getUnitBallBBox()[0])
						metrics.put(v, newMetric);
				}
			}
		}
	}

	public void put(Vertex v, EuclidianMetric3D m)
	{
		metrics.put(v, m);
	}

	public EuclidianMetric3D get(Vertex v)
	{
		return metrics.get(v);
	}

	public AnalyticMetricInterface getAnalyticMetric(int groupId)
	{
		AnalyticMetricInterface metric = metricsPartitionMap.get(groupId);
		if (metric == null)
			metric = analyticMetric;
		return metric;
	}

	public boolean isEmpty()
	{
		return metrics.isEmpty();
	}

	public double interpolatedDistance(Vertex pt1, Vertex pt2)
	{
		return interpolatedDistance(pt1, metrics.get(pt1), pt2, metrics.get(pt2));
	}

	public static double interpolatedDistance(Vertex pt1, Metric m1, Vertex pt2, Metric m2)
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
