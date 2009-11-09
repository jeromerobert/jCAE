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
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.netbeans.mesh.bora;

import org.jcae.netbeans.mesh.*;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.netbeans.cad.BrepDataObject;
import org.jcae.netbeans.cad.BrepNode;
import org.jcae.netbeans.viewer3d.SelectionManager;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.BoraToMesh;
import org.jcae.vtk.View;
import org.jcae.vtk.ViewableMesh;
import org.openide.ErrorManager;
import org.openide.actions.*;
import org.openide.cookies.ViewCookie;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.PasteType;

/**
 * Contains the submeshnode(s)
 */
public class BoraNode extends DataNode implements ViewCookie
{
	private Submeshes subMeshesFactory;

	public BoraNode(BoraDataObject arg0)
	{
		this(arg0, new Submeshes(arg0));
	}
	
	private BoraNode(BoraDataObject arg0, Submeshes subMeshesFactory)
	{
		super(arg0, Children.create(subMeshesFactory, true));
		this.subMeshesFactory = subMeshesFactory;
		setIconBaseWithExtension("org/jcae/netbeans/mesh/bora/MeshNode.png");
		getCookieSet().add(this);
	}

	private SubmeshNode getEnclosedSubMeshNode() {
		assert (getChildren().getNodes().length == 0 || getChildren().getNodes().length == 1 );
		for (Node n : getChildren().getNodes()) {
			return (SubmeshNode)n;
		}
		return null;
	}

	public void view() {
		if ( getEnclosedSubMeshNode() != null && getBModel() != null) {
			 getEnclosedSubMeshNode().viewMesh();
		}
	}
	
	public static void view(String vName,
		Map<String, Collection<BDiscretization>> meshData, BGroupsNode groups) {

		view(vName, meshData, groups,
			org.openide.util.Utilities.actionsGlobalContext().lookup(Node.class));
	}
	
	public static void view(String vName, Map<String, Collection<BDiscretization>> meshData,
			BGroupsNode groups, Node node) {
		if (meshData == null || meshData.isEmpty())
			return;
		View v = ViewManager.getDefault().getCurrentView();
		BoraToMesh toMesh = new BoraToMesh(meshData);
		ViewableMesh vMesh = new BoraViewable(toMesh.getMesh(), node);
		vMesh.setName(vName);
		if (groups != null)
			SelectionManager.getDefault().addInteractor(vMesh, groups);
		v.add(vMesh);
	}

	/**
	 * @return Returns the groups
	 */
	public void refreshGroups()
	{
		SubmeshNode subNode =  getEnclosedSubMeshNode();
		if ( subNode != null)
			subNode.refreshGroupsNode(true);
	}

	public BModel getBModel() {
		return getCookie(BoraDataObject.class).getBModel();
	}

	public BModel getBModel(String geomFile) {
		return getCookie(BoraDataObject.class).getBModel(geomFile);
	}

	@Override
	public void setName(String arg0)
	{	
		try
		{
			String o=getName();
			getDataObject().rename(arg0);
			if (getBModel() != null) {
				getCookie(BoraDataObject.class).updateBModelDir();
			}
			fireDisplayNameChange(o, arg0);
			fireNameChange(o, arg0);
		}
		catch (IOException e)
		{
			ErrorManager.getDefault().notify(e);
		}
	}
	
	@Override
	public String getName()
	{
		return getDataObject().getPrimaryFile().getName();
	}

	@Override
	public String getDisplayName()
	{
		return getDataObject().getPrimaryFile().getName();
	}
	
	@Override
	protected void createPasteTypes(Transferable t, List<PasteType> ls)
	{
		final Node[] ns = NodeTransfer.nodes(t, NodeTransfer.COPY|NodeTransfer.MOVE);
		if (ns != null && ns.length==1) {
			final BrepNode n=ns[0].getCookie(BrepNode.class);
			if (n != null) {
				//TODO : we should save the node when
				//it's a new one (n.save())
				ls.add(new PasteType() {

					public Transferable paste() {
						File f = FileUtil.toFile(n.getLookup().lookup(
							BrepDataObject.class).getPrimaryFile());
						getBModel(f.getPath()).newMesh();
						getBModel().save();
						subMeshesFactory.fireSubmeshNodeChanged();
						firePropertyChange(null, null, null);
						return null;
					}
				});
			}
		}
		// Also try superclass, but give it lower priority:
		super.createPasteTypes(t, ls);
	}
	
	@Override
	public Action[] getActions(boolean b)
	{
		ArrayList<Action> l = new ArrayList<Action>();
		l.add(SystemAction.get(ComputeMeshAction.class));
		l.add(SystemAction.get(ViewAction.class));
		l.add(SystemAction.get(DeleteAction.class));
		l.add(SystemAction.get(RenameAction.class));
		return l.toArray(new Action[l.size()]);
	}

	private static class Submeshes extends ChildFactory<BModel> {
		private final BoraDataObject mObject;

		public Submeshes(BoraDataObject o) {
			this.mObject = o;
		}

		@Override
		protected Node createNodeForKey(BModel bModel) {
			String f = bModel.getCADFile();
			f = f.substring(f.lastIndexOf(File.separator) + 1, f.lastIndexOf("."));
			return new SubmeshNode(f, bModel);
		}

		@Override
		protected boolean createKeys(List<BModel> list) {
			if (mObject.getBModel() == null)
				mObject.load();
			if (mObject.getBModel() != null) {
				list.add(mObject.getBModel());
			}
			return true;
		}

		public void fireSubmeshNodeChanged() {
			this.createKeys(new ArrayList<BModel>());
			this.refresh(true);
		}
	}
}