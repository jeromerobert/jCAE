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
import org.jcae.mesh.sd.*;
import org.jcae.opencascade.jni.*;
import org.jcae.mesh.util.*;
import org.jcae.mesh.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import java.util.Stack;
import org.apache.log4j.Logger;
/**
 *
 * @author Jerome Robert
 */
public class Smoothing
{
	private static Logger logger=Logger.getLogger(Smoothing.class);
	
	/**
	 * Method runSmoothing.
	 * Calls the meshSmoothing method. @see meshSmoothing()
	 * @param nbIt - the number of iteration done to approch the barycentre location
	 */
	static public void runFaceSmoothing(MeshOfCAD myMesh, int nbIt)
	{		
		logger.debug("runFaceSmoothing : begin");		
		Iterator ite = myMesh.getGeometryIterator();
		while (ite.hasNext())
		{
			TopoDS_Shape s = (TopoDS_Shape) ite.next();
			if (s.shapeType() == TopAbs_ShapeEnum.FACE)
			{
				MeshOfCAD m = myMesh.getMeshFromMapOfSubMesh(s);
				// retrive all nodes of the face*/				
				for (int k = 0; k < (nbIt-1); k++)
					meshSmoothing(m.getNodesIterator());
				m=null;
			}
		}
		logger.debug("runFaceSmoothing : end");
	}
	
	static public void runPatchSmoothing(MeshOfCAD myMesh, int nbIt)
	{
		logger.debug("runPatchSmoothing : begin");
		ArrayList edges = mergeDuplicatedEdges(myMesh);
		if (edges.isEmpty())
		{
			logger.info("Mesh does not contain patches (or patches already collapsed)");
			return;
		}
		transformPSTLineInPSTSurface(edges);
		
		for (int k = 0; k < nbIt; k++)
			meshSmoothing(myMesh.getNodesIterator());	

		logger.debug("runPatchSmoothing : end");
	}
	
	/**
	 * Method to optimise the position of nodes without changing the topology of the mesh.
	 * On a surface, this problem should be defined as follow: Optimize node position such that:
	 * - shape of faces are the most equilateral as possible
	 * - the mesh stays compatible for structure analysis
	 * To solve this optimisation problem, the solution used is the Laplacian Smoothing technique.
	 */
	static private void meshSmoothing(Iterator nodesIterator)
	{
		// nbi is the number of iteration done to smooth the mesh		
		int i = 0;
		while (nodesIterator.hasNext())
		{
			MeshNode n = (MeshNode)nodesIterator.next();
			if (n.getElements().isEmpty()) continue;
			// cannot move nodes classified on vertex
			if ( (n.getPosition().getType() == PST_Position.SURFACE)
				|| (n.getPosition().getType() == PST_Position.EDGE)
				|| (n.getPosition().getType() == PST_Position.DEGENERATEDLINE) )
			{
				if (n.getElements().isEmpty())
					logger.debug("n : "+n.getElements().size());
				double [] lpos = computeLinearizedPositionOfNode(n);
				PST_Position pos = checkCorrectnessOfPosition(n,lpos);
				enforcePosition(n, pos);
			}
		}
	}
	
