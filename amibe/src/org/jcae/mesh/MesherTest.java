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
import java.io.StringWriter;
import java.io.Reader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.SequenceInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.FileAppender;
import org.apache.log4j.xml.XMLLayout;
import org.apache.log4j.xml.SAXErrorHandler;
import org.apache.log4j.xml.Log4jEntityResolver;
import org.apache.log4j.LogManager;

public class MesherTest
{
	private Logger root;
	private static final String dir = System.getProperty("test.dir", "test");
	private static int counter = 0;
	private static FileAppender app = null;
	// Hopefully test timing depends linearly on CPU power,
	private static final long timerScale = Long.parseLong(System.getProperty("org.jcae.mesh.timerScale", "1000"));

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
		assertTrue("Min angle too low; expected: "+minAngleDeg+" found: "+resAngle, minAngleDeg < resAngle);
		double resEdgeLength = dataEdgeLength.getValueByPercent(1.0);
		assertTrue("Max edge length too large; expected: "+maxEdgeLength+" found: "+resEdgeLength, maxEdgeLength > resEdgeLength);
	}

	private void startLogger()
	{
		LogManager.resetConfiguration();
		root = LogManager.getRootLogger();
		root.removeAllAppenders();

		XMLLayout xmlLayout = new XMLLayout();
		xmlLayout.setLocationInfo(true);
		root.setLevel(Level.INFO);
		counter++;
		try
		{
			File logDir = new File(dir, "logs");
			logDir.mkdirs();
			if(!logDir.exists() || !logDir.isDirectory())
				throw new RuntimeException("Unable to create directory "+logDir.getPath());
			File logFile = new File(logDir, "test."+counter+".xml");
			app = new FileAppender(xmlLayout, logFile.getPath(), false);
			root.addAppender(app);
		}
		catch (IOException ex)
		{
			throw new RuntimeException();
		}
	}

	private Document stopLogger()
	{
		String logFile = app.getFile();
		app.close();
		app = null;

		DocumentBuilderFactory dbf = null;
		try {
			dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringElementContentWhitespace(true);
		} catch(FactoryConfigurationError fce) {
			throw fce;
		}

		String xmlProlog =
		    "<?xml version=\"1.0\" ?>\n" +
		    "<!DOCTYPE log4j:eventSet SYSTEM \"log4j.dtd\">\n" +
		    "<log4j:eventSet version=\"1.2\" xmlns:log4j=\"http://jakarta.apache.org/log4j/\">\n";
		String xmlEpilog = "</log4j:eventSet>";

		ArrayList<InputStream> inputStreams = new ArrayList<InputStream>();
		inputStreams.add(new ByteArrayInputStream(xmlProlog.getBytes()));
		try {
			inputStreams.add(new FileInputStream(logFile));
		} catch (FileNotFoundException ex)
		{
			throw new RuntimeException();
		}
		inputStreams.add(new ByteArrayInputStream(xmlEpilog.getBytes()));
		Reader sr = new InputStreamReader(new SequenceInputStream(Collections.enumeration(inputStreams)));

		try {
			dbf.setValidating(true);
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			docBuilder.setErrorHandler(new SAXErrorHandler());
			docBuilder.setEntityResolver(new Log4jEntityResolver());
			return docBuilder.parse(new InputSource(sr));
		} catch (ParserConfigurationException ex) {
			ex.printStackTrace();
			throw new RuntimeException();
		} catch (SAXException ex) {
			ex.printStackTrace();
			throw new RuntimeException();
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new RuntimeException();
		}
	}

	private String DomToString(Document document)
	{
		try
		{
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Source source = new DOMSource(document);
			StringWriter sw = new StringWriter();
			Result result = new StreamResult(sw);
			Transformer xformer = transformerFactory.newTransformer();
			xformer.transform(source, result);
			return sw.toString();
		}
		catch (Exception ex)
		{
			throw new RuntimeException();
		}
	}

	private long getMesherRuntimeMillis(Document doc)
	{
		XPath xpath = XPathFactory.newInstance().newXPath();
		try
		{
			XPathExpression xpathTimestamp = xpath.compile("@timestamp");
			// Find first <log4j:message> containing "Meshing face" message
			Node content = (Node) xpath.evaluate("//message/text()[contains(string(), 'Meshing face')]", doc, XPathConstants.NODE);
			// Enclosing <log4j:event> element
			Node event = content.getParentNode().getParentNode();
			long t1 = Long.parseLong(xpathTimestamp.evaluate(event));
			// Next <log4j:event> element
			Node s = event.getNextSibling();
			long t2 = Long.parseLong(xpathTimestamp.evaluate(s));
			return t2 - t1;
		}
		catch (XPathExpressionException ex)
		{
			throw new RuntimeException();
		}
	}

	private void runSingleTest(String type, double length, int nrTriangles, double minAngleDeg)
	{
		startLogger();
		root.info("Running "+type+" test with length: "+length);
		String geoFile = dir + File.separator + "input" + File.separator + type +".brep";
		String outDir = dir + File.separator + "output" + File.separator + "test-"+type+"."+counter;
		Mesher.main(new String[] {geoFile, outDir, ""+length, "0.0"});
		checkNumberOfTriangles(outDir, nrTriangles, 0.1);
		if (nrTriangles < 100000)
			checkMeshQuality(outDir, minAngleDeg, 4.0*length);
		else
			checkLargeMeshQuality(outDir, minAngleDeg, 4.0*length);
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
		runSingleTestTimer("sphere", 2L);
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
		runSingleTestTimer("sphere1000", 2L);
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

}
