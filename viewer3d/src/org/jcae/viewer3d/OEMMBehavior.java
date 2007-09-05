/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;


import javax.media.j3d.*;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.Storage;
import org.jcae.mesh.oemm.MeshReader;
import org.jcae.viewer3d.bg.ViewableBG;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;
import org.apache.log4j.Logger;

import com.sun.jmx.remote.util.CacheMap;

/**
 * Dynamically hide and show voxel in a OEMM viewer
 */
public class OEMMBehavior extends Behavior
{
	private static Logger logger = Logger.getLogger(OEMMBehavior.class);
	private static final int DEFAULT_MAX_TRIANGLES_NBR = -1;
	private boolean frozen = false;
	private MeshReader coarseReader;
	
	private static class ViewHolder
	{
		private BranchGroup viewElem;
		private int id;
		private Mesh mesh;
		private int nrTriangles;

		public ViewHolder(Integer id, Mesh mesh)
		{
			super();
			this.id = id;
			this.mesh = mesh;
			this.nrTriangles = mesh.getTriangles().size();
		}

		public ViewHolder(Integer id, BranchGroup viewElem)
		{
			super();
			this.viewElem = viewElem;
			this.id = id;
		}

		public BranchGroup getViewElement() {
			return viewElem;
		}

		public void setViewElem(BranchGroup viewElem)
		{
			this.viewElem = viewElem;
		}
		
		public int getId()
		{
			return id;
		}

		public static ViewHolder makeViewHolder(OEMM o, int index, Map<Integer, Mesh> nodeToMeshMap)
		{
			Set<Integer> set = new HashSet<Integer>();
			set.add(index);
			Mesh mesh = Storage.loadNodes(o, set, false, true, nodeToMeshMap);
			ViewHolder vh = null;
			if (nodeToMeshMap == null) {
				BranchGroup bg = OEMMViewer.meshOEMM(mesh, false);
				vh = new ViewHolder(index, bg);
			}
			else {
				vh = new ViewHolder(index, mesh);
			}
			vh.nrTriangles = mesh.getTriangles().size();
			return vh;
		}

		public Mesh getMesh()
		{
			return mesh;
		}
	}
	
	private static class VoxelSortHelper implements Comparable<VoxelSortHelper>
	{
		private float distance;
		private int voxelIndex;

		public VoxelSortHelper(float distance, int voxelIndex)
		{
			super();
			this.distance = distance;
			this.voxelIndex = voxelIndex;
		}

		@Override
		public int compareTo(VoxelSortHelper o)
		{
			return Float.compare(this.distance, o.distance);
		}

		public int getVoxelIndex()
		{
			return voxelIndex;
		}
		
	}
	
	interface ChangeListener
	{
		void stateChanged(OEMMBehavior behaviour);
	}
	
	/** Utility field holding list of ChangeListeners. */
	private transient java.util.ArrayList changeListenerList;
	
	/**
	 * The square of the minimal distance between the eye and a displayed
	 * OEMM voxel
	 */ 
	private double d2limit;
	private long maxNumberOfTriangles;
	
	private OEMM oemm;
	
	private boolean oemmActive;
	private View view;
	private Point3d[] voxels;
	
	private WakeupCriterion wakeupFrame;
	
	private WakeupCriterion wakeupTransf;
	private Map<Integer, ViewHolder> coarseOemmNodeId2BranchGroup = new HashMap<Integer, ViewHolder>();
	
	private Map<Integer, ViewHolder> visibleFineOemmNodeId2BranchGroup = new HashMap<Integer, ViewHolder>();
	
	private Map<Integer, ViewHolder> cacheOemmNodeId2BranchGroup ;
	
	private BranchGroup visibleMeshBranchGroup = new BranchGroup();
	
