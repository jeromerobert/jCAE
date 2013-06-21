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
 * (C) Copyright 2005-2009, by EADS France
 */

package org.jcae.mesh.xmldata;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.xmldata.AmibeReader.SubMesh;
import org.xml.sax.SAXException;

/**
 * Manage all the groups of a MeshNode.
 */
public class Groups
{
	private static final Logger LOGGER = Logger.getLogger(Groups.class.getCanonicalName());
	// TODO Create a table selection
	private final PropertyChangeListener groupPropertyChangeListener=new PropertyChangeListener()
	{
		public void propertyChange(PropertyChangeEvent evt)
		{			
			if(evt.getPropertyName().equals("name"))
			{
				modifyXML();
			}
			else if(evt.getPropertyName().equals("visible"))
			{
				throw new RuntimeException("Not yet implemented");
				/*if(viewable!=null)
				{
					Group g=(Group) evt.getSource();					
					Map<Integer, Boolean> map=new HashMap<Integer, Boolean>();
					map.put(g.getId(), (Boolean)evt.getNewValue());
					viewable.setDomainVisible(map);
				}*/
			}
		}	
	};
	protected boolean firstView = true;
	private final ArrayList<Group> groups = new ArrayList<Group>();
	
	/** The absolute path to the mesh directory */
	private String meshFile = null;

	public Groups(String xmlPath)
	{
		meshFile = xmlPath;
	}
		
	public final void addGroup(Group group)
	{
		groups.add(group);
		group.addPropertyChangeListener(groupPropertyChangeListener);
	}

	/**
	 * It makes the fusion of groups.
	 * It modifies the list of groups, the file jcae3d.xml and the file groups.bin.
	 *
	 *@param listGroup the list of groups to fuse.
	 *@return the group resulting of the fusion.
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	final Group fuse(Collection<Group> listGroup, String name)
		throws TransformerException,
		ParserConfigurationException, SAXException, IOException
	{
		String xmlDir=meshFile;
		Group fuseGroup = new Group();
		int number = 0;
		int offset = 0;
		int trianglesNumber = 0;
		
		for (int k = 0; k < groups.size(); k++)
		{
			Group g = groups.get(k);
			trianglesNumber = trianglesNumber + g.getNumberOfElements();
		}
		
		String fileGroups = "jcae3d.files/groups.bin";
		HashMap<Group, int[]> map = new HashMap<Group, int[]>(groups.size());
		Iterator<Group> it = groups.iterator();
		while (it.hasNext())
		{
			Group g = it.next();
			int[] tri = this.readTrianglesGroup(new File(xmlDir, fileGroups), g);
			map.put(g, tri);
		}
		File groupsBin = new File(xmlDir, fileGroups);

		File tmpGroupsBin=File.createTempFile("jcae_groups",".bin");
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
			tmpGroupsBin)));
		
		ArrayList<Integer> listTriFuse = new ArrayList<Integer>();
		for(Group g:listGroup)
		{
			number = number + g.getNumberOfElements();
			if (groups.contains(g))
			{
				groups.remove(g);
			}
			for(int tri:map.get(g))
				listTriFuse.add(tri);
		}
		for (int i = 0; i < groups.size(); i++)
		{
			Group g = groups.get(i);
			if (i == 0)
			{
				g.setOffset(0);
			} else
			{
				Group previousGroup = groups.get(i - 1);
				g.setOffset(previousGroup.getOffset()
					+ previousGroup.getNumberOfElements());
			}
			int[] tri = map.get(g);
			for (int k = 0; k < tri.length; k++)
			{
				out.writeInt(tri[k]);
			}
		}
		Iterator it3 = listTriFuse.iterator();
		
		while (it3.hasNext())
		{
			Integer integer = (Integer) it3.next();
			out.writeInt(integer.intValue());
		}
		
		out.close();
		
		if (!groups.isEmpty())
		{
			Group lastGroup = groups.get(groups.size() - 1);
			offset = lastGroup.getOffset()
				+ lastGroup.getNumberOfElements();
		} else
		{
			offset = 0;
		}

		fuseGroup.setNumberOfElements(number);
		fuseGroup.setOffset(offset);
		fuseGroup.setName(name);
		fuseGroup.addPropertyChangeListener(groupPropertyChangeListener);
		groups.add(fuseGroup);
		out.close();
		
		if(!groupsBin.delete())
		{
			System.err.println(Groups.class+": unable to delete "+groupsBin);
		}
		
		copyFile(tmpGroupsBin, groupsBin);
		
		if(!tmpGroupsBin.delete())
		{
			System.err.println(Groups.class+": unable to delete "+tmpGroupsBin);			
		}
		
		File f = new File(xmlDir, "jcae3d");
		org.w3c.dom.Document xmlDoc = null;
		
		if (f.exists())
		{
			xmlDoc = org.jcae.mesh.xmldata.XMLHelper.parseXML(f);
			modifyXMLGroups(xmlDoc, groupsBin, xmlDir);
			org.jcae.mesh.xmldata.XMLHelper.writeXML(xmlDoc, f);
		}
		
		return fuseGroup;
	}

	private String getFreeName(String template)
	{
		boolean valid;
		String current = template;
		int n = 1;
		do
		{
			valid = true;
			for(Group g: groups)
			{
				if(current.equals(g.getName()))
				{
					valid = false;
					current = template + n;
					n++;
					break;
				}
			}
		}
		while(!valid);
		return current;
	}

	public void fuse(Collection<Group> toFuse)
		throws TransformerException,
		ParserConfigurationException, SAXException, IOException
	{
		fuse(toFuse, getFreeName("fused_groups"));
	}

	/**
	 * @return Returns the Groups
	 */
	public final Group[] getGroups()
	{
		Group[] toReturn = new Group[groups.size()];
		groups.toArray(toReturn);
		return toReturn;
	}

