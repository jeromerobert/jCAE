/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

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

package org.jcae.mesh.oemm;

import org.apache.log4j.Logger;

public class Triangle3d
{
	private static Logger logger=Logger.getLogger(Triangle3d.class);	
	
	// Integer coordinates
	public Vertex3d [] v = new Vertex3d[3];
	private boolean readable = false;
	private boolean writable = false;
	private boolean modified = false;
	private boolean deleted = false;
	
	public Triangle3d(Vertex3d v0, Vertex3d v1, Vertex3d v2)
	{
		v[0] = v0;
		v[1] = v1;
		v[2] = v2;
	}
	
	public void setReadable(boolean r)
	{
		readable = r;
	}
	
	public void setWritable(boolean w)
	{
		writable = w;
	}
	
	public boolean isReadable()
	{
		return readable;
	}
	
	public boolean isWritable()
	{
		return writable;
	}
}
