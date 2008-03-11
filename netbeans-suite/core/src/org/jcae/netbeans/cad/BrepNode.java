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

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.openide.ErrorManager;
import org.openide.actions.*;
import org.openide.cookies.OpenCookie;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.NewType;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;

public class BrepNode extends DataNode implements Node.Cookie, OpenCookie
{		
	private final AbstractAction closeAction = new AbstractAction("Close")
	{		
		public void actionPerformed(ActionEvent e) 
		{
			BrepDataObject o = getLookup().lookup(BrepDataObject.class);
			instanceContent.remove(o.getShape());
			o.save();
			o.unload();
			ShapeChildren sc = getLookup().lookup(ShapeChildren.class);
			sc.remove(sc.getNodes());			
			fireStateChange();
		}
	};
	
	private static class MyLookup extends ProxyLookup
	{		
		public void setDelegates(Lookup... lookups) 
		{
			setLookups(lookups);
		}
	}
	
	private final InstanceContent instanceContent = new InstanceContent();
	public BrepNode(DataObject arg0)
	{
		super(arg0, new ShapeChildren(), new MyLookup());
		((MyLookup)getLookup()).setDelegates(new AbstractLookup(instanceContent));
		setIconBaseWithExtension("org/jcae/netbeans/cad/BRepNode.png");
		instanceContent.add(this);
		instanceContent.add(new ViewShapeCookie(this));
		instanceContent.add(getChildren());
		instanceContent.add(arg0);
	}

	/** just a short cut
	 * @return*/
	private boolean isLoaded()
	{
		return getLookup().lookup(BrepDataObject.class).isLoaded();
	}
	
	/**
	 * Default behaviour is to be open when the node is unfold. We want to have
	 * an explicit open/closed status
	 */
	@Override
	public Image getIcon(int arg0) {
		if(isLoaded())
			return super.getOpenedIcon(arg0);
		else
			return super.getIcon(arg0);
	}

	@Override
	public Image getOpenedIcon(int arg0) {
		return getIcon(arg0);
	}
	
	@Override
	public Action[] getActions(boolean arg0)
	{
		ArrayList<Action> l=new ArrayList<Action>();
		if(isLoaded())
		{			
			l.add(SystemAction.get(ExplodeAction.class));
			l.add(SystemAction.get(ViewAction.class));
			l.add(SystemAction.get(NewAction.class));
			l.add(SystemAction.get(BooleanAction.AllActions.class));
			l.add(SystemAction.get(TransformAction.AllActions.class));
			l.add(SystemAction.get(ShapeNode.UpgradeActions.class));
			l.add(SystemAction.get(FreeBoundsAction.class));
			l.add(SystemAction.get(BoundingBoxAction.class));

			if(GeomUtils.getShape(this).getType()==TopAbs_ShapeEnum.FACE)
			{
				l.add(SystemAction.get(ReverseAction.class));
			}
		}
		else
			l.add(SystemAction.get(OpenAction.class));
		l.add(null);
		l.add(SystemAction.get(RenameAction.class));
		l.add(SystemAction.get(CutAction.class));
		l.add(SystemAction.get(CopyAction.class));
		l.add(SystemAction.get(DeleteAction.class));
		if(isLoaded())
		{
			l.add(null);
			l.add(closeAction);
		}
		return l.toArray(new Action[l.size()]);
	}

	@Override
	public Action getPreferredAction() {
		if(isLoaded())
			return SystemAction.get(OpenAction.class);
		else
			return null;
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
	
	@Override
	public String getName()
	{
		return getDataObject().getPrimaryFile().getName();
	}
	
	@Override
	public String getDisplayName()
	{
		return getDataObject().getPrimaryFile().getName();
	}
	
	@Override
	public Sheet createSheet()
	{
		Sheet sheet=super.createSheet();
		if(isLoaded())
		{
			Sheet.Set set=new Sheet.Set();
			set.put(new PropertySupport.ReadOnly<Double>(
				"tolerance", Double.class, "tolerance", "tolerance")
				{	
					public Double getValue()
					{
						NbShape s= ((BrepDataObject)getDataObject()).getShape();
						return s.getTolerance();
					}
				});
			set.setName("Geometry");
			sheet.put(set);
		}
		return sheet;
	}

	private void fireStateChange()
	{
		fireIconChange();
		fireOpenedIconChange();
		setSheet(createSheet());
	}
	public void open() {
		getLookup().lookup(BrepDataObject.class).load();
		instanceContent.add(getLookup().lookup(BrepDataObject.class).getShape());
		fireStateChange();
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
