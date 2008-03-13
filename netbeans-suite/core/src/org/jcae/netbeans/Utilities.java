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

package org.jcae.netbeans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.api.project.Project;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.propertysheet.PropertySheet;
import org.openide.filesystems.FileObject;
import org.openide.nodes.BeanNode;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

public class Utilities
{
	public static String getFreeName(FileObject object, String prefix, String suffix)
	{
		FileObject[] fos=object.getChildren();
		Set<String> names=new HashSet<String>();
		for(int i=0; i<fos.length; i++)
		{
			names.add(fos[i].getNameExt());
		}
		int i=0;
		String name;
		do
		{
			i++;
			name=prefix+i+suffix;
		}
		while(names.contains(name));
		return name;
	}
	
	static public Project getProject(Node n)
	{
		Object p = n.getLookup().lookup(Project.class);
		if(p==null)
			return getProject(n.getParentNode());
		else
			return (Project) p;
	}
	
	public static boolean showEditBeanDialog(Object bean)
	{
		try
		{
			BeanNode bn = new BeanNode(bean)
			{
				protected void createProperties(Object bean, BeanInfo info)				
				{
					super.createProperties(bean, info);
					getSheet().get(Sheet.PROPERTIES).remove("class");				
				}
			};
			PropertySheet ps=new PropertySheet();
			ps.setNodes(new Node[]{bn});
			String title=Introspector.getBeanInfo(bean.getClass()).getBeanDescriptor().getDisplayName();
			DialogDescriptor dd=new DialogDescriptor(ps, title);
			DialogDisplayer.getDefault().createDialog(dd).setVisible(true);
			return dd.getValue()==NotifyDescriptor.OK_OPTION;

		} catch (IntrospectionException e)
		{
			org.openide.ErrorManager.getDefault().notify(e);
			return false;
		}
	}

	public static String absoluteFileName(String geometryFile, String reference)
	{
		File f=new File(geometryFile);
		if(f.isAbsolute())
		{
			return geometryFile;
		}
		else
		{
			return new File(reference, geometryFile).getPath();
		}
	}
	
	static public String pretty (String s)
	{
		StringBuilder res;
		if (s.indexOf (' ') == -1 && s.length () > 0)
		{
			res = new StringBuilder ();
			res.insert (0, Character.toUpperCase (s.charAt (0)));
			for (int k = 1; k < s.length (); k++)
			{
				boolean minBeforeOrAfter=Character.isLowerCase (s.charAt (k-1));
				if(k<s.length ()-1)
					minBeforeOrAfter = minBeforeOrAfter || Character.isLowerCase (s.charAt (k+1));
				if (Character.isUpperCase (s.charAt (k))&&minBeforeOrAfter)
					res.insert (res.length (), ' ');
				res.insert (res.length (), s.charAt (k));
			}
			return res.toString ();
		} else return s;
	}

	public static String removeExt(String fileName)
	{
		int i=fileName.lastIndexOf('.');
		return fileName.substring(0, i);
	}
	
	public static ExplorerManager[] getExplorerManagers()
	{
		ArrayList<ExplorerManager> al=new ArrayList<ExplorerManager>();
		
		for(Mode m:WindowManager.getDefault().getModes())
		{
			for(TopComponent t:m.getTopComponents())
				if(t instanceof ExplorerManager.Provider)
					al.add(((ExplorerManager.Provider)t).getExplorerManager());
		}
		return al.toArray(new ExplorerManager[al.size()]);
	}	
}
