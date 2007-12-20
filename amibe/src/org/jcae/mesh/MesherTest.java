/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2007, by EADS France

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


package org.jcae.mesh;

import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshToSoupConvert;
import org.jcae.mesh.xmldata.MMesh1DReader;
import org.jcae.mesh.amibe.ds.MMesh1D;
import org.jcae.mesh.amibe.validation.*;
import org.jcae.mesh.Mesher;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.oemm.RawStorage;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;

public class MesherTest
{
	private static final String dir = System.getProperty("test.dir", "test");

	private void checkNumberOfTriangles(String outputDir, int target, double delta)
	{
		int [] res = MeshReader.getInfos(outputDir);
		assertTrue("Number of triangles out of range: "+res[1], res[1] >= target*(1.0-delta) && res[1] <= target*(1.0+delta));
	}

	private void checkMeshQuality(String outputDir, double minAngleDeg, double maxEdgeLength)
	{
		Mesh mesh = new Mesh(new MeshTraitsBuilder());
		try
		{
			MeshReader.readObject3D(mesh, outputDir);
		}
		catch (IOException ex)
		{
			throw new RuntimeException();
		}

		QualityProcedure qprocAngle = new MinAngleFace();
		QualityFloat dataAngle = new QualityFloat(mesh.getTriangles().size());
		dataAngle.setQualityProcedure(qprocAngle);

		QualityProcedure qprocEdgeLength = new MaxLengthFace();
		QualityFloat dataEdgeLength = new QualityFloat(mesh.getTriangles().size());
		dataEdgeLength.setQualityProcedure(qprocEdgeLength);

		for (Triangle f: mesh.getTriangles())
		{
			if (!f.isWritable())
				continue;
			dataAngle.compute(f);
			dataEdgeLength.compute(f);
		}
		dataAngle.finish();
		dataEdgeLength.finish();

		double resAngle = dataAngle.getValueByPercent(0.0) * 180.0 / 3.14159265358979323844;
		assertTrue("Min angle too low: "+resAngle, minAngleDeg < resAngle);
		double resEdgeLength = dataEdgeLength.getValueByPercent(1.0);
		assertTrue("Max edge length too large: "+resEdgeLength, maxEdgeLength > resEdgeLength);
	}

	public static class ComputeTriangleQuality implements RawStorage.SoupReaderInterface
	{
		private final Vertex [] n = new Vertex[3];
		private final Mesh mesh = new Mesh(new MeshTraitsBuilder());
		private final QualityFloat [] data;

		public ComputeTriangleQuality(QualityFloat [] qdata)
		{
			data = qdata;
		}
		@Override
		public void processVertex(int i, double [] xyz)
		{
			n[i] = mesh.createVertex(xyz[0], xyz[1], xyz[2]);
		}
		@Override
		public void processTriangle(int group)
		{
			Triangle t = mesh.createTriangle(n);
			for (QualityFloat qf: data)
				qf.compute(t);
		}
	}

	private void checkLargeMeshQuality(String outputDir, double minAngleDeg, double maxEdgeLength)
	{
		MMesh1D mesh1d =  MMesh1DReader.readObject(outputDir);
		// Create a outputDir/soup file
		MeshToSoupConvert.meshToSoup(outputDir, mesh1d.getGeometry());
		Mesh mesh = new Mesh(new MeshTraitsBuilder());
		QualityProcedure qprocAngle = new MinAngleFace();
		QualityFloat dataAngle = new QualityFloat(100000);
		dataAngle.setQualityProcedure(qprocAngle);

		QualityProcedure qprocEdgeLength = new MaxLengthFace();
		QualityFloat dataEdgeLength = new QualityFloat(100000);
		dataEdgeLength.setQualityProcedure(qprocEdgeLength);

		// Read triangle soup
		ComputeTriangleQuality ctq = new ComputeTriangleQuality(new QualityFloat[] { dataAngle, dataEdgeLength });
		RawStorage.readSoup(outputDir+java.io.File.separator+"soup", ctq);

		dataAngle.finish();
		dataEdgeLength.finish();

		double resAngle = dataAngle.getValueByPercent(0.0) * 180.0 / 3.14159265358979323844;
		assertTrue("Min angle too low: "+resAngle, minAngleDeg < resAngle);
		double resEdgeLength = dataEdgeLength.getValueByPercent(1.0);
		assertTrue("Max edge length too large: "+resEdgeLength, maxEdgeLength > resEdgeLength);
	}

	@Test public void sphere0_05()
	{
		String geoFile = dir + File.separator + "input" + File.separator + "sphere.brep";
		String outDir = dir + File.separator + "output" + File.separator + "test-sphere";
		Mesher.main(new String[] {geoFile, outDir, "0.05", "0.0"});
		checkNumberOfTriangles(outDir, 10000, 0.1);
		checkMeshQuality(outDir, 10.0, 0.2);
	}

	@Test public void sphere0_01()
	{
		String geoFile = dir + File.separator + "input" + File.separator + "sphere.brep";
		String outDir = dir + File.separator + "output" + File.separator + "test-sphere";
		Mesher.main(new String[] {geoFile, outDir, "0.01", "0.0"});
		checkNumberOfTriangles(outDir, 250000, 0.1);
		checkLargeMeshQuality(outDir, 10.0, 0.1);
	}
}
