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

import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.OEMMViewer;
import org.jcae.mesh.oemm.Storage;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.MMesh3D;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.xmldata.MMesh3DReader;
import org.jcae.mesh.xmldata.UNVConverter;
import org.jcae.mesh.amibe.validation.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.jcae.viewer3d.bg.ViewableBG;
import org.jcae.viewer3d.fe.amibe.AmibeProvider;
import org.jcae.viewer3d.fe.ViewableFE;
import org.jcae.viewer3d.fe.FEDomain;
import org.jcae.viewer3d.View;
import gnu.trove.TIntHashSet;

/**
 * This class illustrates how to perform quality checks.
 */
public class MeshOEMMViewer3d
{
	private static Logger logger=Logger.getLogger(MeshOEMMViewer3d.class);
	private static ViewableBG fineMesh;
	private static ViewableFE decMesh;
	private static TIntHashSet leaves = new TIntHashSet();

	private static boolean showOctree = true;
	private static boolean showAxis = true;

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
		final OEMM oemm = Storage.readOEMMStructure(dir);
		final View bgView=new View(feFrame);
		final ViewableBG octree = new ViewableBG(OEMMViewer.bgOEMM(oemm, true));
		try
		{
			bgView.add(octree);
			bgView.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent event)
				{
					if(event.getKeyChar()=='n')
					{
						if (fineMesh != null)
							bgView.remove(fineMesh);
						if (decMesh != null)
							bgView.remove(decMesh);
						fineMesh = new ViewableBG(OEMMViewer.meshOEMM(oemm, octree.getResultSet()));
						octree.unselectAll();
						bgView.add(fineMesh);
					}
					else if(event.getKeyChar()=='o')
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
					else if(event.getKeyChar()=='s')
					{
						Mesh amesh = Storage.loadNodes(oemm, octree.getResultSet(), false);
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
						if (fineMesh != null)
							bgView.remove(fineMesh);
						if (decMesh != null)
							bgView.remove(decMesh);
						Mesh amesh = Storage.loadNodes(oemm, octree.getResultSet(), true);
						HashMap opts = new HashMap();
						opts.put("maxtriangles", Integer.toString(amesh.getTriangles().size() / 100));
						new org.jcae.mesh.amibe.algos3d.DecimateHalfEdge(amesh, opts).compute();
						String xmlDir = "dec-tmp";
						String xmlFile = "jcae3d";
						MeshWriter.writeObject3D(amesh, xmlDir, xmlFile, ".", "tmp.brep", 1);
						octree.unselectAll();
						try
						{
							AmibeProvider ap = new AmibeProvider(new File(xmlDir));
							decMesh = new ViewableFE(ap);                
							logger.info("Nr. of triangles: "+((FEDomain)ap.getDomain(0)).getNumberOfTria3());
							bgView.add(decMesh);
						}
						catch (Exception ex)
						{
							ex.printStackTrace();
						}
					}
					else if(event.getKeyChar()=='a')
					{
						showAxis = !showAxis;
						bgView.setOriginAxisVisible(showAxis);
					}
					else if(event.getKeyChar()=='q')
						System.exit(0);
				}
			});
			bgView.fitAll();
			feFrame.getContentPane().add(bgView);
			feFrame.setVisible(true);
			bgView.setOriginAxisVisible(showAxis);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
