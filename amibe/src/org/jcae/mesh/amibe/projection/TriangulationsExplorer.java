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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Iterate over all non manifold triangulations for a n vertex polyline with
 * a given number of holes.
 * The triangulationCreated method is called for each valid triangulation found.
 * the number of possible triangulations without holes is:
 * <pre>g(n) = 2*(g(n-1)+g(n-2))</pre>
 * <pre>g(n) = 1/8 ((2+sqrt(3)) (1-sqrt(3))^n-(sqrt(3)-2) (1+sqrt(3))^n)</pre>
 * Implemented from An algorithm for triangulating multiple 3D polygons by
 * Ming Zou, Tao Ju, Nathan Carr, Eurographics SGP 2013
 * @author Jerome Robert
 */
abstract class TriangulationsExplorer {

	protected abstract boolean isTriangleValid(int v1, int v2, int v3);

	protected abstract void triangulationCreated();

	/** Push a triangle with 2 edges on the border */
	private boolean pushTriangle(int[] vector, int v1) {
		int v2 = vector[(v1 + 1) % vector.length];
		int v3 = vector[(v1 + 2) % vector.length];
		v1 = vector[v1];
		if (v1 == v2 || v2 == v3 || v3 == v1) {
			return false;
		}
		if (v1 >= externalContourLength && v2 >= externalContourLength && v3 >= externalContourLength) {
			//TODO handle the case of triangles between 2 holes
			return false;
		}
		if (isWeakEdge(v1, v3)) {
			return false;
		}
		if (vector.length > 4 && (isDoubleVertex(v3) || isDoubleVertex(v1))) {
			pushWeakEdge(v1, v3);
		}
		return pushTriangle(v1, v2, v3);
	}

	private boolean pushTriangle(int[] vector, int v1, int v3) {
		int n = vector.length;
		assert v1 == 0;
		assert v3 < n;
		assert v3 != (v1 + 2) % n;
		assert v3 != (v1 - 1) % n;
		int v2 = vector[v1 + 1];
		v1 = vector[v1];
		int vv3 = vector[v3];
		if (v1 == v2 || v2 == vv3 || vv3 == v1) {
			return false;
		}
		if (isWeakEdge(v1, vv3)) {
			return false;
		}
		if (isWeakEdge(v2, vv3)) {
			return false;
		}
		if (n > 4) {
			if (isDoubleVertex(vv3)) {
				if (v3 < n - 2) {
					pushWeakEdge(v1, vv3);
				}
				if (v3 > 3) {
					pushWeakEdge(v2, vv3);
				}
			}
			/*else if(isDoubleVertex(v1) || isDoubleVertex(v2) || isDoubleVertex(vv3))
			System.err.println("WARNING: possible non manifold: "+
			v1+" "+isDoubleVertex(v1)+" "+
			v2+" "+isDoubleVertex(v2)+" "+
			vv3+" "+isDoubleVertex(vv3)+" n="+n+" v3="+v3);*/
		}
		return pushTriangle(v1, v2, vv3);
	}

	/** Push a triangle with a vertex on a hole */
	private boolean pushTriangleHole(int[] vector, int v1, int v3) {
		int v2 = vector[v1 + 1];
		v1 = vector[v1];
		if (isWeakEdge(v1, v3)) {
			return false;
		}
		if (isWeakEdge(v2, v3)) {
			return false;
		}
		assert !isDoubleVertex(v1);
		assert !isDoubleVertex(v2);
		assert !isDoubleVertex(v3);
		return pushTriangle(v1, v2, v3);
	}

	private boolean isWellOriented(int v1, int v2) {
		//TODO may be already tested: remove
		if (v1 == v2) {
			return false;
		}
		if (v1 < externalContourLength && v2 < externalContourLength) {
			int delta = v2 - v1;
			if (delta == -1 || delta == externalContourLength - 1) {
				return false;
			} else {
				return true;
			}
		} else {
			return true;
		}
	}

