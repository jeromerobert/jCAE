/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005,2006, by EADS CRC
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
import org.jcae.mesh.amibe.patch.Vertex2D;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.metrics.KdTree;
import org.jcae.mesh.cad.CADGeomSurface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.Vertex;

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
	private static final Logger LOGGER=Logger.getLogger(ConstraintNormal3D.class.getName());
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
	public final void compute()
	{
		AbstractHalfEdge ot = null;
		AbstractHalfEdge sym = null;
		int cnt = 0;
		LOGGER.config("Enter compute()");
		mesh.pushCompGeom(3);
		LOGGER.fine(" Checking inverted triangles");

		double [] vect1 = new double[3];
		double [] vect2 = new double[3];
		double [] vect3 = new double[3];
		double [] vect4 = new double[3];

		boolean redo = false;
		KdTree kdTree = mesh.getKdTree();
		CADGeomSurface surface = mesh.getGeomSurface();
		int niter = mesh.getTriangles().size();
		Triangle.List newList = new Triangle.List();
		Collection<Triangle> oldList = new ArrayList<Triangle>(mesh.getTriangles());
		// Edges may have been marked by previous algorithms
		for (Triangle t : oldList)
		{
			ot = t.getAbstractHalfEdge(ot);
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				ot.clearAttributes(AbstractHalfEdge.SWAPPED);
			}
			if (sym == null)
				sym = t.getAbstractHalfEdge(sym);
		}

		do {
			redo = false;
			cnt = 0;
			niter--;
			for (Triangle t : oldList)
			{
				ot = t.getAbstractHalfEdge(ot);
				int l = -1;
				double best = 0.0;
				for (int i = 0; i < 3; i++)
				{
					ot = ot.next();
					if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD | AbstractHalfEdge.SHARP | AbstractHalfEdge.OUTER))
						continue;
					sym = ot.sym(sym);
					if (ot.hasAttributes(AbstractHalfEdge.SWAPPED) || sym.hasAttributes(AbstractHalfEdge.SWAPPED))
						continue;
					// Make sure that triangles are not
					// inverted in 2D space
					Vertex2D sa = (Vertex2D) sym.apex();
					Vertex2D oa = (Vertex2D) ot.apex();
					if (sa.onLeft(kdTree, (Vertex2D) ot.destination(), (Vertex2D) ot.apex()) <= 0L || oa.onLeft(kdTree, (Vertex2D) ot.origin(), (Vertex2D) sym.apex()) <= 0L)
						continue;
					// 3D coordinates of vertices
					Vertex p1 = ot.origin();
					Vertex p2 = ot.destination();
					Vertex apex1 = ot.apex();
					Vertex apex2 = sym.apex();
					double [] xo = surface.value(p1.getX(), p1.getY());
					double [] xd = surface.value(p2.getX(), p2.getY());
					double [] xa = surface.value(apex1.getX(), apex1.getY());
					double [] xn = surface.value(apex2.getX(), apex2.getY());
					surface.setParameter(0.5*(p1.getX()+p2.getX()), 0.5*(p1.getY()+p2.getY()));
					double [] normal = surface.normal();
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
					ot = t.getAbstractHalfEdge(ot);
					for (int i = 0; i <= l; i++)
						ot = ot.next();
					// Add adjacent triangles to newList
					for (int i = 0; i < 3; i++)
					{
						ot = ot.next();
						sym = ot.sym(sym);
						newList.addAllowDuplicates(sym.getTri());
					}
					ot = ot.sym();
					for (int i = 0; i < 3; i++)
					{
						ot = ot.next();
						sym = ot.sym(sym);
						newList.addAllowDuplicates(sym.getTri());
					}
					ot = ot.sym();
					mesh.edgeSwap(ot);

					cnt++;
				}
			}
			// Copy newList into oldList and clear SWAPPED attributes
			oldList.clear();
			for (Iterator<Triangle> it = newList.iterator(); it.hasNext(); )
			{
				Triangle t = it.next();
				ot = t.getAbstractHalfEdge(ot);
				for (int i = 0; i < 3; i++)
				{
					ot = ot.next();
					ot.clearAttributes(AbstractHalfEdge.SWAPPED);
				}
				oldList.add(t);
				it.remove();
			}
			assert newList.isEmpty() : "Triangles still in list: "+newList.size();
			LOGGER.fine(" Found "+cnt+" inverted triangles");
			//  The niter variable is introduced to prevent loops.
			//  With large meshes. its initial value may be too large,
			//  so we lower it now.
			if (niter > cnt)
				niter = cnt;
			if (cnt > 0)
				redo = true;
		} while (redo && niter > 0);
		mesh.popCompGeom(3);
		LOGGER.config("Leave compute()");
	}
	
}
