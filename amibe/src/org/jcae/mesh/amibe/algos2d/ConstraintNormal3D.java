/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005,2006, by EADS CRC
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

import org.jcae.mesh.amibe.ds.TriangleVH;
import org.jcae.mesh.amibe.ds.AbstractTriangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.VirtualHalfEdge2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.apache.log4j.Logger;

/**
 * Swap edges if the normals to its adjacent triangles are too different
 * from the normal computed by the CAD engine.  Triangles in the 2D
 * parameter space must not be inverted, otherwise some methods do not
 * work any more.  But even in this case, if the surface parametrization
 * has large variations over a triangle, triangles may be inverted in
 * the 3D space.  The current algorithm is quite naive.  It works well,
 * so we did not try to find a better alternative.
 *
 * <p>
 * For each non-boundary edge, we first check that this edge can be
 * swapped without inverting triangles in 2D space.  The normal to the
 * surface at the middle of the edge is computed by the CAD engine.
 * Inner products between this vector and triangle normals are computed,
 * the quelity of this edge is the minimum of the two values.  The same
 * computations are performed on swapped triangles, and if the quality
 * is improved, this edge is indeed swapped.
 * </p>
 */
public class ConstraintNormal3D
{
	private static Logger logger=Logger.getLogger(ConstraintNormal3D.class);
	private final Mesh2D mesh;
	
	/**
	 * Creates a <code>ConstraintNormal3D</code> instance.
	 *
	 * @param m  the <code>ConstraintNormal3D</code> instance to check.
	 */
	public ConstraintNormal3D(Mesh2D m)
	{
		mesh = m;
	}
	
	/**
	 * Check all edges.
	 */
	public void compute()
	{
		TriangleVH t;
		VirtualHalfEdge2D ot, sym;
		int cnt = 0;
		mesh.pushCompGeom(3);
		logger.debug(" Checking inverted triangles");
		ot = new VirtualHalfEdge2D();
		sym = new VirtualHalfEdge2D();
		double [] vect1 = new double[3];
		double [] vect2 = new double[3];
		double [] vect3 = new double[3];
		double [] vect4 = new double[3];

		boolean redo = false;
		int niter = mesh.getTriangles().size();
		do {
			redo = false;
			cnt = 0;
			niter--;
			for (AbstractTriangle at: mesh.getTriangles())
			{
				t = (TriangleVH) at;
				ot.bind(t);
				for (int i = 0; i < 3; i++)
				{
					ot.next();
					ot.clearAttributes(AbstractHalfEdge.SWAPPED);
				}
			}
			
			for (AbstractTriangle at: mesh.getTriangles())
			{
				t = (TriangleVH) at;
				ot.bind(t);
				int l = -1;
				double best = 0.0;
				for (int i = 0; i < 3; i++)
				{
					ot.next();
					if (!ot.isMutable())
						continue;
					sym.bind((TriangleVH) ot.getTri(), ot.getLocalNumber());
					sym.sym();
					if (ot.hasAttributes(AbstractHalfEdge.SWAPPED) || sym.hasAttributes(AbstractHalfEdge.SWAPPED))
						continue;
					// Make sure that triangles are not
					// inverted in 2D space
					Vertex2D sa = (Vertex2D) sym.apex();
					Vertex2D oa = (Vertex2D) ot.apex();
					if (sa.onLeft(mesh, (Vertex2D) ot.destination(), (Vertex2D) ot.apex()) <= 0L || oa.onLeft(mesh, (Vertex2D) ot.origin(), (Vertex2D) sym.apex()) <= 0L)
						continue;
					// 3D coordinates of vertices
					double p1[] = ot.origin().getUV();
					double p2[] = ot.destination().getUV();
					double apex1[] = ot.apex().getUV();
					double apex2[] = sym.apex().getUV();
					double [] xo = mesh.getGeomSurface().value(p1[0], p1[1]);
					double [] xd = mesh.getGeomSurface().value(p2[0], p2[1]);
					double [] xa = mesh.getGeomSurface().value(apex1[0], apex1[1]);
					double [] xn = mesh.getGeomSurface().value(apex2[0], apex2[1]);
					mesh.getGeomSurface().setParameter(0.5*(p1[0]+p2[0]), 0.5*(p1[1]+p2[1]));
					double [] normal = mesh.getGeomSurface().normal();
					for (int k = 0; k < 3; k++)
					{
						vect1[k] = xd[k] - xo[k];
						vect2[k] = xa[k] - xo[k];
						vect3[k] = xn[k] - xo[k];
					}
					Matrix3D.prodVect3D(vect1, vect2, vect4);
					double norm = Matrix3D.norm(vect4);
					if (norm < 1.e-20)
						norm = 1.0;
					double scal1 = Matrix3D.prodSca(normal, vect4) / norm;
					Matrix3D.prodVect3D(vect3, vect1, vect4);
					norm = Matrix3D.norm(vect4);
					if (norm < 1.e-20)
						norm = 1.0;
					double scal2 = Matrix3D.prodSca(normal, vect4) / norm;
					// No need to check further if triangles are good enough
					if (scal1 > 0.4 && scal2 > 0.4)
						continue;
					// Check if the swapped triangle is better
					for (int k = 0; k < 3; k++)
					{
						vect1[k] = xa[k] - xn[k];
						vect2[k] = xo[k] - xn[k];
						vect3[k] = xd[k] - xn[k];
					}
					Matrix3D.prodVect3D(vect1, vect2, vect4);
					norm = Matrix3D.norm(vect4);
					if (norm < 1.e-20)
						norm = 1.0;
					double scal3 = Matrix3D.prodSca(normal, vect4) / norm;
					Matrix3D.prodVect3D(vect3, vect1, vect4);
					norm = Matrix3D.norm(vect4);
					if (norm < 1.e-20)
						norm = 1.0;
					double scal4 = Matrix3D.prodSca(normal, vect4) / norm;
					double res = Math.min(scal3, scal4) - Math.min(scal1, scal2);
					if (res > best)
					{
						best = res;
						l = i;
					}
				}
				if (l >= 0)
				{
					ot.bind(t);
					for (int i = 0; i <= l; i++)
						ot.next();
					mesh.edgeSwap(ot);
					cnt++;
				}
			}
			logger.debug(" Found "+cnt+" inverted triangles");
			//  The niter variable is introduced to prevent loops.
			//  With large meshes. its initial value may be too large,
			//  so we lower it now.
			if (niter > cnt)
				niter = cnt;
			if (cnt > 0)
				redo = true;
		} while (redo && niter > 0);
		mesh.popCompGeom(3);
	}
	
}
