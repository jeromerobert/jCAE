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

package org.jcae.viewer3d.fe.unv;

import org.jcae.viewer3d.Domain;
import org.jcae.viewer3d.fe.FEDomainAdapter;
import org.jcae.viewer3d.fe.FEProvider;

/**
 * A FEProvider which get data from an IDEAS UNV file.
 * @author Jerome Robert
 * @todo implements, add constructors...
 */
public class UNVProvider implements FEProvider
{

	/* (non-Javadoc)
	 * @see jcae.viewer3d.DomainProvider#getDomainIDs()
	 */
	public int[] getDomainIDs()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.DomainProvider#getDomain(int)
	 */
	public Domain getDomain(int id)
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
}
