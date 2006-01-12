package org.jcae.netbeans.mesh;

import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.nodes.CookieSet;
import org.openide.nodes.Node;
import org.openide.text.DataEditorSupport;

public class UNVDataObject extends MultiDataObject
{
	
	public UNVDataObject(FileObject pf, UNVDataLoader loader) throws DataObjectExistsException, IOException
	{
		super(pf, loader);
		CookieSet cookies = getCookieSet();
		cookies.add((Node.Cookie) DataEditorSupport.create(this, getPrimaryEntry(), cookies));
	}
	
	protected Node createNodeDelegate()
	{
		return new UNVDataNode(this);
	}
	
}
