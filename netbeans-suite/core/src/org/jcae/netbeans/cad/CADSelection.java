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
package org.jcae.netbeans.cad;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import org.jcae.netbeans.viewer3d.EntitySelection;
import java.beans.PropertyVetoException;
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
import org.jcae.opencascade.jni.TopoDS_Edge;
import org.jcae.opencascade.jni.TopoDS_Face;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.opencascade.jni.TopoDS_Vertex;
import org.jcae.vtk.Viewable;
import org.jcae.vtk.ViewableCAD;
import org.jcae.vtk.SelectionListener;
import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;

/**
 *
 * @author Julian Ibarz
 */
public class CADSelection implements EntitySelection, SelectionListener,
	CurrentViewableChangeListener, PropertyChangeListener
{
	private Collection<TopoDS_Shape> selection = Collections.EMPTY_SET;
	private boolean selectionLock = false;
	private NbShape entity = null;
	private Set<ViewableCAD> interactors = new HashSet<ViewableCAD>();


	public CADSelection(NbShape entity)
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
			selection = Collections.EMPTY_SET;
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
		Collection<Viewable> currentViewables = ViewManager.getDefault().getCurrentViewables();
		for (Viewable interactor : interactors)
			if(!currentViewables.contains(interactor))
			{
				interactors.remove(interactor);
				interactor.removeSelectionListener(this);
				break;
			}

		if (newInteractor == null || !(newInteractor instanceof ViewableCAD))
			return;

		ViewableCAD caoInteractor = (ViewableCAD) newInteractor;

		if (SelectionManager.getDefault().getEntity(caoInteractor) == entity)
			if (interactors.add((ViewableCAD) caoInteractor))
			{
				caoInteractor.addSelectionListener(this);			
				refreshHighLight();
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
		selection = ((ViewableCAD) interactor).getSelection();
		SelectionManager.getDefault().prepareSelection();

		final ArrayList<Node> nodes = new ArrayList<Node>(selection.size());

		for (TopoDS_Shape shape : selection)
			nodes.add(entity.getShapeFromImpl(shape).getNode());

		refreshHighLight();

		SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{
				for (ExplorerManager exm : Utilities.getExplorerManagers())
				{
					ArrayList<Node> nnodes = new ArrayList<Node>();

					if (SelectionManager.getDefault().isAppendSelection())
						nnodes.addAll(Arrays.asList(exm.getSelectedNodes()));

					for (Node n : nodes)
						for (Node mn : findModuleNodes(exm))
							nnodes.addAll(GeomUtils.findNode(mn, n));

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
	}

	/**
	 * Refresh HighLigh on all views
	 */
	private void refreshHighLight()
	{
		/*SwingUtilities.invokeLater(new Runnable()
		{
		public void run()
		{*/
		for (ViewableCAD interactor : interactors)
		{
			interactor.setSelection(selection);
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
			Node[] nodes = (Node[]) evt.getNewValue();
			selection = new ArrayList<TopoDS_Shape>(nodes.length);
			// Retrieve the shapes
			for (Node n : nodes)
			{
				// Is the node in our CAO ?
					/*Node root = entity.getNode();
				boolean isChild = false;
				for(Node child : root.getChildren().getNodes())
				if(child == n)
				{
				isChild = true;
				break;
				}
				
				if(!isChild)
				continue;*/
				if (!Arrays.asList(entity.getNode().getChildren().getNodes()).contains(n))
					continue;

				NbShape nbShape = GeomUtils.getShape(n);
				// If it's not a shape node
				if (nbShape == null)
					continue;

				TopoDS_Shape shape = nbShape.getImpl();
				// Add only vertice, edges or faces others are ignored
				if ((shape instanceof TopoDS_Vertex) || (shape instanceof TopoDS_Edge) || (shape instanceof TopoDS_Face))
					selection.add(shape);
			}

			refreshHighLight();
			selectionLock = false;
		}
	}
}