	public OEMMBehavior(View canvas, OEMM oemm, OEMM coarseOEMM)
	{
		
		visibleMeshBranchGroup.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		visibleMeshBranchGroup.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		cacheOemmNodeId2BranchGroup = new CacheMap(100);
		canvas.add(new ViewableBG(visibleMeshBranchGroup));
		boolean cloneBoundaryTriangles = Boolean.getBoolean("org.jcae.viewer3d.OEMMBehavior.cloneBoundaryTringles");
		
		MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		mtb.addTriangleList();
		coarseReader = new MeshReader(oemm);
		coarseReader.buildMeshes(mtb, cloneBoundaryTriangles);
		
		for(int i = 0, n = coarseOEMM.getNumberOfLeaves(); i < n; i++)
		{
			Integer II = Integer.valueOf(i);
			Mesh mesh = coarseReader.getMesh(i);
			ViewHolder vh = new ViewHolder(II, mesh);
			coarseOemmNodeId2BranchGroup.put(II, vh);
			vh.setViewElem(OEMMViewer.meshOEMM(mesh, false));
			addBranchGroup(vh, true);
		}
		
		setSchedulingBounds(new BoundingSphere(
			new Point3d(), Double.MAX_VALUE));
		double[] coords=oemm.getCoords(true);
		computeVoxels(canvas, coarseOEMM, coords);
		
		this.oemm=oemm;
		
		d2limit=2*(coords[0]-coords[6*4*3-6]);
		wakeupFrame=new WakeupOnElapsedFrames(1);
		wakeupTransf=new WakeupOnTransformChange(
			view.getViewingPlatform().getViewPlatformTransform());
		maxNumberOfTriangles = Long.getLong("org.jcae.viewer3d.OEMMBehavior.maxNumberOfTringles", DEFAULT_MAX_TRIANGLES_NBR);
		if (logger.isInfoEnabled()) {
			logger.info("Maximal number of triangles: " + maxNumberOfTriangles);
		}
	}

	public Set<Integer> getIds()
	{
		return visibleFineOemmNodeId2BranchGroup.keySet();
	}

	private void computeVoxels(View canvas, OEMM coarseOEMM, double[] coords)
	{
		final double[] values = new double[3];
//		Collection<Integer> nodesWithoutVoxel = new ArrayList<Integer>();
		view=canvas;
		voxels=new Point3d[coords.length/6/4/3];
		for(int i=0; i<voxels.length; i++)
		{
//			int n=6*4*3*i;
//			voxels[i]=new Point3d(
//				(coords[n+0]+coords[n+6*4*3-6])/2,
//				(coords[n+1]+coords[n+6*4*3-5])/2,
//				(coords[n+2]+coords[n+6*4*3-4])/2);
			ViewHolder vh = coarseOemmNodeId2BranchGroup.get(i);
			Collection nodes = vh.mesh.getNodes();
			if (getAveragePointForVertices(nodes, values)) {
				voxels[i]=new Point3d(values[0], values[1], values[2]);
			} else {
//				nodesWithoutVoxel.add(i);
				int n=6*4*3*i;
				voxels[i]=new Point3d(
					(coords[n+0]+coords[n+6*4*3-6])/2,
					(coords[n+1]+coords[n+6*4*3-5])/2,
					(coords[n+2]+coords[n+6*4*3-4])/2);
			}
			vh.mesh = null;
		}
	}

	/**
	 * Registers ChangeListener to receive events.
	 * @param listener The listener to register.
	 */
	public synchronized void addChangeListener(ChangeListener listener)
	{
		if (changeListenerList == null ) {
			changeListenerList = new java.util.ArrayList ();
		}
		changeListenerList.add (listener);
	}		

	/**
	 * Notifies all registered listeners about the event.
	 * @param object Parameter #1 of the <CODE>ChangeEvent<CODE> constructor.
	 */
	private void fireChangeListenerStateChanged()
	{
		java.util.ArrayList list;
		synchronized (this) {
			if (changeListenerList == null) return;
			list = (java.util.ArrayList)changeListenerList.clone ();
		}
		for (int i = 0; i < list.size (); i++) {
			((ChangeListener)list.get (i)).stateChanged(this);
		}
	}

	public void initialize()
	{
		d2limit=d2limit/Math.tan(view.getView().getFieldOfView()/2);
		d2limit=d2limit*d2limit;
		wakeupOn(wakeupTransf);			
	}

	public boolean isOemmActive()
	{
		return oemmActive;
	}

