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
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.viewer3d.cad;

import org.jcae.viewer3d.DomainProvider;

/**
 * The CADProvider can work in 3 way:
 * <h3>CAD group mode</h3>
 * The provider return one Domain for each CAD group.
 * In the Opencascade world a CAD group is a TopoDS_Compound, TopoDS_CompSolid,
 * TopoDS_Solid or TopoDS_Shell.
 * <h3>Type mode</h3>
 * The provider return 3 domains:
 * <ul>
 * 	<li>One domain for vertices</li>
 *  <li>One domain for edges</li>
 *  <li>One domain for faces</li>
 * </ul>
 * <h3>Mixed mode</h3>
 * The number of domain is 3 times the number of CAD groups. For each CAD group
 * one domain for vertices, edges and faces is returned.
 * @author Jerome Robert
 * @todo add methods to allow to switch between group, type and mixed mode.
 */
public interface CADProvider extends DomainProvider 
{
}
