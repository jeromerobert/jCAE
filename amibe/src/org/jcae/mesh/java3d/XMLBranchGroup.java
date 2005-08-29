/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>
 
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

package org.jcae.mesh.java3d;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Appearance;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.TriangleArray;
import javax.media.j3d.IndexedLineArray;
import javax.media.j3d.Geometry;
import javax.media.j3d.Shape3D;
import javax.media.j3d.LineAttributes;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;
import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.*;
import org.jcae.mesh.xmldata.*;
import org.apache.log4j.*;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.Stripifier;

/** Allow a XML jcae mesh file to be display with Java3D. It create a
 * javax.media.j3D.BranchGroup from a jcae mesh XML file.
 * @author Jerome Robert
 * @todo Handle multiple submesh.
 */

public class XMLBranchGroup
{
	private static Logger logger=Logger.getLogger(XMLBranchGroup.class);
	private final static float absOffsetStep = Float.parseFloat(System.getProperty("javax.media.j3d.zFactorAbs", "20.0f"));
	private final static float relOffsetStep = Float.parseFloat(System.getProperty("javax.media.j3d.zFactorRel", "2.0f"));
	
	class AppearanceBlackFace extends Appearance
	{
		public AppearanceBlackFace()
		{
			PolygonAttributes pa=new PolygonAttributes(PolygonAttributes.POLYGON_FILL,
				PolygonAttributes.CULL_NONE, 2.0f*absOffsetStep, false, relOffsetStep);
			ColoringAttributes ca=
			new ColoringAttributes(0,0,0,ColoringAttributes.SHADE_FLAT);
			setPolygonAttributes(pa);
			setColoringAttributes(ca);
		}
	}
	
	class AppearanceWireFrame extends Appearance
	{
		public AppearanceWireFrame()
		{
			this(0, 0, 1);
		}

		public AppearanceWireFrame(float r, float g, float b)
		{
			PolygonAttributes pa=new PolygonAttributes(PolygonAttributes.POLYGON_LINE,
				PolygonAttributes.CULL_NONE, absOffsetStep);
			ColoringAttributes ca=new ColoringAttributes(r,g,b,ColoringAttributes.SHADE_GOURAUD);
			setPolygonAttributes(pa);
			setColoringAttributes(ca);
		}
	}   
	
	class AppearanceLine extends Appearance
	{
		public AppearanceLine()
		{
			this(1f, 0f, 0f);
		}

		public AppearanceLine(float r, float g, float b)
		{
			LineAttributes pa=new LineAttributes(1,LineAttributes.PATTERN_SOLID,false);
			ColoringAttributes ca=new ColoringAttributes(r,g,b,ColoringAttributes.SHADE_FLAT);
			setLineAttributes(pa);
			setColoringAttributes(ca);
		}
	}   
	
	private File xmlFile;	
	Document document;
	
	public XMLBranchGroup(String xmlDir, String xmlFile)
	{
		this.xmlFile=new File(xmlDir, xmlFile);
	}
	
	public void parseXML() throws ParserConfigurationException, SAXException, IOException
	{
		document=XMLHelper.parseXML(xmlFile);
	}
	
	private float[] readFloat(String filename, int number) throws IOException
	{
		float[] result=new float[number];
		logger.debug("reading "+number+" double from "+filename);
		File file=new File(xmlFile.getParent(), filename);
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		
		for(int i=0; i<number; i++) 		
			result[i]=(float)in.readDouble();			
		
		return result;
	}
	
	private int[] readInteger(String filename, int number) throws IOException
	{
		int[] result=new int[number];
		logger.debug("reading "+number+" int from "+filename);
		File file=new File(xmlFile.getParent(), filename);
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		for(int i=0; i<number; i++)
		{
			result[i]=in.readInt();
		}
		return result;
	}
	
	public BranchGroup getBranchGroup(int i)
		throws TransformerException, IOException
	{
		Node subMesh = XPathAPI.selectSingleNode(document,
			"/jcae/mesh/submesh["+(i+1)+"]");
		return getBranchGroup(subMesh);
	}
	
	public BranchGroup getBranchGroup(String flag)
		throws TransformerException, IOException
	{
		Node subMesh = XPathAPI.selectSingleNode(document,
			"/jcae/mesh/submesh/flag[@value='"+flag+"']/..");
		return getBranchGroup(subMesh);
	}

