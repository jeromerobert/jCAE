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

package org.jcae.netbeans.cad;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import org.jcae.opencascade.jni.*;
import org.openide.ErrorManager;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.FileEntry;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Node;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class BrepDataObject extends MultiDataObject implements ShapeCookie, SaveCookie
{
	public BrepDataObject(FileObject arg0, MultiFileLoader arg1) throws DataObjectExistsException
	{
		super(arg0, arg1);
	}

	protected Node createNodeDelegate()
	{
		return new BrepNode(this);
	}

	protected TopoDS_Shape shape;
	private Document metaDocument;
	
	public TopoDS_Shape getShape()
	{
		if(shape==null)
			open();
		return shape;
	}

	public void save()
	{
		try
		{
			if(shape!=null)
			{		
				String fileName = FileUtil.toFile(getPrimaryFile()
					).getPath();
				BRepTools.write(shape, fileName);
			}
			Set set = secondaryEntries();
			if(set.size()>0)
			{
				FileEntry fe=(FileEntry) set.toArray()[0];
				FileObject fo = fe.getFile();
				FileLock lock = fo.lock();
				OutputStream os = fo.getOutputStream(lock);
				XMLUtil.write(metaDocument, os,"UTF-8");
				os.close();
				lock.releaseLock();
			}
			setModified(false);
		}
		catch(IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
	}
	
	protected void open()
	{		
		String fileName = FileUtil.toFile(getPrimaryFile()).getPath();
        
        if (fileName.endsWith("step") || fileName.endsWith("stp"))
        {
            STEPControl_Reader aReader = new STEPControl_Reader();
            aReader.readFile(fileName);
            aReader.nbRootsForTransfer();
            aReader.transferRoots();
            shape = aReader.oneShape();
        }
        else if (fileName.endsWith("iges") || fileName.endsWith("igs"))
        {
            IGESControl_Reader aReader = new IGESControl_Reader();
            aReader.readFile(fileName);
            aReader.nbRootsForTransfer();
            aReader.transferRoots();
            shape = aReader.oneShape();
        }
        else
        	shape = BRepTools.read(fileName, new BRep_Builder());
        
        if(shape==null)
        {
        	TopoDS_Compound c=new TopoDS_Compound();
        	new BRep_Builder().makeCompound(c);
        	shape=c;
        }        
	}
	
	public Document getMetaDocument()
	{
		if(metaDocument==null)
		{
			try
			{
				Set set=secondaryEntries();
				if(set.size()==0)
				{
					return null;
				}
				
				FileEntry fe=(FileEntry) set.toArray()[0];
				InputStream in=fe.getFile().getInputStream();
				metaDocument=XMLUtil.parse(new InputSource(in), false, false, null, null);
			}
			catch (IOException e)
			{
				ErrorManager.getDefault().notify(e);
			}
			catch (SAXException e)
			{
				ErrorManager.getDefault().log(getClass()+" "+e.getMessage());
			}
			finally
			{
				metaDocument=XMLUtil.createDocument("jcae", null, null, null);
			}
		}			
		return metaDocument;
	}	
}
