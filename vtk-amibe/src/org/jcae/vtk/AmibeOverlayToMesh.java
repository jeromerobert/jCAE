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
 * (C) Copyright 2008, by EADS France
 */

package org.jcae.vtk;


import java.awt.Color;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author Julian Ibarz
 */
public class AmibeOverlayToMesh
{
	private final static Logger LOGGER=Logger.getLogger(AmibeOverlayToMesh.class.getName());

	private final OldMesh mesh;

	public AmibeOverlayToMesh(AmibeOverlayProvider provider)
	{
		mesh = new OldMesh(1);
		mesh.setGroup(0, new BeamData(provider, 0));
	}

	public OldMesh getMesh()
	{
		return mesh;
	}
	
	private static class BeamData extends LeafNode.DataProvider
	{
		private final AmibeOverlayProvider provider;
		private final int id;
		
		BeamData(AmibeOverlayProvider provider, int id)
		{
			this.provider = provider;
			this.id = id;
		}

		@Override
		public void load()
		{
			AmibeBeanDomain domain = null;
			try {
				domain = new AmibeBeanDomain(provider.getDirectory(), provider.getSubMesh(), Color.BLACK);
			}
			catch(IOException e)
			{
				LOGGER.severe("Cannot load node " + id + 
					" from file " + provider.getDirectory()
					+ e.getLocalizedMessage());
			}
			setNodes(domain.getNodes());
			setLines(Utils.createBeamCells(domain.getBeam2()));
		}
	}
}