	public void processStimulus(Enumeration arg0)
	{
		if(arg0.nextElement() instanceof WakeupOnTransformChange)
		{
			wakeupOn(wakeupFrame);
			return;
		}
		if (!frozen) {
			final Set<Integer> ids = new HashSet<Integer>();
			findVoxelsWithFineMesh(ids);
			showFineMesh(ids);
		}
		wakeupOn(wakeupTransf);
		
	}

	private void findVoxelsWithFineMesh(final Set<Integer> ids)
	{
		ViewPyramid vp=new ViewPyramid(view, scaleRectangle(view.getBounds(), 2.5));
		List<VoxelSortHelper> helper = new ArrayList<VoxelSortHelper>();
		long totalNumberTriangles = 0; //number of already visible vertices
		for(int i=0; i<voxels.length; i++)
		{
			if (voxels[i] == null) {
				continue;
			}
			double distance_2 = voxels[i].distanceSquared(vp.getEye());
			if(distance_2 < d2limit	&& vp.intersect(voxels[i])) {
				if (maxNumberOfTriangles > 0) {
					double distanceFromCenter_2 = distanceOfPointFromLine(vp.getStartPoint(), vp.getEye(), voxels[i]);
					helper.add(new VoxelSortHelper((float) (distance_2 + distanceFromCenter_2 / distance_2), i));
				} else {
					ids.add(i);
				}
			}
		
		}
		if (maxNumberOfTriangles > 0) {
			Collections.sort(helper);
			for (VoxelSortHelper voxel: helper) {
				long newTotalNumber = totalNumberTriangles + oemm.leaves[voxel.voxelIndex].tn;
				if (newTotalNumber < maxNumberOfTriangles ) {
					totalNumberTriangles = newTotalNumber;
					ids.add(voxel.voxelIndex);
				} else {
					break;
				}
			}
		}
	}
	
	/**
	 *  Computes squared distance of a point x0 
	 *  from a line (defined by x1 and x2) 
	 * 	t = - (x1 - x0)* (x2 - x1) / |x2 - x1|^2;
	 *  d^2 = [x1 + (x2 - x2) * t - x0]^2
	 *  
	 * @param startPoint is x1
	 * @param eye is x2
	 * @param pt is x0
	 * @return
	 */
	private double distanceOfPointFromLine(Point3d startPoint, Point3d eye, Point3d pt)
	{
		Vector3d x1 = new Vector3d(startPoint);
		Vector3d x2 = new Vector3d(eye);
		Vector3d x0 = new Vector3d(pt);
		Vector3d temp = (Vector3d) x2.clone();
		temp.sub(x1);
		double absolute = temp.length();
		temp = (Vector3d) x1.clone();
		temp.sub(x0);
		Vector3d temp2 = (Vector3d) x2.clone();
		temp2.sub(x1);
		double t = - temp.dot(temp2) / (absolute * absolute);
		
		temp = (Vector3d) x2.clone();
		x2.sub(x1);
		x2.scale(t);
		temp.add(x1);
		temp.sub(x0);
		double result = temp.lengthSquared();
		return result;
	}

	private void showFineMesh(final Set<Integer> ids)
	{
		if (logger.isInfoEnabled()) {
			logger.info("Fine occtree nodes> " + ids);
		}
		if(!getIds().containsAll(ids) || ids.size()==0)
		{
			oemmActive=ids.size()>0;
			if (logger.isDebugEnabled()) {
				logger.debug("We will show fine mesh for nodes: " + ids);
			}
			if(ids.size()>0)
			{
				visibilityRevisionOfCoarseMesh(ids);
			}
			else
			{
				showAllHiddenCoarseNodes(null);
			}

			fireChangeListenerStateChanged();
		}
	}
	
	/**
	 * Removes ChangeListener from the list of listeners.
	 * @param listener The listener to remove.
	 */
	public synchronized void removeChangeListener(ChangeListener listener)
	{
		if (changeListenerList != null ) {
			changeListenerList.remove (listener);
		}
	}
	
	/** 
	 * @param rectangle The rectangle to scale
	 * @param factor The factor to apply
	 * @return The rectangle which was specified as input
	 */
	private Rectangle scaleRectangle(Rectangle rectangle, double factor)
	{
		double k=(1-factor)/2;
		rectangle.x += rectangle.width*k;
		rectangle.y += rectangle.height*k;
		rectangle.width = (int) (rectangle.width*factor);
		rectangle.height = (int) (rectangle.height*factor);
		return rectangle;
	}
	
