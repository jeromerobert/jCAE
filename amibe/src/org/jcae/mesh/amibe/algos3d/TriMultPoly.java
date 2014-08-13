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
 * (C) Copyright 2014, by EADS France
 */

package org.jcae.mesh.amibe.algos3d;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.projection.AbstractLocaleRemesher;
import org.jcae.mesh.xmldata.MeshWriter;

/**
 * A wrapper for TriMultPoly.
 * See:
 * <ul>
 *  <li><a href="http://www.cse.wustl.edu/~zoum/projects/TriMultPoly/">http://www.cse.wustl.edu/~zoum/projects/TriMultPoly/</a></li>
 *  <li><a href="https://bitbucket.org/jeromerobert/trimultpoly/">https://bitbucket.org/jeromerobert/trimultpoly/</a></li>
 * </ul>
 * Run with java -Dorg.jcae.amibe.trimultpoly.path=/path/to/trimultpoly.exe with
 * to enable this algorithm.
 * @author Jerome Robert
 */
public class TriMultPoly extends AbstractLocaleRemesher {
	private final static Logger LOGGER = Logger.getLogger(TriMultPoly.class.getName());
	public final static String EXE_PATH = "org.jcae.amibe.trimultpoly.path";
	private final String executable;
	private boolean initDone;
	private Process process;
	private boolean delauneyTetra = true, minSet = false, normal = false;
	private double areaWeight = 0, edgeWeight = 0, dihedralWeight = 1.0;
	private boolean minMaxDihedral = true;
	private ReadableByteChannel stderr;
	private WritableByteChannel stdin;
	private final ByteBuffer buffer = ByteBuffer.allocate(3 + 4 * 8 + 4 + 4);

	public TriMultPoly()
	{
		this(System.getProperty(EXE_PATH));
	}

	public TriMultPoly(String executable)
	{
		this.executable = executable;
		buffer.order(ByteOrder.nativeOrder());
	}

	public void setDelauneyTetra(boolean delauneyTetra) {
		this.delauneyTetra = delauneyTetra;
	}

	public void setMinSet(boolean minSet) {
		this.minSet = minSet;
	}

	public void setAreaWeight(double areaWeight) {
		this.areaWeight = areaWeight;
	}

	public void setDihedralWeight(double dihedralWeight) {
		this.dihedralWeight = dihedralWeight;
	}

	public void setMinMaxDihedral(boolean minMaxDihedral) {
		this.minMaxDihedral = minMaxDihedral;
	}

	public void setEdgeWeight(double edgeWeight) {
		this.edgeWeight = edgeWeight;
	}

	public boolean isAvailable()
	{
		initExe();
		return process != null;
	}

	private void initExe()
	{
		if(initDone)
			return;
		initDone = true;
		if(executable == null)
			return;
		ProcessBuilder pb = new ProcessBuilder(executable, "-d");
		try {
			pb.redirectOutput(Redirect.INHERIT);
			process = pb.start();
			stdin = Channels.newChannel(process.getOutputStream());
			stderr = Channels.newChannel(process.getErrorStream());
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Cannot run "+executable, ex);
		}
	}

	/** Save in the native TriMultPoly file format for debugging */
	private static void saveCurve(Collection<List<Vertex>> vertices, String fileName) throws IOException
	{
		PrintWriter pw = new PrintWriter(fileName);
		int nbVert = 0;
		for(List<Vertex> vs: vertices)
			nbVert += vs.size();
		pw.println(vertices.size());
		pw.println(nbVert);
		for(List<Vertex> pl: vertices)
		{
			pw.println(pl.size());
			for(Vertex v:pl)
				pw.println(v.getX() + " " + v.getY() + " " + v.getZ());
		}
		pw.close();
	}

	private int sendVertices(Collection<List<Vertex>> vertices) throws IOException
	{
		buffer.put((byte) (delauneyTetra ? 1 : 0));
		buffer.put((byte) (minSet ? 1 : 0));
		buffer.put((byte) (normal ? 1 : 0));
		buffer.putDouble(areaWeight);
		buffer.putDouble(edgeWeight);
		buffer.putDouble(dihedralWeight);
		buffer.putDouble(minMaxDihedral ? 1.0 : 0);
		buffer.putInt(vertices.size());
		int nbVert = 0;
		for(List<Vertex> vs: vertices)
			nbVert += vs.size();
		buffer.putInt(nbVert);
		initExe();
		buffer.rewind();
		stdin.write(buffer);
		buffer.rewind();
		for(List<Vertex> vs: vertices)
		{
			buffer.putInt(0, vs.size());
			buffer.rewind();
			buffer.limit(4);
			stdin.write(buffer);
			buffer.rewind();
			buffer.limit(3*8);
			for(Vertex v:vs)
			{
				buffer.putDouble(v.getX());
				buffer.putDouble(v.getY());
				buffer.putDouble(v.getZ());
				buffer.rewind();
				stdin.write(buffer);
				buffer.rewind();
			}
		}
		process.getOutputStream().flush();
		return nbVert;
	}

