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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.mesher.ds.tools;

import org.jcae.mesh.mesher.ds.MNode2D;
import org.jcae.mesh.mesher.ds.MEdge2D;
import org.jcae.mesh.mesher.ds.MFace2D;

abstract public class Calculus
{
	abstract public double angle(MNode2D n0, MNode2D n1, MNode2D n2);
	abstract public double distance(MNode2D n0, MNode2D n1);
	abstract public double quality(MNode2D n0, MNode2D n1, MNode2D n2);
	abstract public double qualityAniso(MNode2D n0, MNode2D n1, MNode2D n2);
	abstract public double quality(MFace2D f);
	abstract public double length(MEdge2D edge);

}
