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
 * An AnalyticMetric which refine mesh around a set of points
 * @author Jerome Robert
 */
public class PointMetric implements Remesh.AnalyticMetricInterface {

	private class Source
	{
		public double sx,sy,sz;
		/** source_distance^3/target_size */
		public double coef;
		/** The target size at a d distance of the source */
		public double size;

		public double distance(double x, double y, double z)
		{
			double dx = sx-x;
			double dy = sy-y;
			double dz = sz-z;
			return Math.sqrt(dx*dx+dy*dy+dz*dz);
		}
	}
	/** The inverse of the target size at an infinit distance of any source */
	private double invHInf = 1.0;
	private List<Source> sources = new ArrayList<Source>();

	public PointMetric() {
	}

	public PointMetric(double defaultSize, double x, double y, double z, double d, double size) {
		addPoint(x,y,z,d,size);
	}

	public PointMetric(double defaultSize, String fileName) throws IOException {
		setGlobalSize(defaultSize);
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String buffer = br.readLine();
		while(buffer != null)
		{
			String[] line = buffer.trim().split("[\\s,;]+");
			double x = Double.parseDouble(line[0]);
			double y = Double.parseDouble(line[1]);
			double z = Double.parseDouble(line[2]);
			double d = Double.parseDouble(line[3]);
			double size = Double.parseDouble(line[4]);
			addPoint(x, y, z, d, size);
			buffer = br.readLine();
		}
		br.close();
	}

	/** Set the target size at an infinit distance of a source */
	public final void setGlobalSize(double s)
	{
		invHInf = 1/s;
	}

	/**
	 * Add a point around which to refine
	 * @param d Distance at which to define the target size
	 * @param size The target size at a d distance of the source
	 */
	public final void addPoint(double x, double y, double z, double d, double size)
	{
		Source s = new Source();
		s.coef = Math.pow(d, 3)/size;
		s.sx = x;
		s.sy = y;
		s.sz = z;
		s.size = size;
		sources.add(s);
	}

	@Override
	public double getTargetSize(double x, double y, double z) {
		double sum = invHInf;
		double minSize = Double.MAX_VALUE;
		for(Source s:sources)
		{
			sum += s.coef/Math.pow(s.distance(x, y, z), 3);
			minSize = Math.min(s.size, minSize);
		}
		return Math.max(1.0/sum, minSize);
	}
}
