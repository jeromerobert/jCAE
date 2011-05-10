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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.mesh.xmldata;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.mesh.xmldata.AmibeReader.SubMesh;
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
			Amibe2VTK a = new Amibe2VTK(new File("/home/jerome/JCAEProject/amibe10.dir"));
			a.setDummyData(true);
			a.write(p);
			
			p.close();
						
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	private final File directory;
	private boolean dummyData;

	public Amibe2VTK(String directory)
	{
		this.directory=new File(directory);
	}
	
	/** @param directory The directory which contain 3d files */
	public Amibe2VTK(File directory)
	{
		this.directory=directory;
	}

	private long computeNumberOfTriangle(IntFileReader triaFile) throws IOException
	{
		long nbt = triaFile.size()/3;
		long toReturn = 0;
		for(int i=0; i<nbt; i++)
		{
			if(triaFile.get()>=0)
				toReturn++;
			triaFile.get();
			triaFile.get();
		}
		triaFile.close();
		return toReturn;
	}

	public void write(String fileName) throws IOException, SAXException, ParserConfigurationException
	{
		FileOutputStream out= new FileOutputStream(fileName);
		write(out);
		out.close();
	}
	
	/**
	 * Write the VTK file
	 * @param out The stream to write on 
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public final void write(OutputStream out)
		throws ParserConfigurationException, SAXException, IOException
	{
		AmibeReader.Dim3 amibeReader = new AmibeReader.Dim3(directory.getPath());
		SubMesh sm = amibeReader.getSubmeshes().get(0);
		PrintStream os=new PrintStream(out);
		long nbp=sm.getNumberOfNodes();
		long nbt=computeNumberOfTriangle(sm.getTriangles());
		writeHeader(os, nbp, nbt, sm.getNumberOfBeams());
		os.flush();
		DataOutputStream dos=new DataOutputStream(new BufferedOutputStream(out));
		writeNode(dos, sm.getNodes(), nbp);
		writeTriangles(dos, sm.getTriangles(), nbt);
		writeBeams(dos, sm.getBeams(), sm.getNumberOfBeams());
		if(dummyData)
			writeData(dos, nbt);
		dos.flush();
		os.println("</AppendedData></VTKFile>");
		os.flush();
	}
	
	/**
	 * write the triangle connectivity
	 * @param dos the stream to write on
	 * @param triaFile the amibe triangle file
	 * @param nbt the number of triangles
	 * @throws IOException
	 */
	private void writeTriangles(DataOutputStream dos, IntFileReader triaFile, long nbt)
		throws IOException
	{
		//Write the size of the array in octets
		dos.writeInt((int) nbt*4*3);
		
		//Write the connectivity array
		for(int i=0; i<nbt*3; i++)
		{
			int v = triaFile.get();
			if(v>=0)
				dos.writeInt(v);
		}
		
		//Write the size of the array in octets
		dos.writeInt((int) nbt*4);
		
		//Write the offset of each cells (in our case triangles) in the
		//connectivity array
		for(int i=1; i<=nbt; i++)
			dos.writeInt(3*i);

		triaFile.close();
	}

	/**
	 * write the triangle connectivity
	 * @param dos the stream to write on
	 * @param triaFile the amibe triangle file
	 * @param nbt the number of triangles
	 * @throws IOException
	 */
	private void writeBeams(DataOutputStream dos, IntFileReader beamFile, long numberOfBeams)
		throws IOException
	{
		//Write the size of the array in octets
		dos.writeInt((int) numberOfBeams*4*2);

		//Write the connectivity array
		for(int i=0; i<numberOfBeams*2; i++)
			dos.writeInt(beamFile.get());

		//Write the size of the array in octets
		dos.writeInt((int) numberOfBeams*4);

		//Write the offset of each cells (in our case beams) in the
		//connectivity array
		for(int i=1; i<=numberOfBeams; i++)
			dos.writeInt(2*i);

		beamFile.close();
	}

	/**
	 * Write the nodes of the mesh
	 * @param dos the stream to write on
	 * @param nodeFile the amibe node file
	 * @param nbp the number of nodes
	 * @throws IOException
	 */	private void writeNode(DataOutputStream dos, DoubleFileReader nodeFile, long nbp)
		throws IOException
	{
		//Write the size of the array in octets
		dos.writeInt((int) nbp*8*3);
		for(int i=0; i<nbp*3; i++)
			dos.writeDouble(nodeFile.get());

		nodeFile.close();
	}
	 
	/**
	 * write dummy data associated to the triangle
	 * @param dos the stream to write on
	 * @param nbt the number of triangles
	 * @throws IOException
	 */
	private void writeData(DataOutputStream dos, long nbt)
		throws IOException
	{
		//Write the size of the array in octets
		dos.writeInt((int) nbt*8);		
		for(int i=0; i<nbt; i++)
			dos.writeDouble(i);
		
		dos.writeInt((int) nbt*8);
		for(int i=0; i<nbt; i++)
			dos.writeDouble((double)i*i);

		dos.writeInt((int) nbt*8*3);
		for(int i=0; i<nbt; i++)
		{
			dos.writeDouble(i);
			dos.writeDouble(i);
			dos.writeDouble(i);
		}
	}	 

	/**
	 * Write the header of the file (XML)
	 * @param out the stream to write on
	 * @param numberOfNodes the number of nodes
	 * @param numberOfTriangles the number of triangles
	 */
	private void writeHeader(PrintStream out, long numberOfNodes,
		long numberOfTriangles, long numberOfLines)
	{
		//This is Java so we write in big endian		
		out.println("<VTKFile type=\"PolyData\" version=\"0.1\" byte_order=\"BigEndian\">");
		out.println("<PolyData>");
		
		//Everything in one piece
		//TODO write one piece by group
		out.println("<Piece NumberOfPoints=\""+numberOfNodes+
			"\" NumberOfPolys=\""+numberOfTriangles+
			"\" NumberOfLines=\""+numberOfLines+"\">");
		
		out.println("<Points><DataArray type=\"Float64\" NumberOfComponents=\"3\" "+
			"format=\"appended\" offset=\"0\"/></Points>");		
		long offset=4+(numberOfNodes*8*3);		

		//polys
		out.println("<Polys><DataArray type=\"Int32\" Name=\"connectivity\""+
			" format=\"appended\" offset=\""+offset+"\"/>");
		offset+=4+numberOfTriangles*4*3;
		
		out.println("<DataArray type=\"Int32\" Name=\"offsets\" format=\"appended\"" +
			" offset=\""+offset+"\"/></Polys>");		
		offset+=4+numberOfTriangles*4;

		//lines
		out.println("<Lines><DataArray type=\"Int32\" Name=\"connectivity\""+
			" format=\"appended\" offset=\""+offset+"\"/>");
		offset+=4+numberOfLines*4*2;

		out.println("<DataArray type=\"Int32\" Name=\"offsets\" format=\"appended\"" +
			" offset=\""+offset+"\"/></Lines>");
		offset+=4+numberOfLines*4;

		if(dummyData)
		{
			out.println("<CellData Scalars=\"Dummy\">");
			out.println("\t<DataArray type=\"Float64\" Name=\"Dummy\" format=\"appended\" offset=\""
				+offset+"\"/>");
			offset += 4+numberOfTriangles * 8;

			out.println("\t<DataArray type=\"Float64\" Name=\"Dummy x Dummy\" format=\"appended\" offset=\""
				+offset+"\"/>");
			//always keep track of offset in case we want to add thins to the
			//file
			offset += 4+numberOfTriangles * 8;
			
			out.println("\t<DataArray type=\"Float64\" Name=\"Dummy vector\" NumberOfComponents=\"3\""+
				" format=\"appended\" offset=\""+offset+"\"/>");
			//always keep track of offset in case we want to add thins to the
			//file
			offset += 4+numberOfTriangles*8*3;			
			out.println("</CellData>");
		}
		
		out.println("</Piece></PolyData>");
		out.print("<AppendedData encoding=\"raw\"> _");
	}

	public boolean isDummyData()
	{
		return dummyData;
	}

	/**
	 * Write data cell associated to triangles
	 * It's a scalar double value which is the ID of the triangle.
	 * It won't help you much, it's just to have the code somewhere?
	 */
	final void setDummyData(boolean dummyData)
	{
		this.dummyData = dummyData;
	}
}
