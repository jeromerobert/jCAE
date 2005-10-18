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
 * (C) Copyright 2004, by EADS CRC
 */

package org.jcae.netbeans.mesh;


public class Mesh 
{
	private double edgeLength=1.0;
	private double deflection=0;
	private String meshFile;
	private String geometryFile;
	
	public Mesh(String name) {
		meshFile=name;
	}
	
	public Mesh()
	{		
	}
	
	public double getDeflection() {
		return deflection;
	}
	public void setDeflection(double deflection) {
		this.deflection = deflection;
	}
	public double getEdgeLength() {
		return edgeLength;
	}
	public void setEdgeLength(double edgeLength) {
		this.edgeLength = edgeLength;
	}
	public String getGeometryFile() {
		return geometryFile;
	}
	public void setGeometryFile(String geometryFile) {
		this.geometryFile = geometryFile;
	}
	public String getMeshFile() {
		return meshFile;
	}
	public void setMeshFile(String meshFile) {
		this.meshFile = meshFile;
	}	
}