	static private MeshNode enforcePositionOutOfPatch(MeshNode node,
		PST_SurfacePosition pos, double[] bornes)
	{
		logger.debug("enforcePosition out of original patch");
		double[] uv = pos.getParam();
		// I : intersection between the segment linking its final
		// and initial position and the contour of the parametric 
		// space must be computed
		// find I coordinates in the parametric space
		double[] I = new double[2];
		if (uv[0]<bornes[0]) I[0]=bornes[0];
		else if (uv[0]>bornes[1]) I[0]=bornes[1];
		else I[0]=uv[0];
		if (uv[1]<bornes[2]) I[1]=bornes[2];
		else if (uv[1]>bornes[3]) I[1]=bornes[3];
		else I[1]=uv[1];
		PST_SurfacePosition posI =
			new PST_SurfacePosition((TopoDS_Face)pos.getShape(),I[0],I[1]);
		MeshNode nodeI = new MeshNode(posI);
		nodeI.setCoord3D();
		
		// retreive the edge
		MeshEdge edge = node.getPatchEdge(nodeI);
		if (edge == null) edge = node.getClosestEdge(nodeI);
		if (edge == null) 
		{
			logger.debug("edge frontiere pas trouvee");
			return node;
		}
		// if I is on a connected bounbary : the node is located in I on the connected patch					
		if(edge.getFaces().size()>1)
		{
			TopoDS_Face topoface = null;
			// reste plus qu'a trouver la surface(voisin de edge)
			ArrayList sommets = new ArrayList(3);
			sommets.addAll(edge.getNodes());
			HashSet faces = edge.getFaces();
			Iterator itf = faces.iterator();
			while(itf.hasNext())
			{
				MeshFace face = (MeshFace)itf.next();
				MeshNode n = face.apex(edge);
				if (n.getPosition().getShape().equals(node.getPosition().getShape()))
					continue;
				if (n.getPosition().getType()==PST_Position.SURFACE)
				{
					topoface = ((PST_SurfacePosition)n.getPosition()).getSurface();
				}
			}
			if (topoface == null)
			{
				// il faut poursuivre la recherche d'une topoDS_Face sur les noeuds voisins
				MeshNode n1 = edge.getNodes1();
				MeshNode n2 = edge.getNodes2();
				MeshNode nn = null;
				if (n1.getElements().size() > n2.getElements().size())
					nn = n1;
				else
					nn = n2;
				HashSet voisins = nn.getTopologicContour(1, MeshElement.NODE);
				Iterator itv = voisins.iterator();
				while(itv.hasNext())
				{
					MeshNode n = (MeshNode)itv.next();
					if (n.getPosition().getShape().equals(node.getPosition().getShape()))
					continue;
					if (n.getPosition().getType()==PST_Position.SURFACE)
					{
						topoface = ((PST_SurfacePosition)n.getPosition()).getSurface();
					}
				}
				if (topoface == null)
				{
					logger.debug("il faut poursuivre la recherche de la face sur les points voisins.... TODO");
					return node;
				}
			}

			// Project node I on the adjacent topoDS_Surface
			double[] P=new double[3];
			double[] resd=new double[2];
			P[0]=nodeI.getX();
			P[1]=nodeI.getY();
			P[2]=nodeI.getZ();

			/* Projection du point sur la surface:
			 * Les methodes fournies par BRep_Tool ne conviennent pas car ces methodes permettent d'acceder a des informations
			 * topologique de la surface (ou edge) existantes. Or nodeI est un point cree, et n''existe pas dans la topologie de l'objet.
			 * Il faut donc faire appel aux methodes de projection d'un point sur une courbe ou une surface.
			 * La projection du point sur l'edge permet d'obtenir lengthparametre du point sur la courbe, mais pour obtenir ces coordonnees
			 * dans l'espace parametrique de la surface, getSurfacePosition() fait appel aux methodes de BRep_Tool. Cette edge n'appartient 
			 *qu'a une des deux surfaces (les deux edges frontieres entre les patch ont etees mergees en une seule, choisie arbitrairement).
			 * Les coordonnees uv du point ne peuvent pas s'obtenir de cette maniere.
			 * Il faut donc projeter le point sur la surface directement. Plusieurs solutions sont possibles, mais puisque le node I est un point
			 * tres proche de la surface de projectionm la solution donnant la distance minimale est celle voulue.
			 */
			//Geom_Curve Curve = BRep_Tool.curve(topoedge, resd);
			Geom_Surface Surf= BRep_Tool.surface(topoface);

			//GeomAPI_ProjectPointOnCurve projP = new GeomAPI_ProjectPointOnCurve(P, Curve);
			GeomAPI_ProjectPointOnSurf projP = new GeomAPI_ProjectPointOnSurf(P, Surf);
			// nb de solution
			double[] nuv = new double[2];
			int nbSolu = projP.nbPoints();
			if (nbSolu > 0)
			{						
				double dist = projP.lowerDistance();
				projP.lowerDistanceParameters(nuv);
			}
			else 
				logger.debug("pas de solution pour la projection");

			PST_SurfacePosition npos = new PST_SurfacePosition(topoface,nuv[0], nuv[1]);
			//node.setPosition(npos);
			node = changeNodePosition(node,pos);
			node.setCoord3D();

		}
		// if I is not on a connected boundary : the node stay in the same patch in I
		else
		{
			//node.setPosition(posI);
			node = changeNodePosition(node,pos);
			node.setCoord3D();
		}
		return node;
	}
	
	static private MeshNode enforcePosition(MeshNode node, PST_Position pos)
	{
		switch (pos.getType())
		{
			case PST_Position.DEGENERATEDLINE :
			case PST_Position.EDGE :
				//node.setPosition(pos);
				node = changeNodePosition(node, pos);
				node.setCoord3D();
				break;
			case PST_Position.SURFACE :
				// check if the displacement of the node leads it out of its parametric space
				double[] bornes = ((PST_SurfacePosition)pos).isInOut();
				if ( bornes != null)	// out
				{
					enforcePositionOutOfPatch(node, (PST_SurfacePosition)pos, bornes);
				}
				else // in
				{
					//node.setPosition(pos);
					node = changeNodePosition(node,pos);
					node.setCoord3D();
				}
				break;
		}
		return node;
	}

