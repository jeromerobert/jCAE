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

package org.jcae.mesh;

import org.omg.PortableServer.POA;
import org.omg.CORBA.portable.ObjectImpl;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.opencascade.corba.OpencascadeImpl;
import org.jcae.mesh.algos.*;
import org.jcae.mesh.sd.MeshMesh;
import org.jcae.mesh.sd.MeshNode;
import org.jcae.mesh.sd.MeshEdge;
import org.jcae.mesh.sd.MeshOfCAD;
import org.jcae.mesh.util.Pair;
import org.jcae.mesh.drivers.*;
import org.apache.log4j.*;
import java.util.*;
import java.io.*;

/**
 * @author  Jerome Robert
 */

public class MeshObjectImpl implements MeshObjectOperations
{
	private static Logger logger=Logger.getLogger(MeshObjectImpl.class);
	private MeshMesh meshMesh;
	private MeshHypothesis hypo;
	private MeshConstraint cons;
	private double length;
	private double discretisation;
	private MeshModuleImpl module;
	
	/** Creates a new instance of MeshObjectImpl to mesh a TopoDS_Shape */
	public MeshObjectImpl(org.jcae.opencascade.corba.TopoDS_Shape shape, MeshModuleImpl module)
	{
		this.module=module;
		TopoDS_Shape nshape=(TopoDS_Shape)OpencascadeImpl.getDelegate(shape);
		meshMesh=new MeshOfCAD(nshape);
		setTypeAlgo((short)MeshHypothesis.TRIA3);
		setDiscretisation(1);
	}

	/** Creates a new instance of MeshObjectImpl from an already existing MeshMesh */
	public MeshObjectImpl(MeshMesh mesh, MeshModuleImpl module)
	{
		this.module=module;		
		meshMesh=mesh;		
	}

	/** Creates a MeshObjectImpl wrapping an empty MeshMesh */
	public MeshObjectImpl(MeshModuleImpl module)
	{
		this(new MeshMesh(), module);
	}

	
	public void runOldBasicMesh()
	{
		new BasicMesh().compute((MeshOfCAD)meshMesh,cons);
	}
	
	public void runBasicMesh()
	{
		new NewMesher().compute((MeshOfCAD)meshMesh,cons);
	}
	
	public void runBasicMesh2()
	{
		new BasicMesh2().compute((MeshOfCAD)meshMesh,cons);
	}
	
	public void runTargetSizeConvergence()
	{
		double len = cons.getValue();
		new MiscAlgos((MeshOfCAD)meshMesh).runTargetSizeConvergence(len/Math.sqrt(2), len*Math.sqrt(2));
	}

	public void runCleanMesh()
	{
		CleanMesh.compute((MeshOfCAD)meshMesh);		
	}
	
	public void runImproveConnect()
	{
		new MiscAlgos((MeshOfCAD)meshMesh).runImproveConnect();
	}
	
	public void runReduceNodeConnection()
	{
		new MiscAlgos((MeshOfCAD)meshMesh).runReduceNodeConnection();
	}
	
	public void runRefine()
	{
		new MiscAlgos((MeshOfCAD)meshMesh).runRefine(discretisation);
	}
	
	public void runFaceSmoothing(int nbiteration)
	{
		Smoothing.runFaceSmoothing((MeshOfCAD)meshMesh,nbiteration);
	}

	public void runPatchSmoothing(int nbiteration)
	{
		Smoothing.runPatchSmoothing((MeshOfCAD)meshMesh, nbiteration);
	}

	public void runErrorDetection()
	{
		new MiscAlgos((MeshOfCAD)meshMesh).runErrorDetection();
	}
	
	public void runStats()
	{
		new MiscAlgos((MeshOfCAD)meshMesh).runStats();
	}
	
	public void setDiscretisation(double discr)
	{
		discretisation=discr;
		// Compute discretisation length
		if (hypo.getType()==MeshHypothesis.TRIA3)
			length = Math.sqrt(4*discr/Math.sqrt(3.));
		else if (hypo.getType()==MeshHypothesis.BEAM)
			length = discr;
		
		cons = new MeshConstraint(1,hypo.getType(),length);
		MeshConstraint scale = new MeshConstraint(1, 10000);
		MeshNode.scale = new Double(scale.getValue()).intValue();		
	}
	
