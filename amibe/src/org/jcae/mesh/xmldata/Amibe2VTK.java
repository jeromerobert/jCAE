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
 * (C) Copyright 2007, by EADS
 */

package org.jcae.mesh.xmldata;

import java.io.*;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Convert an Amibe mesh to a VTK file.
 * Output file extension should be <cite>.vtp</cite>.
 * The documentation of the file format may be found here:
 * <a href="http://www.vtk.org/pdf/file-formats.pdf">
 * http://www.vtk.org/pdf/file-formats.pdf</a>
 * @todo output one VTK piece by mesh group, support VTK parellel files.
 * @author Jerome Robert
 */
public class Amibe2VTK
{
	/**
	 * A main method for debugging
	 * @param args
	 */
	public static void main(String[] args)
	{		
		try
		{			
			PrintStream p=new PrintStream(new BufferedOutputStream(new FileOutputStream(
				"/tmp/test.vtp")));
			//new Amibe2VTK(new File("/home/jerome/OCCShapeGal/amibe1.dir/")).write(p);
			new Amibe2VTK(new File("/home/jerome/JCAEProject/amibe9.dir")).write(p);
			
			p.close();
						
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	private File directory;
	private Document document;
	
	/** @param directory The directory which contain the jcae3d file */
	public Amibe2VTK(File directory)
	{
		this.directory=directory;
	}
	
	private File getNodeFile()
	{
		Element xmlNodes = (Element) document.getElementsByTagName(
			"nodes").item(0);
		String a=((Element)xmlNodes.getElementsByTagName("file").item(0)).getAttribute("location");
		return new File(directory, a);
	}
	
	private File getTriaFile()
	{
		Element xmlNodes = (Element) document.getElementsByTagName(
			"triangles").item(0);
		Node fn = xmlNodes.getElementsByTagName("file").item(0);
		String a=((Element)fn).getAttribute("location");
		return new File(directory, a);
	}	
		
	public void write(OutputStream out)
		throws ParserConfigurationException, SAXException, IOException
	{
		document=XMLHelper.parseXML(new File(directory,"jcae3d"));

		PrintStream os=new PrintStream(out);
		File nodeFile=getNodeFile();
		File triaFile=getTriaFile();
		long nbp=getNodeFile().length()/8/3;
		long nbt=getTriaFile().length()/4/3;
		writeHeader(os, nbp, nbt);
		os.flush();
		DataOutputStream dos=new DataOutputStream(new BufferedOutputStream(out));
		writeNode(dos, nodeFile, nbp);
		writeTriangles(dos, triaFile, nbt);
		dos.flush();
		os.println("</AppendedData></VTKFile>");
		os.flush();
	}
	
	private void writeTriangles(DataOutputStream dos, File triaFile, long nbt)
		throws IOException
	{
		dos.writeInt((int) nbt*4*3);
		DataInputStream in = new DataInputStream(
			new BufferedInputStream(new FileInputStream(triaFile)));
		
		for(int i=0; i<nbt*3; i++)
			dos.writeInt(in.readInt());
		dos.writeInt((int) nbt*4);
		for(int i=1; i<=nbt; i++)
			dos.writeInt(3*i);
	}

	private void writeNode(DataOutputStream dos, File nodeFile, long nbp)
		throws IOException
	{
		dos.writeInt((int) nbp*8*3);
		DataInputStream in = new DataInputStream(
			new BufferedInputStream(new FileInputStream(nodeFile)));
		
		for(int i=0; i<nbp*3; i++)
			dos.writeDouble(in.readDouble());
	}

	private void writeHeader(PrintStream out, long nbp, long nbt)
	{
		out.println("<VTKFile type=\"PolyData\" version=\"0.1\" byte_order=\"BigEndian\">");
		out.println("<PolyData>");
		out.println("<Piece NumberOfPoints=\""+nbp+"\" NumberOfPolys=\""+nbt+"\">");
		out.println("<Points><DataArray type=\"Float64\" NumberOfComponents=\"3\" "+
			"format=\"appended\" offset=\"0\"/></Points>");
		
		int offset=(int) (4+(nbp*8*3));
		out.println("<Polys><DataArray type=\"Int32\" Name=\"connectivity\""+
			" format=\"appended\" offset=\""+offset+"\"/>");
		offset+=4+nbt*4*3;
		out.println("<DataArray type=\"Int32\" Name=\"offsets\" format=\"appended\"" +
			" offset=\""+offset+"\"/></Polys>");
		out.println("</Piece></PolyData>");
		out.print("<AppendedData encoding=\"raw\"> _");
	}
}
