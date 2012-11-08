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

import gnu.trove.THashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.xmldata.MeshReader;

/**
 *
 * @author Jerome Robert
 */
// TODO possible optimizations:
// - store boundaries in nodes
// - store cut plance in nodes
// - have Node and Leaf node classes

public class TriangleKdTree {

	/**
	 * Same as ArrayList&lt;double[]&gt; but keep removed instances for futur
	 * use.
	 * System.arraycopy(x,x,x,x,6) is more than 10 time faster than
	 * new double[6];
	 */
	private static class BoundaryPool extends ArrayList<double[]>
	{
		private int poolSize;

		public double[] push(double[] toCopy)
		{
			double[] toReturn;
			if(size() <= poolSize)
			{
				toReturn = Arrays.copyOf(toCopy, toCopy.length);
				add(toReturn);
			}
			else
			{
				toReturn = get(poolSize);
				assert toCopy != toReturn;
				System.arraycopy(toCopy, 0, toReturn, 0, toReturn.length);
			}
			poolSize ++;
			return toReturn;
		}

		public double[] last()
		{
			return get(poolSize-1);
		}

		public void removeLast()
		{
			poolSize --;
		}

		@Override
		public boolean isEmpty() {
			return poolSize == 0;
		}

		@Override
		public void clear() {
			poolSize = 0;
		}
	}

	private static class Node
	{
		public byte direction;
		public Node left, right;
		public Triangle[] triangles;
	}
	private final Node root = new Node();
	private double[] globalBounds;
	private final int bucketSize;
	private final TriangleInterAABB triangleInterAABB1 = new TriangleInterAABB();
	private final TriangleInterAABB triangleInterAABB2 = new TriangleInterAABB();
	private transient List<Node> nodeStack;
	private transient BoundaryPool boundaryPool;
	private final transient double[] workBoundary1 = new double[6];

	public TriangleKdTree(Mesh mesh)
	{
		this(mesh, 10);
	}

	public TriangleKdTree(Mesh mesh, int bucketSize)
	{
		this.bucketSize = bucketSize;
		if(mesh.hasNodes())
			globalBounds = getBoundsFormVerts(mesh.getNodes());
		else
			globalBounds = getBoundsFromTria(mesh.getTriangles());
		for(int i = 0; i < 3; i++)
		{
			double offset = 0.01*(globalBounds[i+3]-globalBounds[i]);
			globalBounds[i] -= offset;
			globalBounds[i+3] += offset;
		}
		for(Triangle t:mesh.getTriangles())
			if(!t.hasAttributes(AbstractHalfEdge.OUTER))
				addTriangle(t);
		nodeStack = null;
		boundaryPool = null;
	}

	private String bounds2String(double[] b)
	{
		double[] boxCenter = new double[3];
		double[] halfSize = new double[3];
		for(int j = 0; j < 3; j++)
		{
			boxCenter[j] = (b[j] + b[j+3]) / 2.0;
			halfSize[j] = (b[j+3] - b[j]) / 2.0;
			//assert halfSize[j] > 0: halfSize[j];
		}
		return Arrays.toString(boxCenter)+" "+Arrays.toString(halfSize);
	}

	private final transient Set<Triangle> seen = new THashSet<Triangle>();
	private final transient List<Node> closeNodes = new ArrayList<Node>();
	private final transient BoundaryPool closeBoundaries = new BoundaryPool();
	private final int[] closeIndex = new int[2];

