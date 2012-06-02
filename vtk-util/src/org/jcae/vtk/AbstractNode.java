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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkFloatArray;
import vtk.vtkPainterPolyDataMapper;
import vtk.vtkPointData;
import vtk.vtkPolyData;
import vtk.vtkPolyDataNormals;
import vtk.vtkProperty;

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
 * method, which modifies {@link #selectionActor} actor.
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
	protected final Node parent;
	private final ArrayList<ActorListener> actorListeners = new ArrayList<ActorListener>();
	/** Flag to tell if this node is a manager */
	private boolean manager;
	/** Actor of this node, if it is a manager */
	protected vtkActor actor;
	/** Geometry of this actor */
	protected vtkPainterPolyDataMapper mapper;
	protected vtkPolyData data;

	/** Actor used for selection */
	protected vtkActor selectionActor;
	protected vtkPainterPolyDataMapper selectionMapper;

	/** Last time this actor had been updated */
	protected long lastUpdate;
	/** Last time data of this actor had been modified */
	protected long dataTime;
	/** Last time this actor had been modified (data, color, visibility) */
	protected long modificationTime;
	/** Last time this node had been selected */
	protected long selectionTime;
	
	// Useful for debugging
	private String debugName;

	protected boolean visible = true;
	protected boolean selected;
	protected boolean pickable;
	
	public static interface ActorListener
	{
		void actorCreated(AbstractNode node, vtkActor actor);
		void actorDeleted(AbstractNode node, vtkActor actor);
	}
	
	/**
	 * Customise actor when node is not selected.
	 */
	public interface ActorCustomiser
	{
		void customiseActor(vtkActor actor);
	}
	
	/**
	 * Customise mapper when node is not selected.
	 */
	public interface MapperCustomiser
	{
		void customiseMapper(vtkPainterPolyDataMapper mapper);
	}
	
	/**
	 * Default actor customiser, it does nothing.
	 */
	public static ActorCustomiser DEFAULT_ACTOR_CUSTOMISER =
		new ActorCustomiser()
		{
			@Override
			public void customiseActor(vtkActor actor) {}
		};
		
	/**
	 * Default mapper customiser, it calls vtkMapper.SetResolveCoincidentTopologyToPolygonOffset.
	 */
	public static MapperCustomiser DEFAULT_MAPPER_CUSTOMISER =
		new MapperCustomiser()
		{
			@Override
			public void customiseMapper(vtkPainterPolyDataMapper mapper)
			{
				Utils.setPolygonOffset(mapper, Utils.getOffsetFactor(),
					Utils.getOffsetValue());
			}
		};

	/**
	 * Default actor customiser when cells of this node are selected, it does nothing.
	 */
	public static ActorCustomiser DEFAULT_SELECTION_ACTOR_CUSTOMISER =
		new ActorCustomiser()
		{
			@Override
			public void customiseActor(vtkActor actor) {}
		};
	
	/**
	 * Default mapper customiser when cells of this node are selected, it does nothing.
	 */
	public static MapperCustomiser DEFAULT_SELECTION_MAPPER_CUSTOMISER =
		new MapperCustomiser()
		{
			@Override
			public void customiseMapper(vtkPainterPolyDataMapper mapper) {}
		};
	
	protected ActorCustomiser actorCustomiser;
	protected MapperCustomiser mapperCustomiser;
	
	protected ActorCustomiser selectionActorCustomiser;
	protected MapperCustomiser selectionMapperCustomiser;

	/**
	 * Constructor.  It must not be called directly, only by subclasses.
	 * 
	 * @param parent parent node
	 */
	protected AbstractNode(Node parent)
	{
		this.parent = parent;
		if(parent != null)
		{
			pickable = parent.pickable;
			visible  = parent.visible;
		}
	}

	protected vtkActor createActor()
	{
		return new vtkActor();
	}
	
	public Node getParent()
	{
		return parent;
	}
	
	public abstract List<LeafNode> getLeaves();

	/**
	 * Set pickable the actor of the node. If the node is not a manager,
	 * it does nothing.
	 * @param pickable
	 */
	public void setPickable(boolean pickable)
	{
		this.pickable = pickable;
		
		if(actor != null)
			actor.SetPickable(Utils.booleanToInt(pickable));
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
		timeStampModified();
	}

	public void setMapperCustomiser(MapperCustomiser mapperCustomiser)
	{
		this.mapperCustomiser = mapperCustomiser;
		timeStampModified();
	}

	public ActorCustomiser getSelectionActorCustomiser()
	{
		if(selectionActorCustomiser != null)
			return selectionActorCustomiser;
		else if(parent != null)
			selectionActorCustomiser = parent.getSelectionActorCustomiser();
		
		if(selectionActorCustomiser == null)
			selectionActorCustomiser = DEFAULT_SELECTION_ACTOR_CUSTOMISER;
		
		return selectionActorCustomiser;
	}

	public void setSelectionActorCustomiser(ActorCustomiser selectionActorCustomiser)
	{
		this.selectionActorCustomiser = selectionActorCustomiser;
		timeStampSelected();
	}


	public MapperCustomiser getSelectionMapperCustomiser()
	{
		if(selectionMapperCustomiser != null)
			return selectionMapperCustomiser;
		if(parent != null)
			selectionMapperCustomiser = parent.getSelectionMapperCustomiser();
		
		if(selectionMapperCustomiser == null)
			selectionMapperCustomiser = DEFAULT_SELECTION_MAPPER_CUSTOMISER;
		
		return selectionMapperCustomiser;
	}

	public void setSelectionMapperCustomiser(MapperCustomiser selectionMapperCustomiser)
	{
		this.selectionMapperCustomiser = selectionMapperCustomiser;
		timeStampSelected();
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

	protected void timeStampData()
	{
		dataTime = System.nanoTime();
		// When data are modified, actor must be updated
		timeStampModified();
		if (!manager && parent != null)
			parent.timeStampData();
	}
	
	protected void timeStampModified()
	{
		modificationTime = System.nanoTime();
		if (!manager && parent != null)
			parent.timeStampModified();
	}
	
	protected void timeStampSelected()
	{
		selectionTime = System.nanoTime();
		if (!manager && parent != null)
			parent.timeStampSelected();
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
		
		timeStampModified();
		
		// If node is not a manager, its manager have to update
		// their data.
		if (!manager && parent != null)
			parent.timeStampData();
	}
	
	protected abstract void refresh();
	
	protected void createData(LeafNode.DataProvider dataProvider)
	{
		data = new vtkPolyData();
		data.SetPoints(Utils.createPoints(dataProvider.getNodes()));

		vtkCellArray cells = Utils.createCells(dataProvider.getNbrOfVertices(),
			dataProvider.getVertices());
		data.SetVerts(cells);		
		
		cells=Utils.createCells(dataProvider.getNbrOfLines(), dataProvider.getLines());
		data.SetLines(cells);

		cells=Utils.createCells(dataProvider.getNbrOfPolys(), dataProvider.getPolys());
		data.SetPolys(cells);
				
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
		vtkPointData pointData = data.GetPointData();
		vtkFloatArray computedNormals = (vtkFloatArray) pointData.GetNormals();		
		float[] javaComputedNormals = computedNormals.GetJavaArray();
		float[] javaNormals = dataProvider.getNormals();
		
		// If the normals are not computed change them by the normals computed by the meshes
		for(int i = 0 ; i < javaComputedNormals.length ; i+= 3)
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
		
		pointData.SetNormals(normals);
		//fireDataModified(data);
	}
	
	protected void deleteData()
	{
		if(data != null)
		{
			data = null;
		}
		if(actor != null)
		{
			fireActorDeleted(actor);
			actor = null;
		}
		if(mapper != null)
		{
			mapper = null;
		}
	}
	
	void deleteSelectionActor()
	{
		if(selectionActor == null)
			return;
		
		fireActorDeleted(selectionActor);
		if(selectionActor != null)
		{
			selectionActor = null;
		}
		if(selectionMapper != null)
		{
			selectionMapper = null;
		}
	}
	
	public void select()
	{
		if(selected)
			return;
		
		selected = true;
		timeStampSelected();
	}

	public void unselect()
	{
		if(!selected)
			return;
		
		selected = false;
		timeStampSelected();
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
	abstract void setCellSelection(PickContext pickContext, int [] cellSelection);
	abstract void clearCellSelection();
	
	public void setManager(boolean manager)
	{
		if(this.manager == manager)
			return;
		
		this.manager = manager;
		
		if(!this.manager)
			deleteData();
		
		timeStampModified();
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
		if (selected)
			sb.append(" selected");
		if (actor != null)
		{
			sb.append(" actor@"+Integer.toHexString(actor.hashCode()));
			if (actor.GetVisibility() != 0)
				sb.append(" visible");
			if (actor.GetPickable() != 0)
				sb.append(" pickable");
		}
		if (selectionActor != null)
		{
			sb.append(" selectionActor@"+Integer.toHexString(selectionActor.hashCode()));
			if (selectionActor.GetVisibility() != 0)
				sb.append(" visible");
			if (selectionActor.GetPickable() != 0)
				sb.append(" pickable");
		}
		return sb.toString();
	}

	public void setEdgeVisible(boolean b)
	{
		if(actor != null)
		{
			vtkProperty p = actor.GetProperty();
			p.SetEdgeVisibility(Utils.booleanToInt(b));
		}
	}

	public void setCulling(boolean front, boolean back)
	{
		if(actor != null)
		{
			vtkProperty p = actor.GetProperty();
			p.SetFrontfaceCulling(Utils.booleanToInt(front));
			p.SetBackfaceCulling(Utils.booleanToInt(back));
		}
	}
}