	private BranchGroup getBranchGroup(Node subMesh)
		throws TransformerException, IOException
	{
		int dim = 3;
		Node dimDOM = XPathAPI.selectSingleNode(subMesh, "dimension/text()");
		if (null != dimDOM)
			dim = Integer.parseInt(dimDOM.getNodeValue());
		Node trianglesDOM = XPathAPI.selectSingleNode(subMesh, "triangles");
		Node nodesDOM = XPathAPI.selectSingleNode(subMesh, "nodes");
		BranchGroup branchGroup=new BranchGroup();
		Node flagDOM = XPathAPI.selectSingleNode(subMesh, "flag/@value");
		String flag=null;
		
		if(flagDOM!=null) flag=flagDOM.getNodeValue();
		
		logger.debug("Computing BranchGroup for a subMesh with flag="+flag);
		
		if(nodesDOM!=null)
		{
			String nodesFile = XPathAPI.selectSingleNode(nodesDOM,
				"file/@location").getNodeValue();
			
			int numberOfNodes = Integer.parseInt((XPathAPI.selectSingleNode(
				nodesDOM, "number/text()").getNodeValue()));
			
			float[] nodes=readFloat(nodesFile, numberOfNodes*dim);
			if (dim == 2)
			{
				float[] nodesCoord = new float[3*numberOfNodes];
				for (int i = 0; i < numberOfNodes; i++)
				{
					nodesCoord[3*i] = nodes[2*i];
					nodesCoord[3*i+1] = nodes[2*i+1];
					nodesCoord[3*i+2] = 0.0f;
				}
				nodes=nodesCoord;
			}
			if(trianglesDOM!=null)
			{
				Geometry geom = getGeomForTriangles(trianglesDOM, nodes);
				Shape3D shapeFill=new Shape3D(geom, new AppearanceBlackFace());
				shapeFill.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
				branchGroup.addChild(shapeFill);
				
				Shape3D shapeLine=new Shape3D(geom, new AppearanceWireFrame());
				shapeLine.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
				branchGroup.addChild(shapeLine);
			}
			
			if(flag != null && flag.equals("FreeEdges"))
			{
				Node beamsDOM = XPathAPI.selectSingleNode(subMesh, "beams");
				IndexedLineArray geom = getGeomForWires(beamsDOM, nodes);
				if (null != geom)
				{
					Shape3D shapeFreeEdges=new Shape3D(geom, new AppearanceLine());
					shapeFreeEdges.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
					branchGroup.addChild(shapeFreeEdges);
				}
			}
			if(flag != null && flag.equals("MultiEdges"))
			{
				Node beamsDOM = XPathAPI.selectSingleNode(subMesh, "beams");
				IndexedLineArray geom = getGeomForWires(beamsDOM, nodes);
				if (null != geom)
				{
					Shape3D shapeMultiEdges=new Shape3D(geom, new AppearanceLine(0.7f, 0f, 0.7f));
					shapeMultiEdges.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
					branchGroup.addChild(shapeMultiEdges);
				}
			}
		}
		branchGroup.setCapability(javax.media.j3d.Node.ALLOW_BOUNDS_READ);
		return branchGroup;		
	}
	
	private Geometry getGeomForTriangles(Node trianglesDOM, float[] nodes)
		throws TransformerException, IOException
	{
		String triasFile = XPathAPI.selectSingleNode(trianglesDOM,
			"file/@location").getNodeValue();

		int numberOfTrias = Integer.parseInt((XPathAPI.selectSingleNode(
			trianglesDOM, "number/text()").getNodeValue()));

		int[] trias=readInteger(triasFile, numberOfTrias*3);				
		IndexedTriangleArray geom = new IndexedTriangleArray(
			nodes.length/3,
			TriangleArray.COORDINATES,
			trias.length);
		geom.setCoordinateIndices(0, trias);
		geom.setCapability(IndexedTriangleArray.ALLOW_COUNT_READ);
		geom.setCapability(IndexedTriangleArray.ALLOW_FORMAT_READ);
		geom.setCapability(IndexedTriangleArray.ALLOW_REF_DATA_READ);
		geom.setCapability(IndexedTriangleArray.ALLOW_COORDINATE_INDEX_READ);
		trias=null;
		geom.setCoordinates(0,nodes);
		if(Boolean.getBoolean("org.jcae.mesh.java3d.stripify"))
		{
			GeometryInfo gi=new GeometryInfo(geom);			
			Stripifier s=new Stripifier(Stripifier.COLLECT_STATS );
			s.stripify(gi);
			logger.info(s.getStripifierStats());
			Geometry toReturn=gi.getGeometryArray();
			return toReturn;
		}
		else
		{
			return geom;
		}
	}
	
	private IndexedLineArray getGeomForWires(Node beamsDOM, float[] nodes)
		throws TransformerException, IOException
	{
		String triasFile = XPathAPI.selectSingleNode(beamsDOM,
			"file/@location").getNodeValue();

		int numberOfBeams = Integer.parseInt((XPathAPI.selectSingleNode(
			beamsDOM, "number/text()").getNodeValue()));
		if(numberOfBeams==0)
			return null;
		int[] beams=readInteger(triasFile, numberOfBeams*2);				
		IndexedLineArray geom = new IndexedLineArray(
			nodes.length/3,
			IndexedLineArray.COORDINATES|IndexedLineArray.BY_REFERENCE,
			beams.length);
		geom.setCoordinateIndices(0, beams);
		geom.setCapability(IndexedLineArray.ALLOW_COUNT_READ);
		geom.setCapability(IndexedLineArray.ALLOW_FORMAT_READ);
		geom.setCapability(IndexedLineArray.ALLOW_REF_DATA_READ);
		geom.setCapability(IndexedLineArray.ALLOW_COORDINATE_INDEX_READ);
		geom.setCoordRefFloat(nodes);		
		return geom;
	}	
}
