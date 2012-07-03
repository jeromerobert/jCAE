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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * An AnalyticMetric which refine mesh around a line.
 * The metric around each point is:
 * Sinf * (1 - 2 * (1 - S0 / Sinf) / (1 + (R * d + 1) ^ 3 ) )
 * where Sinf is the mesh size far from the line, S0 is the mesh size on the
 * line, d is the distance from the line and R a coeficient saying how fast
 * Sinf is reached.
 * @author Denis Barbier
 */
public class LineMetric implements MetricSupport.AnalyticMetricInterface {

	private static final Logger LOGGER=Logger.getLogger(LineMetric.class.getName());

	private static class Source
	{
		private final double sx0,sy0,sz0;
		private final double sx1,sy1,sz1;
		private final boolean closed0, closed1;
		private final double [] dir = new double[3];
		private final double maxAbscissa;
		/** alpha = 2 * (1 - SO / Sinf) */
		private final double alpha;
		private final double coef;
		/**
		 * if the distance^2 is greater than this value this source is not
		 * considered
		 */
		private final double threshold;

		public Source(final double sx0, final double sy0, final double sz0, final boolean closed0,
			final double sx1, final double sy1, final double sz1, final boolean closed1,
			final double alpha, final double coef, final double threshold)
		{
			this.sx0 = sx0;
			this.sy0 = sy0;
			this.sz0 = sz0;
			this.sx1 = sx1;
			this.sy1 = sy1;
			this.sz1 = sz1;
			this.closed0 = closed0;
			this.closed1 = closed1;
			this.alpha = alpha;
			this.coef = coef;
			this.threshold = threshold;
			this.dir[0] = this.sx1 - this.sx0;
			this.dir[1] = this.sy1 - this.sy0;
			this.dir[2] = this.sz1 - this.sz0;
			final double norm = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
			if (norm < 1.e-20)
				throw new IllegalArgumentException("Endpoints must be different");
			final double invNorm = 1.0 / norm;
			this.dir[0] *= invNorm;
			this.dir[1] *= invNorm;
			this.dir[2] *= invNorm;
			if (closed0 && closed1) {
				maxAbscissa = norm;
			} else {
				maxAbscissa = Double.MAX_VALUE;
			}
		}

		public double distance2(final double x, final double y, final double z)
		{
			// Compute the projection on the line
			double dx = x - sx0;
			double dy = y - sy0;
			double dz = z - sz0;
			double abscissa = dx * dir[0] + dy * dir[1] + dz * dir[2];
			if (closed0 && abscissa < 0.0) {
				abscissa = 0.0;
			}
			if (closed1 && abscissa > maxAbscissa) {
				abscissa = maxAbscissa;
			}
			dx -= dir[0] * abscissa;
			dy -= dir[1] * abscissa;
			dz -= dir[2] * abscissa;
			return dx*dx+dy*dy+dz*dz;
		}
	}

	private final transient List<Source> sources = new ArrayList<Source>();
	private final transient double sizeInf;
	public LineMetric(final double sizeInf) {
		this.sizeInf = sizeInf;
	}

	public LineMetric(final double sizeInf,
		final double x0, final double y0, final double z0, final boolean closed0,
		final double x1, final double y1, final double z1, final boolean closed1,
		final double size0, final double coef)
	{
		this(sizeInf);
		addLine(x0, y0, z0, closed0, x1, y1, z1, closed1, size0, coef);
	}

	public LineMetric(final double sizeInf, final String fileName) throws IOException {
		this(sizeInf);
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String buffer = br.readLine();
		while(buffer != null)
		{
			String[] line = buffer.trim().split("[\\s,;]+");
			double x0 = Double.parseDouble(line[0]);
			double y0 = Double.parseDouble(line[1]);
			double z0 = Double.parseDouble(line[2]);
			boolean closed0 = (Integer.parseInt(line[3]) != 0);
			double x1 = Double.parseDouble(line[4]);
			double y1 = Double.parseDouble(line[5]);
			double z1 = Double.parseDouble(line[6]);
			boolean closed1 = (Integer.parseInt(line[7]) != 0);
			double size0 = Double.parseDouble(line[8]);
			double coef = Double.parseDouble(line[9]);
			addLine(x0, y0, z0, closed0, x1, y1, z1, closed1, size0, coef);
			buffer = br.readLine();
		}
		br.close();
	}

	/**
	 * Add a line around which to refine
	 * @param size0 metric on the point
	 * @param coef how fast we go from size0 to sizeInf
	 */
	public final void addLine(
		final double x0, final double y0, final double z0, final boolean closed0,
		final double x1, final double y1, final double z1, final boolean closed1,
		final double size0, final double coef)
	{
		double alpha = 2.0 * (1.0 - size0 / sizeInf);
		if (alpha < 0.05)
		{
			// Do nothing
			LOGGER.warning("Source line ignored, size should be lower than target size at infinity");
			return;
		}
		double scoef = 1.0 / (Math.log(sizeInf / size0) * (sizeInf + size0) * coef);
		double threshold = (Math.pow(alpha/0.05 - 1.0, 1.0/3.0) - 1.0) / scoef;
		threshold = threshold * threshold;
		Source s = new Source(x0, y0, z0, closed0, x1, y1, z1, closed1, alpha, scoef, threshold);
		sources.add(s);
	}

	@Override
	public double getTargetSize(final double x, final double y, final double z) {
		double maxValue = 0.0;
		for (Source s : sources) {
			double d2 = s.distance2(x, y, z);
			if(d2 < s.threshold)
			{
				double d = Math.sqrt(d2) * s.coef + 1.0;
				double v = s.alpha / (1.0 + d * d * d);
				maxValue = Math.max(v, maxValue);
			}
		}
		return sizeInf * (1.0 - maxValue);
	}
}
