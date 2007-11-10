/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005,2006, by EADS CRC
    Copyright (C) 2007, by EADS France

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

package org.jcae.mesh.amibe.algos2d;

import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.patch.VirtualHalfEdge2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import org.jcae.mesh.amibe.metrics.Metric3D;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * Split triangles with an absolute deflection greater than
 * requirements.  As explained in {@link Metric3D}, the geometric error
 * may exceed the desired value if triangles are too far away from local
 * tangent planes.  This algorithm computes the deflection of triangle
 * centroids, and if it is larger than the requested value, this
 * centroid is inserted into the mesh and incident edges are swapped
 * if they are not Delaunay.
 */
public class EnforceAbsDeflection
{
	private static Logger logger=Logger.getLogger(EnforceAbsDeflection.class);
	private final Mesh2D mesh;
	
	/**
	 * Creates a <code>EnforceAbsDeflection</code> instance.
	 *
	 * @param m  the <code>EnforceAbsDeflection</code> instance to check.
	 */
	public EnforceAbsDeflection(Mesh2D m)
	{
		mesh = m;
	}
	
	/**
	 * Check all triangles.
	 */
	public void compute()
	{
		mesh.pushCompGeom(3);
		logger.debug(" Enforcing absolute deflection");

		Vertex [] p = new Vertex[4];
		double [] v1 = new double[3];
		double [] v2 = new double[3];
		double [] v3 = new double[3];
		double [] v4 = new double[3];
		boolean redo = false;
		int niter = mesh.getTriangles().size();
		double defl = Metric3D.getDeflection();
		do
		{
			redo = false;
			ArrayList<Triangle> badTriangles = new ArrayList<Triangle>();
			for (Triangle t: mesh.getTriangles())
			{
				if (t.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				double uv[] = Vertex2D.centroid(mesh, (Vertex2D[]) t.vertex).getUV();
				double [] xyz = mesh.getGeomSurface().value(uv[0], uv[1]);
				// mesh.createVertex() cannot be used because mesh is a
				// Mesh2D instance and we want to create 3D instances.
				p[3] = new Vertex(null, xyz[0], xyz[1], xyz[2]);
				for (int i = 0; i < 3; i++)
				{
					uv = t.vertex[i].getUV();
					xyz = mesh.getGeomSurface().value(uv[0], uv[1]);
					p[i] = new Vertex(null, xyz[0], xyz[1], xyz[2]);
				}
				double [] xyz0 = p[0].getUV();
				double [] xyz1 = p[1].getUV();
				double [] xyz2 = p[2].getUV();
				double [] xyz3 = p[3].getUV();
				for (int i = 0; i < 3; i++)
				{
					v1[i] = xyz1[i] - xyz0[i];
					v2[i] = xyz2[i] - xyz0[i];
					v3[i] = xyz3[i] - xyz0[i];
				}
				Matrix3D.prodVect3D(v1, v2, v4);
				double norm = Matrix3D.norm(v4);
				if (norm > 0.0)
				{
					double dist = Math.abs(Matrix3D.prodSca(v4, v3));
					dist /= Matrix3D.norm(v4);
					if (dist > defl)
						badTriangles.add(t);
				}
			}
			for (Triangle t: badTriangles)
			{
				if (!mesh.getTriangles().contains(t) || t.hasAttributes(AbstractHalfEdge.BOUNDARY))
					continue;
				double uv[] = Vertex2D.centroid(mesh, (Vertex2D[]) t.vertex).getUV();
				Vertex2D v = (Vertex2D) mesh.createVertex(uv[0], uv[1]);
				VirtualHalfEdge2D vt = v.getSurroundingOTriangle(mesh);
				if (vt.split3(mesh, v, false))
					redo = true;
			}
			niter--;
			if (logger.isDebugEnabled())
				logger.debug(" Found "+badTriangles.size()+" non-conforming triangles");
		} while (redo && niter > 0);
		mesh.popCompGeom(3);
	}
	
}
