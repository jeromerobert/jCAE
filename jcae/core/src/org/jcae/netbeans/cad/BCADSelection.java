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
 * (C) Copyright 2008,2009, by EADS France
 */
package org.jcae.netbeans.cad;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import org.jcae.netbeans.viewer3d.EntitySelection;
import java.beans.PropertyVetoException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.cad.occ.OCCShape;
import org.jcae.netbeans.NodeSelectionManager;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.mesh.bora.ViewBCellGeometryAction.NbBShape;
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
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 * @author Gautam Botrel
 */
public class BCADSelection implements EntitySelection, SelectionListener,
	CurrentViewableChangeListener, PropertyChangeListener
{
	private Collection<TopoDS_Shape> selection = Collections.emptySet();
	private boolean selectionLock = false;
	private NbShape entity = null;
	private Set<ViewableCAD> interactors = new HashSet<ViewableCAD>();
	private final Node refToExplore;

	public BCADSelection(NbBShape entity)
	{
		this.entity = entity;
		refToExplore = entity.getRefToExploreNode();
		ViewManager.getDefault().addViewableListener(this);
		NodeSelectionManager.getDefault().addPropertyChangeListener(this);
	}

	public void unselectAll()
	{
		if(!selectionLock)
		{
			selectionLock = true;
			selection = Collections.emptySet();
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
			if (interactors.add(caoInteractor))
			{
				caoInteractor.addSelectionListener(this);			
				refreshHighlight();
			}
	}

	public Collection<Node> findNodesWithShape(TopoDS_Shape shape) {
		ArrayList<Node> toReturn = new ArrayList<Node>();
		//explore the mesh to find corresponding nodes
		ArrayDeque<Children> toExplore = new ArrayDeque<Children>();
		toExplore.push(refToExplore.getChildren());
		while (!toExplore.isEmpty()) {
			Children c = toExplore.pop();
			for (Node n : c.getNodes()) {
				if (!n.isLeaf())
					toExplore.push(n.getChildren());
				BCADGraphCell cell = (BCADGraphCell)n.getValue("CELL");
				if (cell != null) {
					TopoDS_Shape nShape = ((OCCShape)cell.getShape()).getShape();
					if (shape.equals(nShape)) {
						boolean isEntitie = (Boolean)n.getValue("ENTITIE");
						if (!isEntitie) // we return only the graph view nodes
							toReturn.add(n);
					}
				}
			}
		}
		return toReturn;
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

		for (TopoDS_Shape shape : selection) {
			Node n = entity.getShapeFromImpl(shape).getNode();
			if (n == null) {
				nodes.addAll(findNodesWithShape(shape));
			}
			else
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
	}

	/**
	 * Refresh HighLigh on all views
	 */
	private void refreshHighlight()
	{
		/*SwingUtilities.invokeLater(new Runnable()
		{
		public void run()
		{*/
		for (ViewableCAD interactor : interactors)
		{
			interactor.setSelection(selection);
			interactor.selectSelectionNodes();
			interactor.highlight();
		}

	/*}
	});*/
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(ExplorerManager.PROP_SELECTED_NODES)
			&& evt.getNewValue() instanceof Node[]
			&& !selectionLock
			&& !SelectionManager.getDefault().isDisableListeningProperty())
		{
			selectionLock = true;
			Node[] nodes = (Node[]) evt.getNewValue();
			selection = new ArrayList<TopoDS_Shape>(nodes.length);
			// Retrieve the shapes
			for (Node n : nodes)
			{
				BCADGraphCell cell = (BCADGraphCell)n.getValue("CELL");
				if (cell == null)
					continue;

				TopoDS_Shape shape = ((OCCShape)cell.getShape()).getShape();
				// Add only vertice, edges or faces others are ignored
				if ((shape instanceof TopoDS_Vertex)
					|| (shape instanceof TopoDS_Edge)
					|| (shape instanceof TopoDS_Face)) 
					selection.add(shape);
				
			}

			refreshHighlight();
			selectionLock = false;
		}
	}
}