	/**
	 * Get the closest triangle for coords
	 * @param coords
	 * @param projection The projection of coords on the triangle. If null the
	 * projection is not computed
	 * @param group Only look for triangles in the given groups. If negative
	 * look for all triangles.
	 * @return
	 */
	public Triangle getClosestTriangle(double[] coords, double[] projection, int group)
	{
		Node n = getNode(coords, workBoundary1);
		assert n != null;
		Triangle toReturn = null;
		double aabbDistance = Double.POSITIVE_INFINITY;
		double triangleDistance;
		seen.clear();
		if(n.triangles == null || n.triangles.length == 0)
		{
			aabbDistance = distanceAABB(coords, workBoundary1);
			triangleDistance = Double.POSITIVE_INFINITY;
		}
		else
		{
			for(Triangle t:n.triangles)
			{
				if(group >= 0 && t.getGroupId() != group)
					continue;
				double d = MeshLiaison.TRIANGLE_DISTANCE.compute(coords, t, closeIndex);
				if(d < aabbDistance)
				{
					aabbDistance = d;
					toReturn = t;
					if(projection != null)
						MeshLiaison.TRIANGLE_DISTANCE.getProjection(projection);
				}
				seen.add(t);
			}
			aabbDistance = Math.sqrt(aabbDistance);
			triangleDistance = aabbDistance;
		}

		getNodes(createCenteredAABB(coords, 1.01*aabbDistance), null, false);
		for(Node nn: closeNodes)
		{
			if(nn != n && nn.triangles != null)
			{
				for(Triangle t:nn.triangles)
				{
					if(group >= 0 && t.getGroupId() != group)
						continue;
					if(!seen.contains(t))
					{
						double d = MeshLiaison.TRIANGLE_DISTANCE.compute(coords, t, closeIndex);
						if(d < triangleDistance)
						{
							triangleDistance = d;
							toReturn = t;
							if(projection != null)
								MeshLiaison.TRIANGLE_DISTANCE.getProjection(projection);
						}
					}
				}
			}
		}
		closeNodes.clear();
		return toReturn;
	}

	/**
	 * Get all triangles present in this kdTree.
	 * It's mainly used to check the concistancy of the kdTree while debugging.
	 */
	public void getTriangles(Set<Triangle> result)
	{
		if(nodeStack == null)
			nodeStack = new ArrayList<Node>();
		else
			nodeStack.clear();

		nodeStack.add(root);
		while(!nodeStack.isEmpty())
		{
			int n = nodeStack.size() - 1;
			Node current = nodeStack.remove(n);
			if(current.triangles != null)
			{
				for(Triangle t:current.triangles)
					result.add(t);
			}
			else
			{
				if(current.left != null)
					nodeStack.add(current.left);
				if(current.right != null)
					nodeStack.add(current.right);
			}
		}
	}

	/** Non zero Manhattan distance between a point and an AABB */
	private double distanceAABB(double[] coord, double[] aabb)
	{
		double d = Double.NEGATIVE_INFINITY;
		for(int i = 0; i < 3; i++)
		{
			double v = Math.abs(coord[i] - aabb[i]);
			if(v > 0)
				d = Math.max(d, v);
			v = Math.abs(aabb[i+3] - coord[i]);
			if(v > 0)
				d = Math.max(d, v);
		}
		return d;
	}

	private double[] createCenteredAABB(double[] center, double size)
	{
		double[] r = new double[6];
		for(int i = 0; i < 3; i++)
		{
			r[i] = center[i] - size;
			r[i+3] = center[i] + size;
		}
		return r;
	}

	private double[] getBoundsFormVerts(Iterable<Vertex> vertices)
	{
		double[] bounds = new double[6];
		for(int i = 0; i < 3; i++)
		{
			bounds[i] = Double.POSITIVE_INFINITY;
			bounds[i+3] = Double.NEGATIVE_INFINITY;
		}
		for(Vertex vert: vertices)
		{
			double[] uv = vert.getUV();
			for(int j = 0; j < 3; j++)
			{
				double v = uv[j];
				bounds[j] = Math.min(bounds[j], v);
				bounds[j+3] = Math.max(bounds[j+3], v);
			}
		}
		return bounds;
	}

