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
 * (C) Copyright 2013, by EADS France
 */

package org.jcae.mesh.stitch;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.junit.Test;

/**
 *
 * @author Jerome Robert
 */
public class TriangleHelperTest {
	@Test public void test()
	{
		Mesh mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
		Vertex v0 = mesh.createVertex(0, 0, 0);
		Vertex v1 = mesh.createVertex(1, 0, 0);
		Vertex v2 = mesh.createVertex(0, 1, 0);
		Triangle triangle = mesh.createTriangle(v0, v1, v2);
		TriangleHelper th = new TriangleHelper(triangle);
		TriangleSplitter ts = new TriangleSplitter();
		ts.setTriangle(th);
		ts.split(new Location(-0.5, 0.5, 0), new Location(2, 0.5, 0), .1);
		assert ts.getSplitVertex(mesh).sqrDistance3D(new Location(0,0.5,0)) < 1E-6: ts.getSplitVertex(
			mesh)+" "+ts.getSplittedEdge();

		v2.moveTo(0.5, 0.1, 0.1);
		assert TriangleHelper.sqrDistance(v0, v1, v2) - 0.02 < 1E-8;

		v0.moveTo(5500.0, 3500.0, 0.0);
		v1.moveTo(-500.0, 3500.0, 0.0);
		v2.moveTo(295.4250418574649, 928.3645397543528, -19.548979250276396);
		th.setTriangle(th.getTriangle());
		Location p1 = new Location(-1.8504563775477978, 1000.2540420059955, -19.002491845838513);
		Location p2 = new Location(195.4932520809972, 980.6765334491752, -19.151315547512326);
		ts.split(p1, p2, 0.010000000000000002);
		assert ts.getSplitVertex(mesh) == null;

		v0.moveTo(4000.0, -2250.0, -1000.0);
		v1.moveTo(-2000.0, 3000.0, -1000.0);
		v2.moveTo(1000.0, 750.0, -1000.0);
		th.setTriangle(th.getTriangle());
		ts.split(new Location(1000.0, -334.87, -1000.0),
			new Location(1000.0, 0, -1000.0), 1);
		assert ts.getSplittedEdge() == null;

		v0.moveTo(5500.0, 3500.0, 0.0);
		v1.moveTo(-500.0, 3500.0, 0.0);
		v2.moveTo(-500.0, 500.0, 0.0);
		th.setTriangle(th.getTriangle());
		p1 = new Location(555.5819503922005, 831.444099215599, 0.0);
		p2 = new Location(707.106, 707.108, 0.0);
		ts.split(p1, p2, 0.010000000000000002);
		assert ts.getSplitVertex(mesh) == null;
	}
}