	protected boolean pushTriangle(int v1, int v2, int v3) {
		if (!isWellOriented(v1, v2)) {
			return false;
		}
		if (!isWellOriented(v2, v3)) {
			return false;
		}
		if (!isWellOriented(v3, v1)) {
			return false;
		}
		if (!isTriangleValid(v1, v2, v3)) {
			return false;
		}
		triangleStack[triangletackPointer++] = v1;
		triangleStack[triangletackPointer++] = v2;
		triangleStack[triangletackPointer++] = v3;
		if (triangletackPointer == triangleStack.length) {
			triangulationCreated();
		}
		return true;
	}

	private void popTriangle() {
		triangletackPointer -= 3;
		assert triangletackPointer >= 0;
	}

	public String getNonManifold() {
		StringBuilder sb = new StringBuilder();
		HashSet<Edge> edges = new HashSet<Edge>();
		for (int i = 0; i < triangletackPointer; i += 3) {
			int v1 = triangleStack[i];
			int v2 = triangleStack[i + 1];
			int v3 = triangleStack[i + 2];
			Edge e1 = new Edge(v1, v2);
			Edge e2 = new Edge(v2, v3);
			Edge e3 = new Edge(v3, v1);
			if (!edges.add(e1)) {
				sb.append(e1).append(' ').append(v3).append('\n');
			}
			if (!edges.add(e2)) {
				sb.append(e2).append(' ').append(v1).append('\n');
			}
			if (!edges.add(e3)) {
				sb.append(e3).append(' ').append(v2).append('\n');
			}
		}
		return sb.toString();
	}
	protected int[] triangleStack;
	protected int triangletackPointer;
	private int externalContourLength;
	protected int[] forbiddenEdges;
	protected int forbiddenEdgesPointer;
	protected int doubleVerticesIndex;
	protected int[] doubleVertices;

	public void travel(int n, int... holes) {
		int[] root = new int[n];
		int sigmaHole = 0;
		for (int i = 0; i < holes.length; i++) {
			if (holes[i] > 1) {
				sigmaHole += holes[i];
			}
		}
		for (int i = 0; i < n; i++) {
			root[i] = i;
		}
		doubleVertices = new int[holes.length];
		/*
		How I found the number of triangles:
		n: size of external border, n,m,q: size of holes.
		f(n)=n-2
		f(n,1)=n
		f(n,1..1)=f(n,p)=n+p
		f(n,m)=n+m
		f(n,m,q,p)=f(3,m)+f(3,q)+f(3,p)+f(n-3)=9+m+q+p+n-5=4+m+q+p+n
		with m != 1:
		f(n,m1,..,mp)=f(n-p)+sum(1,p,f(3,mi))=n-p+sum(1,p,3+mi)=n-p-2+3*p+sum(mi)=n+sum(mi)+2*p-2
		m: !=1
		p: =1
		f(n, [m], [p])=f(n-m-p)+sum(f(3,mi))+3*p=n-m-p-2 + m*3 +sum(mi)+3*p=n+2*m+2*p+sum(mi)-2=n-2+2m+2p+sum(mi)
		 */
		triangleStack = new int[(n + sigmaHole + 2 * holes.length - 2) * 3];
		triangletackPointer = 0;
		externalContourLength = n;
		//3 weak edges for case 1 splitting, 1 or 2 for case 2
		//TODO get the real length
		forbiddenEdges = new int[100 * holes.length];
		forbiddenEdgesPointer = 0;
		int[][] holesArray = new int[holes.length][];
		int k = n;
		for (int i = 0; i < holes.length; i++) {
			holesArray[i] = new int[holes[i]];
			for (int j = 0; j < holes[i]; j++) {
				holesArray[i][j] = k++;
			}
		}
		travel(root, holesArray);
	}

	protected boolean isWeakEdge(int v1, int v2) {
		//TODO possible performance bottle neck
		for (int i = 0; i < forbiddenEdgesPointer; i += 2) {
			if ((v1 == forbiddenEdges[i] && v2 == forbiddenEdges[i + 1]) || (v1 == forbiddenEdges[i + 1] && v2 == forbiddenEdges[i])) {
				return true;
			}
		}
		return false;
	}