	private void addBranchGroup(ViewHolder vh, boolean coarse)
	{
		if (logger.isDebugEnabled()) {
			logger.debug("addBranchGroup> id:" + vh.getId() + ", coarse:" + coarse);
		}
		Map<Integer, ViewHolder> map = coarse ? coarseOemmNodeId2BranchGroup : visibleFineOemmNodeId2BranchGroup;
		BranchGroup branchGroup = vh.getViewElement();
		if (!branchGroup.getCapability(BranchGroup.ALLOW_DETACH))
			branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
		
		visibleMeshBranchGroup.addChild(branchGroup);
		
		if (!coarse) {
			map.put(vh.getId(), vh);
		}
	}
	
	private void removeBranchGroup(int id, boolean coarse)
	{
		if (logger.isDebugEnabled()) {
			logger.debug("removeBranchGroup> id:" + id + ", coarse:" + coarse);
		}
		Map<Integer, ViewHolder> map = coarse ? coarseOemmNodeId2BranchGroup : visibleFineOemmNodeId2BranchGroup;
		
		//branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
		ViewHolder vh = map.get(id);
		visibleMeshBranchGroup.removeChild(vh.getViewElement());
		if (!coarse) {
			map.remove(id);
		}
	}
	
	private void showAllHiddenCoarseNodes(Set<Integer> exceptSet)
	{
		Set<Integer> hidedIs = new HashSet<Integer>( visibleFineOemmNodeId2BranchGroup.keySet());
		
		for (Integer arg0: hidedIs) {
			if (exceptSet == null || !exceptSet.contains(arg0)) {
				addBranchGroup(coarseOemmNodeId2BranchGroup.get(arg0), true);
				removeBranchGroup(arg0, false);
			}
		}
	}

	private void visibilityRevisionOfCoarseMesh(final Set<Integer> ids)
	{
		showAllHiddenCoarseNodes(ids);
		
		int nrTriangles = 0;
		for (Integer arg0: ids) {
			ViewHolder vh = getFineMeshFromCache(arg0);
			if (!visibleFineOemmNodeId2BranchGroup.containsKey(arg0)) {
				addBranchGroup(vh, false);
				removeBranchGroup(arg0, true);
			}
			nrTriangles += vh.nrTriangles;
		}
		logger.info("Number of triangles in fine mesh: " + nrTriangles);
	
	}

	private ViewHolder getFineMeshFromCache(Integer arg0)
	{
		ViewHolder vh = cacheOemmNodeId2BranchGroup.get(arg0);
		if (vh == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("finemesh node:" + arg0 + " is not loaded and I will load it.");
			}
			
			vh = ViewHolder.makeViewHolder(oemm, arg0, null);
			cacheOemmNodeId2BranchGroup.put(arg0, vh);
		}
		return vh;
	}
	
	/**
	 * Computes average center of vertices
	 * @param vertices
	 * @param result vector for store result

	 */
	private boolean getAveragePointForVertices(Collection vertices, double[] result)
	{
		int count = 0;
		for (int ii = 0; ii < result.length; ii++) {
			result[ii] = 0;
		}
		for (Iterator i = vertices.iterator(); i.hasNext();)
		{
			Vertex v = (Vertex) i.next();
			if (!v.isReadable())
				continue;
			count++;
			double []coords = v.getUV();
			for (int ii = 0; ii < result.length; ii++) {
				result[ii] += coords[ii] ;
			}
		}
		if (count > 0) {
			double size = count;
			for (int ii = 0; ii < result.length; ii++) {
				result[ii] /= size;
			}
		}
		return count > 0;
	}

	public Point3d getVoxel(int arg0)
	{
		return voxels[arg0];
	}

	public int getNumberOfCacheNodes()
	{
		return cacheOemmNodeId2BranchGroup.size();
	}

	public int getNumberOfVisibleFineElements()
	{
		return visibleFineOemmNodeId2BranchGroup.size();
	}

	public void switchFreeze()
	{
		frozen = !frozen;
	}
}
