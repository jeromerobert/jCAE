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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.mesher.ds;

import gnu.trove.TObjectIntHashMap;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import org.apache.log4j.Logger;
import org.jcae.mesh.cad.CADFace;
import org.jcae.mesh.cad.CADGeomSurface;


/**
 * 3D discretization of the whole shape.
 * This class contains the 3D mesh of the whole shape.  Here is the typical
 * lifecycle of these instances:
 * <ol>
 *   <li>A geometry file is read and a 1D mesh is generated.</li>
 *   <li>A <code>MMesh3D</code> instance is created.</li>
 *   <li>For all topological surfaces of this shape, computes its 2D meshing
 *       (the 1D mesh is identical for all surfaces) and imports all meshes
 *       into the 3D space by calling {@link #addSubMesh2D}.</li>
 *   <li>Write the 3D mesh onto disk and deletes this instance.</li>
 * </ol>
 */

public class MMesh3D
{
	private static Logger logger=Logger.getLogger(MMesh3D.class);	

	//  Group list.
	private Collection grouplist = new ArrayList();
	
	//  Face list.
	private Collection facelist = new ArrayList();
	
	//  Node list.
	private Collection nodelist = new ArrayList();
	
	//  Mapping 1D -> 3D for boundary nodes.  It must be a class variable
	//  because it is shared between all topological surfaces.
	//  TODO: In particular this implies that addSubMesh2D has to be
	//  synchronized.
	private Map mapNode1DToNode3D = new HashMap();
	private MNode3D [] arrayLabelToNode3D = new MNode3D[10];
	
	/**
	 * Returns an iterator over the set of nodes.
	 *
	 * @return an iterator over the set of nodes.
	 */
	public Iterator getNodesIterator()
	{
		return nodelist.iterator();
	}
	
	/**
	 * Returns the number of nodes.
	 *
	 * @return the number of nodes.
	 */
	public int getNumberOfNodes()
	{
		return nodelist.size();
	}
	
	/**
	 * Returns an iterator over the set of faces.
	 *
	 * @return an iterator over the set of faces.
	 */
	public Iterator getFacesIterator()
	{
		return facelist.iterator();
	}
	
	/**
	 * Returns the number of faces.
	 *
	 * @return the number of faces.
	 */
	public int getNumberOfFaces()
	{
		return facelist.size();
	}
	
	/**
	 * Returns an iterator over the set of groups.
	 *
	 * @return an iterator over the set of groups.
	 */
	public Iterator getGroupsIterator()
	{
		return grouplist.iterator();
	}
	
	/**
	 * Returns the number of groups.
	 *
	 * @return the number of groups.
	 */
	public int getNumberOfGroups()
	{
		return grouplist.size();
	}
	
	public void addGroup(MGroup3D g)
	{
		grouplist.add(g);
	}
	
	public void addFace(MFace3D f)
	{
		facelist.add(f);
	}
	
	public void addNode(MNode3D n)
	{
		nodelist.add(n);
	}
	
