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
 * (C) Copyright 2008, by EADS France
 */

package org.jcae.vtk;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.mesh.xmldata.AmibeReader;
import org.jcae.mesh.xmldata.AmibeReader.Group;
import org.xml.sax.SAXException;

/**
 *
 * @author Julian Ibarz
 */
public class AmibeToMesh
{
	private final static Logger LOGGER=Logger.getLogger(AmibeToMesh.class.getName());

	private final Mesh mesh;

	public Mesh getMesh()
	{
		return mesh;
	}
	
	private static class GroupData extends LeafNode.DataProvider
	{
		private final AmibeReader.Dim3 provider;
		private final String id;
		
		GroupData(AmibeReader.Dim3  provider, String id)
		{
			this.provider = provider;
			this.id = id;
		}

		@Override
		public void load()
		{
			try {
				AmibeDomain domain = new AmibeDomain(provider, id);
				// Nodes
				setNodes(domain.getNodes());
				// Polys
				nbrOfPolys = domain.getTria3().length / 3 + domain.getQuad4().length / 4;
				int[] quads = domain.getQuad4();
				int[] triangles = domain.getTria3();
				this.polys = new int[4 * (triangles.length / 3) + 5 * (quads.length / 4)];
				int offset = 0;
				for (int i = 0; i < triangles.length;) {
					polys[offset++] = 3;
					polys[offset++] = triangles[i++];
					polys[offset++] = triangles[i++];
					polys[offset++] = triangles[i++];
				}
				for (int i = 0; i < quads.length;) {
					polys[offset++] = 4;
					polys[offset++] = quads[i++];
					polys[offset++] = quads[i++];
					polys[offset++] = quads[i++];
					polys[offset++] = quads[i++];
				}
			} catch (IOException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			}
		}
	}
	
	public AmibeToMesh(String filePath)
		throws ParserConfigurationException, SAXException, IOException
	{
		this(filePath, null);
	}

	public AmibeToMesh(String filePath, String[] groupExtraction)
		throws ParserConfigurationException, SAXException, IOException
	{		
		AmibeReader.Dim3 reader = new AmibeReader.Dim3(filePath);
		if(groupExtraction == null)
		{
			List<Group> grps = reader.getSubmeshes().get(0).getGroups();
			groupExtraction = new String[grps.size()];
			for(int i=0; i<groupExtraction.length; i++)
				groupExtraction[i]=grps.get(i).getName();
		}

		mesh = new Mesh(groupExtraction.length);

		for(String id : groupExtraction)
			mesh.setGroup(id, new GroupData(reader, id));
	}
}
