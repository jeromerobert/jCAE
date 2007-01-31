/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

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

import org.jcae.mesh.oemm.*;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.MMesh3D;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.xmldata.MMesh3DReader;
import org.jcae.mesh.xmldata.UNVConverter;
import org.jcae.mesh.amibe.validation.*;
import org.apache.log4j.Logger;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.media.j3d.BranchGroup;
import org.jcae.viewer3d.bg.ViewableBG;
import org.jcae.viewer3d.View;
import gnu.trove.TIntHashSet;

/**
 * This class illustrates how to perform quality checks.
 */
public class MeshOEMMViewer3d
{
	private static Logger logger=Logger.getLogger(MeshOEMMViewer3d.class);
	private static ViewableBG femesh;
	private static TIntHashSet leaves = new TIntHashSet();
	private static ViewableBG fe1;
	private static boolean showOctree = true;

	public static void main(String args[])
	{
		if (args.length < 1)
		{
			System.out.println("Usage: MeshOEMMViewer3d dir");
			System.exit(0);
		}
		String dir=args[0];
		JFrame feFrame=new JFrame("jCAE Demo");
		feFrame.setSize(800,600);
		feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		final OEMM oemm = IndexedStorage.buildOEMMStructure(dir);
		final View bgView=new View();
		boolean onlyLeaves = true;
		try
		{
			BranchGroup octree = OEMMViewer.bgOEMM(oemm, onlyLeaves);
			fe1 = new ViewableBG(octree);
			bgView.add(fe1);
			bgView.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent event)
				{
					if(event.getKeyChar()=='n')
					{
						if (femesh != null)
							bgView.remove(femesh);
						BranchGroup mesh = OEMMViewer.meshOEMM(oemm, fe1.getResultSet());
						mesh.setPickable(false);
						femesh=new ViewableBG(mesh);
						fe1.unselectAll();
						bgView.add(femesh);
					}
					else if(event.getKeyChar()=='o')
					{
						showOctree = !showOctree;
						if (showOctree)
							bgView.add(fe1);
						else
							bgView.remove(fe1);
					}
					else if(event.getKeyChar()=='s')
					{
						Mesh amesh = org.jcae.mesh.oemm.IndexedStorage.loadNodes(oemm, fe1.getResultSet());
						String xmlDir = "oemm-tmp";
						String xmlFile = "jcae3d";
						MeshWriter.writeObject3D(amesh, xmlDir, xmlFile, ".", "tmp.brep", 1);
						new UNVConverter(xmlDir).writeMESH("oemm-tmp.mesh");
						MMesh3D mesh3D = MMesh3DReader.readObject(xmlDir, xmlFile);
						MinAngleFace qproc = new MinAngleFace();
						QualityFloat data = new QualityFloat(1000);
						data.setQualityProcedure(qproc);
						for (Iterator itf = mesh3D.getFacesIterator(); itf.hasNext();)
						{
							Triangle f= (Triangle) itf.next();
							data.compute(f);
						}
						data.finish();
						data.setTarget((float) Math.PI/3.0f);
						data.printMeshBB("oemm-tmp.bb");
					}
					else if(event.getKeyChar()=='d')
					{
						if (femesh != null)
							bgView.remove(femesh);
Mesh amesh = org.jcae.mesh.oemm.IndexedStorage.loadNodes(oemm, fe1.getResultSet());
new org.jcae.mesh.amibe.algos3d.DecimateVertex(amesh, 0.1).compute();
BranchGroup mesh = OEMMViewer.meshOEMM(oemm, fe1.getResultSet());
						mesh.setPickable(false);
						femesh=new ViewableBG(mesh);
						fe1.unselectAll();
						bgView.add(femesh);
					}
				}
			});
			bgView.fitAll();
			feFrame.getContentPane().add(bgView);
			feFrame.setVisible(true);
			bgView.setOriginAxisVisible(true);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
