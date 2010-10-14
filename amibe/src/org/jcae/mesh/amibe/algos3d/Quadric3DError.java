/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2006 by EADS CRC
    Copyright (C) 2007,2009, by EADS France

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

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.ds.Vertex;
import java.io.Serializable;

/**
 * Garland's Quadric Error Metric.  See
 * <a href="http://graphics.cs.uiuc.edu/~garland/research/quadrics.html">quadric error metrics</a>.
 *
 * <p>
 * A plane is fully determined by its normal <code>N</code> and the signed
 * distance <code>d</code> of the frame origin to this plane, or in other 
 * words the equation of this plane is <code>tN V + d = 0</code>.
 * The squared distance of a point to this plane is
 * </p>
 * <pre>
 *   D*D = (tN V + d) * (tN V + d)
 *       = tV (N tN) V + 2d tN V + d*d
 *       = tV A V + 2 tB V + c
 * </pre>
 * <p>
 * The quadric <code>Q=(A,B,c)=(N tN, dN, d*d)</code> is thus naturally
 * defined.  Addition of these quadrics have a simple form:
 * <code>Q1(V)+Q2(V)=(Q1+Q2)(V)</code> with
 * <code>Q1+Q2=(A1+A2, B1+B2, c1+c2)</code>
 * To compute the squared distance of a point to a set of planes, we can
 * then compute this quadric for each plane and sum each element of
 * these quadrics.  
 * </p>
 *
 * <p>
 * When an edge <code>(V1,V2)</code> is contracted into <code>V3</code>,
 * <code>Q1(V3)+Q2(V3)</code> represents the deviation to the set of
 * planes at <code>V1</code> and <code>V2</code>.  The cost of this
 * contraction is thus defined as <code>Q1(V3)+Q2(V3)</code>.
 * We want to minimize this error.  It can be shown that if <code>A</code>
 * is non singular, the optimal placement is for <code>V3=-inv(A) B</code>.
 * </p>
 */
public class Quadric3DError implements Serializable
{
	private static final long serialVersionUID = -9198789443096689948L;

	private final double [] A = new double[6];
	private final double [] b = new double[3];
	private double c;
	private double detA;
	private boolean cachedDet = false;

	public enum Placement {
		// Select the best vertex
		VERTEX("VERTEX"),
		// Contract an edge into its middle point
		MIDDLE("MIDDLE"),
		// Find optimal point on segment
		EDGE("EDGE"),
		// Optimal point
		OPTIMAL("OPTIMAL");
		private final String name;
		private Placement(String str)
		{
			this.name = str;
		}
		public static Placement getByName(String str)
		{
			String ustr = str.toUpperCase();
			for (Placement p: Placement.values())
			{
				if (ustr.equals(p.name))
					return p;
			}
			return null;
		}
		@Override
		public String toString()
		{
			return name;
		}
	}

	// Add 2 quadrics
	public final void computeQuadric3DError(Quadric3DError q1, Quadric3DError q2)
	{
		for (int i = 0; i < 6; i++)
			A[i] = q1.A[i] + q2.A[i];

		for (int i = 0; i < 3; i++)
			b[i] = q1.b[i] + q2.b[i];

		c = q1.c + q2.c;
		cachedDet = false;
	}

	public final double value(double [] vect)
	{
		double ret = c;
		ret += 2.0 * Matrix3D.prodSca(b, vect);
		ret +=
			(A[0] * vect[0] + A[1] * vect[1] + A[2] * vect[2]) * vect[0] +
			(A[1] * vect[0] + A[3] * vect[1] + A[4] * vect[2]) * vect[1] +
			(A[2] * vect[0] + A[4] * vect[1] + A[5] * vect[2]) * vect[2];
		return ret;
	}

	public final void addError(double [] normal, double d, double a)
	{
		for (int k = 0; k < 3; k++)
		{
			b[k] += a * d * normal[k];
			A[k] += a * normal[0] * normal[k];
		}

		A[3] += a * normal[1] * normal[1];
		A[4] += a * normal[1] * normal[2];
		A[5] += a * normal[2] * normal[2];

		c += a * d*d;
		cachedDet = false;
	}

