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
import java.util.logging.Logger;

/**
 * An AbstractDistanceMetric which refine mesh around a set of points and lines.
 * The metric around each point is:
 * S0 if d &lt; d0, Sinf if d &gt; d1 and a family of interpolation between both
 * depending on a alpha parameter.
 * Sinf is the mesh size far from the point, S0 is the mesh size on the
 * point, d is the distance from the point.
 * A scaling may be applied using the setScaling method. Only the getTargetSize
 * and getTargetSizeTopo will take scaling into account. The getSize method
 * won't.
 * @author Jerome Robert
 */
public class SingularMetric extends AbstractDistanceMetric {

	private static final Logger LOGGER=Logger.getLogger(SingularMetric.class.getName());

	public SingularMetric(double sizeInf) {
		super(sizeInf);
	}

	public SingularMetric(double sizeInf, String fileName) throws IOException {
		super(sizeInf, fileName);
	}

	public SingularMetric(double sizeInf, String fileName, double rho) throws
		IOException
	{
		super(sizeInf, fileName, rho);
	}

	public SingularMetric(double sizeInf, String fileName, double rho, boolean mixed) throws
		IOException
	{
		super(sizeInf, fileName, rho, mixed);
	}

	@Override
	protected final void initSources(String fileName) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String buffer;
		while((buffer = br.readLine()) != null)
		{
			if (buffer.startsWith("#"))
				continue;
			buffer = buffer.trim();
			if(buffer.isEmpty())
				continue;
			String[] line = buffer.split("[\\s,;]+");
			int type = Integer.parseInt(line[0]);
			if (type == 1)
			{
				double x = Double.parseDouble(line[1]);
				double y = Double.parseDouble(line[2]);
				double z = Double.parseDouble(line[3]);
				double size0 = Double.parseDouble(line[4]);
				double d0 = Double.parseDouble(line[5]);
				double d1 = Double.parseDouble(line[6]);
				double alpha = Double.parseDouble(line[7]);
				addPoint(x, y, z, size0, d0, d1, alpha);
			} else if (type == 2)
			{
				double x0 = Double.parseDouble(line[1]);
				double y0 = Double.parseDouble(line[2]);
				double z0 = Double.parseDouble(line[3]);
				boolean closed0 = (Integer.parseInt(line[4]) != 0);
				double x1 = Double.parseDouble(line[5]);
				double y1 = Double.parseDouble(line[6]);
				double z1 = Double.parseDouble(line[7]);
				boolean closed1 = (Integer.parseInt(line[8]) != 0);
				double size0 = Double.parseDouble(line[9]);
				double d0 = Double.parseDouble(line[10]);
				double d1 = Double.parseDouble(line[11]);
				double alpha = Double.parseDouble(line[12]);
				addLine(x0, y0, z0, closed0, x1, y1, z1, closed1, size0, d0, d1, alpha);
			}
			else
				throw new IllegalArgumentException("Invalid line found in file "+fileName+": "+line);
		}
		br.close();
	}

	/**
	 * Add a point around which to refine
	 * @param size0 metric on the point
	 * @param sizeInf metric far from the point
	 * @param coef how fast we go from size0 to sizeInf
	 */
	public final void addPoint(double x, double y, double z, double size0,
		double d0, double d1, double alpha)
	{
		PointSource ps = new PointSource(x, y, z);
		ps.size0 = size0;
		ps.sqrD0 = d0 * d0;
		ps.sqrD1 = d1 * d1;
		if(alpha <= 0.0)
			throw new IllegalArgumentException(alpha+" <= 0.0");
		ps.alpha = alpha;
		sources.add(ps);
		update(ps);
	}

	/**
	 * Add a line around which to refine
	 * @param size0 metric on the point
	 * @param coef how fast we go from size0 to sizeInf
	 * @param closed0 true for segment, false for an infinite line, or half
	 * infinite line, depending on closed1
	 */
	public final void addLine(
		final double x0, final double y0, final double z0, final boolean closed0,
		final double x1, final double y1, final double z1, final boolean closed1,
		double size0, double d0, double d1, double alpha)
	{
		LineSource ps = new LineSource(x0, y0, z0, closed0, x1, y1, z1, closed1);
		ps.size0 = size0;
		ps.sqrD0 = d0 * d0;
		ps.sqrD1 = d1 * d1;
		if(alpha <= 0.0)
			throw new IllegalArgumentException(alpha+" <= 0.0");
		ps.alpha = alpha;
		sources.add(ps);
		update(ps);
	}


	/**
	 * Compute the analytic isotropic metric at a node
	 */
	@Override
	public double getTargetSizeAnalytic(double x, double y, double z,
		int groupId)
	{
		double minValue = getSize(groupId);
		for (DistanceMetricInterface s : sources) {
			double d2 = s.getSqrDistance(x, y, z);
			double v;
			if(d2 > s.sqrD1)
				v = sizeInf;
			else if(d2 < s.sqrD0)
				v = s.size0;
			else
			{
				double deltaS = sizeInf - s.size0;
				double deltaD = Math.sqrt(d2) - Math.sqrt(s.sqrD0);
				double deltaD1 = Math.sqrt(s.sqrD1) - Math.sqrt(s.sqrD0);
				double ratio = deltaD / deltaD1;
				v = s.size0 + deltaS * Math.pow(ratio, 1.0 + s.alpha);
			}
			minValue = Math.min(v, minValue);
		}
		return minValue * scaling;
	}

}
