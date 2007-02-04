/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005,2006, by EADS CRC
 
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.bora.algo;

import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.algos2d.*;
import org.jcae.mesh.amibe.metrics.Metric2D;
import org.jcae.mesh.amibe.metrics.Metric3D;
import org.jcae.mesh.mesher.ds.MNode1D;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.cad.*;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * @see org.jcae.mesh.amibe.algos2d.BasicMesh
 */
public class Basic2d implements AlgoInterface
{
	private static Logger logger=Logger.getLogger(Basic2d.class);
	private double maxlen;
	private double deflection;
	private boolean relDefl;
	private boolean isotropic;

	public Basic2d(double len, double defl, boolean rel, boolean iso)
	{
		maxlen = len;
		deflection = defl;
		relDefl = rel;
		isotropic = iso;
	}

	public boolean isAvailable()
	{
		return true;
	}

	public int getOrientation(int o)
	{
		return o;
	}

	public boolean compute(BCADGraphCell mesh, BSubMesh s)
	{
		CADFace F = (CADFace) mesh.getShape();
		logger.debug(""+this+"  shape: "+F);
		Mesh2D m = new Mesh2D(F);
		mesh.setMesh(s, m);
		String xmlFile = "jcae1d";
		Metric2D.setLength(maxlen);
		Metric3D.setLength(maxlen);
		Metric3D.setDeflection(deflection);
		Metric3D.setRelativeDeflection(relDefl);
		Metric3D.setIsotropic(isotropic);

		// new BasicMesh(m, mesh.mesh1D).compute();

		// Insert interior vertices, if any
		ArrayList innerV = new ArrayList();
		for (Iterator it = mesh.shapesIterator(); it.hasNext(); )
		{
			BCADGraphCell sub = (BCADGraphCell) it.next();
			CADShape subV = sub.getShape();
			if (subV instanceof CADVertex)
			{
				MNode1D n = new MNode1D(0.0, (CADVertex) subV);
				innerV.add(n);
			}
		}
		new Initial(m, mesh.getMesh1D(s), innerV).compute();

		m.pushCompGeom(3);
		new Insertion(m, 16.0).compute();
		new ConstraintNormal3D(m).compute();
		new Insertion(m, 4.0).compute();
		new ConstraintNormal3D(m).compute();
		new Insertion(m).compute();
		new ConstraintNormal3D(m).compute();
		m.popCompGeom(3);
		
		new CheckDelaunay(m).compute();
		if (deflection > 0.0 && !relDefl)
			new EnforceAbsDeflection(m).compute();
		m.removeDegeneratedEdges();
		xmlFile = "jcae2d."+mesh.getId();
		String xmlBrepDir = ".";
		BModel model = mesh.getGraph().getModel();
		MeshWriter.writeObject(m, model.getOutputDir(s), xmlFile, xmlBrepDir, model.getCADFile(), mesh.getId());
		assert (m.isValid());
		return true;
	}
	
	public String toString()
	{
		String ret = "Algo: "+getClass().getName();
		ret += "\nTarget size: "+maxlen;
		ret += "\nDeflection: "+deflection;
		if (relDefl)
			ret += " (relative)";
		else
			ret += " (absolute)";
		return ret;
	}
}
