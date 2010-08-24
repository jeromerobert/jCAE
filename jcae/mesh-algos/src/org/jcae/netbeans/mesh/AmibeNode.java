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
 * (C) Copyright 2005-2010, by EADS France
 */

package org.jcae.netbeans.mesh;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.Action;
import org.jcae.mesh.xmldata.Groups;
import org.jcae.netbeans.BeanProperty;
import org.openide.ErrorManager;
import org.openide.actions.DeleteAction;
import org.openide.actions.PropertiesAction;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataNode;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Node.Property;
import org.openide.nodes.Node.PropertySet;
import org.openide.util.Exceptions;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;

/**
 * Contains only the geometric node and the groups node.
 * Lookups :
 * this.
 * @author ibarz
 */
public class AmibeNode extends DataNode
{
	private AbstractNode groupsNode;
	private AbstractNode geomNode;

	public AmibeNode(AmibeDataObject arg0)
	{
		super(arg0, new Children.Array());
		setIconBaseWithExtension("org/jcae/netbeans/mesh/amibe.png");
	}

	public AbstractNode getGroupsNode()
	{
		return groupsNode;
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
				getMesh().setGeometryFile(null);
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

	protected Property[] getMeshProperties()
	{
		try
		{
			return new Property[]{
				new BeanProperty(getMesh(), "deflection"),
				new BeanProperty(getMesh(), "edgeLength"),
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
				new BeanProperty(getMesh(), "geometryFile"),
				new BeanProperty(getMesh(), "meshFile")
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
					return AmibeNode.this.getMeshProperties();
				}

				public String getName()
				{
					return "Mesh";
				}
			},
			new PropertySet()
			{
				public Property[] getProperties() {
					return AmibeNode.this.getExpertProperties();
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

	public Mesh getMesh() {
		return getCookie(AmibeDataObject.class).getMesh();
	}

	@Override
	public String getDisplayName() {
		return getName();
	}

	@Override
	public void setName(String arg0)
	{
		try
		{
			String o=getName();
			getDataObject().rename(arg0+"_mesh");
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

	public void updateGeomNode()
	{
		if(geomNode!=null)
			getChildren().remove(new Node[]{geomNode});
		if(getMesh().getGeometryFile()!=null)
		{
			geomNode=createGeomNode(getMesh().getGeometryFile());
			getChildren().add(new Node[] { geomNode } );
		}

	}

	public Action getPreferredAction()
	{
		return SystemAction.get(PropertiesAction.class);
	}

	@Override
	protected void createPasteTypes(Transferable t, List<PasteType> ls)
	{
		try {			
			if(t.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
			{
				@SuppressWarnings("unchecked")
				List<File> x = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
				if(x != null && !x.isEmpty())
				{
					final String path = FileUtil.getRelativePath(
						getDataObject().getPrimaryFile().getParent(),
						FileUtil.toFileObject(x.get(0)));
					if(path != null)
					{
						ls.add(new PasteType() {

							public Transferable paste() {
								getMesh().setGeometryFile(path);
								updateGeomNode();
								return null;
							}
						});
					}
				}
			}
		} catch (UnsupportedFlavorException ex) {
			Exceptions.printStackTrace(ex);
		} catch (IOException ex) {
			Exceptions.printStackTrace(ex);
		}
		// Also try superclass, but give it lower priority:
		super.createPasteTypes(t, ls);
	}

	public void setGroups(Groups groups)
	{
        if(groupsNode!=null)
        {
        	getChildren().remove(new Node[]{groupsNode});
        }

        if(groups != null)
        {
			GroupChildren groupChildren = new GroupChildren(groups);
        	groupsNode=new AbstractNode(groupChildren, Lookups.fixed(groupChildren));
        	groupsNode.setDisplayName("Groups");
        	getChildren().add(new Node[]{groupsNode});
        }
	}
}