	/**
	 * It writes the file jcae3d.xml to update the groups, after a fusion for example.
	 *
	 *@param xmlDoc the xml document of DOM. It should comes from the parsing of jcae3d.xml.
	 *@param groupFile the file groups.bin.
	 *@param baseDir the directory containing jcae3d.xml.
	 */
	final void modifyXMLGroups(org.w3c.dom.Document xmlDoc,
		java.io.File groupFile, String baseDir)
	{		
		org.w3c.dom.NodeList listGroups = xmlDoc.getElementsByTagName("groups");
		if (listGroups.getLength() > 0)
		{
			org.w3c.dom.Element eltGroups = (org.w3c.dom.Element) listGroups
				.item(0);
			org.w3c.dom.NodeList listGroup = eltGroups
				.getElementsByTagName("group");
			if (listGroup.getLength() > 0)
			{
				while (listGroup.getLength() > 0)
				{
					org.w3c.dom.Element g = (org.w3c.dom.Element) listGroup
						.item(0);
					eltGroups.removeChild(g);
				}
				for (int i = 0; i < groups.size(); i++)
				{
					Group g = groups.get(i);
					eltGroups.appendChild(g.createXMLGroup(xmlDoc, groupFile,
						baseDir));
				}
			}
		}
	}
	
	final void modifyXML()
	{	
		String xmlDir=meshFile;
		String xmlFile = "jcae3d";
		java.io.File f = new java.io.File(xmlDir, xmlFile);
		org.w3c.dom.Document xmlDoc = null;
		if (f.exists())
		{
			try
			{
				xmlDoc = org.jcae.mesh.xmldata.XMLHelper.parseXML(f);
				File groupFile = new File(xmlDir + "/jcae3d.files",
					"groups.bin");
				modifyXMLGroups(xmlDoc, groupFile, xmlDir);
				org.jcae.mesh.xmldata.XMLHelper.writeXML(xmlDoc, f);
			} catch (Exception e)
			{
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * It creates an array of integers which contains the indices of triangles in triangles3d.bin for a particular group.
	 *
	 *@param fileGroup the path of the file groups.bin.
	 *@param g the group.
	 *@return an array of integers.
	 */
	final int[] readTrianglesGroup(File fileGroup, Group g)
	{		
		int[] trianglesGroup = null;
		IntFileReader ifr = null;
		try
		{
			ifr = new PrimitiveFileReaderFactory().getIntReader(fileGroup);
			trianglesGroup = new int[g.getNumberOfElements()];
			ifr.get(g.getOffset(), trianglesGroup);
		}
		catch (IOException ex)
		{
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		}
		finally
		{
			ifr.close();
		}
		return trianglesGroup;
	}

	/**
	 * @param groups The array of groups to set
	 */
	public void setGroups(Group[] groups)
	{
		this.groups.clear();
		this.groups.addAll(Arrays.asList(groups));
		for(int i=0; i<groups.length; i++)
		{
			groups[i].addPropertyChangeListener(groupPropertyChangeListener);			
		}		
	}

	public String getMeshFile()
	{
		return meshFile;
	}
	
	/**
	 * @param meshFile the directory of the mesh of a jcae project.
	 */
	public void setMeshFile(String meshFile)
	{
		this.meshFile = meshFile;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString()
	{
		return "Groups";
	}

	//beware JVM bug: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5056395
	//use transferFrom, not transferTo
	private void copyFile(File src, File dst) throws IOException
	{
		FileChannel sourceChannel = new FileInputStream(src).getChannel();
		FileChannel destinationChannel = new FileOutputStream(dst).getChannel();
		destinationChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
		sourceChannel.close();
		destinationChannel.close();
	}
	
	public void exportGroupsAsUNV() throws ParserConfigurationException, SAXException, IOException
	{
		JScrollPane pane=new JScrollPane();		
		JList list=new JList(getGroups());
		pane.getViewport().add(list);
		JPanel panel=new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(pane, BorderLayout.CENTER);
		panel.add(new JLabel("Groups to export"), BorderLayout.NORTH);
		JFileChooser jfc=new JFileChooser();
		jfc.setAccessory(panel);		
		
		if(jfc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
		{
			Object[] os=list.getSelectedValues();
			String[] ids=new String[os.length];
			for(int i=0; i<os.length; i++)
			{
				ids[i]=((Group)os[i]).getName();
			}			
			PrintStream stream=new PrintStream(new BufferedOutputStream(new FileOutputStream(jfc.getSelectedFile())));
			MeshExporter.UNV exporter=new MeshExporter.UNV(new File(meshFile), ids);
			exporter.write(stream);
			stream.close();
		}
	}

	public static Groups getGroups(String xmlPath)
	{
		Groups groupList = new Groups(xmlPath);
		try
		{
			SubMesh sm = new AmibeReader.Dim3(xmlPath).getSubmeshes().get(0);
			List<AmibeReader.Group> groups = sm.getGroups();
			if (groups.isEmpty())
			{
				int nb = sm.getNumberOfTrias();
				if(nb > 0)
				{
					Group group = new Group();
					group.setName("default");
					group.setNumberOfElements(nb);
					group.setOffset(0);
				}
			}
			else
			{
				for (AmibeReader.Group ag:groups)
				{
					Group group = new Group();
					group.setName(ag.getName());
					group.setNumberOfElements(ag.getNumberOfTrias());
					group.setOffset((int) ag.getTriasOffset());
					groupList.addGroup(group);
				}
			}
		}
		catch (SAXException e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return groupList;
	}

	/**
	 * Remove a set of group from a mesh
	 */
	public static void remove(String amibePath, String ... names) throws IOException
	{
		MeshTraitsBuilder traits = new MeshTraitsBuilder();
		traits.addTriangleSet();
		Mesh m = new org.jcae.mesh.amibe.ds.Mesh(traits);
		MeshReader.readObject3D(m, amibePath);
		int[] ids = m.getGroupIDs(names);
		Iterator<Triangle> it = m.getTriangles().iterator();
		while(it.hasNext())
		{
			Triangle t = it.next();
			if(t.getGroupId() == ids[0])
				it.remove();
		}
		MeshWriter.writeObject3D(m, amibePath, null);
	}
}