	/**
	 * Method used to transform all the PST_LinePosition of nodes in PST_SurfacePosition
	 * @param : an array of edges. 
	 * @see runPatchSmoothing
	 */
	static private void transformPSTLineInPSTSurface(ArrayList edges)
	{
		MeshOfCAD meshF;
		MeshMesh meshE = null;
		TopoDS_Edge E = null;
		TopoDS_Face S = null;
		MeshEdge edge = null;
		MeshNode n1 = null;
		MeshNode n2 = null;  
		HashSet nodes = new HashSet(); 
		
		for (int j = 0; j<edges.size(); j++)
		{
			edge = (MeshEdge)edges.get(j);
			nodes.addAll(edge.getNodes());
		}
	    Iterator itn = nodes.iterator();
		while(itn.hasNext())
		{
			MeshNode n = (MeshNode)itn.next();
			if (n.getPosition().getType() != PST_Position.EDGE)
				continue;
			E = (TopoDS_Edge)n.getPosition().getShape();
			/*meshE = myMesh.getMeshFromMapOfSubMesh(E);
			// retreive the meshmesh that contains meshE
			Iterator itm = myMesh.getSetOfSubMesh().iterator();
			while(itm.hasNext())
			{
				MeshMesh m = (MeshMesh)itm.next();
				if (m.getSetOfSubMesh().contains(meshE))
				{
					meshF = m;
					break;
				}
			}
			if (meshF == null)
			{
				logger.debug("submesh contenant E pas trouve!");
				return;
			}
			S = (TopoDS_Face)meshF.getGeometry();*/
			
			// Find a neighboor surface of the node.
			MeshFace f=(MeshFace)n.getFaces().iterator().next();
			// Find a neighboor node which have a surface position.
			Iterator it=f.getNodesIterator();
			while(it.hasNext())
			{	
				MeshNode anotherNode=(MeshNode)it.next();
				if(anotherNode.getPosition().getType()==PST_Position.SURFACE)
				{
					S=((PST_SurfacePosition)anotherNode.getPosition()).getSurface();
				}
			}
			
			if(it.hasNext()) throw new RuntimeException("Neighboor surface not found");
			
			// pour chaque noeud en LinePosition, creeer un nouveau en SurfacePosition
			PST_SurfacePosition pos = getSurfacePosition(S,  E, ((PST_LinePosition)n.getPosition()).getParam());
			MeshNode p = changeNodePosition(n,pos);
		}	
	}

	/**
	 * Method used to change the PST_Position of a node.
	 * All elements linked to the given node are destroyed and rebuild in order to keep 
	 * a coherent meshing data base
	 * @param n , the MeshNode to move
	 * @param pos, the new PST_Position requiered
	 * @return MeshNode the node with its new PST_Position
	 */
	 static private MeshNode changeNodePosition(MeshNode n, PST_Position pos)
	 {
		n.setPosition(pos);
		 // retreive the submesh containing n
		/*MeshMesh nodeMesh = null;
		Set meshes = myMesh.getSubMeshes();
		Iterator itm = meshes.iterator();
		while (itm.hasNext())
		{
			MeshMesh m = (MeshMesh)itm.next();
			if (m.getNodelist().contains(n))
			{
				nodeMesh = m;
				break;
			}
		}
		if (nodeMesh == null) 
		{
			logger.debug("nodeMesh not found");
			return n;
		}

		// new node
		MeshNode p = new MeshNode(pos);
		p.setCoord3D();
		p = nodeMesh.addNode(p);
		
		// reconstruire tous les elements linkes a n
		HashSet faces = n.getFaces();
		Iterator itf = faces.iterator();
		while(itf.hasNext())
		{
			MeshFace f = (MeshFace)itf.next();
			// recherche du submesh
			MeshMesh meshF = null;
			Iterator itm2 = myMesh.getSubMeshes().iterator();
			while(itm2.hasNext())
			{
				MeshMesh m = (MeshMesh)itm2.next();
				if (m.getFacelist().contains(f))
				{
					meshF = m;
					break;
				}
			}
			if (meshF == null) 
			{
				logger.debug("erreur dans l recherche du submesh");
				return n;
			}
			HashSet fnodes = f.getNodes();
			Iterator itfn = fnodes.iterator();
			MeshNode p1 = null, p2 = null, p3 = null;
			while(itfn.hasNext())
			{
				MeshNode pt = (MeshNode)itfn.next();
				if ( (p1==null) && pt.equals(n) ) p1 = pt;
				else 
				{
					if (p2 == null) p2 = pt;
					else if (p3 == null) p3 = pt;
				}
			}
			// nouvelles edges ou p1 est remplace par p
			MeshEdge e1 = null, e2 = null, e3 = null;
			e1 = myMesh.getEdgeDefinedByNodes(p, p2);
			MeshEdge E1 = myMesh.getEdgeDefinedByNodes(p1, p2);
			if (e1 == null)
			{
				e1 = new MeshEdge(p, p2);
				e1.setWire(E1.isWire());
			}
			e2 = myMesh.getEdgeDefinedByNodes(p, p3);
			MeshEdge E2 = myMesh.getEdgeDefinedByNodes(p1, p3);
			if (e2 == null)
			{
				e2 = new MeshEdge(p, p3);
				e2.setWire(E2.isWire());
			}
			e3 = myMesh.getEdgeDefinedByNodes(p2, p3);
			if (e3 == null)
			{
				e3 = new MeshEdge(p2, p3);
				logger.debug("edge pas trouvee ?!");
			}
			// retreive the submeshes containing E1, E2 and e3
			MeshMesh meshE1 = null;
			MeshMesh meshE2 = null;
			MeshMesh meshE3 = null;
			itm = meshes.iterator();
			while (itm.hasNext() && ((meshE1==null)||(meshE2==null)||(meshE3==null)))
			{
				MeshMesh m = (MeshMesh)itm.next();
				if (m.getEdgelist().contains(E1))
					meshE1 = m;
				if (m.getEdgelist().contains(E2))
					meshE2 = m;
				if (m.getEdgelist().contains(e3))
					meshE3 = m;
			}
			if (meshE1 == null) 
			{
				logger.debug("submesh E1 not found");
				return n;
			}
			if (meshE2 == null) 
			{
				logger.debug("submesh E2 not found");
				return n;
			}
			if (meshE3 == null) 
			{
				logger.debug("submesh E3 not found");
				return n;
			}
			
			e1 = meshE1.addEdge(e1);
			e2 = meshE2.addEdge(e2);
			e3 = meshE3.addEdge(e3);
			meshF.rmFace(f);
			f.clearList();
			MeshFace newf = new MeshFace(e1,e2,e3);
			e1 = newf.addEdge(e1);
			e2 = newf.addEdge(e2);
			e3 = newf.addEdge(e3);
			newf = meshF.addFace(newf);
			meshF.addElement(newf);		
		}
		return p;*/
		return n;
	 }