	// Used when adding virtual planes on boundaries
	public final void addWeightedError(double [] normal, double d, double scale)
	{
		for (int k = 0; k < 3; k++)
		{
			b[k] += scale * d * normal[k];
			A[k] += scale * normal[0] * normal[k];
		}

		A[3] += scale * normal[1] * normal[1];
		A[4] += scale * normal[1] * normal[2];
		A[5] += scale * normal[2] * normal[2];

		c += scale * d*d;
		cachedDet = false;
	}

	private double detA()
	{
		if (!cachedDet)
		{
			detA = A[0] * (A[3] * A[5] - A[4] * A[4]) + A[1] * (A[4] * A[2] - A[1] * A[5]) + A[2] * (A[1] * A[4] - A[3] * A[2]);
			cachedDet = true;
		}
		return detA;
	}

	private void moveAlongSegment(Vertex v1, Vertex v2, Quadric3DError q1, Quadric3DError q2, Vertex ret)
	{
		int nrSegments = 6;
		double [] p1 = v1.getUV();
		double [] p2 = v2.getUV();
		ret.copy(bestCandidateV1V2Ref(v1, v2, q1, q2));
		double [] posMin = new double[] { p1[0], p1[1], p1[2] };
		double [] pos = new double[3];
		double qmin = q1.value(posMin) + q2.value(posMin);
		double dx = (p2[0] - p1[0]) / (double) nrSegments;
		double dy = (p2[1] - p1[1]) / (double) nrSegments;
		double dz = (p2[2] - p1[2]) / (double) nrSegments;
		for (int i = 0; i < nrSegments; i++)
		{
			pos[0] = p1[0] + dx * (i + 1.0);
			pos[1] = p1[1] + dy * (i + 1.0);
			pos[2] = p1[2] + dz * (i + 1.0);
			double q = q1.value(pos) + q2.value(pos);
			if (q < qmin)
			{
				q = qmin;
				posMin[0] = pos[0];
				posMin[1] = pos[1];
				posMin[2] = pos[2];
			}
		}
		ret.moveTo(posMin[0], posMin[1], posMin[2]);
	}

