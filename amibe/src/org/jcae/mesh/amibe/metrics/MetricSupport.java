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
import java.util.Collection;
import java.util.HashMap;
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
	private final static Logger LOGGER = Logger.getLogger(MetricSupport.class.getName());
	private DoubleFileReader dfrMetrics;
	private AnalyticMetricInterface analyticMetric;
	private final Map<Vertex, EuclidianMetric3D> metrics;
	private final Mesh mesh;
	private final TIntObjectHashMap<AnalyticMetricInterface> metricsPartitionMap =
		new TIntObjectHashMap<AnalyticMetricInterface>();
	private final String sizeOptionKey;
	private EuclidianMetric3D uniformMetric;
	public interface AnalyticMetricInterface
	{
		/**
		 * Return the target size when topology information are available.
		 * This is used for exemple before inserting a point into the mesh.
		 */
		double getTargetSize(double x, double y, double z);

		/**
		 * Return the target size when topology information are available.
		 * This is only used at the initialization of algorithms
		 */
		double getTargetSizeTopo(Mesh mesh, Vertex v);
	}

	public MetricSupport(Mesh mesh, Map<String, String> options) {
		this(mesh, options, "size");
	}

	public MetricSupport(Mesh mesh, Map<String, String> options, String sizeOption) {
		this.sizeOptionKey = sizeOption;
		this.mesh = mesh;
		Collection<Vertex> nodeset = mesh.getNodes();
		double size = 0.0;
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (sizeOption.equals(key))
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
		metrics = new HashMap<Vertex, EuclidianMetric3D>(
			nodeset == null ? 0 : 2*nodeset.size());
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
			uniformMetric = new EuclidianMetric3D(size);
		}
	}

	public boolean isKnownOption(String key)
	{
		return "metricsFile".equals(key) || sizeOptionKey.equals(key);
	}

	public void setSize(double size)
	{
		uniformMetric = new EuclidianMetric3D(size);
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
				AnalyticMetricInterface metric = getAnalyticMetric(t.getGroupId());
				if (metric == null)
					throw new NullPointerException("Cannot determine metrics, either set 'size' or 'metricsMap' arguments, or call Remesh.setAnalyticMetric()");
				for (Vertex v : t.vertex)
				{
					EuclidianMetric3D curMetric = metrics.get(v);
					EuclidianMetric3D newMetric = new EuclidianMetric3D(
						metric.getTargetSizeTopo(mesh, v));
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

	/** 
	 * Get the metric of an unknown vertex which aims at being inserted in the
	 * given triangle
	 */
	public EuclidianMetric3D get(Vertex v, Triangle t)
	{
		AnalyticMetricInterface metric = getAnalyticMetric(t.getGroupId());
		EuclidianMetric3D toReturn = null;
		if (metric == null)
		{
			toReturn = uniformMetric;
		}
		else
		{
			double[] uv = v.getUV();
			toReturn = new EuclidianMetric3D(metric.getTargetSize(uv[0], uv[1], uv[2]));
		}
		return toReturn;
	}

	/** Get the metric of a known vertex */
	public EuclidianMetric3D get(Vertex v)
	{
		EuclidianMetric3D r = metrics.get(v);
		return r == null ? uniformMetric : r;
	}

	private AnalyticMetricInterface getAnalyticMetric(int groupId)
	{
		AnalyticMetricInterface metric = metricsPartitionMap.get(groupId);
		if (metric == null)
			metric = analyticMetric;
		return metric;
	}

	public boolean isEmpty()
	{
		return metrics.isEmpty() && uniformMetric == null;
	}

	public double interpolatedDistance(Vertex pt1, Vertex pt2)
	{
		return interpolatedDistance(pt1, get(pt1), pt2, get(pt2));
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
