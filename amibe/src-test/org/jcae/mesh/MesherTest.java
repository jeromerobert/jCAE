/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2007,2008,2009, by EADS France

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
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.oemm.RawStorage;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.FileHandler;
import java.util.logging.XMLFormatter;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

public class MesherTest
{
	private Logger root;
	private static final String dir = System.getProperty("test.dir", "test");
	private static int counter = 0;
	private static FileHandler app = null;
	private static final XMLFormatter xmlLayout = new XMLFormatter();
	private static File logFile = null;
	private static EntityResolver myEntityResolver = new FakeEntityResolver();
	// Hopefully test timing depends linearly on CPU power,
	private static final long timerScale = Long.parseLong(System.getProperty("org.jcae.mesh.timerScale", "1000"));

	// This EntityResolver avoids FileNotFoundException when parsing XML output since logger.dtd is unavailable.
	public static class FakeEntityResolver implements EntityResolver
	{
		public InputSource resolveEntity(String publicId, String systemId)
		{
			return new InputSource(new java.io.ByteArrayInputStream(new byte[0]));
		}
	}

	private void checkNumberOfTriangles(String outputDir, int target, double delta)
	{
		int [] res = MeshReader.getInfos(outputDir);
		assertTrue("Number of triangles out of range ["+(target*(1.0-delta))+" - "+(target*(1.0+delta))+"]: "+res[1], res[1] >= target*(1.0-delta) && res[1] <= target*(1.0+delta));
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
		assertTrue("Min angle too low; expected: "+minAngleDeg+" found: "+resAngle, minAngleDeg < resAngle);
		double resEdgeLength = dataEdgeLength.getValueByPercent(1.0);
		assertTrue("Max edge length too large; expected: "+maxEdgeLength+" found: "+resEdgeLength, maxEdgeLength > resEdgeLength);
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
		public void processVertex(int i, double [] xyz)
		{
			n[i] = mesh.createVertex(xyz[0], xyz[1], xyz[2]);
		}
		public void processTriangle(int group)
		{
			Triangle t = mesh.createTriangle(n);
			for (QualityFloat qf: data)
				qf.compute(t);
		}
	}

	private void checkLargeMeshQuality(String outputDir, double minAngleDeg, double maxEdgeLength)
	{
		String soupFile = outputDir+java.io.File.separator+"soup";
		if (!(new File(soupFile)).exists())
		{
			// Create a outputDir/soup file
			MMesh1D mesh1d =  MMesh1DReader.readObject(outputDir);
			MeshToSoupConvert.meshToSoup(outputDir, mesh1d.getGeometry());
		}

		QualityProcedure qprocAngle = new MinAngleFace();
		QualityFloat dataAngle = new QualityFloat(100000);
		dataAngle.setQualityProcedure(qprocAngle);

		QualityProcedure qprocEdgeLength = new MaxLengthFace();
		QualityFloat dataEdgeLength = new QualityFloat(100000);
		dataEdgeLength.setQualityProcedure(qprocEdgeLength);

		// Read triangle soup
		ComputeTriangleQuality ctq = new ComputeTriangleQuality(new QualityFloat[] { dataAngle, dataEdgeLength });
		RawStorage.readSoup(soupFile, ctq);

		dataAngle.finish();
		dataEdgeLength.finish();

		double resAngle = dataAngle.getValueByPercent(0.0) * 180.0 / 3.14159265358979323844;
		assertTrue("Min angle too low; expected: "+minAngleDeg+" found: "+resAngle, minAngleDeg < resAngle);
		double resEdgeLength = dataEdgeLength.getValueByPercent(1.0);
		assertTrue("Max edge length too large; expected: "+maxEdgeLength+" found: "+resEdgeLength, maxEdgeLength > resEdgeLength);
	}

	private void startLogger()
	{
		LogManager.getLogManager().reset();
		root = Logger.getLogger("");

		counter++;
		try
		{
			File logDir = new File(dir, "logs");
			logDir.mkdirs();
			if(!logDir.exists() || !logDir.isDirectory())
				throw new RuntimeException("Unable to create directory "+logDir.getPath());
			logFile = new File(logDir, "test."+counter+".xml");
			app = new FileHandler(logFile.getPath(), false);
			app.setFormatter(xmlLayout);
			app.setLevel(Level.INFO);
			root.addHandler(app);
		}
		catch (IOException ex)
		{
			throw new RuntimeException();
		}
	}

