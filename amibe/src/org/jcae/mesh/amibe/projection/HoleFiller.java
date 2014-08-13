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
 * (C) Copyright 2014, by Airbus Group SAS
 */

package org.jcae.mesh.amibe.projection;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Matrix3D;

/** An AbstractLocaleRemesher whish use TriangulationsExplorer */
public class HoleFiller extends AbstractLocaleRemesher {
	private final TriangulationsExplorer explorer = new TriangulationsExplorer() {

		@Override
		protected boolean isTriangleValid(int v1, int v2, int v3) {
			return true;
		}

		@Override
		protected void triangulationCreated() {
			int nbT = triangletackPointer / 3;
			double min = Double.POSITIVE_INFINITY;
			for(int i = 0; i < nbT; i++)
			{
				computeNormal(
					vertIndex.get(triangleStack[3*i]),
					vertIndex.get(triangleStack[3*i+1]),
					vertIndex.get(triangleStack[3*i+2]));
				double dot = Matrix3D.prodSca(normal, meanNormal);
				if(dot < min)
					min = dot;
			}
			if(min > minDotProduct)
			{
				minDotProduct = min;
				if(bestTriangulation == null)
					bestTriangulation = new int[triangletackPointer];
				System.arraycopy(triangleStack, 0, bestTriangulation, 0,
					bestTriangulation.length);
			}
		}
	};

	private double minDotProduct;
	private int[] bestTriangulation;
	private final double[] meanNormal = new double[3];
	private final double[] vector1 = new double[3];
	private final double[] vector2 = new double[3];
	private final double[] normal = new double[3];

	private void computeNormal(Vertex o, Vertex d, Vertex a)
	{
		d.sub(o, vector1);
		a.sub(d, vector2);
		Matrix3D.prodVect3D(vector1, vector2, normal);
	}

	/** Bad for highly 3D polyline */
	private void computeMeanNormal(Collection<AbstractHalfEdge> edges)
	{
		meanNormal[0] = meanNormal[1] = meanNormal[2] = 0;
		for(AbstractHalfEdge e: edges)
		{
			computeNormal(e.origin(), e.destination(), e.apex());
			double n = Matrix3D.norm(normal);
			for(int i = 0; i < 3; i++)
				meanNormal[i] += normal[i] / n;
		}
		int n = edges.size();
		for(int i = 0; i < 3; i++)
			meanNormal[i] /= n;
	}

	@Override
	public void triangulate(Mesh mesh, Collection<AbstractHalfEdge> edges,
		Collection<List<Vertex>> vertices)
	{
		computeMeanNormal(edges);
		try {
			super.triangulate(mesh, edges, vertices);
		} catch (IOException ex) {
			// will never happen
			throw new IllegalStateException(ex);
		}
	}

	@Override
	protected void triangulate(Mesh mesh, Collection<List<Vertex>> vertices) {
		int[] holes = new int[vertices.size() - 1];
		int n = 0;
		int k = 0;
		for(List<Vertex> l: vertices)
		{
			if(k == 0)
				n = l.size();
			else
				holes[k-1] = l.size();
			k++;
		}
		minDotProduct = Double.NEGATIVE_INFINITY;
		bestTriangulation = null;
		explorer.travel(n, holes);
		int nbt = bestTriangulation.length / 3;
		for(int i = 0; i < nbt; i++)
		{
			addTriangle(mesh,
				bestTriangulation[3 * i],
				bestTriangulation[3 * i + 1],
				bestTriangulation[3 * i + 2]);
		}
	}
}
