/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005,2006, by EADS CRC
    Copyright (C) 2007,2008, by EADS France

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
import org.jcae.mesh.amibe.metrics.Matrix3D;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Split triangles with an absolute deflection greater than
 * requirements.  As explained in {@link org.jcae.mesh.amibe.patch.MetricBuilder},
 * the geometric error may exceed the desired value if triangles are too far
 * away from local tangent planes.  This algorithm computes the deflection of
 * triangle centroids, and if it is larger than the requested value, this
 * centroid is inserted into the mesh and incident edges are swapped if they
 * are not Delaunay.
 */
public class EnforceAbsDeflection
{
	private static final Logger LOGGER=Logger.getLogger(EnforceAbsDeflection.class.getName());
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
	public final void compute()
	{
		mesh.pushCompGeom(3);
		LOGGER.fine(" Enforcing absolute deflection");

		Vertex [] p = new Vertex[4];
		double [] v1 = new double[3];
		double [] v2 = new double[3];
		double [] v3 = new double[3];
		double [] v4 = new double[3];
		boolean redo = false;
		int niter = mesh.getTriangles().size();
		double defl = mesh.getMeshParameters().getDeflection();
		Vertex2D c = (Vertex2D) mesh.createVertex(0.0, 0.0);
		do
		{
			redo = false;
			ArrayList<Triangle> badTriangles = new ArrayList<Triangle>();
			for (Triangle t: mesh.getTriangles())
			{
				if (t.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				mesh.moveVertexToCentroid(c, t);
				double [] xyz = mesh.getGeomSurface().value(c.getX(), c.getY());
				// mesh.createVertex() cannot be used because mesh is a
				// Mesh2D instance and we want to create 3D instances.
				p[3] = new Vertex(null, xyz[0], xyz[1], xyz[2]);
				for (int i = 0; i < 3; i++)
				{
					Vertex v = t.vertex[i];
					xyz = mesh.getGeomSurface().value(v.getX(), v.getY());
					p[i] = new Vertex(null, xyz[0], xyz[1], xyz[2]);
				}
				p[1].sub(p[0], v1);
				p[2].sub(p[0], v2);
				p[3].sub(p[0], v3);
				Matrix3D.prodVect3D(v1, v2, v4);
				double norm = Matrix3D.norm(v4);
				if (norm > 0.0)
				{
					if (Math.abs(Matrix3D.prodSca(v4, v3)) > defl * norm)
						badTriangles.add(t);
				}
			}
			for (Triangle t: badTriangles)
			{
				if (!mesh.getTriangles().contains(t) || t.hasAttributes(AbstractHalfEdge.BOUNDARY))
					continue;
				mesh.moveVertexToCentroid(c, t);
				Vertex2D v = (Vertex2D) mesh.createVertex(c.getX(), c.getY());
				VirtualHalfEdge2D vt = v.getSurroundingOTriangle(mesh);
				if (vt.split3(mesh, v, null, false) != 0)
					redo = true;
			}
			niter--;
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.fine(" Found "+badTriangles.size()+" non-conforming triangles");
		} while (redo && niter > 0);
		mesh.popCompGeom(3);
	}
	
}
