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
import org.jcae.view3d.*;
import org.apache.log4j.*;


/** This is an implementation of the {@link MeshScene} CORBA object
 * @author Jerome Robert
 */
public class MeshSceneImpl extends org.jcae.view3d.Scene3DImpl implements MeshSceneOperations
{
	private static Logger logger=Logger.getLogger(Object3DImpl.class);	
	
	/** Creates a new instance of MeshSceneImpl
	 * @param poa
	 */
	public MeshSceneImpl(POA poa)
	{
		super(poa);
	}
	
	/** Show or hide free edges. This methodes do not update the scene and
	 * will not display the changes. You must explicitly refresh the display
	 * @param viewableMesh The ViewableMesh whose free edges will be shown/hidden
	 * @param show
	 */		
	public void showFreeEdges(ViewableMesh viewableMesh, boolean show)
	{		
		viewableMesh.showFreeEdges(show);
		pushEvent(new Event(viewableMesh,Scene3D.EVENT_GEOMETRY_CHANGED,
			ViewableMesh.GEOM_FREE_EDGE));
	}
	
	/** Show or hide the mesh. This methodes do not update the scene and
	 * will not display the changes. You must explicitly refresh the display
	 * @param viewableMesh The ViewableMesh whose mesh will be shown
	 * @param show
	 */		
	public void showMesh(ViewableMesh viewableMesh, boolean show)
	{
		viewableMesh.showMesh(show);
		pushEvent(new Event(viewableMesh,Scene3D.EVENT_GEOMETRY_CHANGED,
			ViewableMesh.GEOM_MESH));
	}
	
	/** Show or hide multiple edges. This methodes do not update the scene and
	 * will not display the changes. You must explicitly refresh the display
	 * @param viewableMesh The ViewableMesh whose multi-edges will be shown
	 * @param show
	 */		
	public void showTEdges(ViewableMesh viewableMesh, boolean show)
	{
		viewableMesh.showTEdges(show);
		pushEvent(new Event(viewableMesh,Scene3D.EVENT_GEOMETRY_CHANGED,
			ViewableMesh.GEOM_MULTI_EDGE));
	}	
	
	public void showWorstTriangle(ViewableMesh viewableMesh, float quality)
	{
		viewableMesh.showWorstTriangle(quality);
		pushEvent(new Event(viewableMesh,Scene3D.EVENT_GEOMETRY_CHANGED,
			ViewableMesh.GEOM_BAD_TRIANGLE));
	}	
	
	public void pick(Object3D o, float[] point, float[] closestVertex)
	{
		ViewableMesh vm=ViewableMeshHelper.narrow(o);
		
		// get local implementation of mesh
		ObjectImpl oi=(ObjectImpl)vm;
		org.omg.CORBA.portable.ServantObject _so = oi._servant_preinvoke( "",Object.class);
		ViewableMeshPOATie localServantTie = (ViewableMeshPOATie)_so.servant;
		ViewableMeshImpl viewableMeshImpl=(ViewableMeshImpl)localServantTie._delegate();		
		
		viewableMeshImpl.pick(point, closestVertex);
		showMesh(vm,true);
		showFreeEdges(vm,true);
	}	

}
