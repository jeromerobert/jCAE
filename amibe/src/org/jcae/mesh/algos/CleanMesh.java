/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

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

package org.jcae.mesh.algos;

import java.util.*;
import org.apache.log4j.Logger;
import org.jcae.mesh.sd.*;
import org.jcae.mesh.util.HashSet;
import org.jcae.opencascade.jni.*;

/**
 *
 * @author Jerome Robert
 */
public class CleanMesh
{
	private static Logger logger=Logger.getLogger(CleanMesh.class);

	/**
	 * Method cleanMesh.
	 * Allows to optimize meshing
	 * ei is the number of edges connected to node, fi the number of faces connected to the same node ni:
	 * ei=fi=3 or ei=fi=4 or ei=2 and fi=1 : node removed if not classified on a vertex.
	 */
	public static void compute(MeshOfCAD myMesh)
	{
		logger.debug("cleanMesh: begin");
		boolean toreturn = false;
		int ei = 0;	// the number of edges connected to node ni
		int fi = 0;	// the number of faces connected to the same node ni
		Iterator it = myMesh.getNodesIterator();
		while (it.hasNext())
		{
			MeshNode ni = (MeshNode)it.next();
			if (ni.getPosition().getType()==PST_Position.VERTEX) continue;
			
			//HashSet edges = ni.getEdges();
			//HashSet faces = ni.getFaces();
			ei = ni.numberOfEdges();
			fi = ni.numberOfFaces();
			if ( ((ei == 3)&&(fi == 3)) || ((ei == 4) && (fi == 4)) || ((ei == 2) && (fi == 1)) )
			{
				// contour extraction 
				HashSet contour = ni.getTopologicContour(1,MeshElement.EDGE);
				ArrayList LC = MiscAlgos.isSingleContour(contour);
				if ( (LC==null)&& (fi>2) )
				{
					logger.info("pas un contour, erreur dans maillage");
					continue;
				}
				if ( (ei>2) && (!MiscAlgos.contourIsClosed(LC)) )
				{
					logger.info("contour not closed");
					continue;
				}
				
				TopoDS_Face toposurf = null;
				MeshOfCAD mesh = null;
				switch (ni.getPosition().getType())
				{										
					case PST_Position.SURFACE :	
						// retreive the topoDS_Face and its submesh
						toposurf = ((PST_SurfacePosition)ni.getPosition()).getSurface();
						if (toposurf != null) 
						{
							mesh = myMesh.getMeshFromMapOfSubMesh(toposurf);
						}
						if (mesh == null) mesh = myMesh;
						
						// reconstruction de la face
						new MiscAlgos(mesh).fillContour(LC);
						
						// faces deletion (must be done after filling contour)
						Iterator myIt = ni.getFacesIterator();
						while(myIt.hasNext()) myMesh.rmFace((MeshFace)myIt.next());						

						//logger.debug("Node removed ("+ei+"/"+fi+"-Surface) "+ni+" nb faces crees: "+subMesh.numberOfFaces());
						it = myMesh.getNodesIterator();
						break;

					case PST_Position.DEGENERATEDLINE :	
					case PST_Position.EDGE :						
						TopoDS_Edge topoE = (TopoDS_Edge)ni.getPosition().getShape();
						// recherche des deux edges classifiees sur la TopoDS_Edge
						MeshEdge e1 = null;
						MeshEdge e2 = null;
						Iterator ite = ni.getEdgesIterator();
						while (ite.hasNext())
						{
							MeshEdge e = (MeshEdge)ite.next();
							MeshNode n = null;
							if (e.getNodes1().equals(ni))
								n = e.getNodes2();
							else n = e.getNodes1();
							if (n.getPosition().getType()==PST_Position.SURFACE) 
							{
								toposurf = ((PST_SurfacePosition)n.getPosition()).getSurface();
								mesh = myMesh.getMeshFromMapOfSubMesh(toposurf);
								continue;
							}
							switch (n.getPosition().getType())
							{
								case PST_Position.DEGENERATEDLINE :
								case PST_Position.EDGE :
									TopoDS_Edge te = (TopoDS_Edge)n.getPosition().getShape();
									if (te.equals(topoE)) 
										if (e1==null) e1 = e; else e2 = e;
									break;
								case PST_Position.VERTEX :
									TopoDS_Vertex[] vertices = new TopoDS_Vertex[2];
									vertices = TopExp.vertices(topoE);	
									TopoDS_Vertex v = (TopoDS_Vertex)n.getPosition().getShape();
									if (v.equals(vertices[0]) || v.equals(vertices[1]) )
										if (e1==null) e1 = e; else e2 = e;
									break;
							}
						}
						e1.setWire(false); e1.setFrozen(false);
						e2.setWire(false); e2.setFrozen(false);
						// Construction de l'edge
						MeshEdge edge = null;
						
						if (e1.getNodes1().equals(e2.getNodes1()))
							edge = new MeshEdge(e1.getNodes2(),e2.getNodes2());
						else if (e1.getNodes1().equals(e2.getNodes2()))
							edge = new MeshEdge(e1.getNodes2(),e2.getNodes1());
						else if (e1.getNodes2().equals(e2.getNodes1()))
							edge = new MeshEdge(e1.getNodes1(),e2.getNodes2());
						else if (e1.getNodes2().equals(e2.getNodes2()))
							edge = new MeshEdge(e1.getNodes1(),e2.getNodes1());
						
						if (myMesh.getEdgeDefinedByNodes(edge.getNodes1(),edge.getNodes2()) != null)
							edge = myMesh.getEdgeDefinedByNodes(edge.getNodes1(),edge.getNodes2());
						edge.setWire(true); edge.setFrozen(true);

						if ( (ei == 2) && (fi == 1) )
						{
							Iterator itf = ni.getFacesIterator();
							myMesh.rmFace((MeshFace)itf.next());
							logger.info("Node removed (2/1)"+ni);
							it = myMesh.getNodesIterator();
							break;
						}

						else if ( ((ei == 3)&&(fi == 3)) || (contour.contains(edge)) )
						{
							// reconstruction de la face
							if (mesh == null) mesh = myMesh;
							new MiscAlgos(mesh).fillContour(
								new ArrayList((Collection)contour));	
							
							// faces deletion
							Iterator itf = ni.getFacesIterator();
							while(itf.hasNext()) myMesh.rmFace((MeshFace)itf.next());

							//logger.debug("Node removed ("+ei+"/"+fi+")"+ni+" nb faces crees: "+subMesh.numberOfFaces());
							it = myMesh.getNodesIterator();
							break;
						}

						else if ( ((ei == 4)&&(fi == 4)) && (!contour.contains(edge)) )
						{
							// creation d'un ou plusieurs contours fermes
							ArrayList contourD = new ArrayList();
							ArrayList contourG = new ArrayList();
							MeshOfCAD meshD = null;
							MeshOfCAD meshG = null;
							TopoDS_Shape shapeD = null;
							TopoDS_Shape shapeG = null;
							// cas particulier de l'edge commune aux contours
							contourD.add(0,edge);
							contourG.add(0,edge);

							// classement arbitraire de l'edge suivante
							int tailleContour = contour.size()+2;
							Iterator itc = contour.iterator();
							while (itc.hasNext())
							{
								MeshEdge ed = (MeshEdge)itc.next();
								if ( ed.getNodes1().getPosition().getType() == PST_Position.SURFACE )
								{
									toposurf = ((PST_SurfacePosition)ed.getNodes1().getPosition()).getSurface();
									if (toposurf != null) 
									{										
										mesh = myMesh.getMeshFromMapOfSubMesh(toposurf);
									}
								}
								else if ( ed.getNodes2().getPosition().getType() == PST_Position.SURFACE )
								{
									toposurf = ((PST_SurfacePosition)ed.getNodes2().getPosition()).getSurface();
									if (toposurf != null) 
									{										
										mesh = myMesh.getMeshFromMapOfSubMesh(toposurf);
									}
								}
								else mesh = null;
								if ( (ed.getNodes()).contains(edge.getNodes1()) )
								{
									if (contourD.size() == 1)
									{
										contourD.add(1,ed);
										contour.remove(ed);
										if (mesh != null) 
											meshD = mesh;
										itc = contour.iterator();
									}
									else if (contourG.size() == 1) 
									{
										contourG.add(1,ed);
										contour.remove(ed);
										if (mesh != null) 
											meshG = mesh;
										itc = contour.iterator();
									}										
									else logger.info("erreur dans la recherche des deux edges");
								}
							}
							// classement des edges restantes
							MeshEdge e = null;
							int i = 1, j = 1;						
							boolean fullD = false, fullG = false; 
							while (contourD.size()+contourG.size() != tailleContour)
							{
								// recherche de l'edge suivante pour D et G
								Iterator itt = contour.iterator();
								while (itt.hasNext())
								{
									e = (MeshEdge) itt.next();
									if ( contourD.contains(e) || contourG.contains(e) )
										continue;
									if ( (!fullD) && (((e.getNodes()).contains(((MeshEdge)contourD.get(i)).getNodes1()))
									|| ((e.getNodes()).contains(((MeshEdge)contourD.get(i)).getNodes2()))) )
									{
										contourD.add(i+1,e);
										contour.remove(e);
										itt = contour.iterator();
										i++;
										if ( (e.getNodes().contains(((MeshEdge)contourD.get(0)).getNodes1())) || (e.getNodes().contains(((MeshEdge)contourD.get(0)).getNodes2())) )
											fullD = true;
									}
									else if ( (!fullG) && (((e.getNodes()).contains(((MeshEdge)contourG.get(j)).getNodes1()))
									|| ((e.getNodes()).contains(((MeshEdge)contourG.get(j)).getNodes2()))) )
									{
										contourG.add(j+1,e);
										contour.remove(e);
										itt = contour.iterator();
										j++;
										if ( (e.getNodes().contains(((MeshEdge)contourG.get(0)).getNodes1())) || (e.getNodes().contains(((MeshEdge)contourG.get(0)).getNodes2())) )
											fullG = true;
									}
									// cas contour ouvert: il faut sortir (ca ne devrait plus arriver)
									if (fullD && !fullG)
									{
										HashSet temp = new HashSet(contour);
										temp.addAll(contourG);
										//ArrayList list = isSingleContour(temp);
										if (!MiscAlgos.contourIsClosed(new ArrayList(temp))) fullG=true;
									}
									if (!fullD && fullG)
									{
										HashSet temp = new HashSet(contour);
										temp.addAll(contourD);										
										if (!MiscAlgos.contourIsClosed(new ArrayList(temp))) fullD=true;
									}
										
									
								}
							}
																										
							// reconstruction des faces							
							if ( (MiscAlgos.contourIsClosed(contourD))&&(MiscAlgos.contourIsClosed(contourG)) )
							{
								if (meshD == null) meshD = myMesh;
								new MiscAlgos(meshD).fillContour(contourD);
								if (meshG == null) meshG = myMesh;
								new MiscAlgos(meshG).fillContour(contourG);

								// faces deletion
								Iterator itf = ni.getFacesIterator();
								while(itf.hasNext()) myMesh.rmFace((MeshFace)itf.next());

								//logger.debug("Node removed (4/4-Edge)"+ni+" nbFaces crees: "+subMesh1.numberOfFaces()+"+"+subMesh2.numberOfFaces());
								it = myMesh.getNodesIterator();
							}
							else 
								logger.warn("Incorrect meshing, contour extraction impossible.");

							break;
						} // end ei==fi==4
						break;
						
					default : ;						
				}//end of switch		
			} //end of if
		} //end of loop on nodes
		logger.debug("cleanMesh: end");
		return;
	}	
}
