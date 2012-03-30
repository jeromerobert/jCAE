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
 * (C) Copyright 2010, by EADS France
 */
package org.jcae.mesh.amibe.algos3d;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An AnalyticMetric which refine mesh around a set of points.
 * The metric around each point is:
 * Sinf * (1 - 2 * (1 - S0 / Sinf) / (1 + (R * d + 1) ^ 3 ) )
 * where Sinf is the mesh size far from the point, S0 is the mesh size on the
 * point, d is the distance from the point and R a coeficient saying how fast
 * Sinf is reached.
 * @author Jerome Robert
 */
public class PointMetric implements Remesh.AnalyticMetricInterface {

	private class Source
	{
		public double sx,sy,sz;
		/** alpha = 2 * (1 - SO / Sinf) */
		public double alpha;
		public double coef;
		/** 
		 * if the distance^2 is greater than this value this source is not
		 * concidered
		 */
		public double threshold;

		public double distance2(double x, double y, double z)
		{
			double dx = sx-x;
			double dy = sy-y;
			double dz = sz-z;
			return dx*dx+dy*dy+dz*dz;
		}
	}

	private final List<Source> sources = new ArrayList<Source>();
	private final double sizeInf;
	public PointMetric(double sizeInf) {
		this.sizeInf = sizeInf;
	}

	public PointMetric(double sizeInf, double x, double y, double z,
		double size0, double coef)
	{
		this(sizeInf);
		addPoint(x, y, z, size0, coef);
	}

	public PointMetric(double sizeInf, String fileName) throws IOException {
		this(sizeInf);
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String buffer = br.readLine();
		while(buffer != null)
		{
			String[] line = buffer.trim().split("[\\s,;]+");
			double x = Double.parseDouble(line[0]);
			double y = Double.parseDouble(line[1]);
			double z = Double.parseDouble(line[2]);
			double size0 = Double.parseDouble(line[3]);
			double coef = Double.parseDouble(line[4]);
			addPoint(x, y, z, size0, coef);
			buffer = br.readLine();
		}
		br.close();
	}

	/**
	 * Add a point around which to refine
	 * @param size0 metric on the point
	 * @param sizeInf metric far from the point
	 * @param coef how fast we go from size0 to sizeInf
	 */
	public final void addPoint(double x, double y, double z, double size0, double coef)
	{
		Source s = new Source();
		s.coef = 1.0 / (Math.log(sizeInf / size0) * (sizeInf + size0) * coef);
		s.sx = x;
		s.sy = y;
		s.sz = z;
		s.alpha = 2 * (1 - size0 / sizeInf);
		s.threshold = (Math.pow(s.alpha/0.05-1, 1.0/3) - 1) / s.coef;
		s.threshold = s.threshold * s.threshold;
		sources.add(s);
	}

	@Override
	public double getTargetSize(double x, double y, double z) {
		double maxValue = 0;
		for (Source s : sources) {
			double d = s.distance2(x, y, z);
			if(d < s.threshold)
			{
				d = Math.sqrt(d);
				double v = s.alpha / (1 + Math.pow(s.coef * d + 1, 3));
				maxValue = Math.max(v, maxValue);
			}
		}
		return sizeInf * (1 - maxValue);
	}
}
