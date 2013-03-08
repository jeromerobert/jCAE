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
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.metrics.Matrix3D;

import java.util.logging.Level;
import java.util.logging.Logger;

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
	private static final Logger LOGGER = Logger.getLogger(QuadricProjection.class.getName());
	
	private final Matrix3D qP;
	private final Location origin = new Location();
	private final double[] qD;
	private final boolean discardHyperbolic;

	public QuadricProjection(Vertex o)
	{
		this(o, false);
	}

	public QuadricProjection(Vertex o, boolean d)
	{
		discardHyperbolic = d;
		origin.moveTo(o);

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
	public final boolean project(Location pt)
	{
		double [] glob = new double[3];
		pt.sub(origin, glob);

		// Local coordinates
		double [] loc = new double[3];
		qP.apply(glob, loc);
		// Compute z = a x^2 + b xy + c y^2
		loc[2] = qD[0] * loc[0] * loc[0] + qD[1] * loc[0] * loc[1] + qD[2] * loc[1] * loc[1];
		// Reuse glob
		qP.transp();
		qP.apply(loc, glob);
		qP.transp();
		pt.moveTo(origin.getX() + glob[0], origin.getY() + glob[1], origin.getZ() + glob[2]);
		return true;
	}
	
	public final boolean canProject()
	{
		return qD != null;
	}

	private Matrix3D getMatrix3DLocalFrame(Vertex o)
	{
		if (!o.isManifold())
		{
			if (LOGGER.isLoggable(Level.FINER))
				LOGGER.log(Level.FINER, "Skip boundary vertex: "+o);
			return null;
		}
		double [] normal = new double[3];
		// TODO: Check why discreteCurvatures(normal) does not work well
		if (!o.discreteAverageNormal(normal))
		{
			LOGGER.fine("Cannot compute mean normal");
			return null;
		}
		double [] t1 = new double[3];
		double [] t2 = new double[3];
		if (!o.computeTangentPlane(normal, t1, t2))
		{
			LOGGER.fine("Cannot compute tangent plane");
			return null;
		}
		// Transformation matrix
		Matrix3D P = new Matrix3D(t1, t2, normal);
		P.transp();
		return P;
	}
	
	private double [] getLocalQuadric(Vertex o, Matrix3D P)
	{
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

		AbstractHalfEdge ot = o.getIncidentAbstractHalfEdge(o.getNeighbourIteratorTriangle().next(), null);
		Vertex d = ot.destination();
		do
		{
			ot = ot.nextOriginLoop();
			// TODO: Handle boundary nodes
			if (ot.hasAttributes(AbstractHalfEdge.OUTER))
				return null;
			// Destination point
			ot.destination().sub(o, vect1);
			// Find coordinates in the local frame (t1,t2,n)
			P.apply(vect1, loc);
			// Compute right hand side
			h[0] += loc[2] * loc[0] * loc[0];
			h[1] += loc[2] * loc[0] * loc[1];
			h[2] += loc[2] * loc[1] * loc[1];
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
		boolean addMiddlePoints = discardHyperbolic && h[1] * h[1] >= 4.0 * h[0] * h[2];
		Matrix3D G = null;
		if (!addMiddlePoints)
		{
			g1[1] = g0[2];
			g1[0] = g0[1];
			g2[0] = g0[2];
			g2[1] = g1[2];
			// G = tA A
			G = new Matrix3D(g0, g1, g2);
			if (!G.inv())
				addMiddlePoints = true;
		}
		if (addMiddlePoints)
		{
			boolean coplanar = true;
			do
			{
				ot = ot.nextOriginLoop();
				// TODO: Handle boundary nodes
				if (ot.hasAttributes(AbstractHalfEdge.OUTER))
					return null;
				// Middle point of opposite edge
				Vertex p1 = ot.destination();
				Vertex p2 = ot.apex();
				vect1[0] = 0.5*(p1.getX() + p2.getX()) - o.getX();
				vect1[1] = 0.5*(p1.getY() + p2.getY()) - o.getY();
				vect1[2] = 0.5*(p1.getZ() + p2.getZ()) - o.getZ();
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
			// We do not want F to be hyperbolic, projected point may
			// be very far from other points.  Middle points are also
			// added to find another approximation.
			if (discardHyperbolic && h[1] * h[1] >= 4.0 * h[0] * h[2])
			{
				if (coplanar)
				{
					g0[0] = g0[1] = g0[2] = 0.0;
					return g0;
				}
				LOGGER.fine("Hyperbolic quadric found, projection is discarded");
				return null;
			}
		}
		g1[1] = g0[2];
		g1[0] = g0[1];
		g2[0] = g0[2];
		g2[1] = g1[2];
		// G = tA A
		G = new Matrix3D(g0, g1, g2);
		if (!G.inv())
		{
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.log(Level.FINE, "Singular quadric at vertex "+o);
			return null;
		}
		// Reuse g0 to store our solution (a,b,c)
		G.apply(h, g0);
		return g0;
	}
	
}