	/*
	 * Method used to find a duplicate edge from the edge passed in argument
	 * Duplicate edges are two edges connected to the same two points.
	 * @param MeshEdge - the edge we want to find a duplicate one
	 * @return a MeshEdge - the edge duplicated from edge or null if none edge has been found
	 */
	static private MeshEdge findDuplicateEdge(MeshOfCAD myMesh, MeshEdge edge,
		boolean orient)
	{
		MeshEdge duplicateEdge = null;
 		
		HashSet nodes = edge.getNodes();
		
		if (orient)
		{
			// browse all edges in Mesh			
			Iterator ite = myMesh.getEdgesIterator();
			while ( ite.hasNext() )
			{
				MeshEdge e =(MeshEdge)ite.next();
				if (e.equals(edge))
				{
					if (ite.hasNext()) e = (MeshEdge)ite.next();
					else break;
				}
				if (e.getNodes().containsAll(nodes))
					duplicateEdge = e;
			}
		}
		else // parcours des wire seulememt
		{
			Iterator it = myMesh.getGeometryIterator();
			while (it.hasNext())
			{
				TopoDS_Shape s = (TopoDS_Shape) it.next();
				if (s.shapeType() == TopAbs_ShapeEnum.EDGE)
				{
					MeshMesh m = myMesh.getMeshFromMapOfSubMesh(s);					
					Iterator ite = m.getEdgesIterator();
					while ( ite.hasNext() )
					{
						MeshEdge e =(MeshEdge)ite.next();
						if (e.equals(edge))
						{
							if (ite.hasNext()) e = (MeshEdge)ite.next();
							else break;
						}
						if (e.getNodes().containsAll(nodes))
							duplicateEdge = e;
						if ( e.hasSameEndsCoords(edge) )
							duplicateEdge = e;
					}					
				}
			}			
		}
		return duplicateEdge ;
	}

	
	/**
	 * Browse all edges in mesh and call findDuplicatedEdge method and store all
	 * duplicated edges found.
	 * @return ArrayList - an array containing duplicated edges stored 2 by 2
	 */
	static private ArrayList getDuplicatedEdges(MeshOfCAD myMesh)
	{
		ArrayList duplicatedEdgesList = new ArrayList();
		HashSet duplicatedNodes = new HashSet();
		// on peut accelerer le parcours en ne regardant que dans les submeshes
		// decrivant les TopoDS_Edge (les wires)
		Iterator ite = myMesh.getGeometryIterator();
		while (ite.hasNext())
		{
			TopoDS_Shape s = (TopoDS_Shape) ite.next();
			if (s.shapeType() == TopAbs_ShapeEnum.EDGE)
			{
				MeshMesh m = myMesh.getMeshFromMapOfSubMesh(s);
				int i = 0;
				// parcours de toutes les edges du maillage
				Iterator it = m.getEdgesIterator();
				while(it.hasNext())
				{
					MeshEdge edge = (MeshEdge)it.next();
					if (duplicatedEdgesList.contains(edge)) continue;
					if ( (duplicatedNodes.contains(edge.getNodes1()))
						||(duplicatedNodes.contains(edge.getNodes2())) )
						continue;
					MeshEdge e = findDuplicateEdge(myMesh, edge, false);
					if (e!=null)
					{
						duplicatedEdgesList.add(2*i,edge);
						duplicatedEdgesList.add(2*i+1,e);
						duplicatedNodes.addAll(e.getNodes());
						i++;
					}
				}
			}
		}
		return duplicatedEdgesList;
	}