	public void setTypeAlgo(short algo)
	{
		hypo = new MeshHypothesis("anHypothesis",1,algo);
	}
	
	public void runCollapse(double maxEdgeLength)
	{
		new MiscAlgos((MeshOfCAD)meshMesh).runCollapse(maxEdgeLength);
	}
	
	void importSFM(String str)
	{
		try
		{
			SFMReader reader=new SFMReader(new FileInputStream(str),meshMesh);
			reader.readMesh();
		} catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public MeshGroup getAsGroup()
	{
		MeshGroupImpl impl=new MeshGroupImpl(meshMesh.getAsGroup(),module);
		return (MeshGroup)module.activateObject(impl,""+module+"/"+this+"/"+impl);
	}
	
	public MeshGroup getGroup(int param)
	{
		MeshGroupImpl impl=new MeshGroupImpl(meshMesh.getGroup(param),module);
		return (MeshGroup)module.activateObject(impl,""+module+"/"+this+"/"+impl);
	}
	
	public int[] getGroupIDs()
	{
		int[] r=new int[meshMesh.numberOfGroups()];
		Iterator it=meshMesh.getGroupsIterator();
		for(int i=0;i<r.length;i++)
		{
			r[i]=((org.jcae.mesh.sd.MeshGroup)it.next()).getID();
		}
		return r;
	}
	
	public String toString()
	{
		String toReturn=module.getPath(this);
		if(toReturn==null)
		{	
			//If the MeshObject is not yet binded
			toReturn="Mesh@"+hashCode()+"."+
				MeshModuleImpl.dotEscapedNoImplClassName(getClass());
		}
		else
		{
			toReturn=module.getObjectName(this)+"."+
				MeshModuleImpl.dotEscapedNoImplClassName(getClass());
		}
		return toReturn;
	}
	
	public MeshMesh getImplementation()
	{
		return meshMesh;
	}
	
	void importUNV(String filename)
	{
		try
		{
			UNVReader reader=new UNVReader(new FileInputStream(filename),meshMesh);
			reader.readMesh();
		} catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}	
	
	void importMESH(String filename)
	{
		try
		{
			MSHReader reader=new MSHReader(new FileInputStream(filename),meshMesh);
			reader.readMesh();
		} catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}	
	
	/** Save the group as an UNV file
	 * @param filename The name of the file where to save the mesh
	 */	
	public void saveUNV(String filename)
	{
		try
		{
			UNVWriter writer=new UNVWriter(new FileOutputStream(filename), meshMesh);
			writer.writeMesh();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	/** Save the group as an XML file
	 * @param filename The name of the file where to save the mesh
	 */	
	public void saveXML(String filename)
	{
		try
		{
			XMLWriter writer=new XMLWriter(new FileOutputStream(filename), meshMesh);
			writer.writeMesh();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void insertGroup(MeshGroup group)
	{
		// get local implementation of group
		ObjectImpl oi=(ObjectImpl)group;
		org.omg.CORBA.portable.ServantObject _so = oi._servant_preinvoke( "",Object.class);
		MeshGroupPOATie localServantTie = (MeshGroupPOATie)_so.servant;
		MeshGroupImpl groupImpl=(MeshGroupImpl)localServantTie._delegate();
		meshMesh.insertGroup(groupImpl.getImplementation());
	}	
	
	public void deleteGroup(MeshGroup group)
	{
		org.jcae.mesh.sd.MeshGroup g=meshMesh.getGroup(group.getID());		
		meshMesh.removeGroup(g);

		// get local implementation of group
		ObjectImpl oi=(ObjectImpl)group;
		org.omg.CORBA.portable.ServantObject _so = oi._servant_preinvoke( "",Object.class);
		MeshGroupPOATie localServantTie = (MeshGroupPOATie)_so.servant;
		MeshGroupImpl groupImpl=(MeshGroupImpl)localServantTie._delegate();		
		module.unbind(module.getPath(groupImpl));
	}	
}