	private Document stopLogger()
	{
		app.close();
		app = null;

		DocumentBuilderFactory dbf = null;
		try {
			dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringElementContentWhitespace(true);
			dbf.setValidating(false);
		} catch(FactoryConfigurationError fce) {
			throw fce;
		}

		try {
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			docBuilder.setEntityResolver(myEntityResolver);
			return docBuilder.parse(logFile);
		} catch (ParserConfigurationException ex) {
			ex.printStackTrace();
			throw new RuntimeException();
		} catch (SAXException ex) {
			ex.printStackTrace();
			throw new RuntimeException();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			throw new RuntimeException();
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new RuntimeException();
		}
	}

	private long getMesherRuntimeMillis(Document doc)
	{
		XPath xpath = XPathFactory.newInstance().newXPath();
		try
		{
			XPathExpression xpathTimestamp = xpath.compile("//millis/text()");
			// Find first <message> containing "Meshing face" message
			NodeList events = (NodeList) xpath.evaluate("//message/text()[contains(string(), 'Meshing face')]/ancestor::record", doc, XPathConstants.NODESET);
			long t1 = Long.parseLong(xpathTimestamp.evaluate(events.item(0)));
			// <record> element after last 'Meshing face' message
			long t2 = Long.parseLong(xpathTimestamp.evaluate(events.item(events.getLength() - 1).getNextSibling()));
			return t2 - t1;
		}
		catch (XPathExpressionException ex)
		{
			throw new RuntimeException(ex.getCause());
		}
	}

	// A timer for classes with a main method which calls logger.info() at start and end.
	private void checkMainRuntimeMillis(Document doc, String klass, long seconds)
	{
		XPath xpath = XPathFactory.newInstance().newXPath();
		try
		{
			NodeList events = (NodeList) xpath.evaluate("//record[class='"+klass+"']/millis/text()", doc, XPathConstants.NODESET);
			assertTrue(events.getLength() == 2);
			long t1 = Long.parseLong(events.item(0).getNodeValue());
			long t2 = Long.parseLong(events.item(events.getLength() - 1).getNodeValue());
			long time = t2 - t1;
			assertTrue(""+klass+" too long: max time (ms): "+(seconds * timerScale)+" Effective time (ms): "+time, time < seconds * timerScale);
		}
		catch (XPathExpressionException ex)
		{
			throw new RuntimeException(ex.getCause());
		}
	}

	private static final String getGeometryFile(String type)
	{
		return dir + File.separator + "input" + File.separator + type +".brep";
	}

	private static final String getOutputDirectory(String type, int cnt)
	{
		return dir + File.separator + "output" + File.separator + "test-"+type+"."+cnt;
	}

	private String runSingleTest(String type, double length, int nrTriangles, double minAngleDeg)
	{
		startLogger();
		root.info("Running "+type+" test with length: "+length);
		String geoFile = getGeometryFile(type);
		String outDir = getOutputDirectory(type, counter);
		org.jcae.mesh.Mesher.main(new String[] {geoFile, outDir, ""+length, "0.0"});
		if (nrTriangles > 0)
			checkNumberOfTriangles(outDir, nrTriangles, 0.1);
		if (minAngleDeg > 0)
		{
			int [] res = MeshReader.getInfos(outDir);
			if (res[1] < 100000)
				checkMeshQuality(outDir, minAngleDeg, 4.0*length);
			else
				checkLargeMeshQuality(outDir, minAngleDeg, 4.0*length);
		}
		return outDir;
	}

	@BeforeClass
	public static void checkEnv()
	{
		if (!Boolean.getBoolean("run.test.large"))
			throw new RuntimeException("MesherTest takes too much time, re-run with -Drun.test.large=true if you really want to run this file");
	}

	private void runSingleTestTimer(String type, long seconds)
	{
		Document doc = stopLogger();
		long time = getMesherRuntimeMillis(doc);
		assertTrue("Mesher took too long: max time (ms): "+(seconds * timerScale)+" Effective time (ms): "+time, time < seconds * timerScale);
	}

	@Test public void sphere_0_05()
	{
		runSingleTest("sphere", 0.05, 10000, 10.0);
	}

	@Test public void timer_sphere_0_05()
	{
		runSingleTestTimer("sphere", 3L);
	}

	@Test public void sphere_0_01()
	{
		runSingleTest("sphere", 0.01, 250000, 10.0);
	}

	@Test public void timer_sphere_0_01()
	{
		runSingleTestTimer("sphere", 50L);
	}