	/**
	 * Browse all edges in mesh and call mergeDuplicatedEdge method if duplicated edges are found.
	 * @see mergeDuplicatedEdge(MeshEdge edge)
	 */
	static private ArrayList mergeDuplicatedEdges(MeshOfCAD myMesh)
	{
		logger.debug("mergeDuplicatedEdges: begin");
		ArrayList singleEdgesList = new ArrayList();
		ArrayList duplicatedEdgesList = getDuplicatedEdges(myMesh);		
		logger.debug("edges dupli : "+	duplicatedEdgesList.size()/2);
		logger.debug(""+duplicatedEdgesList);
		for (int i = 0; i<duplicatedEdgesList.size()/2; i++)
		{
			mergeDuplicatedEdge(myMesh,
				(MeshEdge)duplicatedEdgesList.get(2*i),
				(MeshEdge)duplicatedEdgesList.get(2*i+1));
			singleEdgesList.add((MeshEdge)duplicatedEdgesList.get(2*i));
		}

		// parcours des submesh a la recherche de submesh vide ou incorrect a supprimer.

		/*Iterator ite = myMesh.getGeometryIterator();
		while (ite.hasNext())
		{
			TopoDS_Shape s = (TopoDS_Shape) ite.next();
			if (s.shapeType() == TopAbs_ShapeEnum.EDGE)
			{
				MeshMesh m = myMesh.getMeshFromMapOfSubMesh(s);
				if(m.isEmpty()) myMesh.removeSubMesh(s);*/
				/*if (m.isEmpty() ||
				(m.getVolumelist().isEmpty() && m.getFacelist().isEmpty() && !m.getEdgelist().isEmpty() && m.getNodelist().isEmpty() && m.getSetOfSubMesh().isEmpty()) )
				{
					m.getEdgelist().clear();
					Iterator itm = myMesh.getSetOfSubMesh().iterator();
					while (itm.hasNext())
					{
						MeshMesh me = (MeshMesh) itm.next();						
						me.deleteFromSetOfSubMesh(m);
					}	
					myMesh.deleteFromMapOfSubMesh(m);
					ite = myMesh.getMapOfSubMesh().keySet().iterator();
				}*/
			/*}
		}*/
		logger.debug("mergeDuplicatedEdges: fin");	
		return singleEdgesList;
	}

