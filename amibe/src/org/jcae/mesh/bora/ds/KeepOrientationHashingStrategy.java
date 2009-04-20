/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2009, by EADS France

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

import gnu.trove.TObjectHashingStrategy;
import org.jcae.mesh.cad.CADShape;

// In OccJava, two CADShape instances can be equal with different orientations.
// We sometimes need to keep track of shape orientation in our graph, hash
// sets and maps can then use an KeepOrientationHashingStrategy INSTANCE as hashing
// strategy.
class KeepOrientationHashingStrategy implements TObjectHashingStrategy<CADShape>
{
	private static final long serialVersionUID = -8044550982617929038L;

	// We need only one instance, make this class a Singleton.
	private static final KeepOrientationHashingStrategy INSTANCE = new KeepOrientationHashingStrategy();

	private KeepOrientationHashingStrategy() {}

	public static KeepOrientationHashingStrategy getInstance()
	{
		return INSTANCE;
	}

	public int computeHashCode(CADShape o)
	{
		return o.hashCode();
	}

	public boolean equals(CADShape s1, CADShape s2)
	{
		return s1 != null && s1.equals(s2) && s1.orientation() == s2.orientation();
	}
}
