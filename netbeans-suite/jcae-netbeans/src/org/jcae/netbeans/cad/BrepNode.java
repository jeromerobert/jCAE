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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.netbeans.cad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import javax.swing.Action;
import org.jcae.opencascade.Utilities;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.ErrorManager;
import org.openide.actions.*;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.loaders.FileEntry;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.NewType;

public class BrepNode extends DataNode implements Node.Cookie
{		
	private MetaNode metaNode;

	public BrepNode(DataObject arg0)
	{
		super(arg0, new ShapeChildren());
		getCookieSet().add(this);
		getCookieSet().add(new ShapePool());		
		getCookieSet().add(new ShapeOperationCookie(this));
		getCookieSet().add((Cookie) getChildren());
		updateChildren();
		
	}

	public void updateChildren()
	{
		DataObject dob=getDataObject();
		if(dob instanceof BrepDataObject)
		{
			BrepDataObject mob = (BrepDataObject)dob;
			Set entries=mob.secondaryEntries();
			if(entries.size()>0 && metaNode==null)
			{
				FileEntry fe=(FileEntry) entries.toArray()[0];
				Node[] ns=getChildren().getNodes();
				getChildren().remove(ns);
				metaNode=new MetaNode(mob, fe.getFile());
				Node[] ns2=new Node[ns.length+1];
				System.arraycopy(ns, 0, ns2, 1, ns.length);
				ns2[0]=metaNode;
				getChildren().add(ns2);
			}
			else if(entries.size()==0 && metaNode!=null)
			{
				Node[] toRemove=new Node[]{getChildren().getNodes()[0]};
				getChildren().remove(toRemove);
			}
		}
	}
	
	public Action[] getActions(boolean arg0)
	{
		ArrayList l=new ArrayList();
		l.add(SystemAction.get(ExplodeAction.class));
		l.add(SystemAction.get(ViewAction.class));
		l.add(SystemAction.get(NewAction.class));
		l.add(SystemAction.get(BooleanAction.AllActions.class));
		l.add(SystemAction.get(TransformAction.AllActions.class));
		l.add(SystemAction.get(SewAction.class));
		l.add(SystemAction.get(FreeBoundsAction.class));
		l.add(SystemAction.get(BoundingBoxAction.class));
		
		if(GeomUtils.getShape(this).shapeType()==TopAbs_ShapeEnum.FACE)
		{
			l.add(SystemAction.get(GroupFaceAction.class));
			l.add(SystemAction.get(ReverseAction.class));
		}
		l.add(null);
		l.add(SystemAction.get(RenameAction.class));
		l.add(SystemAction.get(CutAction.class));
		l.add(SystemAction.get(CopyAction.class));
		l.add(SystemAction.get(DeleteAction.class));
		return (Action[]) l.toArray(new Action[l.size()]);
	}


	public NewType[] getNewTypes()
	{
		return PrimitiveNewType.getNewType(this);
	}
	
	public boolean canRename()
	{
		return true;
	}
	
	public boolean canCopy()
	{
		return true;
	}
	
	public boolean canDestroy()
	{
		return true;
	}


	public void setName(String arg0)
	{	
		try
		{
			String o=getName();
			getDataObject().rename(arg0);
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
		return getDataObject().getPrimaryFile().getName();
	}
	
	public String getDisplayName()
	{
		return getDataObject().getPrimaryFile().getName();
	}

	public MetaNode getMetaNode() {
		return metaNode;
	}
	
	public Sheet createSheet()
	{
		Sheet sheet=super.createSheet();
		Sheet.Set set=new Sheet.Set();
		set.put(new PropertySupport.ReadOnly(
			"tolerance", Double.class, "tolerance", "tolerance")
			{	
				public Object getValue()
				{
					TopoDS_Shape s= ((BrepDataObject)getDataObject()).getShape();
					return Double.valueOf(Utilities.tolerance(s));
				};
			});
		set.setName("Geometry");
		sheet.put(set);
		return sheet;
	}

	/*public Transferable clipboardCopy() throws IOException
	{
		Multi toReturn = new Multi(new Transferable[]{super.clipboardCopy(), createStringTransferable()});
		System.out.println(toReturn);
		return toReturn;
	}
	
	public Transferable drag() throws IOException
	{
		return new ExTransferable.Multi(new Transferable[]{super.drag(), createStringTransferable()});
	}
	
	public final static DataFlavor OPENCASCADE_DATAFLAVOR;
	
	static
	{
		DataFlavor tmp=null;
		try
		{
			tmp=new DataFlavor("application/x-opencascade;class=java.lang.String");
		}
		catch (ClassNotFoundException e)
		{
			ErrorManager.getDefault().notify(e);
		}
		OPENCASCADE_DATAFLAVOR=tmp;
	}
	
	private Transferable createStringTransferable()
	{
		return new ExTransferable.Single(OPENCASCADE_DATAFLAVOR)
		{
			protected Object getData() throws IOException, UnsupportedFlavorException
			{
				return  getDataObject().getPrimaryFile().getNameExt();
			}
		};
	}*/
}