	static private TopoDS_Edge getTopoDS_EdgeFromNodes(MeshOfCAD myMesh,
		MeshNode n1, MeshNode n2)
	{
		TopoDS_Edge Te;
		if ( (n1.getPosition().getType()==PST_Position.EDGE)
			|| (n1.getPosition().getType()==PST_Position.DEGENERATEDLINE) )
		{
			Te = (TopoDS_Edge)((PST_LinePosition)n1.getPosition()).getShape();
		}
		else if ( (n2.getPosition().getType()==PST_Position.EDGE)
			|| (n2.getPosition().getType()==PST_Position.DEGENERATEDLINE) )
		{
			Te = (TopoDS_Edge)((PST_LinePosition)n2.getPosition()).getShape();
		}
		else Te = new TopoHelper(myMesh.getGeometry()).getTopoEdge(n1, n2);
		return Te;
	}
	/**
	 * Merge two edges in one if their points have same 3d coordinates.
	 * The method substitutes one edge to the other directly in the data structure.
	 * @param edge - the edge to substitute and keep.
	 * @return a MeshEdge e - the edge removed and replace with the input edge. Can be null if none duplicated edge has be found.
	 */
	static private void mergeDuplicatedEdge(MeshOfCAD myMesh, MeshEdge edge,
		MeshEdge e)
	{
		/*MeshNode n1 = null;
		MeshNode n2 = null;
		MeshNode node1 = edge.getNodes1();
		MeshNode node2 = edge.getNodes2();

		// correspondance des noeuds: n1 <-> node1 et n2<->node2
		if (e.getNodes1().getX() == node1.getX() )
		{
			n1 = e.getNodes1();
			n2 = e.getNodes2();
		}
		else
		{
			n1 = e.getNodes2();
			n2 = e.getNodes1();
		}

		//  retreive the TopoDS_Edge of e
		TopoDS_Edge Te = getTopoDS_EdgeFromNodes(n1,n2);

		if (Te==null) {
			logger.debug("pas de TopoDS_Edge trouvee pour e !!!");
			return;
		}
		MeshMesh meshE = myMesh.getMeshFromMapOfSubMesh(Te);

		// Recherche du submesh correspondant a la Topoface contenant e
		TopoDS_Face Tf = null;
		MeshMesh meshF = null;
		ArrayList Tfaces = new TopoHelper(myMesh.getGeometry()).getTopoFaces(Te);
		if (Tfaces.size()==1) 
			Tf = (TopoDS_Face)Tfaces.get(0);
		else if (Tfaces.size()==2)
		{
			if (Tfaces.get(0).equals(Tfaces.get(1)))
				Tf = (TopoDS_Face)Tfaces.get(0);
		}
		else 
		{
			logger.debug("Impossible de trouver une seule topoface: "+Tfaces);
			TopoDS_Shape S = myMesh.getGeometry();
		}
		meshF = myMesh.getMeshFromMapOfSubMesh(Tf);
		// on ne travaille que dans ce submesh : meshF

		// retreive the TopoDS_Edge of edge
		TopoDS_Edge Tedge = getTopoDS_EdgeFromNodes(node1, node2);
		if (Tedge==null)
		{
			logger.debug("pas de TopoDS_Edge trouvee pour edge !!!");
			return;
		}
		MeshMesh meshEdge = myMesh.getMeshFromMapOfSubMesh(Tedge);
		
		// Cas PST-type == SURFACE Recherche du submesh correspondant a la
		// Topoface contenant edge
		TopoDS_Face Tface = null;
		MeshMesh meshFace = null;

		// On commence par la face linkee avec e
		HashSet faces = e.getFaces();
		Iterator itf = faces.iterator();
		while (itf.hasNext())
		{
			MeshFace face = (MeshFace)itf.next();
			MeshNode p = face.apex(e);
			//retreive original submesh
			MeshMesh m1 = null;
			MeshMesh m2 = null;
			MeshEdge e1 = myMesh.getEdgeDefinedByNodes(n1, p);
			MeshEdge e2 = myMesh.getEdgeDefinedByNodes(n2, p);
			Iterator itmesh = meshF.getSetOfSubMesh().iterator();
			while (itmesh.hasNext())
			{
				MeshMesh mm = (MeshMesh)itmesh.next();
				if (mm.getEdgelist().contains(e1))
					m1 = mm;
				if (mm.getEdgelist().contains(e2))
					m2 = mm;
			}
			if (m1 == null) m1 = meshF;
			if (m2 == null) m2 = meshF;
			MeshEdge ed1 = new MeshEdge(node1,p);
			if (m1.getEdgeDefinedByNodes(n1, p)!=null)
				ed1.setWire(myMesh.getEdgeDefinedByNodes(n1, p).isWire());
			ed1=m1.addEdge(ed1);
			MeshEdge ed2 = new MeshEdge(node2,p);
			if (m2.getEdgeDefinedByNodes(n2, p)!=null)
				ed2.setWire(myMesh.getEdgeDefinedByNodes(n2, p).isWire());
			ed2 = m2.addEdge(ed2);
			edge = meshEdge.addEdge(edge);
			HashSet edges = face.getEdges();
			Iterator ite = edges.iterator();
			while (ite.hasNext())
			{
				MeshEdge ed = (MeshEdge)ite.next();
				ed.setWire(false);
				ed.setFrozen(false);
			}
			meshF.rmFace(face);
			face.clearList();
			MeshFace newface = new MeshFace(ed1, ed2, edge);
			edge = newface.addEdge(edge);
			ed1 = newface.addEdge(ed1);
			ed2 = newface.addEdge(ed2);	
			newface = meshF.addFace(newface);
			meshF.addElement(newface);
		}
				
		// Parcours des faces linkees aux noeuds de e. On remplace les deux noeuds de e par ceux de edge
		boolean finish = false;
		MeshNode n = n1;
		MeshNode node = node1;
		while (finish!=true)
		{
			HashSet nfaces = n.getElements();
			Iterator itn = nfaces.iterator();
			while (itn.hasNext())
			{
				MeshFace fn = (MeshFace)itn.next();
				MeshEdge e1 = null;
				MeshEdge e2 = null;
				MeshEdge e3 = null;
				
				HashSet nedges = fn.getEdges();
				if (nedges.contains(edge) 
					//&& (!meshE.isCircular())
					)
					continue; // cas deja traite au-dessus
				Iterator itne = nedges.iterator();
				while (itne.hasNext())
				{
					MeshEdge ed = (MeshEdge)itne.next();
					
					MeshMesh m = null;
					if (ed.equals(e))
					{
						m = meshEdge;
					}
					else
					{
						// find the submesh containing ed
						Iterator itm = meshF.getSetOfSubMesh().iterator();
						while (itm.hasNext())
						{
							m = (MeshMesh)itm.next();
							if (m.getEdgelist().contains(ed))
								break;
							else m = null;
						}
						if (m == null) m = meshF;
					}
					if (!ed.getNodes().contains(n))
					{
						if (e1 == null) 
						{
							e1 = m.addEdge(ed);
							//e1.setWire(ed.isWire());
						}
						else if (e2 == null) 
						{
							e2 = m.addEdge(ed);
							//e2.setWire(ed.isWire());
						}
						else if (e3 == null) 
						{
							e3 = m.addEdge(ed);
							//e3.setWire(ed.isWire());
						}
					}
					if (ed.getNodes1().equals(n))
					{
						if (e1 == null) 
						{
							e1 = m.getEdgeDefinedByNodes(node, ed.getNodes2());
							if (e1==null)
								e1 = new MeshEdge(node,ed.getNodes2());
							e1.setWire(ed.isWire());
							e1 = m.addEdge(e1);
						}
						else if (e2 == null) 
						{
							e2 = m.getEdgeDefinedByNodes(node, ed.getNodes2());
							if (e2==null)
								e2 = new MeshEdge(node,ed.getNodes2());
							e2.setWire(ed.isWire());
							e2 = m.addEdge(e2);
						}
						else if (e3 == null) 
						{
							e3 = m.getEdgeDefinedByNodes(node, ed.getNodes2());
							if (e3==null)
								e3 = new MeshEdge(node,ed.getNodes2());
							e3.setWire(ed.isWire());
							e3 = m.addEdge(e3);
						}
					}
					else if (ed.getNodes2().equals(n))
					{
						if (e1 == null) 
						{
							e1 = m.getEdgeDefinedByNodes(ed.getNodes1(), node);
							if (e1==null)
								e1 = new MeshEdge(ed.getNodes1(), node);
							e1.setWire(ed.isWire());
							e1 = m.addEdge(e1);
						}
						else if (e2 == null) 
						{
							e2 = meshF.getEdgeDefinedByNodes(ed.getNodes1(), node);
							if (e2==null)
								e2 = new MeshEdge(ed.getNodes1(), node);
							e2.setWire(ed.isWire());
							e2 = m.addEdge(e2);
						}
						else if (e3 == null) 
						{
							e3 = m.getEdgeDefinedByNodes(ed.getNodes1(), node);
							if (e3==null)
								e3 = new MeshEdge(ed.getNodes1(), node);
							e3.setWire(ed.isWire());
							e3 = m.addEdge(e3);
						}
					}

					ed.setWire(false);
					ed.setFrozen(false);
				}
				meshF.rmFace(fn);
				fn.clearList();
				MeshFace newfn = new MeshFace(e1,e2,e3);
				e1 = newfn.addEdge(e1);
				e2 = newfn.addEdge(e2);
				e3 = newfn.addEdge(e3);
				newfn = meshF.addFace(newfn);
				meshF.addElement(newfn);
			}
			
			switch (n.getPosition().getType())
			{
				case PST_Position.DEGENERATEDLINE :
					//test
					logger.debug("degenere");
					break;
				case PST_Position.VERTEX :	// il faut supprimer le submesh decrivant n
					if (n.equals(node)) 
						break;
					// rechercher le subMesh contenant le vertex n
					TopoDS_Vertex V = (TopoDS_Vertex)(n.getPosition()).getShape();
					if (V!=null)
					{
						((MeshOfCADCompound)myMesh).removeSubMesh(V);
					}
					break;
			}
			if (n==n2) finish = true;
			n = n2;
			node = node2;
		}*/
	}

