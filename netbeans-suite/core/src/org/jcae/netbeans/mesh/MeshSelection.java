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
 * (C) Copyright 2008, by EADS France
 */
package org.jcae.netbeans.mesh;

import gnu.trove.TIntArrayList;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import org.jcae.netbeans.viewer3d.EntitySelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.jcae.netbeans.NodeSelectionManager;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.viewer3d.CurrentViewableChangeListener;
import org.jcae.netbeans.viewer3d.SelectionManager;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.Viewable;
import org.jcae.vtk.ViewableMesh;
import org.jcae.vtk.SelectionListener;
import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;

/**
 *
 * @author ibarz
 */
public class MeshSelection implements EntitySelection, SelectionListener, CurrentViewableChangeListener, PropertyChangeListener
{

	private TIntArrayList selection = new TIntArrayList();
	private boolean selectionLock = false;
	private Groups entity;
	private Set<ViewableMesh> interactors = new HashSet<ViewableMesh>();

	public MeshSelection(Groups entity)
	{
		this.entity = entity;
		ViewManager.getDefault().addViewableListener(this);
		NodeSelectionManager.getDefault().addPropertyChangeListener(this);
	}

	public void unSelectAll()
	{
		if(!selectionLock)
		{
			selectionLock = true;
			selection = new TIntArrayList();
			refreshHighLight();
			selectionLock = false;
		}
	}

	/**
	 * 
	 * @param newInteractor
	 */
	public void currentViewableChanged(Viewable newInteractor)
	{
		// Find if an interactor is becomed UnInteractive
		System.out.println("Interactors :");
		Collection<Viewable> currentViewables = ViewManager.getDefault().getCurrentViewables();
		for (Viewable interactor : interactors)
			if (!currentViewables.contains(interactor))
			{
				interactors.remove(interactor);
				interactor.removeSelectionListener(this);
				break;
			}
		System.out.println("End interactors");

		if (newInteractor == null || !(newInteractor instanceof ViewableMesh))
			return;

		ViewableMesh meshInteractor = (ViewableMesh) newInteractor;

		if (SelectionManager.getDefault().getEntity(meshInteractor) == entity)
			if (interactors.add((ViewableMesh) meshInteractor))
			{
				meshInteractor.addSelectionListener(this);
				System.out.println("refresh Highlight begin");
				//refreshHighLight();
				System.out.println("refresh Highlight end");
			}
	}

	public void selectionChanged(Viewable interactor)
	{
		// If it is not our interactor leave
		if (!interactors.contains(interactor))
			return;

		if (selectionLock)
			return;

		selectionLock = true;

		selection = new TIntArrayList(((ViewableMesh) interactor).getSelection());
		SelectionManager.getDefault().prepareSelection();

		refreshHighLight();

		SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{
				// Get the mesh node
				for (ExplorerManager exm : Utilities.getExplorerManagers())
					for (Node moduleNode : findModuleNodes(exm))
						for (Node meshProxy : moduleNode.getChildren().getNodes())
						{

							MeshNode meshNode = meshProxy.getLookup().lookup(MeshNode.class);
							if (meshNode != null)
								if (meshNode.hasThisGroupsNode(entity))
								{
									// Mesh founded now
									// Get the groups node
									Node groupsProxy = null;

									for (Node gn : meshProxy.getChildren().getNodes())
										if (gn.getLookup().lookup(GroupChildren.class) != null)
										{
											groupsProxy = gn;
											break;
										}

									// Append to the selection explorer
									Node[] childrenProxy = groupsProxy.getChildren().getNodes();
									Node[] selectionExplorer = exm.getSelectedNodes();
									ArrayList<Node> nodes = new ArrayList<Node>(childrenProxy.length + selectionExplorer.length);
									nodes.addAll(Arrays.asList(selectionExplorer));
									for (Node groupProxy : childrenProxy)
									{
										GroupNode groupNode = groupProxy.getLookup().lookup(GroupNode.class);
										if (groupNode == null)
											throw new RuntimeException("DEBUG node is not a GroupNode but " + groupNode.getClass().getName());

										// Add if it's in the selection
										if (selection.contains(groupNode.getGroup().getId()))
											nodes.add(groupProxy);
										// Remove from the selection if it is
										else
											nodes.remove(groupProxy);
									}

									try
									{
										SelectionManager.getDefault().setDisableListeningProperty(true);
										exm.setSelectedNodes(nodes.toArray(
												new Node[nodes.size()]));
										SelectionManager.getDefault().setDisableListeningProperty(false);
									} catch (PropertyVetoException e)
									{
										ErrorManager.getDefault().notify(e);
									}

									// RETURN
									selectionLock = false;
									return;
								}
						}

				selectionLock = false;
				System.err.println("WARNING : Mesh Node not founded !");
			}
		});
	}
	;

	/**
	 * Refresh HighLigh on all views
	 */
	private void refreshHighLight()
	{
		/*SwingUtilities.invokeLater(new Runnable()
		{
		public void run()
		{*/
		for (ViewableMesh interactor : interactors)
		{
			interactor.setSelection(selection.toNativeArray());
			interactor.highLight();
		}
	/*}
	});*/
	}

	/** Return all ModuleNode
	 * @param exm
	 * @return*/
	private static Collection<Node> findModuleNodes(ExplorerManager exm)
	{
		ArrayList<Node> toReturn = new ArrayList<Node>();
		for (Node n : exm.getRootContext().getChildren().getNodes())
			for (Node nn : n.getChildren().getNodes())
			{
				ModuleNode mn = nn.getLookup().lookup(ModuleNode.class);
				if (mn != null)
				{
					toReturn.add(nn);
					break;
				}
			}
		return toReturn;
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(ExplorerManager.PROP_SELECTED_NODES) && evt.getNewValue() instanceof Node[] && !selectionLock && !SelectionManager.getDefault().isDisableListeningProperty())
		{
			selectionLock = true;

			System.out.println("PROPERTY CHANGED !");
			Node[] nodes = (Node[]) evt.getNewValue();

			selection = new TIntArrayList(nodes.length);
			for (Node node : nodes)
			{
				GroupNode groupNode = node.getLookup().lookup(GroupNode.class);
				if (groupNode == null)
					continue;

				// If it is a group of our mesh
				if (groupNode.getGroups() == entity)
					selection.add(groupNode.getGroup().getId());
			}

			refreshHighLight();

			selectionLock = false;
		/*selection = new ArrayList<TopoDS_Shape>(nodes.length);
		// Retrieve the shapes
		if(!Arrays.asList(entity.getNode().getChildren().getNodes()).contains(n))
		continue;
		
		NbShape nbShape = GeomUtils.getShape(n);
		// If it's not a shape node
		if(nbShape == null)
		continue;
		
		TopoDS_Shape shape = nbShape.getImpl();
		// Add only vertice, edges or faces others are ignored
		if((shape instanceof TopoDS_Vertex) || (shape instanceof TopoDS_Edge) || (shape instanceof TopoDS_Face))
		selection.add(shape);
		}
		
		refreshHighLight();
		selectionLock = false;*/
		}
	}
}
