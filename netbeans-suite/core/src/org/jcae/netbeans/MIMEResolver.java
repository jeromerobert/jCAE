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
 * (C) Copyright 2006-2009, by EADS France
 */

package org.jcae.netbeans;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import org.openide.filesystems.FileObject;

public class MIMEResolver extends org.openide.filesystems.MIMEResolver
{
	private final static Collection<String> EXTENSION=new HashSet<String>(
		Arrays.asList("step", "igs", "iges", "stp", "STEP", "STP", "IGS", "IGES"));
	
	private final static String MESH = "text/mesh+xml";
	public final static String CAD = "application/vnd.jcae.geometry";
	
	public MIMEResolver()
	{
		super(MESH, CAD);
	}
	
	public String findMIMEType(FileObject fileObject)
	{
		if(EXTENSION.contains(fileObject.getExt()))
			return CAD;
		else if(fileObject.getNameExt().endsWith("_mesh.xml"))
			return MESH;
		else
			return null;
	}	
}
