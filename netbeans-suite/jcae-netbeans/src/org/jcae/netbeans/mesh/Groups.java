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
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.netbeans.mesh;

import java.awt.BorderLayout;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import javax.media.j3d.*;
import javax.swing.*;
import javax.vecmath.Color3f;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.jcae.mesh.xmldata.UNVConverter;
import org.jcae.netbeans.viewer3d.View3D;
import org.jcae.viewer3d.SelectionListener;
import org.jcae.viewer3d.fe.ViewableFE;
import org.jcae.viewer3d.fe.amibe.AmibeProvider;
import org.openide.ErrorManager;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class Groups implements SelectionListener
{
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
				if(viewable!=null)
				{
					Group g=(Group) evt.getSource();					
					Map map=new HashMap();
					map.put(new Integer(g.getId()), evt.getNewValue());
					viewable.setDomainVisible(map);
				}
			}
			else if(evt.getPropertyName().equals("selected"))
			{
				if(viewable!=null && !breakhighLightingEvent)
				{
					Group g=(Group) evt.getSource();					
					viewable.hightLight(g.getId(), ((Boolean)evt.getNewValue()).booleanValue());
				}
			}
		}	
	};
	protected boolean firstView = true;
	private ArrayList groups = new ArrayList();
	
	/** The absolute path to the mesh directory */
	protected String meshFile = null;
	protected String meshName = null;
	protected int indexColor = 0;
	private ViewableFE viewable;
	
	private String getXmlDir()
	{
		return meshFile;
	}
	
	public void addGroup(Group group)
	{
		groups.add(group);
		group.addPropertyChangeListener(groupPropertyChangeListener);
	}

	/**
	 * Creates a Java3D BranchGroup whcih represents a group.
	 * It creates two Java3D Shapes3D : one for polygons, one for edges.
	 * @param the Java3D geometry of a Group.
	 */
	public BranchGroup createBranchGroup(IndexedTriangleArray geom)
	{
		BranchGroup branchGroup = new BranchGroup();
		Appearance shapeFillAppearance = new Appearance();
		shapeFillAppearance.setPolygonAttributes(new PolygonAttributes(
			PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE,
			2.0f * Float.parseFloat(System.getProperty(
				"javax.media.j3d.zFactorAbs", "20.0f")), false, Float
				.parseFloat(System.getProperty("javax.media.j3d.zFactorRel",
					"2.0f"))));
		shapeFillAppearance.setColoringAttributes(new ColoringAttributes(
			new Color3f(Color.GRAY), ColoringAttributes.SHADE_FLAT));
		Shape3D shapeFill = new Shape3D(geom, shapeFillAppearance);
		shapeFill.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		branchGroup.addChild(shapeFill);
		Appearance shapeLineAppearance = new Appearance();
		shapeLineAppearance.setPolygonAttributes(new PolygonAttributes(
			PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, Float
				.parseFloat(System.getProperty("javax.media.j3d.zFactorAbs",
					"20.0f"))));
		shapeLineAppearance.setColoringAttributes(new ColoringAttributes(
			new Color3f(Color.BLUE), ColoringAttributes.SHADE_GOURAUD));
		Shape3D shapeLine = new Shape3D(geom, shapeLineAppearance);
		branchGroup.addChild(shapeLine);
		return branchGroup;
	}

	/**
	 * Display in a View3D a list of groups.
	 * The xml Directory xmlDir must be setted. After having load a project, xmlDir may be null the first time.
	 * 
	 * @param the list of groups to display.
	 * @param the View3D in which the Groups are displayed.
	 */
	public void displayGroups(Collection groupsToDisplay, View3D view)
	{
		try
		{
			if(viewable==null)
			{
				AmibeProvider provider = new AmibeProvider(new File(getXmlDir()));
				viewable = new ViewableFE(provider);
				viewable.addSelectionListener(this);
				viewable.setName(meshName);
			}
						
			Map map=new HashMap();
			for(int i=0; i<groups.size(); i++)
			{
				Group g=(Group)groups.get(i);
				map.put(new Integer(g.getId()), Boolean.FALSE);
			}
			
			Iterator it=groupsToDisplay.iterator();
			while(it.hasNext())
			{
				Group g=(Group)it.next();
				map.put(new Integer(g.getId()), Boolean.TRUE);
			}

			viewable.setDomainVisible(map);
			
			if(!Arrays.asList(view.getView().getViewables()).contains(viewable))
			{
				view.getView().add(viewable);	
			}
		}
		catch (ParserConfigurationException e)
		{
			ErrorManager.getDefault().notify(e);
		}
		catch (SAXException e)
		{
			ErrorManager.getDefault().notify(e);
		}
		catch (IOException e)
		{
			ErrorManager.getDefault().notify(e);
		}
	}

	/**
	 * Bean action which makes the fusion of groups.
	 * It displays a dialog-box to select the groups to fuse.
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 * @throws TransformerConfigurationException 
	 */
	public void fuse() throws TransformerConfigurationException, TransformerException, ParserConfigurationException, SAXException, IOException
	{
		PanelFuse panel = new PanelFuse(this);
		if (!panel.cancel())
		{
			ArrayList groupsToFuse = panel.getSelectedGroups();
			fuse(groupsToFuse);
		}
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
	public Group fuse(ArrayList listGroup) throws TransformerConfigurationException, TransformerException, ParserConfigurationException, SAXException, IOException
	{
		String xmlDir=getXmlDir();
		System.err.println(Groups.class+": fusing groups");
		Group fuseGroup = new Group();
		int id = 0;
		int number = 0;
		int offset = 0;
		int trianglesNumber = 0;
		
		for (int k = 0; k < groups.size(); k++)
		{
			Group g = (Group) groups.get(k);
			System.err.println("lecture initiale : " + g.getName());
			trianglesNumber = trianglesNumber + g.getNumberOfElements();
		}
		
		String fileGroups = "jcae3d.files/groups.bin";
		HashMap map = new HashMap(groups.size());
		Iterator it = groups.iterator();
		while (it.hasNext())
		{
			Group g = (Group) it.next();
			int[] tri = this.readTrianglesGroup(new File(xmlDir, fileGroups), g);
			map.put(g, tri);
		}
		File groupsBin = new File(xmlDir, fileGroups);

		File tmpGroupsBin=File.createTempFile("jcae_groups",".bin");
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
			tmpGroupsBin)));
		
		ArrayList listTriFuse = new ArrayList();
		for (int i = 0; i < listGroup.size(); i++)
		{
			number = number
				+ ((Group) listGroup.get(i)).getNumberOfElements();
			if (groups.contains(listGroup.get(i)))
			{
				groups.remove(listGroup.get(i));
			}
			int[] tri = (int[]) map.get(listGroup.get(i));
			for (int k = 0; k < tri.length; k++)
			{
				listTriFuse.add(new Integer(tri[k]));
			}
		}
		for (int i = 0; i < groups.size(); i++)
		{
			Group g = (Group) groups.get(i);
			if (g.getId() > id)
			{
				id = g.getId();
			}
			if (i == 0)
			{
				g.setOffset(0);
			} else
			{
				Group previousGroup = (Group) groups.get(i - 1);
				g.setOffset(previousGroup.getOffset()
					+ previousGroup.getNumberOfElements());
			}
			int[] tri = (int[]) map.get(g);
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
			Group lastGroup = (Group) groups.get(groups.size() - 1);
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
		 
		if(viewable!=null)
		{
			viewable.domainsChanged(null);
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
					Group g = (Group) groups.get(i);
					eltGroups.appendChild(g.createXMLGroup(xmlDoc, groupFile,
						baseDir));
				}
			}
		}
	}
	
	void modifyXML()
	{	
		String xmlDir=getXmlDir();
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
		int[] trianglesGroup=null;
		try
		{
			// Open the file and then get a channel from the stream
	        FileInputStream fis = new FileInputStream(fileGroup);
	        FileChannel fc = fis.getChannel();
	 
	        // Get the file's size and then map it into memory
	        int sz = (int)fc.size();
	        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, g.getOffset()*4, g.getNumberOfElements()*4);
			IntBuffer inb=bb.asIntBuffer();			
			trianglesGroup = new int[g.getNumberOfElements()];
			inb.get(trianglesGroup);
			fc.close();
			fis.close();
			UNVConverter.clean(bb);
			return trianglesGroup;
		}
		catch(IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
			return null;
		}		
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
	public String toString()
	{
		return "Groups";
	}

	/**
	 * Bean action which makes it possible to display groups.
	 * It displays a dialog-box to select the groups to display, and to choose the view.
	 */
	public void viewGroup()
	{
		PanelView dialog = new PanelView(this);
		if (!dialog.cancel())
		{
			this.displayGroups(dialog.getSelectedGroups(), dialog
				.getSelectedView());
		}
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

	private boolean breakhighLightingEvent=false;
	private Object hightLightEventLock=new Object();
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FESelectionListener#elementsSelected(java.util.Map)
	 */
	public void elementsSelected(Map selection)
	{
		Set ids=selection.keySet();
		ArrayList sg=new ArrayList();
		for(int i=0; i<groups.size(); i++)
		{
			Group g=(Group) groups.get(i);
			if(ids.contains(new Integer(g.getId())))
				sg.add(g);
		}
		synchronized(hightLightEventLock)
		{
			breakhighLightingEvent=true;
			//TODO
			breakhighLightingEvent=false;
		}
	}
	
	private int[] getSelectedGroups()
	{
		//TODO
		return new int[0];
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
			new UNVConverter(new File(meshFile), ids).writeUNV(stream);
			stream.close();
		}
	}
	public void setMeshName(String meshName)
	{
		this.meshName = meshName;
	}

	/**
	 * Used by Mesh.notifyComputationFinished to reset groups
	 * without removing all currently viewed viewables
	 * @return Returns the viewable.
	 */
	ViewableFE getViewable()
	{
		return viewable;
	}
	
	/**
	 * Used by Mesh.notifyComputationFinished to reset groups
	 * without removing all currently viewed viewables
	 * @return Returns the viewable.
	 */
	void setViewable(ViewableFE viewable)
	{
		this.viewable = viewable;
	}

	public void selectionChanged() {
		// TODO Auto-generated method stub
		
	}
}
