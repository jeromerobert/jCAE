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

import java.awt.datatransfer.Transferable;
import java.beans.IntrospectionException;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Action;
import org.jcae.netbeans.BeanProperty;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.cad.BrepNode;
import org.jcae.netbeans.viewer3d.View3D;
import org.openide.ErrorManager;
import org.openide.actions.*;
import org.openide.cookies.SaveCookie;
import org.openide.cookies.ViewCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.PasteType;

public class MeshNode extends DataNode implements SaveCookie, ViewCookie
{
	private Mesh mesh;
	private Groups groups;
	private AbstractNode groupsNode;
	private AbstractNode geomNode;
	
	public MeshNode(DataObject arg0)
	{
		super(arg0, new Children.Array());
		getCookieSet().remove(getCookie(ViewCookie.class));
		getCookieSet().add(this);
		mesh=initMesh(arg0);
		updateGeomNode();
		refreshGroups();
	}

	private AbstractNode createGeomNode(String geomFile)
	{
		int i=geomFile.lastIndexOf('.');
		final String s=geomFile.substring(0, i);
		return new AbstractNode(Children.LEAF)
		{
			public String getDisplayName()
			{
				return s;
			}
			
			public void destroy() throws IOException
			{
				super.destroy();
				mesh.setGeometryFile(null);
			}
			
			public Action[] getActions(boolean arg0)
			{
				return new Action[]{SystemAction.get(DeleteAction.class)};
			}
			
			public boolean canDestroy()
			{
				return true;
			}
		};
	}
	
	private static Mesh initMesh(DataObject dataObject)
	{
		try
		{
			XMLDecoder decoder=new XMLDecoder(dataObject.getPrimaryFile().getInputStream());
			return (Mesh)decoder.readObject();			
		}
		catch (Exception ex)
		{
			ErrorManager.getDefault().log(ex.getMessage());
			String name=Utilities.getFreeName(
				dataObject.getPrimaryFile().getParent(),
				"amibe",".dir");
			return new Mesh(name);
		}
	}
	
	protected Property[] getMeshProperties()
	{
		try
		{
			return new Property[]{
				new BeanProperty(mesh, "deflection"),
				new BeanProperty(mesh, "edgeLength"),
			};
		}
		catch (NoSuchMethodException e)
		{
			ErrorManager.getDefault().notify(e);
			return new Property[0];
		}
		catch (IntrospectionException e) {
			ErrorManager.getDefault().notify(e);
			return new Property[0];
		}
	}

	protected Property[] getExpertProperties()
	{
		try
		{
			return new Property[]{
				new BeanProperty(mesh, "geometryFile"),
				new BeanProperty(mesh, "meshFile")
			};
		}
		catch (NoSuchMethodException e)
		{
			ErrorManager.getDefault().notify(e);
			return new Property[0];
		}
		catch (IntrospectionException e) {
			ErrorManager.getDefault().notify(e);
			return new Property[0];
		}
	}
	
	
	public PropertySet[] getPropertySets()
	{
		return new PropertySet[]{
			new PropertySet()
			{
				public Property[] getProperties() {
					return MeshNode.this.getMeshProperties();
				}
				
				public String getName()
				{
					return "Mesh";
				}
			},
			new PropertySet()
			{
				public Property[] getProperties() {
					return MeshNode.this.getExpertProperties();
				}
				
				public String getName()
				{
					return "Expert";
				}
				
				public boolean isExpert() {
					return true;
				}
				
			}
		};
	}

	public void save() throws IOException
	{
		FileObject out = getDataObject().getPrimaryFile();
		FileLock l = out.lock();
		XMLEncoder encoder=new XMLEncoder(out.getOutputStream(l));
		encoder.writeObject(mesh);
		encoder.close();
		l.releaseLock();
	}

	public void view()
	{
		if(groups!=null)
		{
			View3D view=View3D.getView3D();
			groups.displayGroups(
				Arrays.asList(groups.getGroups()),
				view);
			view.getView().fitAll();
		}
	}
	