	/**
	 * private method to compute the PST_SurfacePosition of a node located on an edge on the given surface.
	 * @param topoface : the topoDS_Face to compute node position
	 * @param topoedge : the topoDS_Edge underlying the node
	 * @param param : the linear parameter on the node on the topoedge.
	 * @return the PST_SurfacePosition of the node on the surface topoface.
	 * To work, the topoDS_Edge must belong to the TopoDS_Face .
	 */
	static private PST_SurfacePosition getSurfacePosition(TopoDS_Face topoface, TopoDS_Edge topoedge, double param)
	{
		double[] res = new double[2];
		Geom2d_Curve Curve2d = BRep_Tool.curveOnSurface(topoedge, topoface, res);
		Geom2dAdaptor_Curve C2d = new Geom2dAdaptor_Curve(Curve2d);
		double[] pE = C2d.value((float) param);
		return new PST_SurfacePosition(topoface, pE[0], pE[1]);		 
	}
	
	private static PST_Position checkCorrectnessOfPosition(MeshNode myNode,
		double[] lpos)
	{
		PST_Position npos = null;
		PST_Position pos= myNode.getPosition();
		if ( (lpos[0]==0.)&&(lpos[1]==0.) ) return myNode.getPosition();
		switch (pos.getType())
		{
			case PST_Position.DEGENERATEDLINE :
			case PST_Position.EDGE :
				double param = ((PST_LinePosition)pos).getParam();
				double newparam = param + lpos[0];
				TopoDS_Edge E= (TopoDS_Edge)((PST_LinePosition)pos).getShape();
				// new pos out out range ?
				double range[] = new double[2];
				Geom_Curve curve = BRep_Tool.curve(E, range);
				if ( (range[0]<range[1]) && (newparam < range[0])||(newparam > range[1]) )
					return pos;
				else if ( (range[0]>range[1]) && (newparam > range[0])||(newparam < range[1]) )
					return pos;
				else
				{
					ArrayList voisins = new ArrayList();
					int i = 0;
					Iterator it = myNode.getEdgesIterator();
					while (it.hasNext())
					{
						MeshEdge e = (MeshEdge)it.next();
						if ((e.getNodes1().pos.getShape()).equals(e.getNodes2().pos.getShape()))
						{
							// retrieve the neigbour
							if (e.getNodes1().equals(myNode)) voisins.add(i, e.getNodes2());
							else voisins.add(i,e.getNodes1());
							i++;
						}
					}
					double param1 = ((PST_LinePosition)((MeshNode)voisins.get(0)).pos).getParam();
					double param2 = ((PST_LinePosition)((MeshNode)voisins.get(1)).pos).getParam();
					if (param1 > param2)
					{
						double temp = param1;
						param1 = param2;
						param2 = temp;
					}
					// new pos must be between both neigbours
					if ( (newparam < param1) && (newparam > param2) ) return pos;
					else
					{
						npos = new PST_LinePosition(E,newparam);
						return npos;
					}
				}
//				break;
			case PST_Position.SURFACE :
				TopoDS_Face F = (TopoDS_Face)((PST_SurfacePosition)pos).getShape();
				//Geom_Surface surface = BRep_Tool.surface(F);
				double [] uv = ((PST_SurfacePosition)pos).getParam();
				double [] newuv = new double[2];
				newuv[0] = uv[0] + lpos[0];
				newuv[1] = uv[1] + lpos[1];
				if ( (newuv[0] != uv[0]) || (newuv[1] != uv[1]) )
				{
					npos = new PST_SurfacePosition(F,newuv[0], newuv[1]);
					return npos;
				}
				break;
		}
		return pos;
	}
	
