/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2007, by EADS France

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

package org.jcae.mesh.amibe.validation;

import org.jcae.mesh.cad.CADShapeEnum;
import org.jcae.mesh.cad.CADShapeFactory;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADExplorer;
import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.xmldata.MeshReader;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import java.io.IOException;

public class DistanceSurface extends QualityProcedure
{
	private final CADGeomSurface [] surfaces;
	private final TObjectDoubleHashMap<Vertex> distanceMap = new TObjectDoubleHashMap<Vertex>();

	private DistanceSurface(String file)
	{
		CADShape shape = CADShapeFactory.getFactory().newShape(file);
		CADExplorer expF = CADShapeFactory.getFactory().newExplorer();
		int nrFaces = 0;
		for (expF.init(shape, CADShapeEnum.FACE); expF.more(); expF.next())
			nrFaces++;
		surfaces = new CADGeomSurface[nrFaces];
		nrFaces = 0;
		for (expF.init(shape, CADShapeEnum.FACE); expF.more(); expF.next())
		{
			CADFace F = (CADFace) expF.current();
			surfaces[nrFaces] = F.getGeomSurface();
			nrFaces++;
		}
	}
	
	@Override
	protected void setValidationFeatures()
	{
		usageStr = new String[]{"DistanceSurface", "for a triangle, largest distance between its vertices and original surface"};
		type = QualityProcedure.FACE;
	}

	@Override
	public float quality(Object o)
	{
		if (!(o instanceof Triangle))
			throw new IllegalArgumentException();
		
		Triangle f = (Triangle) o;
		CADGeomSurface surface = surfaces[f.getGroupId()];
		double dmax = 0.0;
		double[] tmp = new double[3];
		for (int i = 0; i < 3; i++)
		{
			Vertex n = f.vertex[i];
			double d;
			if (distanceMap.contains(n))
				d = distanceMap.get(n);
			else
			{
				n.get(tmp);
				d = surface.lowerDistance(tmp);
				distanceMap.put(n, d);
			}
			if (d > dmax)
				dmax = d;
		}
		return (float) dmax;
	}
	
	@Override
	public void finish()
	{
		distanceMap.clear();
	}

	public static void main(String[] args)
	{
		DistanceSurface qproc = new DistanceSurface(args[1]);
		Mesh mesh = new Mesh(qproc.getMeshTraitsBuilder());
		try
		{
			MeshReader.readObject3D(mesh, args[0]);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		qproc.distanceMap.ensureCapacity(mesh.getTriangles().size() / 2);

		QualityFloat data = new QualityFloat(mesh.getTriangles().size());
		data.setQualityProcedure(qproc);
		for (Triangle f: mesh.getTriangles())
		{
			if (f.isWritable())
				data.compute(f);
		}
		data.finish();
		data.printStatistics();
	}
}
