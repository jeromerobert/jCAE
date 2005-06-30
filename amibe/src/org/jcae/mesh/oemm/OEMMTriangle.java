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

public class OEMMTriangle
{
	private static Logger logger=Logger.getLogger(OEMMTriangle.class);	
	public OEMMVertex [] vertex = new OEMMVertex[3];
	
	private boolean readable = false;
	private boolean writable = false;
	private boolean modified = false;
	private boolean deleted = false;
	
	public OEMMTriangle(OEMMVertex v0, OEMMVertex v1, OEMMVertex v2)
	{
		vertex[0] = v0;
		vertex[1] = v1;
		vertex[2] = v2;
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
