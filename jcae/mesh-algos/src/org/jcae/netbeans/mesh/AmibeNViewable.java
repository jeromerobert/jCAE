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
 * (C) Copyright 2009, by EADS France
 */


package org.jcae.netbeans.mesh;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.jcae.vtk.ViewableMesh;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.util.WeakListeners;

/**
 *
 * @author Jerome Robert
 */
public class AmibeNViewable extends ViewableMesh {
	private final static Logger LOGGER =
		Logger.getLogger(AmibeNViewable.class.getName());
	/** The associated AmibeNode or a FilterNode on it */
	private final Node amibeNode;
	private final AmibeDataObject amibeDataObject;
	private ExplorerManager explorerManager;
	/**
	 * The listener is wrapped in a Weak reference, so a reference must be kept
	 * to avoid garbage collection.
	 */
	private PropertyChangeListener propertyChangeListener;

	/**
	 *
	 * @param mesh
	 * @param node an AmibeNode or a FilterNode on an AmibeNode
	 * @param m The explorer which reflect the selection done in the viewable
	 */
	public AmibeNViewable(Node n, ExplorerManager em) {
		this.amibeNode = n;
		this.explorerManager = em;
		amibeDataObject = getADO(n);
		propertyChangeListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName()))
				{
					select((Node[]) evt.getNewValue());
				}
			}
		};
		em.addPropertyChangeListener(
			WeakListeners.propertyChange(propertyChangeListener, this));
	}

	public static AmibeNViewable get(Node n, View view)
	{
		AmibeDataObject ado = getADO(n);
		if(ado != null)
		{
			for(Viewable v:view.getViewables())
			{
				if(v instanceof AmibeNViewable)
				{
					AmibeNViewable av = (AmibeNViewable) v;
					if(ado.equals(getADO(av.amibeNode)))
						return av;
				}
			}
		}
		return null;
	}
	
	private static AmibeDataObject getADO(Node node)
	{
		return node == null ? null : node.getLookup().lookup(AmibeDataObject.class);
	}

	private void select(Node[] nodes)
	{
		ArrayList<String> toSelect = new ArrayList<String>();
		for(Node n:nodes)
		{
			GroupNode gn = n.getLookup().lookup(GroupNode.class);
			if(gn != null &&
				getADO(gn.getParentNode().getParentNode()) == amibeDataObject)
			{
				toSelect.add(gn.getName());
			}
		}
		setSelection(toSelect.toArray(new String[toSelect.size()]));
		selectSelectionNodes();
		highlight();
	}
	
	@Override
	protected void fireSelectionChanged() {
		super.fireSelectionChanged();
		Node groupsProxy = null;

		for (Node gn : amibeNode.getChildren().getNodes())
		{
			if (gn.getLookup().lookup(GroupChildren.class) != null)
			{
				groupsProxy = gn;
				break;
			}
		}
		
		// Append to the selection explorer
		Node[] childrenProxy = groupsProxy.getChildren().getNodes();
		Node[] selectionExplorer = explorerManager.getSelectedNodes();

		ArrayList<Node> nodes = new ArrayList<Node>(
			childrenProxy.length + selectionExplorer.length);

		nodes.addAll(Arrays.asList(selectionExplorer));
		HashSet<String> selection = new HashSet<String>(Arrays.asList(getSelection()));

		for (Node groupProxy : childrenProxy)
		{
			GroupNode groupNode = groupProxy.getLookup().lookup(GroupNode.class);

			// Add if it's in the selection
			if (selection.contains(groupNode.getGroup().getName()))
				nodes.add(groupProxy);
			// Remove from the selection if it is
			else
				nodes.remove(groupProxy);
		}

		try
		{
			explorerManager.setSelectedNodes(nodes.toArray(new Node[nodes.size()]));
		} catch (PropertyVetoException e)
		{
			LOGGER.log(Level.SEVERE, null, e);
		}

		highlight();
	}
}