	/**
	 * Imports a <code>SubMesh2D</code> instance into the current
	 * <code>MMesh3D</code> structure.
	 * The 2D mesh has to be placed on the 3D space, by taking care of
	 * removing duplicate boundary nodes.
	 *
	 * @param submesh  the <code>SubMesh2D</code> instance to insert.
	 */
	public void addSubMesh2D(SubMesh2D submesh, CADFace F, boolean hasLabels)
	{
		HashMap mapNode2DToNode3D = new HashMap();
		CADGeomSurface surface = F.getGeomSurface();
		//  First derivative is needed to compute normals
		surface.dinit(1);
		double [] p;
		Iterator itn = submesh.getNodesIterator();
		while (itn.hasNext())
		{
			MNode2D n2 = (MNode2D) itn.next();
			assert (!mapNode2DToNode3D.containsKey(n2));
			p = n2.getUV();
			surface.setParameter(p[0], p[1]);
			if (hasLabels)
			{
				int label = n2.getLabel();
				if (-1 == label || label >= arrayLabelToNode3D.length || null == arrayLabelToNode3D[label])
				{
					//  If null == n1, the point is not on a boundary and is thus
					//  unique.  Otherwise, if mapNode1DToNode3D does not already
					//  contain n1, this 1D node is inserted for the first time.
					MNode3D n3 = new MNode3D(n2, F);
					n3.addNormal(surface.normal());
					mapNode2DToNode3D.put(n2, n3);
					nodelist.add(n3);
					if (-1 != label)
					{
						if (label >= arrayLabelToNode3D.length)
						{
							MNode3D [] temp = new MNode3D[label+10];
							System.arraycopy(arrayLabelToNode3D, 0, temp, 0, arrayLabelToNode3D.length);
							arrayLabelToNode3D = temp;
						}
						arrayLabelToNode3D[label] = n3;
					}
				}
				else
				{
					mapNode2DToNode3D.put(n2, arrayLabelToNode3D[label]);
					arrayLabelToNode3D[label].addNormal(surface.normal());
				}
			}
			else
			{
				MNode1D n1 = n2.getRef();
				if (null == n1 || !mapNode1DToNode3D.containsKey(n1))
				{
					//  If null == n1, the point is not on a boundary and is thus
					//  unique.  Otherwise, if mapNode1DToNode3D does not already
					//  contain n1, this 1D node is inserted for the first time.
					MNode3D n3 = new MNode3D(n2, F);
					n3.addNormal(surface.normal());
					mapNode2DToNode3D.put(n2, n3);
					nodelist.add(n3);
					if (null != n1)
						mapNode1DToNode3D.put(n1, n3);
				}
				else
				{
					MNode3D n3 = (MNode3D) mapNode1DToNode3D.get(n1);
					n3.addNormal(surface.normal());

					mapNode2DToNode3D.put(n2, n3);
				}
			}
		}
		
		Iterator itf = submesh.getFacesIterator();
		Collection newfacelist = new ArrayList();
		double quality = 2.0*Math.PI;
		while (itf.hasNext())
		{
			MFace2D f2 = (MFace2D) itf.next();
			MNode3D [] pt = new MNode3D[3];
			int i = 0;
			itn = f2.getNodesIterator();
			while (itn.hasNext())
			{
				MNode2D n2 = (MNode2D) itn.next();
 				pt[i] = (MNode3D) mapNode2DToNode3D.get(n2);
				assert null != pt[i];
				i++;
			}
			MFace3D f3 = new MFace3D(pt[0], pt[1], pt[2]);
			if (logger.isDebugEnabled())
				quality = Math.min(quality, f3.quality());
			facelist.add(f3);
			newfacelist.add(f3);
		}
		grouplist.add(new MGroup3D(""+(grouplist.size()+1), newfacelist));
		logger.debug("Submesh 3D quality (deg.): "+quality * 180.0 / Math.PI);
	}
	
