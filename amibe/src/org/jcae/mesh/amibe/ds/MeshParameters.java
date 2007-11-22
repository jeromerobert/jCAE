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

package org.jcae.mesh.amibe.ds;

import java.util.Map;

/**
 * Mesh parameters.
 */
public class MeshParameters
{
	// By convention, Mesh2D.init() will set edgeLength to patch diagonal
	// if size has not been set.
	public double edgeLength = 0.0;
	public double deflection = -1.0;
	public boolean relativeDeflection = true;
	public boolean isotropic = true;
	public double epsilon = -1.0;
	public boolean cumulativeEpsilon = false;

	public MeshParameters()
	{
	}

	public MeshParameters(final Map<String, String> options)
	{
		// First process system properties
		String relDeflProp = System.getProperty("org.jcae.mesh.amibe.ds.Metric3D.relativeDeflection");
		if (relDeflProp == null)
		{
			relDeflProp="true";
			System.setProperty("org.jcae.mesh.amibe.ds.Metric3D.relativeDeflection", relDeflProp);
		}
		relativeDeflection = relDeflProp.equals("true");

		String accumulateEpsilonProp = System.getProperty("org.jcae.mesh.amibe.ds.Mesh.cumulativeEpsilon");
		if (accumulateEpsilonProp == null)
		{
			accumulateEpsilonProp = "false";
			System.setProperty("org.jcae.mesh.amibe.ds.Mesh.cumulativeEpsilon", accumulateEpsilonProp);
		}
		cumulativeEpsilon = accumulateEpsilonProp.equals("true");

		// Next process arguments
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("size"))
				edgeLength = Double.valueOf(val).doubleValue();
			else if (key.equals("deflection"))
				deflection = Double.valueOf(val).doubleValue();
			else if (key.equals("relativeDeflection"))
				relativeDeflection = Boolean.valueOf(val).booleanValue();
			else if (key.equals("isotropic"))
				isotropic = Boolean.valueOf(val).booleanValue();
			else if (key.equals("epsilon"))
				epsilon = Double.valueOf(val).doubleValue();
			else if (key.equals("cumulativeEpsilon"))
				cumulativeEpsilon = Boolean.valueOf(val).booleanValue();
			else
				throw new RuntimeException("Unknown option: "+key);
		}
	}

	public double getLength()
	{
		return edgeLength;
	}

	public void setLength(double e)
	{
		edgeLength = e;
	}

	public double getEpsilon()
	{
		return epsilon;
	}

	public void setEpsilon(double e)
	{
		epsilon = e;
	}

	public double getDeflection()
	{
		return deflection;
	}

	public boolean isIsotropic()
	{
		return isotropic;
	}

	public boolean hasDeflection()
	{
		return deflection > 0.0;
	}

	public boolean hasRelativeDeflection()
	{
		return relativeDeflection;
	}

	public boolean hasCumulativeEpsilon()
	{
		return cumulativeEpsilon;
	}

	public void scaleTolerance(double scale)
	{
		epsilon *= scale;
	}
	
}
