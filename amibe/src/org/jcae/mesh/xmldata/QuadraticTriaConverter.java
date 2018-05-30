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
 * (C) Copyright 2018 by Airbus S.A.S.
 */

package org.jcae.mesh.xmldata;

import gnu.trove.map.hash.TLongIntHashMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.jcae.mesh.xmldata.AmibeReader.SubMesh;
import org.xml.sax.SAXException;

/** Convert linear triangles to quadratic triangles */
public class QuadraticTriaConverter {
	private static long edgeID(int n1, int n2) {
		if(n1 < n2)
			return ((long)n1) | ((long)n2) << 32;
		else
			return ((long)n2) | ((long)n1) << 32;
	}

	protected File verticesFile, trianglesFile;
	protected boolean deleteFiles;
	protected int dimension;
	protected int numberOfVertices;
	/** Constructor and high level wrapper for the convert method */
	public QuadraticTriaConverter(AmibeReader.SubMesh subMesh) throws IOException, SAXException {
		dimension = subMesh.getReader().dim();
		verticesFile = File.createTempFile("jcaevert", ".bin");
		trianglesFile = File.createTempFile("jcaetria", ".bin");
		deleteFiles = true;
		convert(subMesh);
	}

	protected QuadraticTriaConverter() {

	}
	protected void convert(SubMesh sm) throws IOException	{
		// 2E = 3F (euler formula) and we allocate half
		TLongIntHashMap edgeMap = new TLongIntHashMap(3 * sm.getNumberOfTrias() / 4, 0.5f, -1, -1);
		// We need random access on vertices so keeping them on disk would be bad
		double[] vertices = new double[sm.getNumberOfNodes() * dimension];
		int[] triangle = new int[3];
		sm.getNodes().get(vertices);
		int ntria = sm.getNumberOfTrias();
		IntFileReader triangles = sm.getTriangles();
		ByteBuffer vertexBuffer = ByteBuffer.allocate(dimension*8);
		ByteBuffer triangleBuffer = ByteBuffer.allocate(3 * 4);
		FileChannel verticesChan = null;
		FileChannel trianglesChan = null;
		try {
			trianglesChan = new FileOutputStream(trianglesFile).getChannel();
			verticesChan = new FileOutputStream(verticesFile).getChannel();
			for(int i = 0; i < ntria; i++) {
				triangles.get(triangle);
				if(triangle[0] < 0)
					// skip outer triangles
					continue;
				// loop on this triangle edges
				for(int j = 0; j < 3; j++) {
					int n1 = triangle[j];
					int n2 = triangle[(j+1)%3];
					long edge = edgeID(n1, n2);
					int midVertex = edgeMap.get(edge);
					if(midVertex == -1) {
						computeMidVertex(vertices, n1, n2, vertexBuffer);
						((Buffer)vertexBuffer).rewind();
						verticesChan.write(vertexBuffer);
						midVertex = numberOfVertices;
						numberOfVertices++;
						edgeMap.put(edge, midVertex);
					}
					triangleBuffer.putInt(j*4, midVertex);
				}
				((Buffer)triangleBuffer).rewind();
				trianglesChan.write(triangleBuffer);
			}
		} finally {
			if(trianglesChan != null)
				trianglesChan.close();
			if(verticesChan != null)
				verticesChan.close();
		}
	}

	// TODO: To be overriden with projection (MeshLiaison.move)
	protected void computeMidVertex(double[] vertices, int v1, int v2, ByteBuffer output) {
		for(int i = 0; i < dimension; i++) {
			output.putDouble(i*8, (vertices[v1*dimension + i] + vertices[v2*dimension + i])/2.);
		}
	}

	/**
	 * Return file which contains the coordinates of vertices which
	 * were added by the conversion. */
	public DoubleFileReader getVertices() throws IOException {
		return new PrimitiveFileReaderFactory().getDoubleReader(verticesFile);
	}

	public int getNumberOfVertices() {
		return numberOfVertices;
	}

	/**
	 * Return the file which contains the list of vertices added by the
	 * conversion on each triangles.
	 * Each integer is an offset in the getVertices() file.
	 */
	public IntFileReader getTriangles() throws IOException {
		return new PrimitiveFileReaderFactory().getIntReader(trianglesFile);
	}

	public void delete() {
		if(deleteFiles) {
			verticesFile.delete();
			trianglesFile.delete();
		}
	}
}
