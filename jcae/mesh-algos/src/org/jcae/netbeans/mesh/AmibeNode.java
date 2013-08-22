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

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jcae.mesh.xmldata.Groups;
import org.jcae.netbeans.BeanProperty;
import org.openide.ErrorManager;
import org.openide.actions.DeleteAction;
import org.openide.actions.PropertiesAction;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
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
	public final static ImageIcon ICON =
		new ImageIcon(AmibeNode.class.getResource("amibe.png"));
	public AmibeNode(AmibeDataObject arg0)
	{
		super(arg0, new Children.Array());
		setDisplayName(arg0.getDisplayName());
	}

	@Override
	public Image getIcon(int type) {
		return ICON.getImage();
	}

	@Override
	public Image getOpenedIcon(int type) {
		return getIcon(type);
	}

	public AbstractNode getGroupsNode()
	{
		return groupsNode;
	}

	private FileObject getFileObject() {
		//Since NB 7.1 http://netbeans.org/bugzilla/show_bug.cgi?id=199391
		//DataObject may not be ready so we look for FileObject
		FileObject fo = getLookup().lookup(FileObject.class);
		if (fo == null)
		//but in NB 7.0 the node may only contains DataObject and not FileObject
			fo = getLookup().lookup(DataObject.class).getPrimaryFile();
		return fo;
	}

	private AbstractNode createGeomNode(String geomFile)
	{
		int i=geomFile.lastIndexOf('.');
		final String s=geomFile.substring(0, i);
		// fileObject is need to ensure that the node is visible in the
		// favorites tab
		return new AbstractNode(Children.LEAF, Lookups.singleton(getFileObject()))
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
				if(x != null && !x.isEmpty() && !x.get(0).getPath().isEmpty())
				{
					FileObject fo = FileUtil.toFileObject(x.get(0));
					final String path = fo == null ? null : FileUtil.getRelativePath(
						getDataObject().getPrimaryFile().getParent(), fo);
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
			// fileObject is need to ensure that the node is visible in the
			// favorites tab
			GroupChildren groupChildren = new GroupChildren(groups, getFileObject());
			groupsNode=new AbstractNode(groupChildren, Lookups.fixed(groupChildren, getFileObject()));
        	groupsNode.setDisplayName("Groups");
        	getChildren().add(new Node[]{groupsNode});
        }
	}

	@Override
	public String getName() {
		return ((AmibeDataObject)getDataObject()).getDisplayName();
	}

	@Override
	public void setName(String name) {
		super.setName(name);
		setDisplayName(name);
	}
}