	/**
	 * Writes the mesh into a UNV file.
	 * If the filename has a <code>.gz</code> suffix, it is compressed
	 * with gzip.
	 *
	 * @param file  file name
	 */
	public void writeUNV(String file)
	{
		String cr=System.getProperty("line.separator");
		PrintWriter out;
		try {
			if (file.endsWith(".gz") || file.endsWith(".GZ"))
				out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)));
			else
				out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)));
			out.println("    -1"+cr+"  2411");
			TObjectIntHashMap labelsN = new TObjectIntHashMap(nodelist.size());
			int count =  0;
			for(Iterator it=nodelist.iterator();it.hasNext();)
			{
				MNode3D node=(MNode3D) it.next();
				count++;
				labelsN.put(node, count);
				out.println(count+"         1         1         1");
				out.println(""+node.getX()+" "+node.getY()+" "+node.getZ());
			}
			out.println("    -1");
			out.println("    -1"+cr+"  2412");
			TObjectIntHashMap labelsF = new TObjectIntHashMap(facelist.size());
			count =  0;
			for(Iterator it=facelist.iterator();it.hasNext();)
			{
				MFace3D face=(MFace3D)it.next();
				count++;
				labelsF.put(face, count);
				out.println(""+count+"        91         1         1         1         3");
				for(Iterator itn=face.getNodesIterator();itn.hasNext();)
				{
					MNode3D node=(MNode3D) itn.next();
					out.print(" "+labelsN.get(node));
				}
				out.println("");
			}
			out.println("    -1");
			out.println("    -1"+cr+"  2430");
			count =  0;
			for(Iterator it=grouplist.iterator();it.hasNext();)
			{
				MGroup3D group=(MGroup3D)it.next();
				count++;
				out.println("1      0         0         0         0         0         0      "+group.numberOfFaces());
				out.println(group.getName());
				int countg=0;
				for(Iterator itf=group.getFacesIterator();itf.hasNext();)
				{
					MFace3D face=(MFace3D) itf.next();
					out.print("         8"+spaces(""+labelsF.get(face)));
					countg++;
					if ((countg % 4) == 0)
						out.println("");
				}
				if ((countg % 4) !=0 )
					out.println("");
			}
			out.println("    -1");
			out.close();
		} catch (FileNotFoundException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		} catch (IOException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}
	}
	
	public void writeMESH(String file)
	{
		String cr=System.getProperty("line.separator");
		PrintWriter out;
		try {
			if (file.endsWith(".gz") || file.endsWith(".GZ"))
				out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)));
			else
				out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)));
			out.println("\nMeshVersionFormatted 1");
			out.println("\nDimension\n3");
			out.println("\nVertices\n"+nodelist.size());

			TObjectIntHashMap labelsN = new TObjectIntHashMap(nodelist.size());
			int count =  0;
			for(Iterator it=nodelist.iterator();it.hasNext();)
			{
				MNode3D node = (MNode3D) it.next();
				count++;
				labelsN.put(node, count);
				out.println(""+node.getX()+" "+node.getY()+" "+node.getZ()+" 0");
			}
			int normalCount =  0;
			int [] normalOffset = new int[nodelist.size()];
			count =  0;
			for(Iterator it=nodelist.iterator();it.hasNext();)
			{
				MNode3D node = (MNode3D) it.next();
				normalOffset[count] = normalCount;
				count++;
				double [] normal = node.getNormal();
				normalCount += normal.length / 3;
			}
			out.println("\nTriangles\n"+facelist.size());
			count =  0;
			for(Iterator it=grouplist.iterator();it.hasNext();)
			{
				MGroup3D group=(MGroup3D)it.next();
				count++;
				for(Iterator itf=group.getFacesIterator();itf.hasNext();)
				{
					MFace3D face=(MFace3D) itf.next();
					for(Iterator itn=face.getNodesIterator();itn.hasNext();)
					{
						MNode3D node=(MNode3D) itn.next();
						out.print(labelsN.get(node)+" ");
					}
					out.println(" "+count);
				}
			}
			out.println("\nNormals\n"+normalCount);
			for(Iterator it=nodelist.iterator();it.hasNext();)
			{
				MNode3D node=(MNode3D) it.next();
				double [] normal = node.getNormal();
				for (int i = 0; i < normal.length/3; i++)
					out.println(normal[3*i]+" "+normal[3*i+1]+" "+normal[3*i+2]);
			}
			out.println("\nNormalAtTriangleVertices\n"+(3*facelist.size()));
			count =  0;
			for(Iterator it=grouplist.iterator();it.hasNext();)
			{
				MGroup3D group=(MGroup3D)it.next();
				HashSet nodeSet = new HashSet(nodelist.size());
				for(Iterator itf=group.getFacesIterator();itf.hasNext();)
				{
					MFace3D face=(MFace3D) itf.next();
					count++;
					Iterator itn = face.getNodesIterator();
					for (int i = 0; i < 3; i++)
					{
						MNode3D node = (MNode3D) itn.next();       
						if (!nodeSet.contains(node))
						{
							normalOffset[labelsN.get(node)-1] += 3;
							nodeSet.add(node);
						}
						out.println(""+count+" "+(i+1)+" "+(normalOffset[labelsN.get(node)-1] - 2));
					}
				}
			}
			out.println("\nEnd");
			out.close();
		} catch (FileNotFoundException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		} catch (IOException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}
	}
	
	public void removeNode(MNode3D n)
	{
		nodelist.remove(n);
	}	
	
	/** Add spaces before a string to make it 10 characters */
	private String spaces(String s)
	{
		int n = 10 - s.length();
		char[] c = new char[n];
		for (int i=0; i<n; i++)
			c[i]=' ';
		return (new String(c))+s;
	}	
	
	/**
	 * Displays mesh statistics.  This routine does nothing if
	 * <code>logger.debug</code> is not enabled.
	 */
	public void printInfos()
	{
		logger.info("Number of nodes: "+nodelist.size());
		logger.info("Number of faces: "+facelist.size());
		if (!logger.isDebugEnabled())
			return;
		int bnodes = 0;
		for (Iterator itn = nodelist.iterator(); itn.hasNext(); )
		{
			MNode3D n = (MNode3D) itn.next();
			if (-1 != n.getRef())
				bnodes++;
		}
		logger.debug("Number of boundary nodes: "+bnodes);
		logger.debug("Mesh quality (deg.): "+quality() * 180.0 / Math.PI);
	}
	
	/**
	 * Returns the mesh quality.
	 *
	 * @return the mesh quality
	 */
	public double quality()
	{
		double q = 2.0*Math.PI;
		double q2;
		for(Iterator it=facelist.iterator();it.hasNext();)
		{
			MFace3D face=(MFace3D)it.next();
			q2 = face.quality();
			if (q2 < q)
				q = q2;
		}
		return q;
	}
	
	public String toString()
	{
		String cr=System.getProperty("line.separator");
		String r="MMesh3D";
		logger.debug("Printing "+r+"...");
		r+=cr;
		for(Iterator it=nodelist.iterator();it.hasNext();)
		{
			MNode3D node=(MNode3D)it.next();
			r+=node+cr;
		}
		for(Iterator it=facelist.iterator();it.hasNext();)
		{
			MFace3D face=(MFace3D)it.next();
			r+=face+cr;
		}
		logger.debug("...done");
		return r;
	}
}