	public String getMeshDirectory()
	{
		String ref=FileUtil.toFile(getDataObject().getPrimaryFile().getParent()).getPath();
		return Utilities.absoluteFileName(mesh.getMeshFile(), ref);		
	}
	/**
	 * @return Returns the groups
	 */
	public void refreshGroups()
	{
		String meshDir=getMeshDirectory();
		File xmlFile=new File(meshDir, "jcae3d");
        if (xmlFile.exists())
        {
            groups = GroupsReader.getGroups(xmlFile.getPath());
            groups.setMeshFile(meshDir);
        }
        
        if(groupsNode!=null)
        {
        	getChildren().remove(new Node[]{groupsNode});
        }
        
        if(groups!=null && groups.getGroups().length>0)
        {
        	groupsNode=new AbstractNode(new GroupChildren(groups));
        	groupsNode.setDisplayName("Groups");
        	getChildren().add(new Node[]{groupsNode});
        }
	}

	public Mesh getMesh() {
		return mesh;
	}
	
	public Action[] getActions(boolean arg0)
	{
		ArrayList l=new ArrayList();		
		l.add(SystemAction.get(ComputeMeshAction.class));
		l.add(SystemAction.get(PropertiesAction.class));
		l.add(SystemAction.get(ViewAction.class));
		l.add(SystemAction.get(SaveAction.class));
		l.add(null);
		l.add(SystemAction.get(DeleteAction.class));
		l.add(SystemAction.get(RenameAction.class));
		return (Action[]) l.toArray(new Action[l.size()]);
	}
	
	public String getDisplayName()
	{
		return getName();
	}

	public void setName(String arg0)
	{	
		try
		{
			String o=getName();
			getDataObject().rename(arg0+"_mesh.xml");
			fireDisplayNameChange(o, arg0);
			fireNameChange(o, arg0);
		}
		catch (IOException e)
		{
			ErrorManager.getDefault().notify(e);
		}
	}
	
	public String getName()
	{
		String s = getDataObject().getName();
		return s.substring(0, s.length()-"_mesh".length());
	}
	
	/*protected void createPasteTypes(Transferable arg0, List arg1)
	{
		super.createPasteTypes(arg0, arg1);
		DataFlavor[] dts = arg0.getTransferDataFlavors();
		for(int i=0; i<dts.length; i++)
		{
			System.out.println("createPasteTypes"+i+":"+dts[i]);
		}

		if(arg0.isDataFlavorSupported(BrepNode.OPENCASCADE_DATAFLAVOR))
		{
			try
			{
				final String name = (String) arg0.getTransferData(BrepNode.OPENCASCADE_DATAFLAVOR);
				arg1.add(new PasteType()
				{
					public Transferable paste() throws IOException
					{
						mesh.setGeometryFile(name);
						firePropertyChange(null, null, null);
						return null;
					}
				});
			}
			catch (UnsupportedFlavorException e) 
			{
				ErrorManager.getDefault().notify(e);
			}
			catch (IOException e)
			{
				ErrorManager.getDefault().notify(e);
			}
		}
	}*/
	
	protected void createPasteTypes(Transferable t, List ls)
	{
		final Node[] ns = NodeTransfer.nodes(t, NodeTransfer.COPY|NodeTransfer.MOVE);
		if (ns != null && ns.length==1) {
			final BrepNode n=(BrepNode) ns[0].getCookie(BrepNode.class);
			if(n!=null)
			ls.add(new PasteType()
			{
				public Transferable paste()
				{
					mesh.setGeometryFile(n.getDataObject().getPrimaryFile().getNameExt());					
					updateGeomNode();
					firePropertyChange(null, null, null);
					return null;
				}
			});
		}
		// Also try superclass, but give it lower priority:
		super.createPasteTypes(t, ls);
	}
	
	private void updateGeomNode()
	{
		if(geomNode!=null)
			getChildren().remove(new Node[]{geomNode});
		if(mesh.getGeometryFile()!=null)
		{
			geomNode=createGeomNode(mesh.getGeometryFile());
			getChildren().add(new Node[] { geomNode } );
		}
			
	}
		
	public Action getPreferredAction()
	{
		return SystemAction.get(PropertiesAction.class);
	}
}