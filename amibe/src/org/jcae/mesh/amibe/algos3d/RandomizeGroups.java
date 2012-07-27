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

import gnu.trove.TIntArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPathExpressionException;
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

	public RandomizeGroups(String amibeDir, double ratio, String newGroupName)
	{
		this.amibeDir = amibeDir;
		this.ratio = ratio;
		this.newGroupName = newGroupName;
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
			int currentSize = 0;
			for(int triID:g.readTria3Ids())
			{
				if(random.nextDouble() < ratio)
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
	protected boolean filter(String groupName)
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
