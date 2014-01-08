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
 * (C) Copyright 2011, by EADS France
 */
package org.jcae.netbeans.mesh;

import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.jcae.vtk.AmibePolyDataReader;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.xml.sax.SAXException;
import vtk.vtkXMLPolyDataWriter;

public final class ExportVTKAction extends CookieAction
{
	@Override
	protected void performAction(Node[] activatedNodes)
	{
		JFileChooser jfc=new JFileChooser();
		AmibeDataObject meshNode=activatedNodes[0].getLookup().lookup(AmibeDataObject.class);
		File meshDir=new File(meshNode.getMeshDirectory());
		jfc.setCurrentDirectory(meshDir.getParentFile());
		jfc.setFileFilter(new FileFilter(){

			@Override
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().endsWith(".vtp");
			}

			@Override
			public String getDescription() {
				return "VTK PolyData (*.vtp)";
			}
		});
		if(jfc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
		{
			String vtpFile=jfc.getSelectedFile().getPath();
			if(!vtpFile.endsWith(".vtp"))
				vtpFile+=".vtp";
			try {
				AmibePolyDataReader reader = new AmibePolyDataReader(meshDir.getPath());
				vtkXMLPolyDataWriter writer = new vtkXMLPolyDataWriter();
				writer.SetInputData(reader.getPolyData());
				writer.SetFileName(vtpFile);
				writer.Write();
			} catch (IOException ex) {
				Exceptions.printStackTrace(ex);
			} catch (SAXException ex) {
				Exceptions.printStackTrace(ex);
			}
		}
	}

	@Override
	protected int mode()
	{
		return CookieAction.MODE_EXACTLY_ONE;
	}

	@Override
	public String getName()
	{
		return "Export as VTK";
	}

	@Override
	protected Class<?>[] cookieClasses()
	{
		return new Class<?>[] { AmibeDataObject.class };
	}
	
	@Override
	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}
	
	@Override
	protected boolean asynchronous()
	{
		return false;
	}
}

