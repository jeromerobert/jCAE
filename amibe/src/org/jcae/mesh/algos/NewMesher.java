/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.algos;

import org.jcae.opencascade.jni.*;
import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.mesher.algos1d.*;
import org.jcae.mesh.mesher.algos2d.*;
import org.jcae.mesh.sd.MeshOfCAD;
import org.jcae.mesh.MeshConstraint;
import org.jcae.mesh.drivers.*;
import org.jcae.mesh.xmldata.*;
import org.jcae.mesh.cad.*;
import java.io.*;
import org.apache.log4j.Logger;

public class NewMesher
{
	private static Logger logger=Logger.getLogger(NewMesher.class);

	public 	NewMesher()
	{
	}
	
	/**
	 * Interface with the new mesher.
	 * This algo is an interface to the new mesher until it gets
	 * fully supported.
	 */
	public MeshOfCAD compute(MeshOfCAD mesh, MeshConstraint constraint)
	{
		TopoDS_Shape shape = mesh.getGeometry();
		CADShape tshape = CADShapeBuilder.factory.newShape(shape);
		double discr = constraint.getValue();
		
		String brepFile="unknown";
		String xmlDir = (new File(brepFile+".jcae")).getAbsolutePath();
		String xmlFile = "jcae1d";
		String xmlBrepDir = "..";
		
		org.jcae.mesh.mesher.metrics.Metric3D.setLength(discr);
		MMesh1D mesh1D = new MMesh1D(tshape);
		mesh1D.setMaxLength(discr);
		new UniformLength(mesh1D).compute();
		//  Store the 1D mesh onto disk
		//MMesh1DWriter.writeObject(mesh1D, xmlDir, xmlFile, xmlBrepDir, brepFile);
		//  Prepare 2D discretization
		mesh1D.duplicateEdges();
		
		MMesh3D mesh3D = new MMesh3D();
		CADExplorer expF = CADShapeBuilder.factory.newExplorer();
		int iFace = 0;
		for (expF.init(tshape, CADExplorer.FACE); expF.more(); expF.next())
		{
			CADFace F = (CADFace) expF.current();
			try
			{
				iFace++;
				SubMesh2D submesh = new SubMesh2D(F);
				submesh.pushCompGeom(2);
				submesh.init(mesh1D);
				new InnerRefine(submesh).compute();
				new InnerRefine(submesh).compute();
				submesh.popCompGeom(2);
				submesh.pushCompGeom(3);
				new TargetSize(submesh).compute();
				new ImproveDeflection(submesh).compute();
				// TODO: filename should be the brep filename
				xmlFile = "jcae2d."+iFace;
				SubMesh2DWriter.writeObject(submesh, xmlDir, xmlFile, xmlBrepDir, brepFile, iFace);
				mesh3D.addSubMesh2D(submesh, false);
				mesh3D.printInfos();
				submesh.popCompGeom(3);
			}
			catch(Exception ex)
			{
				logger.warn(ex.getMessage());
				ex.printStackTrace();
			}
		}
		xmlFile = "jcae3d";
		MMesh3DWriter.writeObject(mesh3D, xmlDir, xmlFile, xmlBrepDir);
		try 
		{
			String unvfile = File.createTempFile("jcae",".unv").getPath();
			mesh3D.writeUNV(unvfile);
			UNVReader unv = new UNVReader(new FileInputStream(unvfile), mesh);
			unv.readMesh();
		} catch (Exception e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}
		return mesh;
	}
}