	public final void optimalPlacement(Vertex v1, Vertex v2, Quadric3DError q1, Quadric3DError q2, Placement p, Vertex ret)
	{
		/* FIXME: add an option so that boundary nodes may be frozen.  */
		double norm, norm2Row0, norm2Row1, norm2Row2;
		switch(p)
		{
		case VERTEX:
			ret.copy(bestCandidateV1V2(v1, v2, q1, q2));
			break;
		case MIDDLE:
			{
				// Keep a reference if there is one
				double [] p1 = v1.getUV();
				double [] p2 = v2.getUV();
				ret.copy(bestCandidateV1V2Ref(v1, v2, q1, q2));
				ret.moveTo(0.5*(p1[0]+p2[0]), 0.5*(p1[1]+p2[1]), 0.5*(p1[2]+p2[2]));
			}
			break;
		case OPTIMAL:
			norm2Row0 = A[0]*A[0] + A[1]*A[1] + A[2]*A[2];
			norm2Row1 = A[1]*A[1] + A[3]*A[3] + A[4]*A[4];
			norm2Row2 = A[2]*A[2] + A[4]*A[4] + A[5]*A[5];
			norm = Math.sqrt(Math.max(norm2Row0, Math.max(norm2Row1, norm2Row2)));
			ret.copy(bestCandidateV1V2Ref(v1, v2, q1, q2));
			if (detA() > 1.e-10*(norm*norm*norm))
			{
				double cfxx = A[3] * A[5] - A[4] * A[4];
				double cfxy = A[2] * A[4] - A[1] * A[5];
				double cfxz = A[1] * A[4] - A[2] * A[3];
				double cfyy = A[0] * A[5] - A[2] * A[2];
				double cfyz = A[2] * A[1] - A[0] * A[4];
				double cfzz = A[0] * A[3] - A[1] * A[1];
				double dx = (cfxx * b[0] + cfxy * b[1] + cfxz * b[2]) / detA;
				double dy = (cfxy * b[0] + cfyy * b[1] + cfyz * b[2]) / detA;
				double dz = (cfxz * b[0] + cfyz * b[1] + cfzz * b[2]) / detA;
				ret.moveTo(-dx, -dy, -dz);
			}
			else
				moveAlongSegment(v1, v2, q1, q2, ret);
			break;
		case EDGE:
			norm2Row0 = A[0]*A[0] + A[1]*A[1] + A[2]*A[2];
			norm2Row1 = A[1]*A[1] + A[3]*A[3] + A[4]*A[4];
			norm2Row2 = A[2]*A[2] + A[4]*A[4] + A[5]*A[5];
			norm = Math.sqrt(Math.max(norm2Row0, Math.max(norm2Row1, norm2Row2)));
			ret.copy(bestCandidateV1V2Ref(v1, v2, q1, q2));
			if (detA() > 1.e-10*(norm*norm*norm))
			{
				// Find M = v1 + s(v2-v1) which minimizes
				//   q(M) = s^2 (v2-v1)A(v2-v1) + s(v1A(v2-v1)+(v2-v1)Av1+2b(v2-v1))+cte
				//   q'(M) = 2 s (v2-v1)A(v2-v1) + 2(v1A(v2-v1)+b(v2-v1))
				double [] p1 = v1.getUV();
				double [] p2 = v2.getUV();
				double dx = p2[0] - p1[0];
				double dy = p2[1] - p1[1];
				double dz = p2[2] - p1[2];
				double den = 0.0;

				double num = b[0] * dx + b[1] * dy + b[2] * dz;
				den += A[0] * dx * dx + 2.0 * A[1] * dx * dy + 2.0 * A[2] * dx * dz + A[3] * dy * dy + 2.0 * A[4] * dy * dz + A[5] * dz * dz;
				num += A[0] * dx * p1[0] + A[1] * (dx * p1[1] + dy * p1[0]) + A[2] * (dx * p1[2] + dz * p1[0]) + A[3] * dy * p1[1] + A[4] * (dy * p1[2] + dz * p1[1]) + A[5] * dz * p1[2];
				if (den > 1.0e-4 * Math.abs(num))
				{
					double s = - num / den;
					if (s < 1.0e-4)
						s = 0.0;
					else if (s > 1.0 - 1.0e-4)
						s = 1.0;
					ret.moveTo(p1[0]+s*dx, p1[1]+s*dy, p1[2]+s*dz);
				}
				else
					moveAlongSegment(v1, v2, q1, q2, ret);
			}
			else
				moveAlongSegment(v1, v2, q1, q2, ret);
			break;
		default:
			throw new IllegalArgumentException("Unknown placement strategy: "+p);
		}
	}

	private static Vertex bestCandidateV1V2(Vertex v1, Vertex v2, Quadric3DError q1, Quadric3DError q2)
	{
		if (q1.value(v1.getUV()) + q2.value(v1.getUV()) < q1.value(v2.getUV()) + q2.value(v2.getUV()))
			return v1;
		return v2;
	}

	private static Vertex bestCandidateV1V2Ref(Vertex v1, Vertex v2, Quadric3DError q1, Quadric3DError q2)
	{
		if (v1.getRef() == 0 && v2.getRef() == 0)
			return bestCandidateV1V2(v1, v2, q1, q2);
		else if (v1.getRef() == 0)
			return v2;
		else if (v2.getRef() == 0)
			return v1;
		else
			return bestCandidateV1V2(v1, v2, q1, q2);
	}

	@Override
	public final String toString()
	{
		return "A: data|0][]  "+A[0]+" "+A[1]+" "+A[2]+"\ndata|1][]  "+A[1]+" "+A[3]+" "+A[4]+"\ndata|2][]  "+A[2]+" "+A[4]+" "+A[5]+"\n"+
		       " b: "+b[0]+" "+b[1]+" "+b[2]+"\n"+
		       " c: "+c;
	}
	
}
