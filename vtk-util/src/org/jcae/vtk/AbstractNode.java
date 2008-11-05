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

package org.jcae.vtk;

import gnu.trove.TIntArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vtk.vtkActor;
import vtk.vtkFloatArray;
import vtk.vtkMapper;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkPolyDataNormals;

/**
 * Nodes of scene graph.
 * The aim of a viewer is of course to display graphical objects.  But a scene is
 * not static, view has to be refreshed when properties change.  For instance
 * when an object is selected, it is highlighted.  Color or material may also
 * be edited by user, and scene has to be rebuild quickly.
 * 
 * With OpenGL, one can concatenate static objects into so-called display lists,
 * and compile them so that rendering is very fast.  But if an object changes,
 * display list has to be rebuilt, which can be slow.  This must be taken into
 * account when designing a data structure, otherwise performance can become
 * very poor.
 *
 * The same logic applies to VTK as well.  A vtkActor is an object which will
 * be compiled and can be rendered efficiently.  Its geometry is defined by
 * a vtkMapper, more exactly a vtkPolyDataMapper in our case.  It takes some
 * time to build OpenGL primitives from this geometry, but when this is done,
 * rendering is very fast when camera moves or vtkActor is highlighted.
 * It is also possible to change only a subset of properties associated to
 * geometric data, to highlight only some cells, but this may also take some
 * time if dataset is very large.
 * 
 * VTK becomes too slow when there are many actors, but on the other hand
 * interactive changes become also too slow if too many data are put into
 * actors.  We implemented a tree graph to help merging geometric entities
 * into single actors.  There are two types of nodes:
 * <ul>
 * <li>{@link Node}: containers, its children may be Node or LeafNode instances;</li>
 * <li>{@link LeafNode}: leaf nodes contain geometric data.</li>
 * </ul>
 * 
 * This merge is performed only if the Node is set as managing (by using the 
 * {@link #setManager()} method). FIXME: a managing Node can currently contain
 * either Node or LeafNode instances, but in practice it almost always contains
 * LeafNode.  If we make this a rule, recursive behavior of these nodes will
 * be much easier to implement and more efficient too.
 *
 * This is a general framework, only application developers can know how to
 * organize their geometrical objects into Node and LeafNode.
 *
 * Nodes can be highlighted using the {@link #select} method.  A subset of
 * their underlying geometry can be highlighted using the {@link #setCellSelection}
 * method, which modifies {@link #selectionHighlighter} actor.
 *
 * Nodes can be declared as being not pickable to speed up picking, since non
 * pickable nodes are ignored.
 *
 * You can give customisers to the nodes. This works as follows: if a leaf node
 * has no customiser it takes the first parent that have one and if nobody have
 * customiser the DEFAULT customiser is taken. This permits to create a
 * customiser for the parent node and this will be used for all of its children
 * (unless if the child has a customiser).  Customisers are created to permit
 * to change and customise VTK objects easily.  Actually only color can be
 * specified for shading of the geometry. If you want to customise the nodes
 * more you can use the VTK interface but if you merge the leaves in one node
 * and they have different materials this will cause problems.  The solution to
 * this is that VTK permits to make materials data arrays like color array.
 *
 * The node by default take some characteristics of the parent node for example the pickability
 * and the visibility.
 * When applying a customiser the actor customisation is refreshed and if we are in a Node,
 * all the children inherit it.
 * 
 * @author Julian Ibarz
 */
public abstract class AbstractNode
{
	private final static Logger LOGGER = Logger.getLogger(AbstractNode.class.getName());
	/** Parent node */
	protected AbstractNode parent;
	private final ArrayList<ActorListener> actorListeners = new ArrayList<ActorListener>();
	/** Flag to tell if this node is a manager */
	private boolean manager;
	/** Actor of this node, if it is a manager */
	protected vtkActor actor;
	/** Geometry of this actor */
	protected vtkPolyDataMapper mapper;
	protected vtkPolyData data;

	/** Actor used for cell selection */
	protected vtkActor selectionHighlighter;
	protected vtkPolyDataMapper selectionHighlighterMapper;

