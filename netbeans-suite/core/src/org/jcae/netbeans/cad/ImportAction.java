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

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.jcae.netbeans.Utilities;
import org.jcae.opencascade.jni.*;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.MainProjectSensitiveActions;
import org.netbeans.spi.project.ui.support.ProjectActionPerformer;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;

public class ImportAction extends CallableSystemAction
{
	private static FileFilter brepFileFilter=new FileFilter()
	{
		public boolean accept(File f)
		{
			if (f.isDirectory()) return true;
			String name = f.getName();
			return name.endsWith(".brep") || name.endsWith(".BREP");
		}
		public String getDescription()
		{
			return "BREP files (*.brep, *.BREP)";
		}
	};

	private static FileFilter igesFileFilter=new FileFilter()
	{
	   public boolean accept(File f)
	   {
		   if (f.isDirectory()) return true;
			   String name = f.getName();
		   return name.endsWith(".igs") || name.endsWith(".IGS") ||
			   name.endsWith(".iges") || name.endsWith(".IGES");
	   }

	   public String getDescription()
	   {
		   return "IGES files (*.iges, *.igs)";
	   }
   };
   
	private static FileFilter stepFileFilter = new FileFilter()
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
	
	public void importGeometry(FileObject outputDir)
	{
		ProgressHandle h;
		try
		{
			JFileChooser chooser = new JFileChooser();
			chooser.setFileFilter(igesFileFilter);
			chooser.addChoosableFileFilter(brepFileFilter);		
			chooser.addChoosableFileFilter(stepFileFilter);
			int ret = chooser.showOpenDialog(null);
			boolean warnUnit=false;
			if (ret == JFileChooser.APPROVE_OPTION)
			{				
				String fileName = chooser.getSelectedFile().getPath();
				h = ProgressHandleFactory.createHandle("Reading "+fileName);
				h.start();
		        TopoDS_Shape brepShape;
		        if (stepFileFilter.accept(chooser.getSelectedFile()))
		        {
		            STEPControl_Reader aReader = new STEPControl_Reader();
		            aReader.readFile(fileName);
		            aReader.nbRootsForTransfer();
		            aReader.transferRoots();
		            brepShape = aReader.oneShape();
					warnUnit=true;
		        }
		        else if (igesFileFilter.accept(chooser.getSelectedFile()))
		        {
		            IGESControl_Reader aReader = new IGESControl_Reader();
		            aReader.readFile(fileName);
		            aReader.nbRootsForTransfer();
		            aReader.transferRoots();
		            brepShape = aReader.oneShape();
					warnUnit=true;
		        }
		        else
		            brepShape = BRepTools.read(fileName, new BRep_Builder());				
				h.finish();
				
		        String outputName=Utilities.getFreeName(
					outputDir,
					Utilities.removeExt(chooser.getSelectedFile().getName()),
					".brep");
				outputName=new File(FileUtil.toFile(outputDir), outputName).getPath();
				
				BRepTools.write(brepShape, outputName);
				if(warnUnit)
				{
					JOptionPane.showMessageDialog(
						null,
						"The geometry has been converted to millimeter."+
						"Use the bounding box command to know its new dimensions.",
						"Geometry converted to mm",
						JOptionPane.WARNING_MESSAGE);
				}
			}
		}
		catch(Exception ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
	}

	public void performAction()
	{
		MainProjectSensitiveActions.mainProjectSensitiveAction(new ProjectActionPerformer()
		{
			public boolean enable(Project arg0)
			{
				return true;
			}

			public void perform(Project arg0)
			{
				importGeometry(arg0.getProjectDirectory());
			}
		}, null, null).actionPerformed(null);
	}

	public String getName()
	{
		return "Import geometry";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}

	@Override
	protected boolean asynchronous()
	{
		return true;
	}
}
