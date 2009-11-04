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

import java.io.File;
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
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
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

	/**
	 * Copy/past need a DataObject.
	 * To be able to paste a TopoDS_Shape we create a dummy DataObject in memory
	 * which wrap it.
	 */
	public static BrepDataObject createInMemory(NbShape shape)
	{
		try {
			FileSystem fs = FileUtil.createMemoryFileSystem();
			FileObject fob = fs.getRoot().createData(shape.getName(), ".brep");
			BrepDataObject bdo = (BrepDataObject) DataObject.find(fob);
			bdo.shape = shape;
			bdo.memory = true;
			return bdo;
		} catch (IOException ex) {
			//should never happen on a memory file system
			Exceptions.printStackTrace(ex);
			return null;
		}
	}
	@Override
	protected Node createNodeDelegate()
	{	
		return new BrepNode(this);
	}

	protected NbShape shape;	
	private boolean memory;
	
	public NbShape getShape()
	{		
		if(!isLoaded())
			throw new IllegalStateException(
				"BRep file not loaded. Call BrepataObject.load() first");
		return shape;
	}

	@Override
	protected DataObject handleCopy(DataFolder df) throws IOException {
		if(memory)
		{
			FileObject f = df.getPrimaryFile();
			String s = FileUtil.findFreeFileName(f, shape.getName(), "brep")+".brep";
			String p = new File(FileUtil.toFile(f), s).getPath();
			shape.saveImpl(p);
			f.refresh(true);
			return DataObject.find(f.getFileObject(s));
		}
		else
			return super.handleCopy(df);
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
