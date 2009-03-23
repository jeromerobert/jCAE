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
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Manage all the groups of a MeshNode.
 */
public class Groups
{	
	// TODO Create a table selection
	private PropertyChangeListener groupPropertyChangeListener=new PropertyChangeListener()
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
	protected String meshFile = null;
	protected String meshName = null;
	//private final MeshSelection meshSelection;
	
	Groups()
	{
		//meshSelection = new MeshSelection(this);
		//SelectionManager.getDefault().addEntitySelection(this, meshSelection);	
	}
		
	public void addGroup(Group group)
	{
		groups.add(group);
		group.addPropertyChangeListener(groupPropertyChangeListener);
	}

	/**
	 * It makes the fusion of groups.
	 * It modifies the list of groups, the file jcae3d.xml and the file groups.bin.
	 *
	 *@param the list of groups to fuse.
	 *@return the group resulting of the fusion.
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public Group fuse(Collection<Group> listGroup) throws TransformerConfigurationException, TransformerException, ParserConfigurationException, SAXException, IOException
	{
		String xmlDir=meshFile;
		System.err.println(Groups.class+": fusing groups");
		Group fuseGroup = new Group();
		int id = 0;
		int number = 0;
		int offset = 0;
		int trianglesNumber = 0;
		
		for (int k = 0; k < groups.size(); k++)
		{
			Group g = groups.get(k);
			System.err.println("lecture initiale : " + g.getName());
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
			if (g.getId() > id)
			{
				id = g.getId();
			}
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
		
		fuseGroup.setId(id + 1);
		fuseGroup.setNumberOfElements(number);
		fuseGroup.setOffset(offset);
		fuseGroup.setName("fuse_group");
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

	/**
	 * @return Returns the Groups
	 */
	public Group[] getGroups()
	{
		Group[] toReturn = new Group[groups.size()];
		groups.toArray(toReturn);
		return toReturn;
	}

	/**
	 *@param the xml element of DOM tree corresponding to the tag "groups".
	 *@param a group.
	 *@return the xml element of DOM tree corresponding to the group.
	 */
	public Element getXmlGroup(Element xmlGroups, Group g)
	{
		NodeList list = xmlGroups.getElementsByTagName("group");
		Element elt = null;
		int length=list.getLength();
		int i = 0;
		boolean found = false;
		while (!found && i < length)
		{
			elt = (Element) list.item(i);
			int id = -1;
			try
			{
				id = Integer.parseInt(elt.getAttribute("id"));
			} catch (Exception e)
			{
				e.printStackTrace(System.out);
			}
			if (id == g.getId())
			{
				found = true;
			} else
			{
				i++;
			}
		}
		if (found)
		{
			return elt;
		} else
		{
			return null;
		}
	}

	/**
	 * It writes the file jcae3d.xml to update the groups, after a fusion for example.
	 *
	 *@param the xml document of DOM. It should comes from the parsing of jcae3d.xml.
	 *@param the file groups.bin.
	 *@param the directory containing jcae3d.xml.
	 */
	public void modifyXMLGroups(org.w3c.dom.Document xmlDoc,
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
	
	void modifyXML()
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
	 *@param the path of the file groups.bin.
	 *@param the number of all triangles of the mesh.
	 *@param the group.
	 *@return an array of integers.
	 */
	public int[] readTrianglesGroup(File fileGroup, Group g)
	{		
		int[] trianglesGroup = null;
		// Open the file and then get a channel from the stream
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(fileGroup);
			FileChannel fc = fis.getChannel();
			// Get the file's size and then map it into memory
			int sz = (int) fc.size();
			MappedByteBuffer bb =
				fc.map(FileChannel.MapMode.READ_ONLY, g.getOffset() * 4,
				g.getNumberOfElements() * 4);
			IntBuffer inb = bb.asIntBuffer();
			trianglesGroup = new int[g.getNumberOfElements()];
			inb.get(trianglesGroup);
			fc.close();
			fis.close();
			MeshExporter.clean(bb);
			return trianglesGroup;
		}
		catch (IOException ex)
		{
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, ex.getMessage(), ex);
		}
		finally
		{
			try
			{
				fis.close();
			}
			catch (IOException ex)
			{
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, ex.getMessage(), ex);
			}
		}
		return trianglesGroup;
	}

	/**
	 * @param The array of groups to set
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
	 * @param the directory of the mesh of a jcae project.
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
	public String toString()
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
			int[] ids=new int[os.length];
			for(int i=0; i<os.length; i++)
			{
				ids[i]=((Group)os[i]).getId();
			}			
			PrintStream stream=new PrintStream(new BufferedOutputStream(new FileOutputStream(jfc.getSelectedFile())));
			MeshExporter.UNV exporter=new MeshExporter.UNV(new File(meshFile), ids);
			exporter.write(stream);
			stream.close();
		}
	}
	public void setMeshName(String meshName)
	{
		this.meshName = meshName;
	}
}
