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
 * (C) Copyright 2012, by EADS France
 */

package org.jcae.mesh.amibe.algos3d;

import gnu.trove.list.array.TIntArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPathExpressionException;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.util.HashFactory;
import org.jcae.mesh.xmldata.AmibeReader;
import org.jcae.mesh.xmldata.JCAEXMLData;
import org.jcae.mesh.xmldata.XMLReader;
import org.jcae.mesh.xmldata.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Set a given group on random triangles
 * @author Jerome Robert
 */
public class RandomizeGroups {
	private final String amibeDir;
	private final double ratio;
	private final String newGroupName;

	public RandomizeGroups(double ratio, String newGroupName)
	{
		this(null, ratio, newGroupName);
	}

	public RandomizeGroups(String amibeDir, double ratio, String newGroupName)
	{
		this.amibeDir = amibeDir;
		this.ratio = ratio;
		this.newGroupName = newGroupName;
	}

	/**
	 * same as compute but do not change group of non-manifold elements and
	 * work in-core.
	 */
	public void compute(Mesh mesh) throws IOException, SAXException
	{
		compute(mesh, false);
	}

	private boolean canRandomize(Vertex v, Collection<Vertex> forbidden)
	{
		return v.isManifold() && !forbidden.contains(v);
	}

	public void compute(Mesh mesh, boolean neighbors) throws IOException, SAXException
	{
		Random random = new Random(0);
		Set<Vertex> vertices = HashFactory.createSet();
		for(Collection<Vertex> l:mesh.getVertexGroup().values())
			vertices.addAll(l);
		vertices.addAll(mesh.getBeams());
		int newGroupId =  mesh.getNumberOfGroups();
		mesh.setGroupName(newGroupId, newGroupName);
		triangles: for(Triangle t:mesh.getTriangles())
		{
			String groupName = mesh.getGroupName(t.getGroupId());
			if(groupName != null && canRandomize(groupName))
			{
				if(!canRandomize(t.getV0(), vertices) ||
					!canRandomize(t.getV1(), vertices) ||
					!canRandomize(t.getV2(), vertices))
					continue;
				AbstractHalfEdge e = t.getAbstractHalfEdge();
				if(neighbors)
				{
					if(!canRandomize(e.sym().apex(), vertices) ||
						!canRandomize(e.next().sym().apex(), vertices) ||
						!canRandomize(e.prev().sym().apex(), vertices))
						continue;
				}
				if(random.nextDouble() < ratio)
				{
					t.setGroupId(newGroupId);
					if(neighbors)
					{
						e.sym().getTri().setGroupId(newGroupId);
						e.next().sym().getTri().setGroupId(newGroupId);
						e.prev().sym().getTri().setGroupId(newGroupId);
					}
				}
			}
		}
	}

	public void compute() throws IOException, SAXException
	{
		AmibeReader.Dim3 ar = new AmibeReader.Dim3(amibeDir);
		AmibeReader.SubMesh sm = ar.getSubmeshes().get(0);
		List<AmibeReader.Group> groups = sm.getGroups();
		File groupsFile = new File(new File(amibeDir, "jcae3d.files"),
			JCAEXMLData.groupsFilename);
		String tmpFile = groupsFile.getPath()+".tmp";
		FileChannel fc = new FileOutputStream(tmpFile).getChannel();
		TIntArrayList newGroup = new TIntArrayList(
			100 + (int) (ratio * sm.getNumberOfTrias()));
		ByteBuffer bb = ByteBuffer.allocate(4096);
		TIntArrayList groupSize = new TIntArrayList(groups.size()+1);
		Random random = new Random(0);
		for(AmibeReader.Group g: sm.getGroups())
		{
			if(g.getNumberOfBeams() == 0 && g.getNumberOfTrias() == 0)
				continue;
			boolean canRandom = canRandomize(g.getName());
			int currentSize = 0;
			for(int triID:g.readTria3Ids())
			{
				if(canRandom && random.nextDouble() < ratio)
				{
					newGroup.add(triID);
				}
				else
				{
					currentSize++;
					bb.putInt(triID);
					flushChannel(fc, bb, false);
				}
			}
			groupSize.add(currentSize);
		}

		for(int i = 0; i < newGroup.size(); i++)
		{
			bb.putInt(newGroup.get(i));
			flushChannel(fc, bb, false);
		}
		groupSize.add(newGroup.size());
		newGroup = null;
		flushChannel(fc, bb, true);
		updateXML(new File(amibeDir, JCAEXMLData.xml3dFilename), groupSize);
		groupsFile.delete();
		new File(tmpFile).renameTo(groupsFile);
	}

	private void updateXML(final File file, final TIntArrayList groupSize) throws SAXException, IOException {
		new XMLReader() {
			@Override
			protected void read(Document dom) throws SAXException,
				XPathExpressionException, IOException {
				Element groupsE = getElement(
					dom.getDocumentElement(), "mesh", "submesh", "groups");
				int gid = 0;
				long offset = 0;
				Element last = null;
				for(Element e:getElements(groupsE, "group"))
				{
					int size = groupSize.get(gid++);
					Element numberElement = getElement(e, "number");
					//check that this is not a beam only group
					if(numberElement != null)
					{
						numberElement.setTextContent(Integer.toString(size));
						Element fileE = getElement(e, "file");
						if(offset == 0)
							fileE.removeAttribute("offset");
						else
							fileE.setAttribute("offset", Long.toString(offset));
						offset += size;
						last = e;
					}
				}
				int size = groupSize.get(gid);
				Element e = (Element) last.cloneNode(true);
				//the id attribut is now ignored but we remove it instead of
				//having a wrong one.
				e.removeAttribute("id");
				getElement(e, "name").setTextContent(newGroupName);
				getElement(e, "number").setTextContent(Integer.toString(size));
				getElement(e, "file").setAttribute("offset", Long.toString(offset));
				groupsE.appendChild(e);
				XMLWriter.writeDocument(dom, file.getPath());
			}

			@Override
			protected String getXSD() {
				return null;
			}
		}.read(file);
	}

	private static void flushChannel(FileChannel fc, ByteBuffer bb, boolean force) throws IOException
	{
		if(force || bb.position() == bb.limit())
		{
			bb.limit(bb.position());
			bb.rewind();
			fc.write(bb);
			bb.clear();
		}
	}
	/**
	 * Override to specify which groups can be changed.
	 * Default allow to change any groups
	 */
	protected boolean canRandomize(String groupName)
	{
		return true;
	}

	public static void main(final String[] args) {
		try {
			new RandomizeGroups("/home/robert/ast-a319-neo/demo-anabelle/demo/AST_mesh.amibe",
				0.01, "CR_EXT_random").compute();
		} catch (IOException ex) {
			Logger.getLogger(RandomizeGroups.class.getName()).log(Level.SEVERE,
				null, ex);
		} catch (SAXException ex) {
			Logger.getLogger(RandomizeGroups.class.getName()).log(Level.SEVERE,
				null, ex);
		}
	}
}
