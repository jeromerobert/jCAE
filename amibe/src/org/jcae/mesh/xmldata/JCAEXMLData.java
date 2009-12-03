/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.xmldata;

public interface JCAEXMLData
{
	String xml1dFilename = "jcae1d";
	String xml2dFilename = "jcae2d.";  // Face number is appended to this string
	String xml3dFilename = "jcae3d";

	String nodes1dFilename = "nodes1d.bin";
	String ref1dFilename = "nodes1dref.bin";
	String beams1dFilename = "beams1d.bin";
	
	String nodes2dFilename = "nodes2d.bin";
	String triangles2dFilename = "triangles2d.bin";
	
	String nodes3dFilename = "nodes3d.bin";
	String normals3dFilename = "normals3d.bin";
	String triangles3dFilename = "triangles3d.bin";
	String groupsFilename = "groups.bin";
}
