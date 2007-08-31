/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC

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

package org.jcae.mesh;

import gnu.trove.TIntProcedure;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.media.j3d.BoundingBox;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.Storage;
import org.jcae.viewer3d.FPSBehavior;
import org.jcae.viewer3d.OEMMBehavior;
import org.jcae.viewer3d.OEMMViewer;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.bg.ViewableBG;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;

public class MeshOEMMCoarseViewer
{
	private static boolean showOctree = false;
	private static Logger logger = Logger.getLogger(MeshOEMMCoarseViewer.class);
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		//Logger.getLogger(OEMMBehavior.class).setLevel(Level.DEBUG);
		if (args.length < 1 || !new File(args[0]).exists() || !new File(args[0] + ".decimated").exists()) {
			System.out.println("MeshOEMMCoarseViewer oemm  (file {oemm}, {oemm}.decimated should exist");
		}
		JFrame f=new JFrame("jcae-viewer3d-fd demo");
		f.setSize(800,600);
		f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		final View view=new View(f);
		f.getContentPane().add(view);		
		
		final OEMM oemm = Storage.readOEMMStructure(args[0]);
		String decimatedPath = args.length > 1?args[1]:args[0] + ".decimated";
		final OEMM decimatedOemm = Storage.readOEMMStructure(decimatedPath);


		BranchGroup bg=new BranchGroup();
		final OEMMBehavior oemmBehavior=new OEMMBehavior(view, oemm, decimatedOemm);
		
		bg.addChild(oemmBehavior);
		final ViewableBG octree = new ViewableBG(OEMMViewer.bgOEMM(oemm, true));
		
		view.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event)
			{
				if(event.getKeyChar() == 'o')
				{
					showOctree = !showOctree;
					if (showOctree)
					{
						view.add(octree);
						view.setCurrentViewable(octree);
					}
					else
						view.remove(octree);
				}
				else if (event.getKeyChar() == 'i')
				{
					Set<Integer> result = new HashSet<Integer>();
					
					for(int i:octree.getResultSet().toArray()) {
						result.add(i);
					}
					if (logger.isInfoEnabled()) {
						logger.info("Selected: " + result);
					}
				}
				else if (event.getKeyChar() == 'v')
				{
					octree.unselectAll();
					for (int i: oemmBehavior.getIds()) {
						octree.highlight(i, true);	
					}
				}
				else if (event.getKeyChar() == 'f')
				{
					oemmBehavior.switchFreeze();
				}
				else if (event.getKeyChar() == 'n')
				{
					octree.getResultSet().forEach(new TIntProcedure() {

						@Override
						public boolean execute(int arg0) {
							Mesh mesh = Storage.loadNodes(oemm, arg0);
							Mesh coarseMesh = Storage.loadNodes(decimatedOemm, arg0);
							Triangle triangle = (Triangle) mesh.getTriangles().iterator().next();
							
							double[] tempn1 = getTriangleNormal(triangle);
							double[] tempn2 = getTriangleNormal((Triangle) coarseMesh.getTriangles().iterator().next());
							System.out.println("Coarse normal for leaf: " + arg0 + " [" 
									+ tempn1[0] + ", "+ tempn1[1] + ", "+ tempn1[2] + "] and fine " +
									" [" 
									+ tempn2[0] + ", "+ tempn2[1] + ", "+ tempn2[2] + "]" + " and "
									+ " orientation: " + (tempn1[0]*tempn2[0] + tempn1[1]*tempn2[1] + tempn1[2]*tempn2[2]  ));
							return true;
						}
					});
				}
				else if (event.getKeyChar() == 'p')
				{
					octree.getResultSet().forEach(new TIntProcedure() {

						@Override
						public boolean execute(int arg0) {
							Point3d vector = oemmBehavior.getVoxel(arg0);

							if (logger.isInfoEnabled()) {
								logger.info("Node: " + arg0 + ", vector: [" + vector.x + ", " + vector.y + ", " + vector.z + "]");
							}
							return true;
						}
						
					});
					if (logger.isInfoEnabled()) {
						logger.info("Visible oemm nodes: " + oemmBehavior.getNumberOfVisibleFineElements() + ", cache: " + oemmBehavior.getNumberOfCacheNodes());
					}
				}
			}
		});
		FPSBehavior fps = new FPSBehavior();
		fps.setSchedulingBounds(new BoundingSphere(
				new Point3d(), Double.MAX_VALUE));
		fps.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (logger.isInfoEnabled()) {
					logger.info("FPS>" + evt.getNewValue());
				}
			}
			
		});
		bg.addChild(fps);
		
		view.addBranchGroup(bg);
		view.setOriginAxisVisible(true);
		view.fitAll();
		f.setVisible(true);
		
	}
	private static double[] getTriangleNormal(Triangle triangle)
	{
		double []tempD1 = new double[3];
		double []tempD2 = new double[3];
		double []tempD3 = new double[3];
		Matrix3D.computeNormal3D(triangle.vertex[0].getUV(), triangle.vertex[1].getUV(), triangle.vertex[2].getUV(), tempD1, tempD2, tempD3);
		return tempD3;
	}

}