	private boolean isDoubleVertex(int v) {
		for (int i = 0; i < doubleVerticesIndex; i++) {
			if (v == doubleVertices[i]) {
				return true;
			}
		}
		return false;
	}

	protected void pushWeakEdge(int v1, int v2) {
		assert v1 != v2;
		assert forbiddenEdgesPointer < forbiddenEdges.length - 1 : v1 + " " + v2 + " l=" + forbiddenEdges.length + " " + forbiddenEdgesPointer;
		forbiddenEdges[forbiddenEdgesPointer++] = v1;
		forbiddenEdges[forbiddenEdgesPointer++] = v2;
	}

	private void pushWeakEdge(int v1, int v2, int v3) {
		pushWeakEdge(v1, v2);
		pushWeakEdge(v1, v3);
		pushWeakEdge(v2, v3);
		doubleVertices[doubleVerticesIndex++] = v3;
	}

	private void popWeakEdge() {
		forbiddenEdgesPointer -= 2 * 3;
		doubleVerticesIndex--;
	}

	private static class HoleSplittedSet {

		int[][] ones;
		int[][] zeros;
	}

	/**
	 * Return all way to split a set of holes in 2.
	 * This is needed for case 2 split.
	 */
	private HoleSplittedSet[] create2Partitions(int[][] holes) {
		int hl = holes.length;
		int n = 1 << hl;
		HoleSplittedSet[] toReturn = new HoleSplittedSet[n];
		for (int i = 0; i < n; i++) {
			int bc = Integer.bitCount(i);
			toReturn[i] = new HoleSplittedSet();
			toReturn[i].ones = new int[bc][];
			toReturn[i].zeros = new int[hl - bc][];
			int oc = 0;
			int oz = 0;
			for (int j = 0; j < hl; j++) {
				if ((i & (1 << j)) == 0) {
					toReturn[i].zeros[oz++] = holes[j];
				} else {
					toReturn[i].ones[oc++] = holes[j];
				}
			}
		}
		return toReturn;
	}

	/** connect the 0,1 edge of vector to any vertices of any holes */
	private void travelCase1(int[] vector, int[]... holes) {
		int[][] newHoles = null;
		if (holes.length > 1) {
			newHoles = new int[holes.length - 1][];
		}
		//loop on holes
		for (int i = 0; i < holes.length; i++) {
			int holeLength = holes[i].length;
			int k = vector.length;
			int[] newVector = new int[k + holeLength + 1];
			System.arraycopy(vector, 1, newVector, 0, k - 1);
			newVector[k - 1] = vector[0];
			if (holes.length > 1) {
				System.arraycopy(holes, 0, newHoles, 0, i);
				System.arraycopy(holes, i + 1, newHoles, i, holes.length - i - 1);
			}
			// loop on possible apex for the 0,1 edge in the current hole
			for (int j = 0; j < holeLength; j++) {
				if (pushTriangleHole(vector, 0, holes[i][j])) {
					pushWeakEdge(vector[0], vector[1], holes[i][j]);
					k = vector.length;
					// open the selected hole and link it to vector to make
					// a new polyline loop
					for (int m = 0; m < holeLength + 1; m++) {
						assert (j + m) % holeLength < holes[i].length;
						assert k < newVector.length;
						newVector[k++] = holes[i][(j + m) % holeLength];
					}
					travel(newVector, newHoles);
					// switch the orientation of the hole
					if (holeLength > 2) {
						k = vector.length + 1;
						for (int m = holeLength - 1; m >= 1; m--) {
							newVector[k++] = holes[i][(j + m) % holeLength];
						}
						travel(newVector, newHoles);
					}
					popTriangle();
					popWeakEdge();
				}
			}
		}
	}