	private double[] getBoundsFromTria(Iterable<Triangle> triangles)
	{
		double[] bounds = new double[6];
		for(int i = 0; i < 3; i++)
		{
			bounds[i] = Double.POSITIVE_INFINITY;
			bounds[i+3] = Double.NEGATIVE_INFINITY;
		}
		for(Triangle t: triangles)
		{
			if(t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			for(int i = 0; i < 3; i++)
			{
				double[] uv = t.vertex[i].getUV();
				for(int j = 0; j < 3; j++)
				{
					double v = uv[j];
					bounds[j] = Math.min(bounds[j], v);
					bounds[j+3] = Math.max(bounds[j+3], v);
				}
			}
		}
		return bounds;
	}

	/**
	 * Return the nodes intersecting the given AABB
	 * @param aabb
	 * @param resultBounds
	 * @param emptyNodes if true empty nodes (not existing) node are returned.
	 * Set true when filling the KdTree, and to false when consulting it.
	 */
	private void getNodes(double[] aabb, BoundaryPool resultBounds, boolean emptyNodes)
	{
		if(nodeStack == null)
			nodeStack = new ArrayList<Node>();
		else
			nodeStack.clear();

		if(boundaryPool == null)
			boundaryPool = new BoundaryPool();
		else
			boundaryPool.clear();

		nodeStack.add(root);
		boundaryPool.push(globalBounds);
		assert closeNodes.isEmpty();
		assert resultBounds == null || resultBounds.isEmpty();
		if(resultBounds != null)
			resultBounds.clear();
		assert intersect(aabb, globalBounds): Arrays.toString(aabb)+" doesn't intersect "+Arrays.toString(globalBounds);
		while(!nodeStack.isEmpty())
		{
			int n = nodeStack.size() - 1;
			Node current = nodeStack.remove(n);
			double[] cBounds = boundaryPool.last();
			if(current.left == null && current.right == null)
			{
				//this is a leaf node
				boundaryPool.removeLast();
				closeNodes.add(current);
				if(resultBounds != null)
					resultBounds.push(cBounds);
			}
			else
			{
				double newB = (cBounds[current.direction] + cBounds[3+current.direction]) / 2.0;
				boolean inLeft = aabb[current.direction] < newB;
				boolean inRight = aabb[current.direction+3] >= newB;
				if(emptyNodes)
				{
					if(inLeft && current.left == null)
						current.left = new Node();
					if(inRight && current.right == null)
						current.right = new Node();
				}
				boolean leafLeft = current.left == null;
				boolean leafRight = current.right == null;
				if(inLeft && !inRight)
				{
					if(leafLeft)
						boundaryPool.removeLast();
					else
					{
						cBounds[3 + current.direction] = newB;
						nodeStack.add(current.left);
					}
				}
				else if(!inLeft && inRight)
				{
					if(leafRight)
						boundaryPool.removeLast();
					else
					{
						cBounds[current.direction] = newB;
						nodeStack.add(current.right);
					}
				}
				else if(inLeft && inRight)
				{
					double[] rightBounds = null;
					if(leafLeft)
					{
						if(leafRight)
							//leaf node so we though the boundary array away
							boundaryPool.removeLast();
						else
							//reuse left bounds as right bounds
							rightBounds = cBounds;
					}
					else
					{
						//copy left boundaries to right boundaries before
						//changing it
						if(current.right != null)
						rightBounds = boundaryPool.push(cBounds);
						cBounds[3 + current.direction] = newB;
						nodeStack.add(current.left);
					}

					if(!leafRight)
					{
						rightBounds[current.direction] = newB;
						nodeStack.add(current.right);
					}
				}
				else
				{
					boundaryPool.removeLast();
				}
			}
		}
		assert !closeNodes.isEmpty(): bounds2String(aabb);
	}

	private boolean intersect(double[] aabb1, double[] aabb2)
	{
		return aabb1[0] <= aabb2[3] && aabb1[3] >= aabb2[0] &&
		aabb1[1] <= aabb2[4] && aabb1[4] >= aabb2[1] &&
		aabb1[2] <= aabb2[5] && aabb1[5] >= aabb2[2];
	}

	private Node getNode(double[] coords, double[] tmpBounds)
	{
		System.arraycopy(globalBounds, 0, tmpBounds, 0, tmpBounds.length);
		Node current = root;
		while(true)
		{
			byte d = current.direction;
			double cut = (tmpBounds[d] + tmpBounds[d+3]) / 2.0;
			if(coords[d] < cut)
			{
				if(current.left == null)
				{
					return current;
				}
				else
				{
					current = current.left;
					tmpBounds[3+d] = cut;
				}
			}
			else
			{
				if(current.right == null)
				{
					return current;
				}
				else
				{
					current = current.right;
					tmpBounds[d] = cut;
				}
			}
		}
	}

	private void bounds(Triangle triangle, double[] bounds)
	{
		for(int i = 0; i < 3; i++)
		{
			bounds[i] = Double.POSITIVE_INFINITY;
			bounds[i+3] = Double.NEGATIVE_INFINITY;
		}
		for(int i = 0; i < 3; i++)
		{
			double[] uv = triangle.vertex[i].getUV();
			for(int j = 0; j < 3; j++)
			{
				double v = uv[j];
				bounds[j] = Math.min(bounds[j], v);
				bounds[j+3] = Math.max(bounds[j+3], v);
			}
		}
	}

	private final double[] triangleBounds = new double[6];
	public void addTriangle(Triangle triangle)
	{
		bounds(triangle, triangleBounds);
		getNodes(triangleBounds, closeBoundaries, true);
		triangleInterAABB1.setTriangle(triangle);
		for(int i = 0; i < closeNodes.size(); i++)
		{
			double[] b = closeBoundaries.get(i);

			if(triangleInterAABB1.triBoxOverlap(b, true))
				addTriange(triangle, closeNodes.get(i), b);
		}
		closeNodes.clear();
		closeBoundaries.clear();
	}

	private void addTriange(Triangle triangle, Node node, double[] bounds)
	{
		if(node.triangles == null)
		{
			node.triangles = new Triangle[]{triangle};
		}
		else
		{
			int n = node.triangles.length;
			node.triangles = Arrays.copyOf(node.triangles, n + 1);
			node.triangles[n] = triangle;
			if(node.triangles.length > bucketSize)
				split(node, bounds);
		}
	}

	private byte getSplitDirection(double[] bounds)
	{
		double max = 0;
		byte maxDir = 0;
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
	private double[] splitBoundsLeft = new double[6];
	private double[] splitBoundsRight = new double[6];
	private void split(Node node, double[] bounds)
	{
		node.direction = getSplitDirection(bounds);
		node.left = new Node();
		node.right = new Node();
		node.left.triangles = new Triangle[node.triangles.length];
		node.right.triangles = new Triangle[node.triangles.length];
		int iLeft = 0, iRight = 0;
		double cut = (bounds[node.direction] + bounds[node.direction+3]) / 2.0;
		System.arraycopy(bounds, 0, splitBoundsLeft, 0, 6);
		System.arraycopy(bounds, 0, splitBoundsRight, 0, 6);
		splitBoundsLeft[node.direction + 3] = cut;
		splitBoundsRight[node.direction] = cut;
		for(Triangle t: node.triangles)
		{
			boolean inLeft = false;
			boolean inRight = false;
			for(int i = 0; i < 3; i++)
			{
				if(t.vertex[i].getUV()[node.direction] < cut)
				{
					inLeft = true;
					break;
				}
			}

			for(int i = 0; i < 3; i++)
			{
				if(t.vertex[i].getUV()[node.direction] >= cut)
				{
					inRight = true;
					break;
				}
			}
			triangleInterAABB2.setTriangle(t);
			if(inLeft && triangleInterAABB2.triBoxOverlap(splitBoundsLeft, false))
				node.left.triangles[iLeft++] = t;
			if(inRight && triangleInterAABB2.triBoxOverlap(splitBoundsRight, false))
				node.right.triangles[iRight++] = t;
		}
		if(iLeft > 0)
			node.left.triangles = Arrays.copyOf(node.left.triangles, iLeft);
		else
			node.left = null;

		if(iRight > 0)
			node.right.triangles = Arrays.copyOf(node.right.triangles, iRight);
		else
			node.right = null;

		node.triangles = null;
	}

	private void stats()
	{
		ArrayList<Node> nodes =new ArrayList<Node>();
		nodes.add(root);
		int nbLeaf = 0;
		int maxDepth = 0;
		while(!nodes.isEmpty())
		{
			Node n = nodes.remove(nodes.size() - 1);
			if(n.left != null)
				nodes.add(n.left);
			if(n.right != null)
				nodes.add(n.right);
			if(n.triangles != null)
				nbLeaf++;
			maxDepth = Math.max(nodes.size(), maxDepth);
		}
		System.out.println("number of leaves: "+nbLeaf+" max depth: "+maxDepth);
	}
	/** The 8 vertices of a voxel */
	private static int[][] VOXEL_VERTICES = new int[][]
	{
		{0, 0, 0},
		{1, 0, 0},
		{0, 1, 0},
		{1, 1, 0},
		{0, 0, 1},
		{1, 0, 1},
		{0, 1, 1},
		{1, 1, 1}
	};

	/** The 6 faces of a voxel */
	private static int[][] VOXEL_FACES = new int[][]
	{
		{0, 1, 3, 2},
		{0, 1, 5, 4},
		{0, 4, 6, 2},
		{4, 5, 7, 6},
		{1, 3, 7, 5},
		{2, 6, 7, 3}
	};

	public Object[] getPolyData()
	{
		getNodes(createCenteredAABB(new double[3], Double.POSITIVE_INFINITY), closeBoundaries, false);

		int vertexCounter = 0;
		ArrayList<int[]> quads = new ArrayList<int[]>();
		ArrayList<double[]> coords = new ArrayList<double[]>();
		int n = closeNodes.size();
		for(int i = 0; i < n; i++)
		{
			Node node = closeNodes.get(i);
			double[] nBounds = closeBoundaries.get(i);
			if(node.triangles != null && node.triangles.length > 0)
			{
				int[] quad = new int[24];
				quads.add(quad);
				int voxelVertexId = 0;
				for(int[] face:VOXEL_FACES)
				{
					for(int localVertexId = 0; localVertexId < 4; localVertexId++)
					{
						int[] lv = VOXEL_VERTICES[face[localVertexId]];
						int vertexId = vertexCounter ++;
						double[] coord = new double[3];
						for(int j = 0; j < 3; j ++)
							coord[j] = nBounds[j + 3 * lv[j]];
						coords.add(coord);
						quad[voxelVertexId++] = vertexId;
					}
				}
			}
		}
		closeNodes.clear();
		closeBoundaries.clear();
		int[] quadArray = new int[quads.size() * 24];
		int k = 0;
		for(int[] q:quads)
		{
			System.arraycopy(q, 0, quadArray, k, q.length);
			k += q.length;
		}
		quads = null;
		k = 0;
		double[] coordArray = new double[coords.size() * 3];
		for(double[] q:coords)
		{
			System.arraycopy(q, 0, coordArray, k, q.length);
			k += q.length;
		}
		coords = null;
		return new Object[]{coordArray, quadArray};
	}

	public static void main(final String[] args) {
		try {
			Mesh mesh = new Mesh();
			MeshReader.readObject3D(mesh, "/tmp/bidule.amibe");
			System.out.println(mesh.getTriangles().size());
			int[] bucketSize = new int[]{7,8,9,10,11,12,13,14};
			for(int bs: bucketSize)
			{
				System.out.println("******** "+bs+" **********");
				long l1 = System.nanoTime();
				TriangleKdTree t = new TriangleKdTree(mesh, bs);
				long l2 = System.nanoTime();
				double[] coords = new double[3];
				loop: for(Triangle tria:mesh.getTriangles())
				{
					if(tria.hasAttributes(AbstractHalfEdge.BOUNDARY))
						continue;
					for(int i = 0; i < 3; i++)
					{
						coords[i] = 0;
						for(int j = 0; j < 3; j++)
							coords[i] += tria.vertex[j].getUV()[i];
						coords[i] /= 3;
					}
					Triangle tp = t.getClosestTriangle(coords, null, -1);
					if(tp != tria)
						throw new IllegalStateException(tria.toString()+" "+Arrays.toString(
							coords)+" "+tp);
				}
				System.out.println(t.getClosestTriangle(new double[]{400, -800, 0}, null, -1));
				long l3 = System.nanoTime();
				t.stats();
				System.out.println((l2-l1)/1E9+" "+(l3-l2)/1E9);
			}
		} catch (IOException ex) {
			Logger.getLogger(TriangleKdTree.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
