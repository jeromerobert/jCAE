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

package org.jcae.mesh;
import org.omg.PortableServer.POA;
import org.omg.CORBA.ORB;
import org.jcae.opencascade.corba.*;
import org.omg.CORBA.portable.ObjectImpl;
import org.jcae.util.Palette;
import java.lang.reflect.*;
import org.jcae.mesh.sd.MeshMesh;
/**
 *
 * @author  Jerome Robert
 */

public class MeshModuleImpl extends org.jcae.util.JCAEModuleImpl implements MeshModuleOperations
{
	/** palette for color of ViewableMesh */
	private Palette palette;
	private int paletteIndex;
	
	/** Creates a new instance of MeshModuleImpl */
	public MeshModuleImpl(ORB orb)
	{
		super(orb);
		palette=new Palette(37);
		paletteIndex=0;
	}
	
	public MeshObject createMeshObject(TopoDS_Shape shape)
	{		
		MeshObjectImpl impl=new MeshObjectImpl(shape,this);		
		return (MeshObject)activateObject(impl, ""+this+"/"+impl);
	}
	
	public MeshScene createMeshScene()
	{
		MeshScenePOATie s=new MeshScenePOATie(new MeshSceneImpl(_poa));
		try
		{
			_poa.activate_object(s);
		} catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex.toString());
		}
		return s._this();	
	}
	
	private boolean colorEquals(float[] c1, float[] c2)
	{
		for(int i=0;i<2;i++)
		{
			if(c1[i]!=c2[i]) return false;
		}
		return true;
	}
	
	private float[] getColor()
	{
		float[] color;
		boolean b1,b2,b3;
		do
		{
			color=palette.getColor(paletteIndex);
			paletteIndex++;
			b1=colorEquals(color,ViewableMeshImpl.FREE_EDGE_COLOR);
			b2=colorEquals(color,ViewableMeshImpl.T_EDGE_COLOR);
			b3=colorEquals(color,ViewableMeshImpl.BAD_TRIANGLE_COLOR);
		}
		while(b1||b2||b3);
		return color;
	}
	
	public ViewableMesh createViewableMesh(MeshGroup meshGroup)
	{
		ViewableMeshImpl vmi=new ViewableMeshImpl(meshGroup,_poa);
		vmi.setColor(getColor());
		ViewableMeshPOATie s=new ViewableMeshPOATie(vmi);
		try
		{
			_poa.activate_object(s);
		} catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex.toString());
		}
		return s._this();
	}
	
	public String getJavaClassName()
	{
		return "org.jcae.mesh.MeshModule";
	}
	
	public String getName()
	{
		return "MESH";
	}
	
	public org.jcae.mesh.MeshGroup createMeshGroup(org.jcae.mesh.MeshObject mesh)
	{
		// get local implementation of mesh
		ObjectImpl oi=(ObjectImpl)mesh;
		org.omg.CORBA.portable.ServantObject _so = oi._servant_preinvoke( "",Object.class);
		MeshObjectPOATie localServantTie = (MeshObjectPOATie)_so.servant;
		MeshObjectImpl meshImpl=(MeshObjectImpl)localServantTie._delegate();
				
		MeshGroupImpl impl=new MeshGroupImpl(
			new org.jcae.mesh.sd.MeshGroup(meshImpl.getImplementation()),this);
		return (MeshGroup)activateObject(impl, ""+this+"/"+mesh+"/"+impl);		
	}
	
	public MeshObject importSFM(String filename)
	{
		MeshObjectImpl impl=new MeshObjectImpl(this);
		impl.importSFM(filename);
		return (MeshObject)activateObject(impl, renameInPath(""+this+"/"+impl,filename));
	}
	
	public MeshObject importUNV(String filename)
	{
		MeshObjectImpl impl=new MeshObjectImpl(this);
		impl.importUNV(filename);
		return (MeshObject)activateObject(impl, renameInPath(""+this+"/"+impl,filename));
	}	
	
	public MeshObject importMESH(String filename)
	{
		MeshObjectImpl impl=new MeshObjectImpl(this);
		impl.importMESH(filename);
		return (MeshObject)activateObject(impl, renameInPath(""+this+"/"+impl,filename));
	}	
	
	/**
	 * Creates a <code>MeshProcessor</code> instance.
	 * Classes <code>org.jcae.mesh.<i>name</i></code> and
	 * <code>org.jcae.mesh.<i>name</i>Impl</code> must exist.
	 */
	public MeshProcessor createMeshProcessor(String name) throws MeshException
	{		
		MeshProcessorOperations impl;
		try
		{
			Class clazz=Class.forName("org.jcae.mesh."+name+"Impl");
			Constructor constructor=clazz.getConstructor(new Class[0]);
			impl = (MeshProcessorOperations)constructor.newInstance(new Object[0]);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new MeshException(ex.toString());
		}
		return (MeshProcessor)activateObject(impl, null);
	}
	
	public static MeshMesh getMeshMeshFromMeshObject(MeshObject o)
	{
		ObjectImpl oi=(ObjectImpl)o;
		org.omg.CORBA.portable.ServantObject _so = oi._servant_preinvoke( "",Object.class);
		MeshObjectPOATie localServantTie = (MeshObjectPOATie)_so.servant;
		MeshObjectImpl impl=(MeshObjectImpl)localServantTie._delegate();
		return impl.getImplementation();	
	}
}