	/** Last time this actor had been updated */
	protected long lastUpdate;
	/** Last time this actor had been modified (data, color, visibility) */
	protected long modificationTime;
	/** Last time this node had been selected */
	protected long selectionTime;
	
	// Useful for debugging
	private String debugName;

	@Deprecated
	protected int[] selectionPoint = new int[0];

	protected boolean visible = true;
	protected boolean selected;
	protected boolean pickable;
	
	public static interface ActorListener
	{
		void actorCreated(AbstractNode node, vtkActor actor);
		void actorDeleted(AbstractNode node, vtkActor actor);
	}
		
	public interface ActorCustomiser
	{
		void customiseActor(vtkActor actor);
	}
	
	public interface ActorHighlightedCustomiser
	{
		void customiseActorHighlighted(vtkActor actor);
	}
	
	public interface MapperCustomiser
	{
		void customiseMapper(vtkMapper mapper);
	}
	
	public interface MapperHighlightedCustomiser
	{
		void customiseMapperHighlighted(vtkMapper mapper);
	}
	
	public interface ActorSelectionCustomiser
	{
		void customiseActorSelection(vtkActor actor);
	}
	
	public interface MapperSelectionCustomiser
	{
		void customiseMapperSelection(vtkMapper mapper);
	}
	
	public static ActorCustomiser DEFAULT_ACTOR_CUSTOMISER =
		new ActorCustomiser()
		{
			public void customiseActor(vtkActor actor) {}
		};
	public static ActorHighlightedCustomiser DEFAULT_ACTOR_HIGHLIGHTED_CUSTOMISER =
		new ActorHighlightedCustomiser()
		{
			public void customiseActorHighlighted(vtkActor actor) {}
		};
	public static MapperCustomiser DEFAULT_MAPPER_CUSTOMISER =
		new MapperCustomiser()
		{
			public void customiseMapper(vtkMapper mapper)
			{
				mapper.SetResolveCoincidentTopologyToPolygonOffset();
				mapper.SetResolveCoincidentTopologyPolygonOffsetParameters(Utils.getOffsetFactor(), Utils.getOffsetValue());
			}
		};
	public static MapperHighlightedCustomiser DEFAULT_MAPPER_HIGHLIGHTED_CUSTOMISER =
		new MapperHighlightedCustomiser()
		{
			public void customiseMapperHighlighted(vtkMapper mapper) {}
		};
	public static ActorSelectionCustomiser DEFAULT_ACTOR_SELECTION_CUSTOMISER =
		new ActorSelectionCustomiser()
		{
			public void customiseActorSelection(vtkActor actor) {}
		};
	public static MapperSelectionCustomiser DEFAULT_MAPPER_SELECTION_CUSTOMISER =
		new MapperSelectionCustomiser()
		{
			public void customiseMapperSelection(vtkMapper mapper) {}
		};
	
	protected ActorCustomiser actorCustomiser;
	protected ActorHighlightedCustomiser actorHighlightedCustomiser;
	protected MapperCustomiser mapperCustomiser;
	protected MapperHighlightedCustomiser mapperHighlightedCustomiser;
	protected ActorSelectionCustomiser actorSelectionCustomiser;
	protected MapperSelectionCustomiser mapperSelectionCustomiser;

	/**
	 * Constructor.  It must not be called directly, only by subclasses.
	 * 
	 * @param parent parent node
	 */
	protected AbstractNode(AbstractNode parent)
	{
		this.parent = parent;
		if(parent != null)
		{
			parent.addChild(this);
			setPickable(parent.isPickable());
			setVisible(parent.isVisible());
		}
	}

	public AbstractNode getRoot()
	{
		if(parent != null)
			return parent.getRoot();
		return this;
	}
	
	public AbstractNode getParent()
	{
		return parent;
	}
	
	protected abstract void addChild(AbstractNode parent);

	public abstract List<LeafNode> getLeaves();

	/**
	 * Find the leaf node that contains the cellID
	 * @param cellID
	 * @return
	 */
	protected abstract LeafNode getNode(int cellID);

