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
 * (C) Copyright 2009, by EADS France
 */

package org.jcae.vtk;


import java.awt.Color;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.vtk.LeafNode.DataProvider;
import org.jcae.vtk.AbstractNode.ActorCustomiser;
import vtk.vtkActor;
import vtk.vtkProperty;

/**
 *
 * @author Jerome Robert
 */
public class AmibeOverlayToMesh extends Viewable
{
	private final static Logger LOGGER = Logger.getLogger(
		AmibeOverlayToMesh.class.getName());
	
	public AmibeOverlayToMesh(final AmibeOverlayProvider provider, Color color)
	{
		DataProvider dataProvider = new LeafNode.DataProvider(){
			@Override
			public void load()
			{
				try {
					AmibeBeanDomain domain = new AmibeBeanDomain(provider.getDirectory(),
						provider.getSubMesh(), Color.BLACK);
					setNodes(domain.getNodes());
					setLines(Utils.createBeamCells(domain.getBeam2()));
				} catch (IOException ex) {
					LOGGER.log(Level.SEVERE, null, ex);
				}
			}
		};
		rootNode.setManager(true);		
		rootNode.setActorCustomiser(new ActorCustomiser() {
			public void customiseActor(vtkActor actor) {
				vtkProperty p = actor.GetProperty();
				p.SetLineWidth(5.0);
				p.Delete();
			}
		});
		new LeafNode(rootNode, dataProvider, color);
		rootNode.refresh();
	}
}