	/**
	 * Method computeLinearizedPositionOfNode.
	 * @return double[] - an array containing the displacement of the node . in u and v if the node is classified on a
	 * surface, t and 0 if the node is classified on an edge.
	 */
	static public double [] computeLinearizedPositionOfNode(MeshNode myNode)
	{
		double [] toreturn = new double[2];
		double bary[]= new double[3];
		
		// Retrieves the neighbours of n
		int nc = 0;
		HashSet temp = new HashSet();
		
		Collection nodes=myNode.getNeighboursNodes();
		
		switch (myNode.getPosition().getType())
		{
			case PST_Position.EDGE :
				// retrieve the TopoDS_Edge
				TopoDS_Edge EE = (TopoDS_Edge)myNode.getPosition().getShape();
				// Retrieves the neighbours of n classified on the same edge
				temp.clear();
				Iterator itn = nodes.iterator();
				while (itn.hasNext())
				{
					MeshNode node = (MeshNode)itn.next();
					if ((node.getPosition().getType() == PST_Position.EDGE) &&
						(((TopoDS_Edge)node.getPosition().getShape()).equals(EE)))
						temp.add(node);
					else if ( (node.getPosition().getType() == PST_Position.VERTEX) &&
						(node.getPosition().getShape().equals(EE)))
						temp.add(node);
				}
				nodes.clear();
				nodes.addAll(temp);
				// Compute barycentre location of the neighbours
				Iterator it = nodes.iterator();
				while (it.hasNext())
				{
					MeshNode nj = (MeshNode)it.next();
					bary[0] += nj.getX();
					bary[1] += nj.getY();
					bary[2] += nj.getZ();
				}
				nc = nodes.size();
				if (nc != 0)
				{
					bary[0] /= nc;
					bary[1] /= nc;
					bary[2] /= nc;
				}
				else return toreturn;
				
				// retrieve the Geom_Curve
				double range[] = new double[2];
				Geom_Curve curve = BRep_Tool.curve(EE, range);
				GeomLProp_CLProps cprop = new GeomLProp_CLProps(1,0.0001d);
				cprop.setCurve(curve);
				PST_LinePosition posc = (PST_LinePosition)myNode.getPosition();
				Pair Et = posc.getCurvePosition();
				double param = ((Double)Et.second).doubleValue();
				cprop.setParameter(param);
				double d = Calculs.prodSca(cprop.d1(), cprop.d1());
				if (d != 0)
				{
					double [] N = new double[3];
					N[0] = myNode.getX()+bary[0];
					N[1] = myNode.getY()+bary[1];
					N[2] = myNode.getZ()+bary[2];
					toreturn [0] = Calculs.prodSca(cprop.d1(),N)/d;
					toreturn [0] = 0.;
				}
				break;
				
			case PST_Position.SURFACE :
				// Compute barycentre location of the neighbours
				nc = nodes.size();
				if (nc == 0)
					return toreturn;
				it = nodes.iterator();
				while (it.hasNext())
				{
					MeshNode nj = (MeshNode)it.next();
					bary[0] += nj.getX();
					bary[1] += nj.getY();
					bary[2] += nj.getZ();
				}
				bary[0] /= nc;
				bary[1] /= nc;
				bary[2] /= nc;
				// retrieve the Geom_Surface
				TopoDS_Face FF = (TopoDS_Face)myNode.getPosition().getShape();
				
				Geom_Surface surface = BRep_Tool.surface(FF);
				GeomLProp_SLProps sprop = new GeomLProp_SLProps(1,0.0001d);
				sprop.setSurface(surface);
				PST_SurfacePosition poss = (PST_SurfacePosition)myNode.getPosition();
				Pair uv = poss.getSurfacePosition();
				sprop.setParameter(((Double)uv.first).doubleValue(),((Double)uv.second).doubleValue());
				double a = Calculs.prodSca(sprop.d1U(), sprop.d1U());
				double b = Calculs.prodSca(sprop.d1U(),sprop.d1V());
				double c = 0.;
				double [] N = new double[3];
				N[0] = bary[0]-myNode.getX();
				N[1] = bary[1]-myNode.getY();
				N[2] = bary[2]-myNode.getZ();
				c = Calculs.prodSca(sprop.d1U(),N);
				double e = Calculs.prodSca(sprop.d1V(),sprop.d1V());
				double f = Calculs.prodSca(sprop.d1V(),N);
				if ((a*e)-(b*b) != 0.)
				{
					toreturn[0] = ( ((c*e)-(b*f))/((a*e)-(b*b)) );
					toreturn[1] = ( ((a*f)-(c*b))/((a*e)-(b*b)) );
				}
				break;
			default :;
		}
		return toreturn;
	}	
}
