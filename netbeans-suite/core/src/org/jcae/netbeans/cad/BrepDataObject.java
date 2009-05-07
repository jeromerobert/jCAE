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
 * (C) Copyright 2008,2009, by EADS France
 */

package org.jcae.netbeans.cad;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;
import org.openide.ErrorManager;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Node;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class BrepDataObject extends MultiDataObject implements SaveCookie
{
	public BrepDataObject(FileObject arg0, MultiFileLoader arg1)
		throws DataObjectExistsException
	{
		super(arg0, arg1);
	}

	@Override
	protected Node createNodeDelegate()
	{	
		return new BrepNode(this);
	}

	protected NbShape shape;	
	
	public NbShape getShape()
	{		
		if(!isLoaded())
			throw new IllegalStateException(
				"BRep file not loaded. Call BrepataObject.load() first");
		return shape;
	}

	public void save()
	{
		try
		{
			FileObject pf = getPrimaryFile();
			if(shape!=null)
			{		
				String fileName = FileUtil.toFile(pf).getPath();
				shape.saveImpl(fileName);
			}
			FileObject fo = pf.getParent().getFileObject(pf.getNameExt(), "xml");
			if(fo == null)
				fo = pf.getParent().createData(pf.getNameExt(), "xml");
			FileLock lock = fo.lock();
			OutputStream os = fo.getOutputStream(lock);
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
			shape.dump(pw);
			pw.close();
			lock.releaseLock();
			setModified(false);
		}
		catch(IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
	}
	
	public void load() throws IOException, SAXException
	{		
		String fileName = FileUtil.toFile(getPrimaryFile()).getPath();
		NbShape aShape = new NbShape(fileName);
		aShape.setNode(getNodeDelegate());		
		Set<MultiDataObject.Entry> se = secondaryEntries();
		if(!se.isEmpty())
		{
			InputStream in = se.iterator().next().getFile().getInputStream();			
			Document d = XMLUtil.parse(new InputSource(in), false, false, null, null);
			d.normalizeDocument();
			aShape.load(d.getDocumentElement());
			in.close();			
		}
		//Do the affectation at the end, to ensure that if a error occure nothing
		//is done
		shape=aShape;
	}
	
	public void unload()
	{
		shape = null;
	}
	
	public boolean isLoaded()
	{
		return shape!=null;
	}
}
