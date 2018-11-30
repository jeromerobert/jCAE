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
 * (C) Copyright 2012, by EADS France
 */

package org.jcae.mesh.amibe.projection;

import java.io.IOException;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.projection.MeshLiaison.TriangleDistance;

public class BoundingVolumeHierarchy {

	private static class Node
	{
		public final double[] bbox = new double[6];
		/* If numberOfTriangles >= 0, this node is a leaf node.
		   It contains 'numberOfTriangles' triangles
		     trianglesArray[sortedIndices[offset:offset+numberOfTriangles]]
		   Members 'left' and 'right' are null.

		   If numberOfTriangles < 0, this is an inner node; it
		   contains 2 children stored in 'left' and 'right' members.
		   Split axis is stored as (- numberOfTriangles - 1).
		   Member 'offset' is irrelevant.
                 */
		public Node left, right;
		public int numberOfTriangles;
		public int offset;
	}
	private final static Logger LOGGER = Logger.getLogger(BoundingVolumeHierarchy.class.getName());
	private final int bucketSize;
	private final Node rootNode;
	// Triangles (size: number of non-outer triangles)
	private final Triangle[] trianglesArray;
	// Nodes bounds (size: 6 * number of non-outer triangles)
	private final double[] boundsArray;
	// Nodes center (size: 3 * number of non-outer triangles)
	private final double[] centerArray;
	// Triangle indirection (size: number of non-outer triangles)
	private final int[] sortedIndices;
	private final transient double[] workMiddle = new double[6];
	private final int [] closeIndex = new int[2];

	private final static TriangleDistance TRIANGLE_DISTANCE = new TriangleDistance(){
		@Override
		protected double handleDegenerated(double det, Triangle tri) {
			LOGGER.info(tri+" is degenerated");
			return Double.POSITIVE_INFINITY;
		}
	};

	public BoundingVolumeHierarchy(Mesh mesh)
	{
		this(mesh, 20);
	}

	public BoundingVolumeHierarchy(Iterable<Triangle> triangles)
	{
		this(triangles, 20);
	}

	public BoundingVolumeHierarchy(Mesh mesh, int bucketSize)
	{
		this(mesh.getTriangles(), bucketSize);
	}

