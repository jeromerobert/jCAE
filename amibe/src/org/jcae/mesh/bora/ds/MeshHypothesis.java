/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

   (C) Copyright 2006, by EADS CRC

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


package org.jcae.mesh.bora.ds;

//  This class is useful to improve error reporting in Mesh.computeHypothesis().
public class MeshHypothesis
{
	protected Mesh mesh = null;
	protected Hypothesis hypothesis = null;

	public MeshHypothesis(Mesh m, Hypothesis h)
	{
		mesh = m;
		hypothesis = h;
	}
	public Mesh getMesh()
	{
		return mesh;
	}
	public Hypothesis getHypothesis()
	{
		return hypothesis;
	}

	public String toString()
	{
		String ret = ""+hypothesis.getClass().getName()+"@"+Integer.toHexString(hypothesis.hashCode());
		ret += " "+mesh.getClass().getName()+"@"+Integer.toHexString(mesh.hashCode());
		return ret;
	}
}