	protected void triangulate(Mesh mesh, Collection<List<Vertex>> vertices) throws IOException
	{
		buffer.limit(4);
		stderr.read(buffer);
		int nbTria = buffer.getInt(0);
		if(nbTria == 0)
			throw new RuntimeException("trimulpoly was not able to create a triangulation");
		buffer.limit(12);
		buffer.rewind();
		for(int i = 0; i < nbTria; i++)
		{
			stderr.read(buffer);
			addTriangle(mesh, buffer.getInt(0), buffer.getInt(4), buffer.getInt(8));
			buffer.rewind();
		}
		buffer.clear();
	}

	public void close()
	{
		process.destroy();
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}

	public static List<Vertex> createVertices(Mesh mesh, double[] array)
	{
		List<Vertex> r = new ArrayList<Vertex>();
		for(int i = 0; i < array.length / 3; i++)
			r.add(mesh.createVertex(
				array[3 * i], array[3 * i + 1], array[3 * i + 2]));
		return r;
	}

	public static void main(final String[] args) {
		TriMultPoly tmp = new TriMultPoly(
			"/home/robert/tetramesh/TriMultPoly_src_v1.1.0/TriMultPoly");
		double[] points = {
			4.502539, -1.103106, 0.548982,
			2.502539, -1.103106, 0.548982,
			2.362264, -1.014786, 0.548982,
			2.258357, -0.853729, 0.548982,
			2.242771, -0.604352, 0.548982,
			2.398632, -0.308217, 0.548982,
			2.845433, -0.053644, 0.548982,
			3.396141, -0.017276, 0.548982,
			3.941654, -0.038058, 0.548982,
			4.372869, 0.097022, 0.548982,
			4.585879, 0.455501, 0.548982,
			4.497558, 0.658120, 0.548982,
			4.081929, 0.855544, 0.548982,
			3.650714, 0.954256, 0.548982,
			3.048052, 0.969842, 0.548982,
			2.299920, 0.881521, 0.548982,
			2.299920, 0.881521, -1.451018,
			3.048052, 0.969842, -1.451018,
			3.650714, 0.954256, -1.451018,
			4.081929, 0.855544, -1.451018,
			4.497558, 0.658120, -1.451018,
			4.585879, 0.455501, -1.451018,
			4.372869, 0.097022, -1.451018,
			3.941654, -0.038058, -1.451018,
			3.396141, -0.017276, -1.451018,
			2.845433, -0.053644, -1.451018,
			2.398632, -0.308217, -1.451018,
			2.242771, -0.604352, -1.451018,
			2.258357, -0.853729, -1.451018,
			2.362264, -1.014786, -1.451018,
			2.502539, -1.103106, -1.451018,
			4.502539, -1.103106, -1.4510180};
		Mesh mesh = new Mesh();
		List<Vertex> vertices = createVertices(mesh, points);
		try {
			tmp.triangulate(mesh, Collections.singleton(vertices));
			for(Triangle t: tmp.getNewTriangles())
				mesh.add(t);
			MeshWriter.writeObject3D(mesh, "/tmp/pbl/bug.zebra/trimultpoly.amibe", null);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

		mesh = new Mesh();
		List<Vertex> vertices1 = createVertices(mesh, new double[]{
			0,0,0,
			1,0,0,
			1,1,0,
			0,1,0
		});
		List<Vertex> vertices2 = createVertices(mesh, new double[]{
			0.3, 0.2, 0,
			0.8, 0.9, 0,
			(0.3+0.8) / 2, (0.2+0.9) / 2, 0
		});
		tmp = new TriMultPoly("/home/robert/tetramesh/TriMultPoly_src_v1.1.0/TriMultPoly");
		//tmp.setDelauneyTetra(false);
		try {
			List<List<Vertex>> input = Arrays.asList(vertices1, vertices2);
			saveCurve(input, "/tmp/trimultpoly.txt");
			tmp.triangulate(mesh, input);
			for(Triangle t: tmp.getNewTriangles())
				mesh.add(t);
			MeshWriter.writeObject3D(mesh, "/tmp/pbl/bug.zebra/trimultpoly2.amibe", null);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}
}