	private void travelCase2(int[] vector, int[]... holes) {
		int n = vector.length;
		int[] subVect = null;
		if (pushTriangle(vector, n - 1)) {
			subVect = new int[vector.length - 1];
			System.arraycopy(vector, 1, subVect, 0, subVect.length);
			travel(subVect, holes);
			popTriangle();
		}
		if (pushTriangle(vector, 0)) {
			if (subVect == null) {
				subVect = new int[vector.length - 1];
				System.arraycopy(vector, 2, subVect, 1, subVect.length - 1);
			}
			subVect[0] = vector[0];
			travel(subVect, holes);
			popTriangle();
		}
		HoleSplittedSet[] holesPartitions = create2Partitions(holes);
		for (int j = 2; j < n - 2; j++) {
			// Create triangles with the vertices 0, 1 and an other point
			int p = (1 + j) % n;
			if (!pushTriangle(vector, 0, p)) {
				continue;
			}
			assert p != 0 && p != 1;
			int size1;
			int size2;
			size2 = p;
			size1 = n - size2 + 1;
			assert size1 >= 3 : p + " " + Arrays.toString(vector);
			assert size2 >= 3 : p + " " + Arrays.toString(vector);
			int[] subVect1 = new int[size1];
			for (int k = 0; k < size1; k++) {
				subVect1[k] = vector[(k + p) % n];
			}
			int[] subVect2 = new int[size2];
			for (int k = 0; k < size2; k++) {
				subVect2[k] = vector[(k + 1) % n];
			}
			for (HoleSplittedSet holePartition : holesPartitions) {
				travel(subVect1, holePartition.zeros);
				travel(subVect2, holePartition.ones);
			}
			popTriangle();
		}
	}

	protected void travel(int[] vector, int[]... holes) {
		assert vector.length >= 3;
		//TODO check orientation of triangles
		if (holes == null || holes.length == 0) {
			if (vector.length == 3) {
				pushTriangle(vector, 0);
				popTriangle();
			} else {
				travel(vector);
			}
			return;
		}
		travelCase1(vector, holes);
		if (vector.length > 3) {
			travelCase2(vector, holes);
		}
	}

	protected void travel(int[] vector) {
		int n = vector.length;
		assert n >= 3 : n;
		int triangleStackBackup = triangletackPointer;
		int weakEdgeBackup = forbiddenEdgesPointer;
		for (int j = 1; j < n - 1; j++) {
			triangletackPointer = triangleStackBackup;
			forbiddenEdgesPointer = weakEdgeBackup;
			// Create triangles with the vertices 0, 1 and an other point
			int p = (1 + j) % n;
			if (p == 2) {
				if (!pushTriangle(vector, 0)) {
					continue;
				}
			} else if (p == n - 1) {
				if (!pushTriangle(vector, p)) {
					continue;
				}
			} else {
				if (!pushTriangle(vector, 0, p)) {
					continue;
				}
			}
			assert p != 0 && p != 1;
			int size1;
			int size2;
			if (p < 0) {
				size1 = 1 - p;
				size2 = n - size1 + 1;
			} else {
				size2 = p;
				size1 = n - size2 + 1;
			}
			if (size1 == 3 && !pushTriangle(vector, p)) {
				continue;
			}
			if (size2 == 3 && !pushTriangle(vector, 1)) {
				continue;
			}
			if (size1 > 3) {
				int[] subVect1 = new int[size1];
				for (int k = 0; k < size1; k++) {
					subVect1[k] = vector[(k + p) % n];
				}
				travel(subVect1);
			}
			if (size2 > 3) {
				int[] subVect2 = new int[size2];
				for (int k = 0; k < size2; k++) {
					subVect2[k] = vector[(k + 1) % n];
				}
				travel(subVect2);
			}
		}
		triangletackPointer = triangleStackBackup;
		forbiddenEdgesPointer = weakEdgeBackup;
	}

	public void printStack() {
		for (int i = 0; i < triangletackPointer / 3; i++) {
			System.err.println(triangleStack[3 * i] + " " + triangleStack[3 * i + 1] + " " + triangleStack[3 * i + 2]);
		}
	}

