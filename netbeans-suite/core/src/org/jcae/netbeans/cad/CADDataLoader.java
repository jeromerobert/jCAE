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
 * (C) Copyright 2009, by EADS France
 */

package org.jcae.netbeans.cad;

import org.openide.filesystems.FileObject;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.loaders.UniFileLoader;
import org.openide.nodes.Node;

/**
 *
 * @author Jerome Robert
 */
public class CADDataLoader extends UniFileLoader {

	
	public CADDataLoader() {
		super(CADDataObject.class.getName());
	}

	@Override
	protected void initialize()
	{
		super.initialize();
		getExtensions().addMimeType(MIMEResolver.CAD);
	}

	@Override
	protected MultiDataObject createMultiObject(FileObject primaryFile) throws DataObjectExistsException {
		return new CADDataObject(primaryFile, this);
	}

	public static class CADDataObject extends MultiDataObject
	{
		public CADDataObject(FileObject o, MultiFileLoader loader) throws DataObjectExistsException
		{
			super(o, loader);
		}

		@Override
		protected Node createNodeDelegate() {
			DataNode n = (DataNode) super.createNodeDelegate();
			n.setIconBaseWithExtension("org/jcae/netbeans/cad/CADNode.png");
			n.setDisplayName(getPrimaryFile().getName());
			return n;
		}
	}

	@Override
	protected String actionsContext()
	{
		return "Loaders/" + MIMEResolver.CAD + "/Actions";
	}
}
