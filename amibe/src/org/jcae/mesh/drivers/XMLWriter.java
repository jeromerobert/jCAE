/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
	Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>
 
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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.drivers;

/**
 * @author  Jerome Robert
 */
import org.jcae.mesh.sd.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;

public class XMLWriter extends MeshWriter
{
	private static Logger logger=Logger.getLogger(XMLWriter.class);
	public XMLWriter(OutputStream out, MeshMesh mesh)
	{
		super(out,mesh);
	}
	
	public XMLWriter(OutputStream out)
	{
		super(out);
	}

	public void writeMesh() 
	{
		PrintWriter out=new PrintWriter(this);

		//entete xml
		out.println("<?xml version=\"1.0\" ?>");
		out.println("<ANSYSDOC xmlns=\"http://www.ansys.com/caedoc_ns\">");
		out.println("<HEADER>");
		out.println("   <NAME>Mesh</NAME>");
		out.println("	<DESCRIPTION></DESCRIPTION>");
		out.println("	<VERSION>6.0</VERSION>");
		out.println("	<OS>Linux</OS>");
		out.println("</HEADER>");

		//Discretisation utilisee
		out.println("<FEAMODEL>");
		out.println("	<TABLES>");
		out.println("		<ELEMENTTYPE ID = \"\" TOPOLOGY = \""+
			"\" ORDER = \"\" NATIVETYPE= \"\" CATEGORY = \"\"/>");
		out.println("	</TABLES>");

		//Description du maillage
		out.println("		<MESH ID = \"1\">\n");
		// nodes
		// faces
		out.println("			<GLOBAL>");
		out.println("				<NUMNODES>"+mesh.numberOfNodes()+"</NUMNODES>");
		out.println("				<NUMELEMENTS>"+mesh.numberOfFaces()+
			"</NUMELEMENTS>");
		out.println("			<GLOBAL>");

		Iterator itnode = mesh.getNodesIterator();
		int i = 1;
		while (itnode.hasNext())
		{
			MeshNode p = (MeshNode) itnode.next();
			// test in order to avoid useless node
			if (p.getElements().size()>0)
			{
				String ind = new Integer(i).toString();
				out.println("			<NODE ID = \""+i+"\">");
				p.setID(i);
				out.println("				<LOC>"+p.getX()+" "+p.getY()+" "+p.getZ()+" </LOC>");
				out.println("			</NODE>");
				i++;
			}
		}
		Iterator itface = mesh.getFacesIterator();
		i = 0;
		while (itface.hasNext())
		{
			MeshFace f = (MeshFace) itface.next();
			String ind = new Integer(i + 1).toString();
			out.println("			<ELEMENT ID = \""+(i+1)+
				"\" ELEMENTTYPE = \"TRIA3\" DATA = \"1\" MATERIAL = \"1\">");

			Iterator itn = f.getNodesIterator();
			String toWrite = new String();
			while (itn.hasNext())
			{
				MeshNode myn = (MeshNode) itn.next();
				toWrite = toWrite + myn.getID() + " ";
				if (myn.getID() == 0)
				{
					logger.warn("Index NULL ");
					myn.setCoord3D();
					logger.warn(myn);
				}
			}
			out.println("				<CORNERNODES>"+toWrite+"</CORNERNODES>");
			out.println("				<EDGENODES></EDGENODES>");
			out.println("				<METRIC value = \" \" />");
			out.println("				<VOLUME value = \" \" />");
			out.println("			</ELEMENT>");
			i++;
		}
		out.println("			<MeshMassPropertiesTable>\n");
		out.println("				<MassProperty property= v\"xc\" value=\""+0+"\" />");
		out.println("				<MassProperty property= v\"yc\" value=\""+0+"\" />");
		out.println("				<MassProperty property= v\"zc\" value=\""+0+"\" />");
		out.println("			</MeshMassPropertiesTable>");
		out.println("		</MESH>");
		out.println("	</FEAMODEL>");
		out.println("</ANSYSDOC>");
	}	
}