	/**
	 * Set pickable the actor of the node. If the node is not a manager,
	 * set the parent to be pickable.
	 * @param pickable
	 */
	public void setPickable(boolean pickable)
	{
		this.pickable = pickable;
		
		if(actor != null)
		{
			actor.SetPickable(Utils.booleanToInt(pickable));
		}
		else if(parent != null)
			parent.setPickable(pickable);
	}

	public boolean isPickable()
	{
		return pickable;
	}
	
	public void addActorListener(ActorListener listener)
	{
		actorListeners.add(listener);
	}
	
	public void removeActorListener(ActorListener listener)
	{
		actorListeners.remove(listener);
	}
	
	public vtkActor getActor()
	{
		return actor;
	}

	protected void fireActorCreated(vtkActor actor)
	{
		for (ActorListener listener : actorListeners)
			listener.actorCreated(this, actor);
	}

	protected void fireActorDeleted(vtkActor actor)
	{
		for (ActorListener listener : actorListeners)
			listener.actorDeleted(this, actor);
	}

	public void setActorCustomiser(ActorCustomiser actorCustomiser)
	{
		this.actorCustomiser = actorCustomiser;
	}

	public void setActorHighlightedCustomiser(ActorHighlightedCustomiser actorHighlightedCustomiser)
	{
		this.actorHighlightedCustomiser = actorHighlightedCustomiser;
	}

	public void setMapperCustomiser(MapperCustomiser mapperCustomiser)
	{
		this.mapperCustomiser = mapperCustomiser;
	}

	public void setMapperHighlightedCustomiser(MapperHighlightedCustomiser mapperHighlightedCustomiser)
	{
		this.mapperHighlightedCustomiser = mapperHighlightedCustomiser;
	}

	public ActorSelectionCustomiser getActorSelectionCustomiser()
	{
		if(actorSelectionCustomiser != null)
			return actorSelectionCustomiser;
		else if(parent != null)
			actorSelectionCustomiser = parent.getActorSelectionCustomiser();
		
		if(actorSelectionCustomiser == null)
			actorSelectionCustomiser = DEFAULT_ACTOR_SELECTION_CUSTOMISER;
		
		return actorSelectionCustomiser;
	}

	public void setActorSelectionCustomiser(ActorSelectionCustomiser actorSelectionCustomiser)
	{
		this.actorSelectionCustomiser = actorSelectionCustomiser;
	}


	public MapperSelectionCustomiser getMapperSelectionCustomiser()
	{
		if(mapperSelectionCustomiser != null)
			return mapperSelectionCustomiser;
		if(parent != null)
			mapperSelectionCustomiser = parent.getMapperSelectionCustomiser();
		
		if(mapperSelectionCustomiser == null)
			mapperSelectionCustomiser = DEFAULT_MAPPER_SELECTION_CUSTOMISER;
		
		return mapperSelectionCustomiser;
	}

	public void setMapperSelectionCustomiser(MapperSelectionCustomiser mapperSelectionCustomiser)
	{
		this.mapperSelectionCustomiser = mapperSelectionCustomiser;
	}

	public void applyActorCustomiser()
	{
		if(!isSelected() && actor != null)
			getActorCustomiser().customiseActor(actor);
	}

	public void applyMapperCustomiser()
	{
		if(!isSelected() && mapper != null)
			getMapperCustomiser().customiseMapper(mapper);
	}
	
	public abstract void applyActorHighlightedCustomiser();
	
	public abstract void applyMapperHighlightedCustomiser();
	
	public void applyActorSelectionCustomiser()
	{
		if(selectionHighlighter != null)
			getActorSelectionCustomiser().customiseActorSelection(selectionHighlighter);
	}
	
	public void applyMapperSelectionCustomiser()
	{
		if(selectionHighlighterMapper != null)
			getMapperSelectionCustomiser().customiseMapperSelection(selectionHighlighterMapper);
	}
	
	public ActorCustomiser getActorCustomiser()
	{
		if(actorCustomiser != null)
			return actorCustomiser;
		if(parent != null)
			actorCustomiser = parent.getActorCustomiser();
		
		if(actorCustomiser == null)
			actorCustomiser = DEFAULT_ACTOR_CUSTOMISER;
		
		return actorCustomiser;
	}

