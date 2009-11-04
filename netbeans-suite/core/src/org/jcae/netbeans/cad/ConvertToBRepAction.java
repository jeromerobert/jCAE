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
 * (C) Copyright 2005-2009, by EADS France
 */

package org.jcae.netbeans.cad;

import java.io.File;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.jcae.opencascade.jni.*;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

public class ConvertToBRepAction extends CookieAction
{   
	private static final FileFilter stepFileFilter = new FileFilter()
	{
		public boolean accept(File f)
		{
			if (f.isDirectory()) return true;
			String name = f.getName();
			return name.endsWith(".step") || name.endsWith(".STEP")
				|| name.endsWith(".stp") || name.endsWith(".STP");
		}

		public String getDescription()
		{
			return "STEP files (*.step, *.stp)";
		}
	};   
	
	private static void importGeometry(File fileName, String outputName)
	{
		ProgressHandle h;
		try
		{
			h = ProgressHandleFactory.createHandle("Reading "+fileName);
			h.start();
			TopoDS_Shape brepShape;
			if (stepFileFilter.accept(fileName))
			{
				STEPControl_Reader aReader = new STEPControl_Reader();
				aReader.readFile(fileName.getPath().getBytes());
				aReader.nbRootsForTransfer();
				aReader.transferRoots();
				brepShape = aReader.oneShape();
			}
			else
			{
				IGESControl_Reader aReader = new IGESControl_Reader();
				aReader.readFile(fileName.getPath().getBytes());
				aReader.nbRootsForTransfer();
				aReader.transferRoots();
				brepShape = aReader.oneShape();
			}

			h.finish();

			BRepTools.write(brepShape, outputName);
			JOptionPane.showMessageDialog(
				null,
				"The geometry has been converted to millimeter."+
				"Use the bounding box command to know its new dimensions.",
				"Geometry converted to mm",
				JOptionPane.WARNING_MESSAGE);
		}
		catch(Exception ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
	}

	@Override
	protected int mode() {
		return MODE_EXACTLY_ONE;
	}

	@Override
	protected Class<?>[] cookieClasses() {
		return new Class[] { CADDataLoader.CADDataObject.class };
	}

	@Override
	protected void performAction(Node[] activatedNodes) {
		FileObject f = activatedNodes[0].getLookup().lookup(
			CADDataLoader.CADDataObject.class).getPrimaryFile();
		FileObject p = f.getParent();
		String s = FileUtil.findFreeFileName(p, f.getName(), "brep");
		File out = new File(FileUtil.toFile(p), s+".brep");
		importGeometry(FileUtil.toFile(f), out.getPath());
		p.refresh(true);
	}

	@Override
	public String getName() {
		return "Convert to BRep";
	}

	@Override
	public HelpCtx getHelpCtx() {
		return HelpCtx.DEFAULT_HELP;
	}
}
