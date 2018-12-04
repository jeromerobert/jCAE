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
import java.util.logging.Level;
import java.util.logging.Logger;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.stack.array.TIntArrayStack;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.projection.MeshLiaison.TriangleDistance;

public class BoundingVolumeHierarchy {

	/*
	Tree is linearized.
	It it were not linearized, nodes would contain:
	  * 6 doubles for its bounding box
	  * for inner nodes, 2 pointers to children
	  * for leaves, 2 integers to represent the numbers of triangles
	    stored in this leaf, and the index of the first triangle in a
	    sorted list.

	The total number of tree nodes is stored in 'numberOfNodes'.
	Bounding boxes are stored in 'bboxNodes'.
	In order to have a more compact representation in memory, pointers
	to children are not used, we encode the same information into the same
	2 integers as for leaf nodes; they are stored into 'linearTree' member.

	By convention, if linearTree[2*index] >= 0, this is a leaf node and
	this number represents the number of triangles stored in this leaf.
	Offset of first triangle in sortedIndices is stored in linearTree[2*index+1].
	If linearTree[2*index] < 0, this is an inner node, and this number gives
	its split axis (-1 for axis 0, -2 for axis 1 and -3 for axis 2).  Its first
	child is stored just after this one, its index is thus (index+1).  And
	linearTree[2*index+1] gives the index of its right child.
	 */
	private final static Logger LOGGER = Logger.getLogger(BoundingVolumeHierarchy.class.getName());
	private final int bucketSize;
	private int numberOfNodes;
	private final TIntArrayList linearTree = new TIntArrayList();
	private final TDoubleArrayList bboxNodes = new TDoubleArrayList();
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
		// There will be at least (countTriangles/bucketSize) leaves, and thus
		// 2*(countTriangles/bucketSize) nodes.
		int capacity = Math.max(2 * countTriangles / bucketSize, 1000);
		linearTree.ensureCapacity(2 * capacity);
		bboxNodes.ensureCapacity(6 * capacity);
		numberOfNodes = 1;
		recursiveBuild(0, 0, countTriangles);
		LOGGER.info("BVH built");
		linearTree.trimToSize();
		bboxNodes.trimToSize();
	}

	void recursiveBuild(int current, int firstTriangleIndex, int lastTriangleIndex)
	{
		/* compute node bounding box */
		System.arraycopy(boundsArray, 6 * sortedIndices[firstTriangleIndex], workMiddle, 0, 6);
		for(int i = firstTriangleIndex + 1; i < lastTriangleIndex; i++)
		{
			for(int j = 0; j < 3; j++)
			{
				workMiddle[j] = Math.min(workMiddle[j], boundsArray[6 * sortedIndices[i] + j]);
				workMiddle[j + 3] = Math.max(workMiddle[j + 3], boundsArray[6 * sortedIndices[i] + 3 + j]);
			}
		}
		bboxNodes.add(workMiddle);
		/* create a leaf node if there are few triangles */
		if (lastTriangleIndex - firstTriangleIndex <= bucketSize)
		{
			// number of triangles
			linearTree.add(lastTriangleIndex - firstTriangleIndex);
			// offset in sortedIndices of the first triangle
			linearTree.add(firstTriangleIndex);
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
			// number of triangles
			linearTree.add(lastTriangleIndex - firstTriangleIndex);
			// offset in sortedIndices of the first triangle
			linearTree.add(firstTriangleIndex);
			return;
		}
		// A negative value tells that this is an inner node, and we encode split axis
		linearTree.add(- splitAxis - 1);
		// Second int contains the index of the right child in linearTree; we will know
		// this index only after left child is built, thus store a fake one for now.
		linearTree.add(0);
		double splitValue = 0.5 * (workMiddle[splitAxis] + workMiddle[3 + splitAxis]);
		int index = BVH_pivot(splitAxis, splitValue, firstTriangleIndex, lastTriangleIndex);
		// Build left child
		numberOfNodes++;
		bboxNodes.ensureCapacity(6 * numberOfNodes);
		linearTree.ensureCapacity(2 * numberOfNodes);
		recursiveBuild(numberOfNodes - 1, firstTriangleIndex, index);
		// Left child had been fully built, we can now store its index into its parent node.
		linearTree.set(2 * current + 1, numberOfNodes);
		// Build right child
		numberOfNodes++;
		bboxNodes.ensureCapacity(6 * numberOfNodes);
		linearTree.ensureCapacity(2 * numberOfNodes);
		recursiveBuild(numberOfNodes - 1, index, lastTriangleIndex);
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
		TIntArrayStack stack = new TIntArrayStack(100);
		double minDist2 = Double.POSITIVE_INFINITY;
		int toReturn = -1;
		stack.push(0);
		double [] bbox = new double[6];
		while(stack.size() > 0)
		{
			int current = stack.pop();
			bboxNodes.toArray(bbox, 6 * current, 0, 6);
			if (distanceAABB(coords, bbox) >= minDist2)
				continue;
			int numberOfTriangles = linearTree.get(2 * current);
			int offset = linearTree.get(2 * current + 1);
			if (numberOfTriangles >= 0)
			{
				// Leaf node
				for(int i = 0; i < numberOfTriangles; i++)
				{
					int index = sortedIndices[offset + i];
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
				int splitAxis = - numberOfTriangles - 1;
				if (bbox[splitAxis] + bbox[splitAxis + 3] < 2.0 * coords.get(splitAxis))
				{
					stack.push(current + 1);
					stack.push(offset);
				}
				else
				{
					stack.push(offset);
					stack.push(current + 1);
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
