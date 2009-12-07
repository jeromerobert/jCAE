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
package org.jcae.netbeans.mesh.bora;

import java.beans.XMLEncoder;
import java.io.IOException;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.xmldata.Storage;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.netbeans.mesh.AmibeDataObject;
import org.jcae.netbeans.mesh.Mesh;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileSystem.AtomicAction;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

/**
 *
 * @author Jerome Robert
 */
public class ToAmibeAction extends CookieAction {

	@Override
	protected int mode() {
		return CookieAction.MODE_EXACTLY_ONE;
	}

	@Override
	protected Class<?>[] cookieClasses() {
		return new Class[] { BoraDataObject.class };
	}

	@Override
	protected void performAction(Node[] activatedNodes) {
		final BoraDataObject o = activatedNodes[0].getLookup().lookup(BoraDataObject.class);
		if(o.getBModel() == null)
			o.load();
		final BCADGraphCell root = o.getBModel().getGraph().getRootCell();
		try {
			o.getPrimaryFile().getFileSystem().runAtomicAction(new AtomicAction() {
				public void run() throws IOException {
					ToAmibeAction.this.run(root, o);
				}
			});
		} catch (IOException ex) {
			Exceptions.printStackTrace(ex);
		}
	}

	private void run(BCADGraphCell root, BoraDataObject o) throws IOException
	{
		org.jcae.mesh.amibe.ds.Mesh m = new org.jcae.mesh.amibe.ds.Mesh();
		Storage.readAll(m, root);
		String n = o.getName();
		FileObject dir = o.getPrimaryFile().getParent();
		FileObject xmlFile;
		FileObject mDir;
		int i = 0;
		do {
			i++;
			xmlFile = dir.getFileObject(n + i + "_mesh.xml");
			mDir = dir.getFileObject(n + i + ".amibe");
		} while (xmlFile != null || mDir != null);
		mDir = dir.createFolder(n + i + ".amibe");
		xmlFile = dir.createData(n + i + "_mesh.xml");
		MeshWriter.writeObject3D(m, FileUtil.toFile(mDir).getPath(), "");
		Mesh mesh = new Mesh();
		mesh.setMeshFile(FileUtil.toFile(mDir).getName());
		XMLEncoder encoder = new XMLEncoder(xmlFile.getOutputStream());
		encoder.writeObject(mesh);
		encoder.close();
	}
	
	@Override
	public String getName() {
		return "Convert to Amibe";
	}

	@Override
	public HelpCtx getHelpCtx() {
		return HelpCtx.DEFAULT_HELP;
	}

}
