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
 * (C) Copyright 2011, by EADS France
 */
package org.jcae.mesh.xmldata;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jcae.mesh.xmldata.AmibeReader.Group;
import org.jcae.mesh.xmldata.AmibeReader.SubMesh;
import org.xml.sax.SAXException;

/**
 * Take group define as list of elements (Amibe or UNV group representation)
 * and change them to elements attributs (VTK representation).
 * Attribut are saved to ordered binary files. Triangles attribut are saved first
 * then beams are. This is compatible with Amibe2VTK.
 * @author Jerome Robert
 */
public class GroupInverter {

	public interface IntegerWriter {

		void writeInt(ByteBuffer i) throws IOException;
	}
	private AmibeReader.Dim3 amibeReader;
	private Map<IntegerWriter, List<String>> partitions = new HashMap<IntegerWriter, List<String>>();

	public GroupInverter(String amibeDir) throws SAXException, IOException {
		amibeReader = new AmibeReader.Dim3(amibeDir);
	}

	/** Create a GroupInverted with a unique partition */
	public GroupInverter(String amibeDir, IntegerWriter outputFile)
		throws SAXException, IOException {
		this(amibeDir);
		uniquePartition(outputFile);
	}

	/** Shortcut for addPartition with all groups */
	public void uniquePartition(IntegerWriter outputFile) {
		List<Group> groups = amibeReader.getSubmeshes().get(0).getGroups();
		ArrayList<String> groupNames = new ArrayList<String>();
		for (Group g : groups) {
			groupNames.add(g.getName());
		}
		addPartition(groupNames, outputFile);
	}

	/** 
	 * Add a partition to this group inverted. Each partition is transformed
	 * to a new element attribut
	 * @param groupNames the list of group names to add in this partition. The
	 * first group will be converted to the 1 attribut, and so on. 0 is reserved
	 * for elements which are not in any groups
	 * @param w An interface describing where to write the element attribut
	 * array.
	 */
	public void addPartition(List<String> groupNames, IntegerWriter w) {
		partitions.put(w, groupNames);
	}

	public void invert() throws IOException {
		for (Entry<IntegerWriter, List<String>> e : partitions.entrySet()) {
			e.getKey().writeInt(invertPartition(e.getValue()));
		}
	}

	private ByteBuffer invertPartition(List<String> groupNames)
		throws IOException {
		SubMesh sm = amibeReader.getSubmeshes().get(0);
		int nt = sm.getNumberOfTrias();
		int ne = sm.getNumberOfBeams();
		int triaOffset = ne * 4;
		ByteBuffer toReturn = ByteBuffer.allocate(4 * (ne+nt));
		int gid = 1;
		for (String gn : groupNames) {
			Group g = sm.getGroup(gn);
			for (int id : g.readTria3Ids()) {
				toReturn.putInt(triaOffset + id * 4, gid);
			}

			for (int id : g.readBeamsIds()) {
				toReturn.putInt(id * 4, gid);
			}
			gid++;
		}
		return toReturn;
	}
}
