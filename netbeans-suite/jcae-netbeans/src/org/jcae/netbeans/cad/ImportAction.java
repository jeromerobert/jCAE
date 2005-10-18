package org.jcae.netbeans.cad;

import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.jcae.opencascade.jni.*;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.MainProjectSensitiveActions;
import org.netbeans.spi.project.ui.support.ProjectActionPerformer;
import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;

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
	
	public void importGeometry(String output)
	{
		try
		{
			JFileChooser chooser = new JFileChooser();
			chooser.setFileFilter(igesFileFilter);
			chooser.addChoosableFileFilter(brepFileFilter);		
			chooser.addChoosableFileFilter(stepFileFilter);
			int ret = chooser.showOpenDialog(null);
			if (ret == JFileChooser.APPROVE_OPTION)
			{
				String fileName = chooser.getSelectedFile().getPath();
		        TopoDS_Shape brepShape;
		        if (stepFileFilter.accept(chooser.getSelectedFile()))
		        {
		            STEPControl_Reader aReader = new STEPControl_Reader();
		            aReader.readFile(fileName);
		            aReader.nbRootsForTransfer();
		            aReader.transferRoots();
		            brepShape = aReader.oneShape();
		        }
		        else if (igesFileFilter.accept(chooser.getSelectedFile()))
		        {
		            IGESControl_Reader aReader = new IGESControl_Reader();
		            aReader.readFile(fileName);
		            aReader.nbRootsForTransfer();
		            aReader.transferRoots();
		            brepShape = aReader.oneShape();
		        }
		        else
		            brepShape = BRepTools.read(fileName, new BRep_Builder());

		        BRepTools.write(brepShape, output);
		        
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
				try
				{
					File dir=FileUtil.toFile(arg0.getProjectDirectory());
					File f = File.createTempFile("Geometry",".brep", dir);
					importGeometry(f.getPath());
				}
				catch (IOException e)
				{
					ErrorManager.getDefault().notify(e);
				}				
			}}, null, null).actionPerformed(null);
	}

	public String getName()
	{
		return "Import geometry";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}
}