	private static class Edge
	{
		public final int v1, v2;

		public Edge(int v1, int v2) {
			this.v1 = v1;
			this.v2 = v2;
		}

		@Override
		public boolean equals(Object other) {
			Edge obj = (Edge) other;
			return v1 == obj.v1 && v2 == obj.v2;
		}

		@Override
		public int hashCode() {
			return v1+v2;
		}

		@Override
		public String toString() {
			return "["+v1+", "+v2+"]";
		}
	}

	public static int numberOfPossibleTriangulation(int n)
	{
		// from g(n) = 2*(g(n-1)+g(n-2))
		return (int) (((2 + Math.sqrt(3)) * Math.pow(1 - Math.sqrt(3), n) -
			(Math.sqrt(3) - 2) * Math.pow(1 + Math.sqrt(3), n)) / 8.0 + 0.5);
	}

	/**
	 * Disable weaks edges to count the exact number of possible triangulations
	 */
	protected static class NoWEExplorer extends TriangulationsExplorer
	{
		int numberOfTriangulation;
		@Override
		protected boolean isWeakEdge(int v1, int v2) {
			return false;
		}

		@Override
		protected boolean isTriangleValid(int v1, int v2, int v3) {
			return true;
		}

		@Override
		protected void triangulationCreated() {
			if(getNonManifold().isEmpty())
				numberOfTriangulation++;
		}

		public void travel(int n, int... holes) {
			numberOfTriangulation = 0;
			super.travel(n, holes);
		}

		@Override
		protected void pushWeakEdge(int v1, int v2)
		{
		}
	}