	private BoundingVolumeHierarchy(Iterable<Triangle> triangles, int bucketSize)
	{
		this.bucketSize = bucketSize;
		int countTriangles = 0;
		for(Triangle t:triangles)
			if(!t.hasAttributes(AbstractHalfEdge.OUTER))
				countTriangles++;
		trianglesArray = new Triangle[countTriangles];
		// Compute sortedIndices, boundsArray and centerArray
		sortedIndices = new int[countTriangles];
		boundsArray = new double[6*countTriangles];
		centerArray = new double[3*countTriangles];
		countTriangles = 0;
		for(Triangle t:triangles)
		{
			if(t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			trianglesArray[countTriangles] = t;
			sortedIndices[countTriangles] = countTriangles;
			for(int j = 0; j < 3; j++)
			{
				boundsArray[6 * countTriangles + j] = Double.POSITIVE_INFINITY;
				boundsArray[6 * countTriangles + 3 + j] = Double.NEGATIVE_INFINITY;
			}
			for(int i = 0; i < 3; i++)
			{
				Vertex v = t.getV(i);
				for(int j = 0; j < 3; j++)
				{
					double c = v.get(j);
					boundsArray[6 * countTriangles + j] = Math.min(boundsArray[6 * countTriangles + j], c);
					boundsArray[6 * countTriangles + 3 + j] = Math.max(boundsArray[6 * countTriangles + 3 + j], c);
				}
			}
			for(int j = 0; j < 3; j++)
				centerArray[3 * countTriangles + j] = 0.5 * (boundsArray[6 * countTriangles + j] + boundsArray[6 * countTriangles + 3 + j]);
			countTriangles++;
		}
		LOGGER.info("Inserting "+countTriangles+" triangles");
		// Recursively build tree
		rootNode = new Node();
		recursiveBuild(rootNode, 0, countTriangles);
		LOGGER.info("BVH built");
	}

	void recursiveBuild(Node current, int firstTriangleIndex, int lastTriangleIndex)
	{
		/* compute node bounding box */
		for(int j = 0; j < 6; j++)
			current.bbox[j] = boundsArray[6 * sortedIndices[firstTriangleIndex] + j];
		for(int i = firstTriangleIndex + 1; i < lastTriangleIndex; i++)
		{
			for(int j = 0; j < 3; j++)
			{
				current.bbox[j] = Math.min(current.bbox[j], boundsArray[6 * sortedIndices[i] + j]);
				current.bbox[j + 3] = Math.max(current.bbox[j + 3], boundsArray[6 * sortedIndices[i] + 3 + j]);
			}
		}
		/* create a leaf node if there are few triangles */
		if (lastTriangleIndex - firstTriangleIndex <= bucketSize)
		{
			current.numberOfTriangles = lastTriangleIndex - firstTriangleIndex;
			current.offset = firstTriangleIndex;
			return;
		}
		/* otherwise split node */

		/* find the largest extent */
		for(int j = 0; j < 3; j++)
		{
			workMiddle[j] = workMiddle[j + 3] = centerArray[3 * sortedIndices[firstTriangleIndex] + j];
		}
		for(int i = firstTriangleIndex + 1; i < lastTriangleIndex; i++)
		{
			for(int j = 0; j < 3; j++)
			{
				workMiddle[j] = Math.min(workMiddle[j], boundsArray[6 * sortedIndices[i] + j]);
				workMiddle[j + 3] = Math.max(workMiddle[j + 3], boundsArray[6 * sortedIndices[i] + 3 + j]);
			}
		}
		byte splitAxis = getSplitDirection(workMiddle);
		if (4 == splitAxis)
		{
			// All centers are at the same place, node cannot be split
			current.numberOfTriangles = lastTriangleIndex - firstTriangleIndex;
			current.offset = firstTriangleIndex;
			return;
		}
		current.numberOfTriangles = - splitAxis - 1;
		double splitValue = 0.5 * (workMiddle[splitAxis] + workMiddle[3 + splitAxis]);
		int index = BVH_pivot(splitAxis, splitValue, firstTriangleIndex, lastTriangleIndex);
		// Build left child
		current.left = new Node();
		this.recursiveBuild(current.left, firstTriangleIndex, index);
		// Build right child
		current.right = new Node();
		recursiveBuild(current.right, index, lastTriangleIndex);
	}

	/* Sort sortedIndices and return pivot index such that
	     centerArray[3*sortedIndices[i]+splitAxis] < splitValue for i in [firstTriangleIndex,pivot[
	     centerArray[3*sortedIndices[i]+splitAxis] >= splitValue for i in [pivot, lastTriangleIndex[
	 */
	int BVH_pivot(byte splitAxis, double splitValue, int firstTriangleIndex, int lastTriangleIndex)
	{
		int left = firstTriangleIndex;
		int right = lastTriangleIndex - 1;
		int tmp;
		
		while (left <= right)
		{
			if (centerArray[3 * sortedIndices[left] + splitAxis] < splitValue)
			{
				++left;
			}
			else
			{
				tmp = sortedIndices[left];
				sortedIndices[left] = sortedIndices[right];
				sortedIndices[right] = tmp;
				--right;
			}
		}
		return left;
	}

	/**
	 * Get the closest triangle for coords
	 * @param coords
	 * @param projection The projection of coords on the triangle. If null the
	 * projection is not computed. It must be different of coords else strange
	 * things will happen.
	 * @param group Only look for triangles in the given groups. If negative
	 * look for all triangles.
	 * @return
	 */
	public Triangle getClosestTriangle(Location coords, Location projection, int group)
	{
		Deque<Node> stack = new ArrayDeque<Node>(100);
		double minDist2 = Double.POSITIVE_INFINITY;
		int toReturn = -1;
		stack.push(rootNode);
		double [] bbox = new double[6];
		while(!stack.isEmpty())
		{
			Node current = stack.pop();
			if (distanceAABB(coords, current.bbox) >= minDist2)
				continue;
			if (current.numberOfTriangles >= 0)
			{
				// Leaf node
				for(int i = 0; i < current.numberOfTriangles; i++)
				{
					int index = sortedIndices[current.offset + i];
					System.arraycopy(boundsArray, 6 * index, bbox, 0, 6);
					if (distanceAABB(coords, bbox) < minDist2)
					{
						double d = TRIANGLE_DISTANCE.compute(coords, trianglesArray[index], closeIndex);
						if (d < minDist2)
						{
							minDist2 = d;
							toReturn = index;
							if(projection != null)
								TRIANGLE_DISTANCE.getProjection(projection);
						}
					}
				}
			}
			else
			{
				int splitAxis = - current.numberOfTriangles - 1;
				if (current.bbox[splitAxis] + current.bbox[splitAxis + 3] < 2.0 * coords.get(splitAxis))
				{
					stack.push(current.left);
					stack.push(current.right);
				}
				else
				{
					stack.push(current.right);
					stack.push(current.left);
				}
			}
		}
		return trianglesArray[toReturn];
	}

	/** Euclidian distance between a point and an AABB */
	private static final double distanceAABB(Location coord, double[] aabb)
	{
		double sqrDistance = 0.0;
		for (int j = 0; j < 3; ++j)
		{
			double xLeft = coord.get(j) - aabb[j + 3];
			double xRight = aabb[j] - coord.get(j);
			// max(coord.get(j) - aabb[j + 3], 0.0, aabb[j] - coord.get(j))
			double maxValue = xLeft > xRight ? (xLeft > 0.0 ? xLeft : 0.0) : (xRight > 0.0 ? xRight : 0.0);
			sqrDistance += maxValue * maxValue;
		}
		return sqrDistance;
	}

	public Triangle getClosestTriangleDebug(Mesh mesh, Location coords, Location projection, int group)
	{
		double minDist2 = Double.POSITIVE_INFINITY;
		Triangle toReturn = null;
		for(Triangle t:mesh.getTriangles())
		{
			if(t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			if(t.getGroupId() == group || group < 0)
			{
				double d = TRIANGLE_DISTANCE.compute(coords, t, closeIndex);
				if(d < minDist2)
				{
					minDist2 = d;
					toReturn = t;
					TRIANGLE_DISTANCE.getProjection(projection);
				}
			}
		}
		Location otherProj = new Location();
		Triangle other = getClosestTriangle(coords, otherProj, group);
		if(other == null || (other != toReturn && Math.abs(coords.sqrDistance3D(projection) - coords.sqrDistance3D(otherProj)) > 1.e-12 * coords.sqrDistance3D(projection)))
		{
			System.err.println("--- real solution ---");
			System.err.println(toReturn);
			System.err.println(Math.sqrt(minDist2));
			System.err.println(projection);
			System.err.println("--- kdtree solution ---");
			System.err.println(other);
			if(other != null)
			{
				System.err.println(otherProj.distance3D(coords));
				System.err.println(otherProj);
			}
			throw new IllegalStateException();
		}
		return toReturn;
	}

	private byte getSplitDirection(double[] bounds)
	{
		double max = 0;
		byte maxDir = 4;
		for(byte i = 0; i < 3; i++)
		{
			double s = bounds[i+3] - bounds[i];
			if(s > max)
			{
				maxDir = i;
				max = s;
			}
		}
		return maxDir;
	}

}
