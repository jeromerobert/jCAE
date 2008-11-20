/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.amibe.projection;

import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.metrics.Metric3D;

/**
 * Project a point on the approximated surface.  This algorithm is
 * described by Pascal J. Frey in
 * <a href="http://www.lis.inpg.fr/pages_perso/attali/DEA-IVR/PAPERS/frey00.ps">About Surface Remeshing</a>.
 * The idea if to approximate locally the surface by a quadric
 * <code>F(x,y) = a x^2 + b xy + c y^2 - z</code>.
 * To that end, the local frame at the current vertex is
 * computed.  The <code>(x,y)</code> coordinates of neighbour
 * vertices are computed in that frame, and we search for the quadric
 * which fits best for all neighbour vertices (in a least squares
 * sense).  The vertex is then projected onto this quadric.
 *
 * Note1: Several improvements exist in the litterature, see eg.
 * <a href="http://prism.asu.edu/research/data/publications/paper05_cestmubbp.pdf">this paper</a>
 * by Anshuman Razdan and MyungSoo Bae for a survey of several
 * methods.
 * Note2: According to Pascal J. Frey, the key point is to have
 * reliable input.  We can have good approximation of quadrics
 * if the normal to the surface is accurate, and normal to the
 * surface can be approximated accurately if the quadric is
 * precise.  So we should certainly read normals from a file
 * if they are available.
 */
public class QuadricProjection implements LocalSurfaceProjection
{
	final Matrix3D qP;
	final double[] origin = new double[3];
	final double[] qD;

	public QuadricProjection(Vertex o)
	{
		double [] param = o.getUV();
		for (int i = 0; i < 3; i++)
			origin[i] = param[i];

		// Transformation matrix
		qP = getMatrix3DLocalFrame(o);
		if (qP == null)
		{
			qD = null;
			return;
		}
		qD = getLocalQuadric(o, qP);
	}

	/**
	 * Project a point on this quadric.
	 *
	 * @param pt   point to project on the approximated surface.
	 * @return <code>true</code> if projection has been performed
	 * successfully, <code>false</code> otherwise.
	 */
	public boolean project(Vertex pt)
	{
		double [] glob = new double[3];
		double [] param = pt.getUV();
		for (int i = 0; i < 3; i++)
			glob[i] = param[i] - origin[i];

		// Local coordinates
		double [] loc = new double[3];
		qP.apply(glob, loc);
		// Compute z = a x^2 + b xy + c y^2
		loc[2] = qD[0] * loc[0] * loc[0] + qD[1] * loc[0] * loc[1] + qD[2] * loc[1] * loc[1];
		// Reuse glob
		qP.transp();
		qP.apply(loc, glob);
		qP.transp();
		pt.moveTo(origin[0] + glob[0], origin[1] + glob[1], origin[2] + glob[2]);
		return true;
	}
	
	public boolean canProject()
	{
		return qD != null;
	}

	private Matrix3D getMatrix3DLocalFrame(Vertex o)
	{
		if (!o.isManifold() || !o.isMutable())
			return null;
		double [] normal = new double[3];
		// TODO: Check why discreteCurvatures(normal) does not work well
		if (!o.discreteAverageNormal(normal))
			return null;
		double [] t1 = new double[3];
		double [] t2 = new double[3];
		if (!o.computeTangentPlane(normal, t1, t2))
			return null;
		// Transformation matrix
		Matrix3D P = new Matrix3D(t1, t2, normal);
		P.transp();
		return P;
	}
	
