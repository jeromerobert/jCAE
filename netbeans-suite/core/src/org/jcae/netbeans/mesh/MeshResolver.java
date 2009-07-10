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
 * (C) Copyright 2006, by EADS CRC
 */

package org.jcae.netbeans.mesh;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;

public class MeshResolver extends MIMEResolver
{
	public String findMIMEType(FileObject fileObject)
	{
		if(fileObject.getNameExt().endsWith("_mesh.xml"))
			return "text/mesh+xml";
		else if (fileObject.getNameExt().endsWith("_meshBora.xml"))
			return "text/meshBora+xml";
		else
			return null;
	}	
}
