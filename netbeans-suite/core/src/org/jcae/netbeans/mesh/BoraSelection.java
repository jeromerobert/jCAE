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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.ArrayDeque;
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
import org.jcae.vtk.SelectionListener;
import org.jcae.vtk.ViewableMesh;
import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 *
 * @author ibarz
 */
public class BoraSelection implements EntitySelection, SelectionListener, CurrentViewableChangeListener, PropertyChangeListener
{

	private ArrayList<String> selection = new ArrayList<String>();
	private boolean selectionLock = false;
	private BGroupsNode entity;
	private Set<ViewableMesh> interactors = new HashSet<ViewableMesh>();

	public BoraSelection(BGroupsNode entity)
	{
		this.entity = entity;
		ViewManager.getDefault().addViewableListener(this);
		NodeSelectionManager.getDefault().addPropertyChangeListener(this);
	}

	public void unselectAll()
	{
		if(!selectionLock)
		{
			selectionLock = true;
			selection = new ArrayList<String>();
			refreshHighlight();
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
		Collection<Viewable> currentViewables = ViewManager.getDefault().getCurrentViewables();
		for (Viewable interactor : interactors)
			if (!currentViewables.contains(interactor))
			{
				interactors.remove(interactor);
				interactor.removeSelectionListener(this);
				break;
			}

		if (newInteractor == null || !(newInteractor instanceof ViewableMesh))
			return;

		ViewableMesh meshInteractor = (ViewableMesh) newInteractor;

		if (SelectionManager.getDefault().getEntity(meshInteractor) == entity)
			if (interactors.add(meshInteractor))
			{
				meshInteractor.addSelectionListener(this);
				//refreshHighlight();
			}
	}

	public void selectionChanged(Viewable interactor)
	{
		if( ! (interactor instanceof BoraViewable) )
			return;
		// If it is not our interactor leave
		if (!interactors.contains(interactor))
			return;

		if (selectionLock)
			return;

		selectionLock = true;

		selection = new ArrayList<String>();
		for (String s : ((ViewableMesh) interactor).getSelection()) {
			selection.add(s);
		}
		SelectionManager.getDefault().prepareSelection();
		final ArrayList<Node> nodes = new ArrayList<Node>();
		Node nnn = getFilterNodeToExplore(((BoraViewable)interactor).getNode());

		for (Node n : nnn.getChildren().getNodes(true)) {
			if (selection.contains(n.getName()))
				nodes.add(n);
		}
		refreshHighlight();

		SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{
				for (ExplorerManager exm : Utilities.getExplorerManagers())
				{
					ArrayList<Node> nnodes = new ArrayList<Node>();

					if (SelectionManager.getDefault().isAppendSelection())
						nnodes.addAll(Arrays.asList(exm.getSelectedNodes()));

					nnodes.addAll(nodes);
					try
					{
						SelectionManager.getDefault().setDisableListeningProperty(true);
						exm.setSelectedNodes(nnodes.toArray(
								new Node[nnodes.size()]));
						SelectionManager.getDefault().setDisableListeningProperty(false);
					} catch (PropertyVetoException e)
					{
						ErrorManager.getDefault().notify(e);
					}
				}

				selectionLock = false;
			}
		});
	};

	private Node getFilterNodeToExplore(Node n) {
		ArrayDeque<Children> toExplore = new ArrayDeque<Children>();
		toExplore.push(n.getChildren());
		while (!toExplore.isEmpty()) {
			Children c = toExplore.pop();
			for (Node nn : c.getNodes()) {
				if (!nn.isLeaf()) {
					toExplore.add(nn.getChildren());
				}
				BGroupsNode mn = nn.getLookup().lookup(BGroupsNode.class);
				if (mn != null && mn.equals(entity)) {
					return nn;
				}
			}
		}
		throw new IllegalStateException("Could not find node " + entity);
	}

	/**
	 * Refresh highlight on all views
	 */
	private void refreshHighlight()
	{
		for (ViewableMesh interactor : interactors)
		{
			interactor.setSelection(selection.toArray(new String[0]));
			interactor.selectSelectionNodes();
			interactor.highlight();
		}
	}


	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(ExplorerManager.PROP_SELECTED_NODES) && evt.getNewValue() instanceof Node[] && !selectionLock && !SelectionManager.getDefault().isDisableListeningProperty())
		{
			selectionLock = true;		
			Node[] nodes = (Node[]) evt.getNewValue();
			selection = new ArrayList<String>(nodes.length);
			for (Node node : nodes)
			{
				for (Node n : entity.getChildren().getNodes()) {
					if (node.equals(n))
						selection.add(n.getName());
				}
			}

			refreshHighlight();

			selectionLock = false;
		}
	}
}
