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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;

/**
 * An AnalyticMetric which refine mesh around a set of points and lines.
 * The metric around each point is:
 * S0 if d &lt; d0, Sinf if d &gt; d1 and quadradic interpolation between both.
 * Sinf is the mesh size far from the point, S0 is the mesh size on the
 * point, d is the distance from the point.
 * A scaling may be applied using the setScaling method. Only the getTargetSize
 * and getTargetSizeTopo will take scaling into account. The getSize method
 * won't.
 * @author Jerome Robert
 */
public class DistanceMetric implements MetricSupport.AnalyticMetricInterface {

	private static final Logger LOGGER=Logger.getLogger(DistanceMetric.class.getName());

	public abstract class DistanceMetricInterface
	{
		public abstract double getSqrDistance(double x, double y, double z);
		public double sqrD0;
		public double size0;
		/**
		 * if the distance^2 is greater than this value this source is not
		 * concidered
		 */
		public double sqrD1;
		/** cache for sqrD0 - sqrD1 */
		public double delta;
		/** cache for sqrD0 / delta */
		public double ratio;
	}

	private class PointSource extends DistanceMetricInterface
	{
		private final double sx,sy,sz;

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
		private final double sx0,sy0,sz0;
		private final double sx1,sy1,sz1;
		private final boolean closed0, closed1;
		private final double [] dir = new double[3];
		private final double maxAbscissa;

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
	protected double scaling = 1.0;
	public DistanceMetric(double sizeInf) {
		this.sizeInf = sizeInf;
	}

	public DistanceMetric(double sizeInf, String fileName) throws IOException {
		this(sizeInf);
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
				addPoint(x, y, z, size0, d0, d1);
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
				addLine(x0, y0, z0, closed0, x1, y1, z1, closed1, size0, d0, d1);
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
		double d0, double d1)
	{
		PointSource ps = new PointSource(x, y, z);
		ps.size0 = size0;
		ps.sqrD0 = d0 * d0;
		ps.sqrD1 = d1 * d1;
		sources.add(ps);
		update(ps);
	}

	/**
	 * Add a line around which to refine
	 * @param size0 metric on the point
	 * @param coef how fast we go from size0 to sizeInf
	 * @param closed0 true for segment, false for an infinit line, or half
	 * infinit line, depending on closed1
	 */
	public final void addLine(
		final double x0, final double y0, final double z0, final boolean closed0,
		final double x1, final double y1, final double z1, final boolean closed1,
		double size0, double d0, double d1)
	{
		LineSource ps = new LineSource(x0, y0, z0, closed0, x1, y1, z1, closed1);
		ps.size0 = size0;
		ps.sqrD0 = d0 * d0;
		ps.sqrD1 = d1 * d1;
		sources.add(ps);
		update(ps);
	}

	public void setScaling(double v)
	{
		this.scaling = v;
	}

	/** Must be called with sizeInf is changed */
	private void update(DistanceMetricInterface ps)
	{
		if(ps.sqrD1 < ps.sqrD0)
			ps.sqrD1 = sizeInf * sizeInf * 4;
		ps.delta = ps.sqrD1 - ps.sqrD0;
		ps.ratio = ps.sqrD0 / ps.delta;
	}

	@Override
	public double getTargetSize(double x, double y, double z, int groupId) {
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
				v = deltaS * d2 / s.delta + (s.size0 - s.ratio * deltaS);
			}
			minValue = Math.min(v, minValue);
		}
		return minValue * scaling;
	}

	@Override
	public double getTargetSizeTopo(Mesh mesh, Vertex v)
	{
		double[] uv = v.getUV();
		int groupId = -1;
		if(v.isManifold())
			groupId = ((Triangle)v.getLink()).getGroupId();
		return getTargetSize(uv[0], uv[1], uv[2], groupId);
	}

	public void save(String fileName) throws IOException
	{
		FileChannel fc = new FileOutputStream(fileName).getChannel();
		save(fc);
		fc.close();
	}

	public void save(WritableByteChannel out) throws IOException
	{
		ArrayList<PointSource> ps = new ArrayList<PointSource>();
		ArrayList<LineSource> ls = new ArrayList<LineSource>();
		for(DistanceMetricInterface s:sources)
		{
			if(s instanceof PointSource)
				ps.add((PointSource)s);
			else
				ls.add((LineSource)s);
		}
		ByteBuffer bb = ByteBuffer.allocate(
			ps.size() * 6 * 8 + ls.size() * 9 * 8 + 2 * 4);
		bb.order(ByteOrder.nativeOrder());
		bb.putInt(ps.size());
		for(PointSource s:ps)
		{
			bb.putDouble(s.size0);
			bb.putDouble(s.sqrD0);
			bb.putDouble(s.sqrD1);
			bb.putDouble(s.sx);
			bb.putDouble(s.sy);
			bb.putDouble(s.sz);
		}
		bb.putInt(ls.size());
		for(LineSource s:ls)
		{
			bb.putDouble(s.size0);
			bb.putDouble(s.sqrD0);
			bb.putDouble(s.sqrD1);
			bb.putDouble(s.sx0);
			bb.putDouble(s.sy0);
			bb.putDouble(s.sz0);
			bb.putDouble(s.sx1);
			bb.putDouble(s.sy1);
			bb.putDouble(s.sz1);
		}
		bb.rewind();
		out.write(bb);
	}

	public double getSize(int group)
	{
		return sizeInf;
	}
}