	public ActorHighlightedCustomiser getActorHighlightedCustomiser()
	{
		if(actorHighlightedCustomiser != null)
			return actorHighlightedCustomiser;
		if(parent != null)
			actorHighlightedCustomiser = parent.getActorHighlightedCustomiser();
		
		if(actorHighlightedCustomiser == null)
			actorHighlightedCustomiser = DEFAULT_ACTOR_HIGHLIGHTED_CUSTOMISER;
		
		return actorHighlightedCustomiser;
	}

	public MapperCustomiser getMapperCustomiser()
	{
		if(mapperCustomiser != null)
			return mapperCustomiser;
		if(parent != null)
			mapperCustomiser = parent.getMapperCustomiser();
		
		if(mapperCustomiser == null)
			mapperCustomiser = DEFAULT_MAPPER_CUSTOMISER;
			
		return mapperCustomiser;
	}

	public MapperHighlightedCustomiser getMapperHighlightedCustomiser()
	{
		if(mapperHighlightedCustomiser != null)
			return mapperHighlightedCustomiser;
		if(parent != null)
			mapperHighlightedCustomiser = parent.getMapperHighlightedCustomiser();
		
		if(mapperHighlightedCustomiser == null)
			mapperHighlightedCustomiser = DEFAULT_MAPPER_HIGHLIGHTED_CUSTOMISER;
		
		return mapperHighlightedCustomiser;
	}

	public boolean isVisible()
	{
		return visible;
	}

	public void setVisible(boolean visible)
	{
		if(this.visible == visible)
			return;
		
		this.visible = visible;
		
		if(actor != null)
			actor.SetVisibility(Utils.booleanToInt(visible));
		
		modified();
	}
	
	protected long getModificationTime()
	{
		return modificationTime;
	}
	
	public void modified()
	{
		modificationTime = System.nanoTime();
	}
	
	protected abstract void refresh();
	
	protected void refreshMapper()
	{
		mapper.SetInput(data);
		mapper.Update();
	}
	
	protected abstract void refreshData();
	
	protected void refreshActor()
	{
		boolean actorCreated = false;
		
		if(actor == null)
		{
			actorCreated = true;
			actor = new vtkActor();
			getActorCustomiser().customiseActor(actor);
			mapper = new vtkPolyDataMapper();
			getMapperCustomiser().customiseMapper(mapper);
			actor.SetMapper(mapper);
			actor.SetVisibility(Utils.booleanToInt(visible));
			actor.SetPickable(Utils.booleanToInt(pickable));
		}
		refreshMapper();

		// Call fire after the map creation
		if(actorCreated)
			fireActorCreated(actor);
	}
	
	protected void createData(LeafNode.DataProvider dataProvider)
	{
		data = new vtkPolyData();
		data.SetPoints(Utils.createPoints(dataProvider.getNodes()));
		data.SetVerts(Utils.createCells(dataProvider.getNbrOfVertices(), dataProvider.getVertices()));
		data.SetLines(Utils.createCells(dataProvider.getNbrOfLines(), dataProvider.getLines()));
		data.SetPolys(Utils.createCells(dataProvider.getNbrOfPolys(), dataProvider.getPolys()));
				
		if(LOGGER.isLoggable(Level.FINEST))
		{
			LOGGER.finest("Number of points : " + data.GetPoints().GetNumberOfPoints());
			LOGGER.finest("Number of vertices : " + data.GetVerts().GetNumberOfCells());
			LOGGER.finest("Number of lines : " + data.GetLines().GetNumberOfCells());
			LOGGER.finest("Number of polys : " + data.GetPolys().GetNumberOfCells());
			LOGGER.finest("vertex coherance : " + Utils.isMeshCoherent(dataProvider.getNodes(), dataProvider.getVertices()));
			LOGGER.finest("line coherance : " + Utils.isMeshCoherent(dataProvider.getNodes(), dataProvider.getLines()));
			LOGGER.finest("polys coherance : " + Utils.isMeshCoherent(dataProvider.getNodes(), dataProvider.getPolys()));
		}
		
		if(dataProvider.getNormals() == null)
			return;
		
		// Compute normals that are not given
		vtkPolyDataNormals algoNormals = new vtkPolyDataNormals();
		algoNormals.SetInput(data);
		algoNormals.SplittingOff();
		algoNormals.FlipNormalsOff();
		algoNormals.AutoOrientNormalsOff();
		algoNormals.ComputePointNormalsOn();
		algoNormals.Update();

		data = algoNormals.GetOutput();
		
		vtkFloatArray computedNormals = (vtkFloatArray) data.GetPointData().GetNormals();
		float[] javaComputedNormals = computedNormals.GetJavaArray();
		float[] javaNormals = dataProvider.getNormals();
		
		// If the normals are not computed change them by the normals computed by the meshes
		for(int i = 0 ; i < javaComputedNormals.length / 3 ; i += 3)
		{
			if(javaNormals[i] == 0. && javaNormals[i + 1] == 0. && javaNormals[i + 2] == 0.)
			{
				javaNormals[i] = javaComputedNormals[i];
				javaNormals[i + 1] = javaComputedNormals[i  + 1];
				javaNormals[i + 2] = javaComputedNormals[i + 2];
			}
		}
		
		vtkFloatArray normals = new vtkFloatArray();
		normals.SetNumberOfComponents(3);
		normals.SetJavaArray(javaNormals);
		
		data.GetPointData().SetNormals(normals);
		//fireDataModified(data);
	}
	