	private static String indent(String text, int n)
	{
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < n; i++)
			sb.append(' ');
		sb.append(text);
		return sb.toString();
	}
	/** For performance measurement */
	protected static class NOPExplorer extends TriangulationsExplorer
	{

		@Override
		protected boolean isTriangleValid(int v1, int v2, int v3) {
			return true;
		}

		@Override
		protected void triangulationCreated() {
		}
	}
	/** Ensure that we do not create a triangulation 2 times */
	protected static class NoDoubleExplorer extends TriangulationsExplorer
	{
		@Override
		public void travel(int n, int... holes) {
			triangulations.clear();
			super.travel(n, holes);
		}

		@Override
		protected boolean pushTriangle(int v1, int v2, int v3) {
			boolean r = super.pushTriangle(v1, v2, v3);
			if(!getNonManifold().isEmpty())
				throw new RuntimeException(v1+" "+v2+" "+v3);
			return r;
		}

		@Override
		protected boolean isTriangleValid(int v1, int v2, int v3) {
			return true;
		}

		Set<HashSet<HashSet<Integer>>> triangulations = new HashSet<HashSet<HashSet<Integer>>>();

		@Override
		protected void triangulationCreated() {
			HashSet<HashSet<Integer>> ta = new HashSet<HashSet<Integer>>();
			for(int i = 0; i < triangleStack.length / 3; i++)
			{
				HashSet<Integer> triangle = new HashSet<Integer>();
				triangle.add(triangleStack[3 * i]);
				triangle.add(triangleStack[3 * i + 1]);
				triangle.add(triangleStack[3 * i + 2]);
				ta.add(triangle);
			}
			if(!triangulations.add(ta))
				throw new RuntimeException("identical triangulation");
		}
	}

	/** For debugging */
	protected static class DebugExplorer extends TriangulationsExplorer
	{

		@Override
		public void travel(int n, int... holes) {
			triangulations.clear();
			super.travel(n, holes);
		}

		public void printNonManifold()
		{
			System.err.println("------- Non manifold -------");
			System.err.println(getNonManifold());
			System.err.println("---");
			System.err.println(Arrays.toString(forbiddenEdges));
			System.err.println("---------------------------");
		}

		private int depth = 0;
		private String toString(int[][] a)
		{
			if(a == null)
				return "null";
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			for(int[] aa: a)
			{
				sb.append(Arrays.toString(aa));
			}
			sb.append(']');
			return sb.toString();
		}
		protected void travel(int[] vector)
		{
			System.err.println("+"+indent(Arrays.toString(vector), depth)+" => nbt:"+triangletackPointer/3+" "+weToStr());
			depth++;
			super.travel(vector);
			depth--;
		}

		@Override
		protected void travel(int[] vector, int[]... holes) {
			System.err.println("*"+indent(Arrays.toString(vector)+" "+toString(holes), depth)+" => nbt:"+triangletackPointer/3+" "+weToStr());
			depth++;
			super.travel(vector, holes);
			depth--;
		}

		@Override
		protected boolean pushTriangle(int v1, int v2, int v3) {
			boolean r = super.pushTriangle(v1, v2, v3);
			if(!getNonManifold().isEmpty())
			{
				System.err.println(getNonManifold());
				System.err.println(weToStr());
				printStack();
				throw new RuntimeException(v1+" "+v2+" "+v3);
			}
			return r;
		}

		@Override
		protected boolean isTriangleValid(int v1, int v2, int v3) {
			return true;
		}

		private String weToStr()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("we:{");
			for(int i = 0; i < forbiddenEdgesPointer / 2; i++)
			{
				sb.append(forbiddenEdges[2*i]).append('-');
				sb.append(forbiddenEdges[2*i+1]).append(", ");
			}
			sb.append(Arrays.toString(doubleVertices));
			sb.append('}');
			return sb.toString();
		}
		HashMap<HashSet<HashSet<Integer>>, String> triangulations = new HashMap<HashSet<HashSet<Integer>>, String>();
		protected void printTriangulation() {
			System.err.println("********************");
			HashSet<HashSet<Integer>> ta = new HashSet<HashSet<Integer>>();
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < triangleStack.length / 3; i++)
			{
				sb.append(triangleStack[3 * i] + " " +
					triangleStack[3 * i + 1] + " " + triangleStack[3 * i + 2]+"\n");
				HashSet<Integer> triangle = new HashSet<Integer>();
				triangle.add(triangleStack[3 * i]);
				triangle.add(triangleStack[3 * i + 1]);
				triangle.add(triangleStack[3 * i + 2]);
				ta.add(triangle);
			}
			System.err.println(weToStr());
			String existing = triangulations.get(ta);
			if(existing == null)
			{
				triangulations.put(ta, sb.toString());
				System.err.println(sb);
			}
			else
			{
				throw new RuntimeException("identical triangulation:\n"+sb+"\n****************\n"+existing);
			}
		}

		@Override
		protected void triangulationCreated() {
			printTriangulation();
		}
	}

	private static void test()
	{
		NoDoubleExplorer hf = new NoDoubleExplorer();
		NoWEExplorer ttnwe = new NoWEExplorer();
		for(int i = 3; i < 7; i++)
		{
			int n = numberOfPossibleTriangulation(i);
			hf.travel(i);
			assert hf.triangulations.size() == n: n+" "+hf.triangulations.size();
		}

		for(int j = 2; j < 5; j++)
		{
			for(int i = 3; i < 7; i++)
			{
				System.err.println(i+" "+j);
				ttnwe.travel(i,j);
				hf.travel(i,j);
				assert hf.triangulations.size() == ttnwe.numberOfTriangulation:
					hf.triangulations.size()+" "+ttnwe.numberOfTriangulation;
			}
		}

		ttnwe.travel(3,2,2);
		hf.travel(3,2,2);
		assert hf.triangulations.size() == ttnwe.numberOfTriangulation:
			hf.triangulations.size()+" "+ttnwe.numberOfTriangulation;
	}

	public static void main(final String[] args) {
		DebugExplorer ttd = new DebugExplorer();
		//TODO buggy cases
		ttd.travel(3, 4);
		ttd.travel(3, 2, 2);
		test();
	}
}