	private double [] getLocalQuadric(Vertex o, Matrix3D P)
	{
		if (P == null)
			return null;
		// We search for the quadric
		//   F(x,y) = a x^2 + b xy + c y^2 - z
		// which fits best for all neighbour vertices.
		// First set (t1,t2) to be an arbitrary map of the
		// tangent plane.  In this local frame, each neighbor
		// has coordinates (u[i], v[i], w[i]).  We want to find (a,b,c)
		// to minimize
		//   sum (a u[i]^2 + b u[i] v[i] + c v[i]^2 - w[i], i=1...m)
		// In matricial form, we solve this usually overdetermined system
		//      / u[1]^2    u[1] v[1]   v[1]^2 \           / w[1]\
		//     |  u[2]^2    u[2] v[2]   v[2]^2  |  /a\    |  w[2] |
		//     |  u[3]^2    u[3] v[3]   v[3]^2  | | b | = |  w[3] |
		//     |   ...         ...       ...    |  \c/    |   ... |
		//      \ u[m]^2    u[m] v[m]   v[m]^2 /           \ w[m]/
		//                   A                      X   =     b
		// by multiplying by tA to the left
		//    tA A X = tA b
		// If G = tA A is not singular, X = inv(tA A) tA b
		double [] vect1 = new double[3];
		double [] h = new double[3];
		double [] g0 = new double[3];
		double [] g1 = new double[3];
		double [] g2 = new double[3];
		double [] loc = new double[3];
		for (int i = 0; i < 3; i++)
			g0[i] = g1[i] = g2[i] = h[i] = 0.0;

		double [] param = o.getUV();
		AbstractHalfEdge ot = o.getIncidentAbstractHalfEdge(o.getNeighbourIteratorTriangle().next(), null);
		Vertex d = ot.destination();
		for (int pass = 0; pass < 2; pass++)
		{
			boolean coplanar = true;
			do
			{
				ot = ot.nextOriginLoop();
				// TODO: Handle boundary nodes
				if (ot.hasAttributes(AbstractHalfEdge.OUTER))
					return null;
				if (pass == 0)
				{
					// Destination point
					double [] p1 = ot.destination().getUV();
					for (int i = 0; i < 3; i++)
						vect1[i] = p1[i] - param[i];
				}
				else
				{
					// Middle point of opposite edge
					double [] p1 = ot.destination().getUV();
					double [] p2 = ot.apex().getUV();
					for (int i = 0; i < 3; i++)
						vect1[i] = 0.5*(p1[i] + p2[i])- param[i];
				}
				// Find coordinates in the local frame (t1,t2,n)
				P.apply(vect1, loc);
				// Compute right hand side
				h[0] += loc[2] * loc[0] * loc[0];
				h[1] += loc[2] * loc[0] * loc[1];
				h[2] += loc[2] * loc[1] * loc[1];
				// Flag to check if all incident triangles are coplanar
				if (coplanar && loc[2] * loc[2] > 1.e-60 * (loc[0] * loc[0] + loc[1] * loc[1]))
					coplanar = false;
				// Matrix assembly
				g0[0] += loc[0] * loc[0] * loc[0] * loc[0];
				g0[1] += loc[0] * loc[0] * loc[0] * loc[1];
				g0[2] += loc[0] * loc[0] * loc[1] * loc[1];
				g1[2] += loc[0] * loc[1] * loc[1] * loc[1];
				g2[2] += loc[1] * loc[1] * loc[1] * loc[1];
			}
			while (ot.destination() != d);
			// On a plane, h[0] = h[1] = h[2] = 0.
			// We do not need to compute G, return value will be 0.
			if (h[0] == 0.0 && h[1] == 0.0 && h[2] == 0.0)
				return h;
			if (h[1] * h[1] < 4.0 * h[0] * h[2])
				break;
			// We do not want F to be hyperbolic, projected point may
			// be very far from other points.  Middle points are also
			// added to find another approximation.
			if (pass > 0)
			{
				if (coplanar)
				{
					g0[0] = g0[1] = g0[2] = 0.0;
					return g0;
				}
				return null;
			}
		}
		g1[1] = g0[2];
		g1[0] = g0[1];
		g2[0] = g0[2];
		g2[1] = g1[2];
		// G = tA A
		Metric3D G = new Metric3D(g0, g1, g2);
		if (!G.inv())
			return null;
		// Reuse g0 to store our solution (a,b,c)
		G.apply(h, g0);
		return g0;
	}
	
}