	@Test public void sphere_0_005()
	{
		runSingleTest("sphere", 0.005, 1000000, 10.0);
	}

	@Test public void timer_sphere_0_005()
	{
		runSingleTestTimer("sphere", 200L);
	}

	// Results should be similar with sphere1000
	@Test public void sphere1000_50()
	{
		runSingleTest("sphere1000", 50.0, 10000, 10.0);
	}

	@Test public void timer_sphere1000_50()
	{
		runSingleTestTimer("sphere1000", 3L);
	}

	@Test public void sphere1000_10()
	{
		runSingleTest("sphere1000", 10.0, 250000, 10.0);
	}

	@Test public void timer_sphere1000_10()
	{
		runSingleTestTimer("sphere1000", 50L);
	}

	@Test public void sphere1000_5()
	{
		runSingleTest("sphere1000", 5, 1000000, 10.0);
	}
	
	@Test public void timer_sphere1000_5()
	{
		runSingleTestTimer("sphere1000", 200L);
	}
	
	@Test public void cylinder_0_05()
	{
		runSingleTest("cylinder", 0.05, 30000, 20.0);
	}

	@Test public void timer_cylinder_0_05()
	{
		runSingleTestTimer("cylinder", 3L);
	}

	@Test public void cylinder_0_01()
	{
		runSingleTest("cylinder", 0.01, 800000, 20.0);
	}

	@Test public void timer_cylinder_0_01()
	{
		runSingleTestTimer("cylinder", 80L);
	}

	// cylinder1000 is different, radius is multiplied by 1000 but height by 600
	@Test public void cylinder1000_50()
	{
		runSingleTest("cylinder1000", 50.0, 3000, 20.0);
	}

	@Test public void timer_cylinder1000_50()
	{
		runSingleTestTimer("cylinder1000", 1L);
	}

	@Test public void cylinder1000_10()
	{
		runSingleTest("cylinder1000", 10.0, 80000, 20.0);
	}

	@Test public void timer_cylinder1000_10()
	{
		runSingleTestTimer("cylinder1000", 10L);
	}

	@Test public void cylinder1000_5()
	{
		runSingleTest("cylinder1000", 5.0, 320000, 20.0);
	}

	@Test public void timer_cylinder1000_5()
	{
		runSingleTestTimer("cylinder1000", 45L);
	}

	@Test public void cone_0_01()
	{
		runSingleTest("cone", 0.01, 150000, 10.0);
	}

	@Test public void timer_cone_0_01()
	{
		runSingleTestTimer("cone", 20L);
	}

	@Test public void torus_0_01()
	{
		runSingleTest("torus", 0.01, 250000, 20.0);
	}

	@Test public void timer_torus_0_01()
	{
		runSingleTestTimer("torus", 30L);
	}

	@Test public void shellHole()
	{
		runSingleTest("shell_hole", 0.5, 250000, 20.0);
	}

	@Test public void timer_shellHole()
	{
		runSingleTestTimer("shell_hole", 30L);
	}

	@Test public void oemm()
	{
		String geoFile = getGeometryFile("15_cylinder_head");
		if (!(new File(geoFile).exists()))
		{
			throw new RuntimeException("Missing brep file; you must download, uncompress http://www.opencascade.org/ex/att/15_cylinder_head.brep.gz and copy it into "+geoFile);
		}
		System.setProperty("org.jcae.mesh.Mesher.triangleSoup", "true");
		String coarseDir = runSingleTest("15_cylinder_head", 5.0, 37000 , 0.0);
		runSingleTestTimer("15_cylinder_head", 30L);
		String fineDir = runSingleTest("15_cylinder_head", 1.2, 460000 , 0.0);
		runSingleTestTimer("15_cylinder_head", 100L);
		System.setProperty("org.jcae.mesh.Mesher.triangleSoup", "false");

		startLogger();
		org.jcae.mesh.MeshOEMMIndex.main(new String[] {fineDir, fineDir+"-oemm", "4", "10000", geoFile});
		org.jcae.mesh.MeshOEMMPopulate.main(new String[] {fineDir+"-oemm", coarseDir+"-oemm", coarseDir+java.io.File.separator+"soup"});
		Document doc = stopLogger();
		checkMainRuntimeMillis(doc, "org.jcae.mesh.MeshOEMMIndex", 40L);
		checkMainRuntimeMillis(doc, "org.jcae.mesh.MeshOEMMPopulate", 5L);
	}

}
