/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
    Copyright (C) 2008, by EADS France

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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.vecmath.Point3d;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.Storage;
import org.jcae.mesh.oemm.MeshReader;
import org.jcae.viewer3d.FPSBehavior;
import org.jcae.viewer3d.OEMMBehavior;
import org.jcae.viewer3d.OEMMViewer;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.bg.ViewableBG;

public class MeshOEMMCoarseViewer
{
	static boolean showOctree = false;
	static boolean showAxis = true;
	static boolean showFPS = true;
	static Logger logger = Logger.getLogger(MeshOEMMCoarseViewer.class.getName());
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		//Logger.getLogger(OEMMBehavior.class).setLevel(Level.DEBUG);
		String decimatedPath = args.length > 1 ? args[1] : args[0] + ".decimated";
		if (!new File(args[0]).exists() || !new File(decimatedPath).exists()) {
			System.out.println("MeshOEMMCoarseViewer oemm [decimated_oemm]");
			return;
		}
		JFrame feFrame=new JFrame("jcae-viewer3d-fd demo");
		feFrame.setSize(800,600);
		feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		final View bgView=new View(feFrame);

		final OEMM oemm = Storage.readOEMMStructure(args[0]);
		final OEMM decimatedOemm = Storage.readOEMMStructure(decimatedPath);

		final ViewableBG octree = new ViewableBG(OEMMViewer.bgOEMM(oemm, true));

		BranchGroup bg=new BranchGroup();
		final OEMMBehavior oemmBehavior=new OEMMBehavior(bgView, oemm, decimatedOemm);
		bg.addChild(oemmBehavior);
		
		FPSBehavior fpsB = new FPSBehavior();
		fpsB.setSchedulingBounds(new BoundingSphere(new Point3d(), Double.MAX_VALUE));
		fpsB.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				logger.info("FPS>" + evt.getNewValue());
			}
		});
		BranchGroup fpsBG = new BranchGroup();
		fpsBG.addChild(fpsB);
		final ViewableBG fps = new ViewableBG(fpsBG);

		bgView.addBranchGroup(bg);
		if (showFPS)
			bgView.add(fps);

		bgView.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event)
			{
				char k = event.getKeyChar();
				if(k == '?')
				{
					printInteractiveUsage();
				}
				else if (k == 'o')
				{
					showOctree = !showOctree;
					if (showOctree)
					{
						bgView.add(octree);
						bgView.setCurrentViewable(octree);
					}
					else
						bgView.remove(octree);
				}
				else if (k == 'a')
				{
					showAxis = !showAxis;
					bgView.setOriginAxisVisible(showAxis);
				}
				else if (k == 'F')
				{
					showFPS = !showFPS;
					if (showFPS)
					{
						bgView.add(fps);
						bgView.setCurrentViewable(fps);
					}
					else
						bgView.remove(fps);
				}
				else if (k == 'i')
				{
					if (logger.isLoggable(Level.INFO))
						logger.info("Selected: " + octree.getResultSet());
				}
				else if (k == 'v')
				{
					octree.unselectAll();
					for (int i: oemmBehavior.getIds()) {
						octree.highlight(i, true);
					}
				}
				else if (k == 'f')
				{
					oemmBehavior.switchFreeze();
				}
				else if (k == 'n')
				{
					MeshReader fineReader = new MeshReader(oemm);
					MeshReader coarseReader = new MeshReader(decimatedOemm);
					double[] tempD1 = new double[3];
					double[] tempD2 = new double[3];
					double[] tempn1 = new double[3];
					double[] tempn2 = new double[3];
					TIntHashSet leaves = new TIntHashSet();
					for (TIntIterator it = octree.getResultSet().iterator(); it.hasNext(); )
					{
						int i = it.next();
						leaves.clear();
						leaves.add(i);
						Mesh mesh = fineReader.buildMesh(leaves);
						Mesh coarseMesh = coarseReader.buildMesh(leaves);
						Triangle tf = mesh.getTriangles().iterator().next();
						Matrix3D.computeNormal3D(tf.vertex[0].getUV(), tf.vertex[1].getUV(), tf.vertex[2].getUV(), tempD1, tempD2, tempn1);
						Triangle tc = coarseMesh.getTriangles().iterator().next();
						Matrix3D.computeNormal3D(tc.vertex[0].getUV(), tc.vertex[1].getUV(), tc.vertex[2].getUV(), tempD1, tempD2, tempn2);
						System.out.println("Coarse normal for first triangle of leaf: " + i + " [" 
								+ tempn1[0] + ", "+ tempn1[1] + ", "+ tempn1[2] + "] and fine " +
								" [" 
								+ tempn2[0] + ", "+ tempn2[1] + ", "+ tempn2[2] + "]" + " and "
								+ " orientation: " + (tempn1[0]*tempn2[0] + tempn1[1]*tempn2[1] + tempn1[2]*tempn2[2]  ));
					}
				}
				else if (k == 'c')
				{
					for (TIntIterator it = octree.getResultSet().iterator(); it.hasNext(); )
					{
						int i = it.next();
						Point3d vector = oemmBehavior.getVoxel(i);

						if (logger.isLoggable(Level.INFO)) {
							logger.info("Node: " + i + ", vector: [" + vector.x + ", " + vector.y + ", " + vector.z + "]");
						}
					}
					if (logger.isLoggable(Level.INFO)) {
						logger.info("Visible oemm nodes: " + oemmBehavior.getNumberOfVisibleFineElements() + ", cache: " + oemmBehavior.getNumberOfCacheNodes());
					}
				}
				else if (k == 'p')
				{
					printMeshStatistics("Coarse mesh", decimatedOemm);
					printMeshStatistics("Fine mesh", oemm);
				}
				else if (k == 'q')
					System.exit(0);
			}
		});
		bgView.fitAll();
		bgView.setOriginAxisVisible(showAxis);
		feFrame.getContentPane().add(bgView);
		feFrame.setVisible(true);
	}

	static final void printInteractiveUsage()
	{
		System.out.println("Key usage:");
		System.out.println("  ?: Display this help message");
		System.out.println("  q: Exit");
		System.out.println("  o: Toggle display of octree boxes");
		System.out.println("  a: Toggle axis display");
		System.out.println("  F: Toggle FPS display");
		System.out.println("  i: Print selected nodes");
		System.out.println("  v: Highlight octree nodes containing fine mesh");
		System.out.println("  f: Toggle freeze of coarse/fine mesh adaptation");
		System.out.println("  n: Print mesh normals in selected octree nodes");
		System.out.println("  p: Print mesh statistics");
		System.out.println("  c: Print cache statistics");
	}

	static final void printMeshStatistics(String header, OEMM oemm)
	{
		int triangles = 0;
		int vertices = 0;
		for(OEMM.Node current: oemm.leaves)
		{
			triangles += current.tn;
			vertices += current.vn;
		}
		System.out.println(header+": "+triangles+" triangles and "+vertices+" vertices");
	}
}
