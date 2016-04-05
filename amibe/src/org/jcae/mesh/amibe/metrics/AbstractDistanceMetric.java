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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Locale;

import java.util.logging.Logger;

/**
 * An abstract AnalyticMetric which refines a mesh around a set of sources.
 * A source is a point, a line, etc. around which a metric is defined as
 * follows:
 * S0 if d &lt; d0, Sinf if d &gt; d1 and some interpolation to be implemented
 * between d0 and d1.
 * Sinf is the mesh size far from the point, S0 is the mesh size on the
 * source, d is the distance from the source.
 * A source must implement a distance method that returns the distance from a
 * point to that source.
 * A scaling may be applied using the setScaling method. Only the getTargetSize
 * and getTargetSizeTopo will take scaling into account. The getSize method
 * won't.
 */
public abstract class AbstractDistanceMetric extends MetricSupport.AnalyticMetric {

	public abstract class DistanceMetricInterface
	{
		public abstract double getSqrDistance(double x, double y, double z);
		public double sqrD0;
		public double size0;
		/**
		 * if the distance^2 is greater than this value this source is not
		 * considered
		 */
		public double sqrD1;
		/** cache for sqrD0 - sqrD1 */
		public double delta;
		/** cache for sqrD0 / delta */
		public double ratio;
		/** singularity order */
		public double alpha;
	}

	protected class PointSource extends DistanceMetricInterface
	{
		public final double sx,sy,sz;

		public PointSource(final double sx, final double sy, final double sz)
		{
			this.sx = sx;
			this.sy = sy;
			this.sz = sz;
		}

		public double getSqrDistance(final double x, final double y, final double z)
		{
			final double dx = sx-x;
			final double dy = sy-y;
			final double dz = sz-z;
			return dx*dx+dy*dy+dz*dz;
		}
	}

	protected class LineSource extends DistanceMetricInterface
	{
		protected final double sx0,sy0,sz0;
		protected final double sx1,sy1,sz1;
		protected final boolean closed0, closed1;
		protected final double [] dir = new double[3];
		protected final double maxAbscissa;

		public LineSource(final double sx0, final double sy0, final double sz0,
			final boolean closed0, final double sx1, final double sy1,
			final double sz1, final boolean closed1)
		{
			this.sx0 = sx0;
			this.sy0 = sy0;
			this.sz0 = sz0;
			this.sx1 = sx1;
			this.sy1 = sy1;
			this.sz1 = sz1;
			this.closed0 = closed0;
			this.closed1 = closed1;
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

		public double getSqrDistance(final double x, final double y, final double z)
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
			return dx * dx + dy * dy + dz * dz;
		}
	}


	protected final List<DistanceMetricInterface> sources = new ArrayList<DistanceMetricInterface>();
	protected double sizeInf;
	/**
	 * maximum length ratio between two adjacent edges; this parameter is
	 * used in the numeric (and mixed) metric
	 */
	protected double rho = 0.0;
	/** choose analytic metric with numerical criterion if true */
	protected boolean mixed = false;
	protected double scaling = 1.0;
	public AbstractDistanceMetric(double sizeInf) {
		this.sizeInf = sizeInf;
	}

	/**
	 * Initialize the sources
	 * @param fileName file defining the sources, format may changes within implementing
	 * subclasses.
	 */
	protected abstract void initSources(String fileName) throws IOException;
	
	public AbstractDistanceMetric(double sizeInf, String fileName) throws IOException {
		this(sizeInf);
		initSources(fileName);
	}

	public AbstractDistanceMetric(double sizeInf, String fileName, double rho) throws
		IOException
	{
		this(sizeInf, fileName);
		if(rho <= 1.0)
			throw new IllegalArgumentException(rho+" <= 1.0");
		this.rho = rho;
	}

	public AbstractDistanceMetric(double sizeInf, String fileName, double rho, boolean mixed) throws
		IOException
	{
		this(sizeInf, fileName, rho);
		this.mixed = mixed;
	}

	public void setScaling(double v)
	{
		this.scaling = v;
	}

	/** Must be called with sizeInf is changed */
	protected void update(DistanceMetricInterface ps)
	{
		if(ps.sqrD1 < ps.sqrD0)
			ps.sqrD1 = sizeInf * sizeInf * 4;
		ps.delta = ps.sqrD1 - ps.sqrD0;
		ps.ratio = ps.sqrD0 / ps.delta;
	}

	/**
	 * Compute the value of the isotropic metric at a node
	 * @param x x-coordinate of the node
	 * @param y y-coordinate of the node
	 * @param z z-coordinate of the node
	 * @param groupId ID of the element group
	 */
	@Override
	public double getTargetSize(double x, double y, double z, int groupId)
	{
		if(rho > 1.0)
			if(mixed)
				return getTargetSizeMixed(x, y, z, groupId);
			else
				return getTargetSizeNumeric(x, y, z, groupId);
		else
			return getTargetSizeAnalytic(x, y, z, groupId);
	}

	/**
	 * Compute the mixed (analytic-numeric) isotropic metric at a node
	 */
	public double getTargetSizeMixed(double x, double y, double z,
		int groupId)
	{
		double ha = getTargetSizeAnalytic(x, y, z, groupId);
		double hn = getTargetSizeNumeric(x, y, z, groupId);
		return Math.min(ha, hn);
	}

	/**
	 * Compute the analytic isotropic metric at a node
	 */
	public abstract double getTargetSizeAnalytic(double x, double y, double z,
		int groupId);

	/**
	 * Compute the numeric isotropic metric at a node
	 */
	public double getTargetSizeNumeric(double x, double y, double z,
		int groupId)
	{
		double minValue = getSize(groupId);
		for (DistanceMetricInterface s : sources) {
			double d = Math.sqrt(s.getSqrDistance(x, y, z));
			double v;
			/** constant metric [0 s.size0] */
			if(d < s.size0)
				v = s.size0;
			/** geometric interpolation on first interval */
			else if(d < s.size0 * (1. + rho))
			{
				double t = (d - s.size0) / (rho * s.size0);
				v = s.size0 * Math.pow(rho, t);
			}
			/** linear interpolation otherwise */
			else
			{
				double deltaS = sizeInf - s.size0;
				double arho = (rho - 1.0) / rho;
				double drho = s.size0 + deltaS / arho;
				if (d > drho)
					v = sizeInf;
				else
					v = s.size0 + arho * (d - s.size0);
			}
			minValue = Math.min(v, minValue);
		}
		return minValue * scaling;
	}

	public double getSize(int group)
	{
		return sizeInf;
	}

	/**
	 * Save the metric to a binary file.
	 * Do not use this for long term storage, the format may change.
	 */
	public void save(String fileName) throws IOException
	{
		FileChannel fc = new FileOutputStream(fileName).getChannel();
		save(fc);
		fc.close();
	}

	public abstract void save(WritableByteChannel out) throws IOException;

	/** Read a metric in binary format */
	public void load(String fileName) throws IOException
	{
		FileChannel channel = new FileInputStream(fileName).getChannel();
		load(channel);
		channel.close();
	}

	public abstract void load(ReadableByteChannel in) throws IOException;

	/**
	 * Save the metric to a text file.
	 * Do not use this for long term storage, the format may change.
	 */
	public void saveTxt(String fileName) throws IOException
	{
		PrintWriter out = new PrintWriter(fileName);
		saveTxt(out);
		out.close();
	}

	public abstract void saveTxt(PrintWriter out) throws IOException;

}