	protected void deleteDatas()
	{
		data = null;
		if(actor != null)
		{
			fireActorDeleted(actor);
			actor = null;
		}
		mapper = null;
	}
	
	protected abstract void manageHighlight();
			
	protected void highlight()
	{
		assert actor != null;
		
		getActorHighlightedCustomiser().customiseActorHighlighted(actor);
		getMapperHighlightedCustomiser().customiseMapperHighlighted(mapper);
	}
	
	protected void unHighlight()
	{
		assert actor != null;
		
		getActorCustomiser().customiseActor(actor);
		getMapperCustomiser().customiseMapper(mapper);
	}
	
	public abstract void highlightSelection();

	protected void unHighlightSelection()
	{
		if(selectionHighlighter == null)
			return;
		
		fireActorDeleted(selectionHighlighter);
		selectionHighlighter = null;
		selectionHighlighterMapper = null;
	}
	
	protected long selectionTime()
	{
		return selectionTime;
	}
	
	public void select()
	{
		if(selected)
			return;
		
		selected = true;
		selectionTime = System.nanoTime();
	}

	public void unselect()
	{
		if(!selected)
			return;
		
		selected = false;
		selectionTime = System.nanoTime();
	}
	
	public boolean isSelected()
	{
		return selected;
	}
	
	/**
	 * Declare some cells as being selected.  This is called by
	 * {@link Scene#pick(vtk.vtkCanvas, int[], int[])} to store the list
	 * of selected cells in each node, and this result is used by renderer. 
	 * 
	 * @param cellSelection  list of cell ids being selected
	 */
	abstract void setCellSelection(TIntArrayList cellSelection);
	
	public void setManager(boolean manager)
	{
		if(this.manager == manager)
			return;
		
		this.manager = manager;
		
		if(!this.manager)
			deleteDatas();
		
		modified();
	}

	public boolean isManager()
	{
		return manager;
	}
	
	public void setDebugName(String name)
	{
		debugName = name;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(getClass().getName()+"@"+Integer.toHexString(hashCode()));
		if (debugName != null)
			sb.append(" "+debugName);
		if (manager)
			sb.append(" manager");
		if (actor != null)
		{
			sb.append(" actor@"+Integer.toHexString(actor.hashCode()));
			if (actor.GetVisibility() != 0)
				sb.append(" visible");
			if (actor.GetPickable() != 0)
				sb.append(" pickable");
		}
		if (selectionHighlighter != null)
		{
			sb.append(" selectionHighlighter@"+Integer.toHexString(selectionHighlighter.hashCode()));
			if (selectionHighlighter.GetVisibility() != 0)
				sb.append(" visible");
			if (selectionHighlighter.GetPickable() != 0)
				sb.append(" pickable");
		}
		return sb.toString();
	}
}
