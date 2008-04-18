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

package org.jcae.viewer3d.fd;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.logging.Logger;
import javax.media.j3d.*;
import javax.vecmath.Point3d;
import org.jcae.viewer3d.fd.sd.*;
import com.sun.j3d.utils.picking.PickIntersection;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickTool;

/**
 * @author nicolas
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class PL02BranchGroup extends BranchGroup
{
	private final static Logger LOGGER=Logger.getLogger(PL02BranchGroup.class.getName());
	protected FDProvider provider;
	//private HashSet currentSelection;
	protected ArrayList<Shape3D> allEdgeShapes;
	// one of the values above
	protected static final int PICK_FACES = 0;
	protected static final int PICK_EDGES = 1;
	protected static final int PICK_VERTICES = 2;
	protected int pickType;
	// Maintain a list of selected quads and edges
	protected ArrayList<SelectionQuad> selection = new ArrayList<SelectionQuad>();
	protected ArrayList<SelectionEdge> edgeSelection = new ArrayList<SelectionEdge>();
	protected Shape3D selectionShape, edgeSelectionShape;
	float[][] grid;
	private Map<Integer, Color> baseColor=new HashMap<Integer, Color>();
	
	public PL02BranchGroup(FDProvider provider)
	{						
		this.provider=provider;
		setUserData(this);
		setCapability(Node.ALLOW_BOUNDS_READ);
		setCapability(Group.ALLOW_CHILDREN_EXTEND);
		setCapability(BranchGroup.ALLOW_DETACH);
		setCapability(Node.ALLOW_PICKABLE_WRITE);
		update();
	}
	
	public void update()
	{		
		removeAllChildren();
		setPickable(true);

		//currentSelection = new HashSet();
		allEdgeShapes = new ArrayList<Shape3D>();
		
		grid=new float[3][];
		grid[0]=new float[provider.getXGridCount()];
		grid[1]=new float[provider.getYGridCount()];
		grid[2]=new float[provider.getZGridCount()];
		
		for(int i=0; i<grid[0].length; i++)
		{
			grid[0][i]=(float)provider.getXGrid(i);
		}

		for(int i=0; i<grid[1].length; i++)
		{
			grid[1][i]=(float)provider.getYGrid(i);
		}

		for(int i=0; i<grid[2].length; i++)
		{
			grid[2][i]=(float)provider.getZGrid(i);
		}

		makeGroups();
		makeWires();
	}
	
	private Plate[] domainToPlates(FDDomain domain)
	{
		LOGGER.finest("<creating plates >");
		int n=domain.getNumberOfXPlate()+domain.getNumberOfYPlate()+domain.getNumberOfZPlate();
		LOGGER.finest("number of plates in domain is "+n);
		Plate[] plates=new Plate[n];
		
		Iterator<int[]> it=domain.getXPlateIterator();
		int i=0;
		while(it.hasNext())
		{			
			int[] indices=it.next();
			Plate p=new PlateX();			
			p.position=indices[0];
			p.min1=indices[1];
			p.min2=indices[2];
			p.max1=indices[3];
			p.max2=indices[4];
			plates[i]=p;
			i++;
		}

		if(i!=domain.getNumberOfXPlate())
			throw new IllegalStateException(i+"!="+domain.getNumberOfXPlate());

		it=domain.getYPlateIterator();	
		while(it.hasNext())
		{
			int[] indices=it.next();
			Plate p=new PlateY();			
			p.position=indices[0];
			p.min1=indices[1];
			p.min2=indices[2];
			p.max1=indices[3];
			p.max2=indices[4];
			plates[i]=p;
			i++;
		}
		
		if(i!=domain.getNumberOfXPlate()+domain.getNumberOfYPlate())
			throw new IllegalStateException(i+"!="+domain.getNumberOfXPlate()+domain.getNumberOfYPlate());

		
		it=domain.getZPlateIterator();
		while(it.hasNext())
		{
			int[] indices=it.next();
			Plate p=new PlateZ();			
			p.position=indices[0];
			p.min1=indices[1];
			p.min2=indices[2];
			p.max1=indices[3];
			p.max2=indices[4];
			plates[i]=p;
			i++;
		}
		
		if(i!=n)
			throw new IllegalStateException(i+"!="+n);
		
		LOGGER.finest("</creating plates>");
		return plates;
	}
	
	
	protected void makeGroups()
	{
		int totalQuads = 0;
		int totalInternalEdges = 0;
		int totalExternalEdges = 0;
		// loop over each group
		// loop over each group
		int[] groupID = provider.getDomainIDs();
		
		for (int g = 0; g < groupID.length; ++g)
		{
			LOGGER.finest("generating java3d tree for group number "+groupID[g]);
			// Set of EdgeLine objects. Overlapping edges on the same line are
			// merged together
			HashMap<EdgeLine, EdgeLine> externalEdges = new HashMap<EdgeLine, EdgeLine>();
			// Same trick for internal edges.
			HashMap<EdgeLine, EdgeLine> internalEdges = new HashMap<EdgeLine, EdgeLine>();
			FDDomain fdDomain=(FDDomain) provider.getDomain(groupID[g]);
			baseColor.put(new Integer(g), fdDomain.getColor());
			Plate[] plates = domainToPlates(fdDomain);
			
			if(plates.length==0)
				continue;
			
			// Create plates for this group
			FloatBuffer nioCoords = ByteBuffer.allocateDirect(
				plates.length * 4 * 3 * 4).order(ByteOrder.nativeOrder())
				.asFloatBuffer();
			FloatBuffer nioColors = ByteBuffer.allocateDirect(
				plates.length * 4 * 3 * 4).order(ByteOrder.nativeOrder())
				.asFloatBuffer();
			float[] baseColor = getColorForOrder(g, 0);
			//System.out.println(baseColor[0]+" "+baseColor[1]+" "+baseColor[2]);
			for (int np = 0; np < plates.length; ++np)
			{
				Plate p = plates[np];
				// put coordinates
				nioCoords.put(p.getCoordinates(grid));
				// put colors for the 4 vertices
				nioColors.put(baseColor);
				nioColors.put(baseColor);
				nioColors.put(baseColor);
				nioColors.put(baseColor);
				// Merge external edges
				addEdge(externalEdges, getLine(p, 2, p.min1), p.min2, p.max2);
				addEdge(externalEdges, getLine(p, 2, p.max1), p.min2, p.max2);
				addEdge(externalEdges, getLine(p, 1, p.min2), p.min1, p.max1);
				addEdge(externalEdges, getLine(p, 1, p.max2), p.min1, p.max1);
				// Merge internal edges
				for (int i = p.min1 + 1; i < p.max1; ++i)
					addEdge(internalEdges, getLine(p, 2, i), p.min2, p.max2);
				for (int j = p.min2 + 1; j < p.max2; ++j)
					addEdge(internalEdges, getLine(p, 1, j), p.min1, p.max1);
			}
			// use by reference array of colors => fast to change!			
			QuadArray qa = new NioQuadArray(plates.length * 4,
				GeometryArray.COORDINATES | GeometryArray.COLOR_3);
			qa.setCoordRefBuffer(new J3DBuffer(nioCoords));
			qa.setColorRefBuffer(new J3DBuffer(nioColors));
			qa.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
			qa.setCapabilityIsFrequent(GeometryArray.ALLOW_COLOR_WRITE);
			Appearance a = new Appearance();
			PolygonAttributes pa = new PolygonAttributes(
				PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0);
			pa.setPolygonOffset(1);
			pa.setPolygonOffsetFactor(1);
			a.setPolygonAttributes(pa);
			Shape3D s3d = new Shape3D(qa, a);
			PickTool.setCapabilities(s3d, PickTool.INTERSECT_FULL);
			s3d.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
			s3d.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
			s3d.setCapability(Node.ALLOW_PICKABLE_READ);
			s3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
			s3d.setPickable(true);
			s3d.setUserData(new BehindShape(s3d, plates, g));
			
			this.addChild(s3d);
			
			// Create edge shapes directly, don't make them appear in graph
			int nInternalEdges = 0;
			for (Iterator<EdgeLine> it = internalEdges.keySet().iterator(); it.hasNext();)
			{
				EdgeLine el = it.next();
				nInternalEdges += el.getNumberOfEdges();
			}
			if (nInternalEdges > 0)
			{
				DoubleBuffer nioInternalEdges = ByteBuffer.allocateDirect(
					nInternalEdges * 2 * 3 * 8).order(ByteOrder.nativeOrder())
					.asDoubleBuffer();
				// create edge coords
				for (Iterator<EdgeLine> it = internalEdges.keySet().iterator(); it
					.hasNext();)
				{
					EdgeLine el = it.next();
					nioInternalEdges.put(el.getCoords(grid));
				}
				LineArray la = new NioLineArray(nInternalEdges * 2,
					GeometryArray.COORDINATES | GeometryArray.COLOR_3);
				la.setCoordRefBuffer(new J3DBuffer(nioInternalEdges));
				int colSize = nInternalEdges * 2 * 3;
				FloatBuffer nioInternalColors = ByteBuffer.allocateDirect(
					colSize * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
				float[] colors = getColorForOrder(g, 2);
				for (int i = 0; i < colSize; i += 3)
					nioInternalColors.put(colors);
				la.setColorRefBuffer(new J3DBuffer(nioInternalColors));
				la.setUserData(new int[]{g, 2});
				a = new Appearance();
				//pa = new PolygonAttributes(PolygonAttributes.POLYGON_LINE,
				// PolygonAttributes.CULL_NONE, 0);
				//pa.setPolygonOffset(4);
				//pa.setPolygonOffsetFactor(4);
				//a.setPolygonAttributes(pa);
				//LineAttributes lat = new LineAttributes();
				//lat.setLineAntialiasingEnable(true);
				//a.setLineAttributes(lat);
				//RenderingAttributes ra = new RenderingAttributes();
				//ra.setAlphaTestFunction(RenderingAttributes.GREATER);
				//ra.setAlphaTestValue(0.5f);
				//a.setRenderingAttributes(ra);
				s3d = new Shape3D(la, a);
				PickTool.setCapabilities(s3d, PickTool.INTERSECT_FULL);
				s3d.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
				s3d.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
				s3d.setCapability(Node.ALLOW_PICKABLE_READ);
				s3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
				s3d.setPickable(false); // by default, see actions
				s3d.setUserData(this); // this object will handle edges
				this.addChild(s3d);
				allEdgeShapes.add(s3d);
			}
			// Now, create external edge
			int nExternalEdges = 0;
			for (Iterator<EdgeLine> it = externalEdges.keySet().iterator(); it.hasNext();)
			{
				EdgeLine el = it.next();
				nExternalEdges += el.getNumberOfEdges();
			}
			if (nExternalEdges > 0)
			{
				DoubleBuffer nioExternalEdges = ByteBuffer.allocateDirect(
					nExternalEdges * 2 * 3 * 8).order(ByteOrder.nativeOrder())
					.asDoubleBuffer();
				// create edge coords
				for (Iterator<EdgeLine> it = externalEdges.keySet().iterator(); it
					.hasNext();)
				{
					EdgeLine el = it.next();
					nioExternalEdges.put(el.getCoords(grid));
				}
				LineArray la = new NioLineArray(nExternalEdges * 2,
					GeometryArray.COORDINATES | GeometryArray.COLOR_3);
				la.setCoordRefBuffer(new J3DBuffer(nioExternalEdges));
				int colSize = nExternalEdges * 2 * 3;
				FloatBuffer nioExternalColors = ByteBuffer.allocateDirect(
					colSize * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
				float[] colors = getColorForOrder(g, 4);
				for (int i = 0; i < colSize; i += 3)
					nioExternalColors.put(colors);
				la.setColorRefBuffer(new J3DBuffer(nioExternalColors));
				la.setUserData(new int[]{g, 4});
				a = new Appearance();
				//pa = new PolygonAttributes(PolygonAttributes.POLYGON_LINE,
				// PolygonAttributes.CULL_NONE, 0);
				//pa.setPolygonOffset(3);
				//pa.setPolygonOffsetFactor(3);
				//a.setPolygonAttributes(pa);
				s3d = new Shape3D(la, a);
				PickTool.setCapabilities(s3d, PickTool.INTERSECT_FULL);
				s3d.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
				s3d.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
				s3d.setCapability(Node.ALLOW_PICKABLE_READ);
				s3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
				s3d.setPickable(false); // by default, see actions
				s3d.setUserData(this); // this object will handle edges
				this.addChild(s3d);
				allEdgeShapes.add(s3d);
			}
			totalQuads += plates.length;
			totalInternalEdges += nInternalEdges;
			totalExternalEdges += nExternalEdges;
		}
		System.out.println("Total quads: " + totalQuads);
		System.out.println("Total Internal Plate edges: " + totalInternalEdges);
		System.out.println("Total External Plate edges: " + totalExternalEdges);
	}
	

	/**
	 * Adds a new edge to a line, merging with other edges on this line if
	 * necessary
	 * 
	 * @param edges :
	 *            An EdgeLine map
	 * @param a
	 *            line for this map, may already exist or not
	 * @param i
	 *            first grid index for the nex edge on this line
	 * @param j
	 *            second grid index for the nex edge on this line
	 */
	protected void addEdge(HashMap<EdgeLine, EdgeLine> edges, EdgeLine line, int i, int j)
	{
		EdgeLine el = edges.get(line);
		if (el == null) edges.put(line, line);
		else line = el;
		line.add(i, j);
	}

	/**
	 * Gets the EdgeLine object corresponding to a plate edge
	 * 
	 * @param p :
	 *            the plate for which an edge is wanted
	 * @param dirnum :
	 *            the direction 1 or 2 of the plate
	 * @param the
	 *            index in the other direction of the plate (p.min? and p.max?
	 *            for the external edges)
	 * @return an EdgeLine with these characteristics
	 */
	protected EdgeLine getLine(Plate p, int dirnum, int idx)
	{
		if (p instanceof PlateX)
		{
			// Parameters match by incredible chance!
			return new EdgeLine(p.position, idx, dirnum);
		}
		if (p instanceof PlateY)
		{
			// first direction of Y plate is X, second is Z => 0 and 2
			if (dirnum == 1) return new EdgeLine(p.position, idx, 0);
			else return new EdgeLine(idx, p.position, 2);
		}
		if (p instanceof PlateZ)
		{
			// first direction of Z plate is X, second is Y => 0 and 1
			return new EdgeLine(idx, p.position, dirnum - 1);
		}
		return null;
	}
	/**
	 * Overloads the quad array and implements ref to double translation for
	 * Picking
	 */
	protected static class NioQuadArray extends QuadArray
	{
		//double[] array;
		public NioQuadArray(int arg0, int arg1)
		{
			super(arg0, arg1 | GeometryArray.BY_REFERENCE
				| GeometryArray.USE_NIO_BUFFER);
		}

		@Override
		public double[] getCoordRefDouble()
		{			
			float[] fs=getCoordRefFloat();
			double[] toReturn=new double[fs.length];
			for(int i=0; i<fs.length; i++)
			{
				toReturn[i]=fs[i];
			}
			return toReturn;
		}
		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.media.j3d.GeometryArray#getCoordRefDouble()
		 */
		@Override
		public float[] getCoordRefFloat()
		{
			// Get ref to Nio buffer, of type Float by construction in the
			// calling code
			// Since we used allocateDirect to have it in main memory (thus
			// fastening transfer
			// Video ram), the buffer is not backed by a JVM array
			// => array() doesn't work
			// => keep Float buffer and make a double one here for the occasion
			// => better to copy a buffer from RAM to JVM only when picking,
			// rather than copy a buffer from JVM to RAM each time the scene is
			// rendered, as would
			// be the case without NIO.
			// Also, when picking, tests have shown that NIO buffer copy has no
			// noticeable impact
			// on performance. So, it is actually better to copy the array each
			// time the picking is
			// done than to hog memory with a permanent copy.
			float[] array = new float[getVertexCount() * 3];
			FloatBuffer db = (FloatBuffer) super.getCoordRefBuffer()
				.getBuffer();
			db.rewind();
			db.get(array); // optimized get
			return array;
		}

		@Override
		public void setCoordRefBuffer(J3DBuffer buffer)
		{
			super.setCoordRefBuffer(buffer);
			/*
			 * // Use this to cache data => too little perf gained for the
			 * memory used array = new double[getVertexCount()*3]; DoubleBuffer
			 * db = (DoubleBuffer)buffer.getBuffer(); db.rewind();
			 * db.get(array); // optimized get
			 */
		}
	}
	/**
	 * Overloads the quad array and implements ref to double translation for
	 * Picking
	 */
	protected static class NioLineArray extends LineArray
	{
		public NioLineArray(int arg0, int arg1)
		{
			super(arg0, arg1 | GeometryArray.BY_REFERENCE
				| GeometryArray.USE_NIO_BUFFER);
		}
		
		@Override
		public double[] getCoordRefDouble()
		{			
			float[] fs=getCoordRefFloat();
			double[] toReturn=new double[fs.length];
			for(int i=0; i<fs.length; i++)
			{
				toReturn[i]=fs[i];
			}
			return toReturn;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.media.j3d.GeometryArray#getCoordRefDouble()
		 */
		@Override
		public float[] getCoordRefFloat()
		{
			// Get ref to Nio buffer, of type Float by construction in the
			// calling code
			// Since we used allocateDirect to have it in main memory (thus
			// fastening transfer
			// Video ram), the buffer is not backed by a JVM array
			// => array() doesn't work
			// => keep Float buffer and make a double one here for the occasion
			// => better to copy a buffer from RAM to JVM only when picking,
			// rather than copy a buffer from JVM to RAM each time the scene is
			// rendered, as would
			// be the case without NIO.
			// Also, when picking, tests have shown that NIO buffer copy has no
			// noticeable impact
			// on performance. So, it is actually better to copy the array each
			// time the picking is
			// done than to hog memory with a permanent copy.
			float[] array = new float[getVertexCount() * 3];
			FloatBuffer db = (FloatBuffer) super.getCoordRefBuffer()
				.getBuffer();
			db.rewind();
			db.get(array); // optimized get
			return array;
		}
	}

	/**
	 * returns a color for a group/order and for a state
	 * 
	 * @param order
	 *            A color order number. This determines the base color (hue).
	 *            <br>
	 *            -2 => Wire <br>
	 *            else group index
	 * @param what
	 *            The goal for this color. <br>
	 *            0 = unselected face. <br>
	 *            1 = selected face. <br>
	 *            2 = unselected internal edge <br>
	 *            3 = selected internal edge <br>
	 *            4 = unselected external edge <br>
	 *            5 = selected external edge <br>
	 *            6 = selected quad <br>
	 */
	public float[] getColorForOrder(int order, int what)
	{
		// wires have no group
		if (order == -2)
		{
			switch (what)
			{
				case 0 :
					return new float[]{0.4f, 0.4f, 0.4f};
				case 1 :
					return new float[]{1.0f, 1.0f, 1.0f};
				case 2 :
					return new float[]{0.95f, 0.95f, 0.95f};
			}
		}
				
		float s,h;
		float b = 1.0f;
		Color c=baseColor.get(new Integer(order));
		float[] hsb=Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
		h=hsb[0];
		s=hsb[1];
		b=hsb[2];
		
		switch (what)
		{
			case 0 :
				s = s*1.0f;
				break; // unactive face has saturated color
			case 1 :
				s = s*0.75f;
				break; // selected face is whiter
			case 2 :
				s = s*0.5f;
				break; // internal edges
			case 3 :
				s = s*0.0f;
				break; // selected internal edges
			case 4 :
				s = s*1.0f;
				b = b*0.8f;
				break; // external edges
			case 5 :
				s = 0.0f;
				break; // selected external edges
			case 6 :
				s = s*0.3f;
				break; // selected quad is whiter
			default :
				s = s*1.0f;
				break;
		}
		return Color.getHSBColor(h,s,b).getRGBColorComponents(null);
	}
	/*
	 * public static float[] getColorForOrder(int order, int what, float alpha) {
	 * float[] ret = new float[4]; float[] base = getColorForOrder(order,what);
	 * ret[0] = base[0]; ret[1] = base[1]; ret[2] = base[2]; ret[3] = alpha;
	 * return ret; }
	 */
	/** Class named like this because the quads will always show behind the edges */
	public class BehindShape
	{
		protected FloatBuffer colors;
		protected int groupIdx;
		protected Shape3D shape;
		// temporary variables used during picking => allocate them only once
		float[][] vertices = new float[4][3];
		double[] point = new double[3];		
		private Plate[] plates;
		
		public BehindShape(Shape3D s3d, Plate[] plates, int g)
		{			
			this.shape=s3d;
			this.groupIdx = g;
			colors = (FloatBuffer) ((NioQuadArray) (shape.getGeometry()))
				.getColorRefBuffer().getBuffer();
			this.plates=plates;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see syn3d.nodes.xith3d.ShapeNodeXith3D#setAppearanceForHighlight(boolean)
		 */
		public void setAppearanceForHighlight(boolean on)
		{
			if (colors == null) return; // parent constructor
			// Great by ref color buffer = just modify the thing and it is OK!
			float[] color = getColorForOrder(groupIdx, on ? 1 : 0);
			colors.rewind();
			int l3 = plates.length * 3;
			for (int i = 0; i < l3; i += 3)
				colors.put(color);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see syn3d.base.ActiveNode#highlight(boolean, java.lang.Object)
		 */
		public void highlight(boolean on, Object parameter)
		{
			System.out.println("Total memory: "+Runtime.getRuntime().totalMemory());
			System.out.println("Free memory: "+Runtime.getRuntime().freeMemory());
			System.out.println(System.currentTimeMillis()+" starting highlight with "+parameter);
			if (parameter instanceof PickResult)
			{
				PickResult result = (PickResult) parameter;
				result.setFirstIntersectOnly(true);
				PickIntersection pi = result.getIntersection(0);
				// indices of the picked quad
				// Indices are set to vertex indices, as this is not an Index
				// Geometry object
				// => easy to find the plate index from this
				int[] idx = pi.getPrimitiveCoordinateIndices();
				int plateNum = idx[0] / 4;				
				Plate p = plates[plateNum];
				Point3d point3d = pi.getPointCoordinates();
				point3d.get(point);
				FloatBuffer coords = (FloatBuffer) ((NioQuadArray) (shape
					.getGeometry())).getCoordRefBuffer().getBuffer();
				for (int i = 0; i < idx.length; ++i)
				{
					coords.position(idx[i] * 3);
					coords.get(vertices[i]);
				}
				int d1 = 0, d2 = 0;
				if (p instanceof PlateX)
				{
					d1 = 1;
					d2 = 2;
				} else if (p instanceof PlateY)
				{
					d1 = 0;
					d2 = 2;
				} else if (p instanceof PlateZ)
				{
					d1 = 0;
					d2 = 1;
				}
				int u = (int) Math.floor((point[d1] - vertices[0][d1])
					* (p.max1 - p.min1) / (vertices[3][d1] - vertices[0][d1]));
				int v = (int) Math.floor((point[d2] - vertices[0][d2])
					* (p.max2 - p.min2) / (vertices[1][d2] - vertices[0][d2]));
				int quadIdx = v * (p.max1 - p.min1) + u;
				u += p.min1;
				v += p.min2;
				System.out.println((on ? "" : "de") + "selected quad "
					+ quadIdx + " in plate " + plateNum + " in group ");
				System.out
					.println("Grid positions for the quad (x,y,z) indices:");
				int[] pos = p.getXYZGridIndices(u, v);
				System.out.println("vertex1 = (" + pos[0] + ", " + pos[1]
					+ ", " + pos[2] + ")");
				pos = p.getXYZGridIndices(u, v + 1);
				System.out.println("vertex2 = (" + pos[0] + ", " + pos[1]
					+ ", " + pos[2] + ")");
				pos = p.getXYZGridIndices(u + 1, v + 1);
				System.out.println("vertex3 = (" + pos[0] + ", " + pos[1]
					+ ", " + pos[2] + ")");
				pos = p.getXYZGridIndices(u + 1, v);
				System.out.println("vertex4 = (" + pos[0] + ", " + pos[1]
					+ ", " + pos[2] + ")");
				float[] color = getColorForOrder(groupIdx, on ? 1 : 0);
				for (int i = 0; i < idx.length; ++i)
				{
					colors.position(idx[i] * 3);
					colors.put(color);
				}
				toggleSelectedQuad(on, new SelectionQuad(p, u, v, groupIdx));
				// Use event propagation, but don't call
				// setAppearanceForHighlight
				FloatBuffer tmp = colors;
				colors = null;				
				colors = tmp;
			}
			System.out.println(System.currentTimeMillis()+" end of highlight");
		}		
	}

	/**
	 * Simple algorithm to add or remove a quad from the selection Ideas: 1. Use
	 * one shape per quad => easy, but can become very fast too big to fit in
	 * memory. => pb: flicker when adding / removing because the scene is
	 * detached 2. Use only one shape, modify geometry TODO: pre-create an empty
	 * shape to avoid initial flicker
	 * 
	 * @param on
	 * @param p
	 * @param u
	 * @param v
	 * @param groupIdx2
	 */
	protected void toggleSelectedQuad(boolean on, SelectionQuad sq)
	{
		LOGGER.finest("on="+on+" selectionQuad="+sq);
		if (on)
		{
			// add the given quad to the list
			if (selection.contains(sq)) return; // already in
			selection.add(sq);
		} else
		{
			// remove the given quad from the list
			if (!selection.contains(sq)) return; // not present
			selection.remove(sq);
			if (selection.size() == 0)
			{
				QuadArray qa = (QuadArray) selectionShape.getGeometry();
				qa.setValidVertexCount(0);
				return;
			}
		}
		// Use in-memory arrays instead of NIO because selection should not be
		// too big
		// => faster
		QuadArray qa = new QuadArray(selection.size() * 4,
			GeometryArray.COORDINATES | GeometryArray.COLOR_3
				| GeometryArray.BY_REFERENCE);
		float[] coords = new float[selection.size() * 4 * 3];
		float[] colors = new float[selection.size() * 4 * 3];

		for (int i = 0; i < coords.length; i += 12)
		{
			SelectionQuad quad = selection.get(i / 12);
			quad.updateCoords(grid, coords, i);
			quad.updateColors(colors, i);
		}
		qa.setCoordRefFloat(coords);
		qa.setColorRefFloat(colors);
		qa.setCapability(GeometryArray.ALLOW_COUNT_WRITE);
		// update selection Shape with the new selection list
		if (selectionShape == null)
		{
			Appearance a = new Appearance();
			PolygonAttributes pa = new PolygonAttributes(
				PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0);
			pa.setPolygonOffset(0.5f); // between faces and edges
			pa.setPolygonOffsetFactor(0.5f);
			a.setPolygonAttributes(pa);
			selectionShape = new Shape3D(qa, a);
			selectionShape.setUserData(this);
			selectionShape.setPickable(false);
			selectionShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
			selectionShape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
			BranchGroup bg=new BranchGroup();
			bg.addChild(selectionShape);
			addChild(bg);
		}
		else selectionShape.setGeometry(qa);
	}
	protected class SelectionQuad
	{
		public Plate p;
		public int u, v;
		public int groupIdx;

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof SelectionQuad)) return false;
			SelectionQuad sq = (SelectionQuad) obj;
			return (sq.p.equals(p)) && (sq.u == u) && (sq.v == v)
				&& (sq.groupIdx == groupIdx);
		}

		/**
		 * @param colors
		 *            A color buffer
		 * @param i
		 *            The index in this buffer where to put the 12 components of
		 *            this quad
		 */
		public void updateColors(float[] colors, int i)
		{
			float[] baseColor = getColorForOrder(groupIdx, 6);
			System.arraycopy(baseColor, 0, colors, i, 3);
			System.arraycopy(baseColor, 0, colors, i + 3, 3);
			System.arraycopy(baseColor, 0, colors, i + 6, 3);
			System.arraycopy(baseColor, 0, colors, i + 9, 3);
		}

		/**
		 * @param coords
		 *            A coordinate buffer
		 * @param i
		 *            The index in this buffer where to put the 12 components of
		 *            this quad
		 */
		public void updateCoords(float[][] grid, float[] coords, int i)
		{
			System.arraycopy(p.getCoordinates(grid, u, v), 0, coords,
				i, 3);
			System.arraycopy(p.getCoordinates(grid, u, v + 1), 0,
				coords, i + 3, 3);
			System.arraycopy(p.getCoordinates(grid, u + 1, v + 1), 0,
				coords, i + 6, 3);
			System.arraycopy(p.getCoordinates(grid, u + 1, v), 0,
				coords, i + 9, 3);
		}

		public SelectionQuad(Plate p, int u, int v, int groupIdx)
		{
			this.p = p;
			this.u = u;
			this.v = v;
			this.groupIdx = groupIdx;
		}
	}

	/**
	 * Simple algorithm to add or remove an edge from the selection Ideas: 1.
	 * Use one shape per quad => easy, but can become very fast too big to fit
	 * in memory. => pb: flicker when adding / removing because the scene is
	 * detached 2. Use only one shape, modify geometry TODO: pre-create an empty
	 * shape to avoid initial flicker
	 * 
	 * @param on
	 * @param p
	 * @param u
	 * @param v
	 * @param groupIdx2
	 */
	protected void toggleSelectedEdge(boolean on, SelectionEdge se)
	{
		if (on)
		{
			// add the given edge to the list
			if (edgeSelection.contains(se)) return; // already in
			edgeSelection.add(se);
		} else
		{
			// remove the given edge from the list
			if (!edgeSelection.contains(se)) return; // not present
			edgeSelection.remove(se);
			if (edgeSelection.size() == 0)
			{
				LineArray la = (LineArray) edgeSelectionShape.getGeometry();
				la.setValidVertexCount(0);
				return;
			}
		}
		// Use in-memory arrays instead of NIO because edgeSelection should not
		// be too big
		// => faster
		LineArray la = new LineArray(edgeSelection.size() * 2,
			GeometryArray.COORDINATES | GeometryArray.COLOR_3
				| GeometryArray.BY_REFERENCE);
		double[] coords = new double[edgeSelection.size() * 2 * 3];
		float[] colors = new float[edgeSelection.size() * 2 * 3];

		for (int i = 0; i < coords.length; i += 6)
		{
			SelectionEdge edge = edgeSelection.get(i / 6);
			edge.updateCoords(grid, coords, i);
			edge.updateColors(colors, i);
		}
		la.setCoordRefDouble(coords);
		la.setColorRefFloat(colors);
		la.setCapability(GeometryArray.ALLOW_COUNT_WRITE);
		// update edgeSelection Shape with the new edgeSelection list
		if (edgeSelectionShape == null)
		{
			Appearance a = new Appearance();
			//PolygonAttributes pa = new
			// PolygonAttributes(PolygonAttributes.POLYGON_LINE,
			// PolygonAttributes.CULL_NONE, 0);
			//pa.setPolygonOffset(1); // above edges
			//pa.setPolygonOffsetFactor(1);
			LineAttributes lat = new LineAttributes();
			lat.setLineWidth(2.0f);
			lat.setLineAntialiasingEnable(true);
			a.setLineAttributes(lat);
			//a.setPolygonAttributes(pa);
			edgeSelectionShape = new Shape3D(la, a);
			edgeSelectionShape.setUserData(this);
			edgeSelectionShape.setPickable(false);
			edgeSelectionShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
			edgeSelectionShape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);

			BranchGroup bg=new BranchGroup();
			bg.addChild(edgeSelectionShape);
			addChild(bg);
		} else edgeSelectionShape.setGeometry(la);
	}
	protected class SelectionEdge
	{
		int[] end1;
		int[] end2;
		int groupIdx;
		int edgeType;

		public SelectionEdge(int[] end1, int[] end2, int groupIdx, int edgeType)
		{
			this.end1 = end1;
			this.end2 = end2;
			this.groupIdx = groupIdx;
			this.edgeType = edgeType;
		}

		/**
		 * @param colors
		 *            A color buffer
		 * @param i
		 *            The index in this buffer where to put the 12 components of
		 *            this quad
		 */
		public void updateColors(float[] colors, int i)
		{
			float[] baseColor = getColorForOrder(groupIdx, edgeType + 1);
			System.arraycopy(baseColor, 0, colors, i, 3);
			System.arraycopy(baseColor, 0, colors, i + 3, 3);
		}

		/**
		 * @param coords
		 *            A coordinate buffer
		 * @param i
		 *            The index in this buffer where to put the 12 components of
		 *            this quad
		 */
		public void updateCoords(float[][] grid, double[] coords, int i)
		{
			coords[i + 0] = grid[0][end1[0]];
			coords[i + 1] = grid[1][end1[1]];
			coords[i + 2] = grid[2][end1[2]];
			coords[i + 3] = grid[0][end2[0]];
			coords[i + 4] = grid[1][end2[1]];
			coords[i + 5] = grid[2][end2[2]];
		}

		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof SelectionEdge)) return false;
			SelectionEdge se = (SelectionEdge) obj;
			return Arrays.equals(end1, se.end1) && Arrays.equals(end2, se.end2)
				&& (groupIdx == se.groupIdx);
		}
	}

	/** Handles highlighting for edges */
	public void highlight(boolean on, Object parameter)
	{		
		// Should always be a pick result since those Shape3D do not appear in
		// the JTree
		if (parameter instanceof PickResult)
		{
			PickResult result = (PickResult) parameter;
			result.setFirstIntersectOnly(true);
			PickIntersection pi = result.getIntersection(0);
			// indices of the picked line
			// should always be line at this point
			// Indices are set to vertex indices, as this is not an Index
			// Geometry object
			int[] idx = pi.getPrimitiveCoordinateIndices();
			Point3d point3d = pi.getPointCoordinates();
			double[] point = new double[3];
			point3d.get(point);
			FloatBuffer coords = (FloatBuffer) (pi.getGeometryArray())
				.getCoordRefBuffer().getBuffer();
			float[] pt1 = new float[3];
			float[] pt2 = new float[3];
			coords.position(idx[0] * 3);
			coords.get(pt1);
			coords.position(idx[1] * 3);
			coords.get(pt2);
			int[] gpt1 = getGridCoordinate(pt1);
			int[] gpt2 = getGridCoordinate(pt2);
			int dim = 0;
			// lines are parallel to one of the axis => only one coordinate
			// changes
			if (gpt1[0] != gpt2[0]) dim = 0;
			else if (gpt1[1] != gpt2[1]) dim = 1;
			else if (gpt1[2] != gpt2[2]) dim = 2;
			else System.err
				.println("Error: edge is not parallel to one of the axis");
			// use gpt1 and gpt2 as a variables for the new point => destroy
			// previous content
			gpt1[dim] = (int) Math.floor(gpt1[dim] + (point[dim] - pt1[dim])
				* (gpt2[dim] - gpt1[dim]) / (pt2[dim] - pt1[dim]));
			System.out.println("Edge end 0 vertex grid coordinates = ("
				+ gpt1[0] + ", " + gpt1[1] + ", " + gpt1[2] + ")");
			gpt2[dim] = gpt1[dim] + 1;
			System.out.println("Edge end 1 vertex grid coordinates = ("
				+ gpt2[0] + ", " + gpt2[1] + ", " + gpt2[2] + ")");
			
			System.out.println("pi.getGeometryArray()="+pi.getGeometryArray());
			System.out.println("pi.getGeometryArray().getUserData()="+pi.getGeometryArray().getUserData());
			
			// handle wire case
			Object userData=pi.getGeometryArray().getUserData();
			if(userData!=null && userData instanceof int[])
			{
				int[] info = (int[])userData; 
				if (info[0] < 0)
				{
					float[] color = getColorForOrder(info[0], on ? 2 : 0);
					idx = pi.getPrimitiveColorIndices();
					FloatBuffer colors = (FloatBuffer) (pi.getGeometryArray())
						.getColorRefBuffer().getBuffer();
					colors.position(idx[0] * 3);
					colors.put(color);
					colors.position(idx[1] * 3);
					colors.put(color);
				}
				toggleSelectedEdge(on, new SelectionEdge(gpt1, gpt2, info[0],
					info[1]));
			}
		}
		// event propagation		
		System.out.println(System.currentTimeMillis()+" end of highlight");
	}

	/**
	 * @param pt
	 * @return the grid coordinates for a given point
	 */
	protected int[] getGridCoordinate(float[] pt)
	{
		int[] ret = new int[3];		
		for (int xyz = 0; xyz < 3; ++xyz)
		{
			double min = Double.POSITIVE_INFINITY;
			int idx = -1;
			for (int i = 0; i < grid[xyz].length; ++i)
			{
				double dist = Math.abs(grid[xyz][i] - pt[xyz]); // N1 norm
				if (dist < min)
				{
					min = dist;
					idx = i;
				}
			}
			ret[xyz] = idx;
		}
		return ret;
	}

	/**
	 * Each EdgeLine object reports all edges along a particular infinite line
	 * This line is parallel to one of the axis, thus defined by: - Its
	 * direction: X, Y, or Z - The constant values in the other 2 directions Ex:
	 * X line defined by Y=5, Z=2 The values for the constants are indices in
	 * the grid array, thus integers
	 * 
	 * Alongside each line, a certain number of edges can be defined. All
	 * overlapping edges are merged to form unique lines, thus reducing the
	 * geometry => Optimum solution in the end : no edge is duplicated, minimum
	 * geometrical description => easy to find back the grid indices in picking
	 * operations
	 *  
	 */
	protected static class EdgeLine
	{
		/** The constants : (Y,Z) or (X,Z) or (X,Y) */
		int c1, c2;
		/** The direction : 0,1,2 for X,Y,Z */
		int direction;
		/**
		 * The edges on this line. Each edge end is written in turn, in a sorted
		 * order.
		 */
		LinkedList<Integer> edges;

		/**
		 * @param c1
		 *            The first constant of one couple : (Y,Z) or (X,Z) or (X,Y)
		 * @param c2
		 *            The second constant of one couple : (Y,Z) or (X,Z) or
		 *            (X,Y)
		 * @param direction
		 *            0,1,2 for X,Y,Z
		 */
		public EdgeLine(int c1, int c2, int direction)
		{
			this.c1 = c1;
			this.c2 = c2;
			this.direction = direction;
			edges = new LinkedList<Integer>();
		}

		/**
		 * @param grid
		 * @return an array of vertex coordinates, one vertex per edge end, 3
		 *         coords per vertex
		 */
		public double[] getCoords(float[][] grid)
		{
			double[] ret = new double[edges.size() * 3];
			Iterator<Integer> it = edges.iterator();
			switch (direction)
			{
				case 0 :
					for (int i = 0; i < ret.length; i += 3)
					{
						ret[i + 0] = grid[0][it.next().intValue()];
						ret[i + 1] = grid[1][c1];
						ret[i + 2] = grid[2][c2];
					}
					break;
				case 1 :
					for (int i = 0; i < ret.length; i += 3)
					{
						ret[i + 0] = grid[0][c1];
						ret[i + 1] = grid[1][it.next().intValue()];
						ret[i + 2] = grid[2][c2];
					}
					break;
				case 2 :
					for (int i = 0; i < ret.length; i += 3)
					{
						ret[i + 0] = grid[0][c1];
						ret[i + 1] = grid[1][c2];
						ret[i + 2] = grid[2][it.next().intValue()];
					}
					break;
			}
			return ret;
		}

		/**
		 * @return
		 */
		public int getNumberOfEdges()
		{
			if ((edges.size() & 1) != 0)
			{
				System.out
					.println("Error: Odd number of vertex in edge line : "
						+ edges.size());
			}
			return edges.size() / 2;
		}

		/**
		 * Add an edge along this line. Overlapping edges are merged.
		 * 
		 * @param e1
		 *            The first edge end
		 * @param e2
		 *            The second edge end
		 */
		public void add(int e1, int e2)
		{
			// sort the ends
			int min = (e1 < e2) ? e1 : e2;
			e2 = (e1 > e2) ? e1 : e2;
			e1 = min;
			// state of the current segment end : first end or last end of the
			// current segment
			boolean first = true;
			boolean inMerge = false;
			for (int i = 0; i < edges.size(); ++i)
			{
				int end = edges.get(i).intValue();
				// wait till newsegment match this interval in edges list order
				if (e1 > end)
				{
					first = !first;
					continue;
				}
				// now e1<=end. Handle case where end is the first segment end
				if (first)
				{
					// If not already merging, see if merging is necessary
					if (!inMerge)
					{
						// segments overlap => extend existing segment to match
						// the new one
						if (end <= e2)
						{
							edges.set(i, new Integer(e1));
							inMerge = true; // now merging the new segment
						} else
						{
							// Segments do not overlap => add a new segment,
							// finished
							edges.add(i, new Integer(e2)); // insert e2 before
														   // the current end
							edges.add(i, new Integer(e1)); // insert e1 before
														   // e2
							return;
						}
						// In merge already => it normal that e1<=end. Let's
						// check e2
					} else
					{
						// new segment terminates before current segment
						// => add the new point, finished
						if (e2 < end)
						{
							edges.add(i, new Integer(e2));
							return;
							// new segment terminates after or inside current
							// segment => merge this end
						} else if (e2 >= end)
						{
							// remove this end, as the segments merge to make
							// one bigger segment
							edges.remove(i--); // remove corresponding index too
						}
					}
				}
				// Handle second case : end is the last end of the segment.
				// Note: e1<=end, see above
				else
				{
					// new segment terminates inside current one
					// => end of the merge, keep this segment's end
					if (e2 <= end) return;
					// New segment terminates strictly after current segment
					// remove this end, as the segments merge to make one bigger
					// segment
					edges.remove(i--); // remove corresponding index too
					// If not already merging, this is necessary
					inMerge = true; // now merging the new segment
				}
				first = !first;
			}
			// Still here? => new segment terminates after any existing segment
			// If not merging, must add the first end of the new segment since
			// it was larger than all existing segment ends
			if (!inMerge) edges.add(new Integer(e1));
			// Close the new segment
			edges.add(new Integer(e2));
		}

		/**
		 * Equals is a bit special : test for direction and constant equality,
		 * but the edges need not be the same along this line. Only the infinite
		 * line is guaranteed to be the same.
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 * @see hashCode()
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof EdgeLine)) return false;
			EdgeLine el = (EdgeLine) obj;
			return (el.direction == direction) && (el.c1 == c1)
				&& (el.c2 == c2);
		}

		/**
		 * Hashcode the direction and constants. Thus, an hashMap of edges can
		 * be used to simply add new adges to a given direction without worrying
		 * if another EdgeLine exists (@see equals(java.lang.Object) too)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			return ((direction & 3) << 30) | ((c1 & 0x7F) << 15) | (c2 & 0x7f);
		}
	}

	private Wire[] createWireList()
	{
		int[] ids=provider.getDomainIDs();
		LOGGER.finest("computing wires for "+ids.length+" domain.");
		int numberOfWire=0;

		for(int i=0; i<ids.length; i++)
		{
			FDDomain domain=(FDDomain)provider.getDomain(ids[i]);
			numberOfWire+=domain.getNumberOfXWire();
			numberOfWire+=domain.getNumberOfYWire();
			numberOfWire+=domain.getNumberOfZWire();
		}
		LOGGER.finest("found "+numberOfWire+" wires.");
		
		Wire[] wires=new Wire[numberOfWire];
		int iw=0;
		for(int i=0; i<ids.length; i++)
		{
			FDDomain domain=(FDDomain)provider.getDomain(ids[i]);
			Iterator<int[]> it=domain.getXWireIterator();
			while(it.hasNext())
			{
				int[] indices=it.next();
				Wire w=new WireX();
				w.position1=indices[1];
				w.position2=indices[2];
				w.min=indices[0];
				w.max=indices[3];
				wires[iw]=w;
				iw++;
			}
			
			domain=(FDDomain)provider.getDomain(ids[i]);
			it=domain.getYWireIterator();
			while(it.hasNext())
			{
				int[] indices=it.next();
				Wire w=new WireY();
				w.position1=indices[0];
				w.position2=indices[2];
				w.min=indices[1];
				w.max=indices[3];
				wires[iw]=w;
				iw++;
			}		

			domain=(FDDomain)provider.getDomain(ids[i]);
			it=domain.getZWireIterator();
			while(it.hasNext())
			{
				int[] indices=it.next();
				Wire w=new WireZ();
				w.position1=indices[0];
				w.position2=indices[1];
				w.min=indices[2];
				w.max=indices[3];
				wires[iw]=w;
				iw++;
			}		
			
		}
		return wires;
	}
	
	protected void makeWires()
	{		
		Wire[] wires=createWireList();
		
		if(wires.length==0)
			return;
		
		FloatBuffer nioWires = ByteBuffer.allocateDirect(
			wires.length * 2 * 3 * 4).order(ByteOrder.nativeOrder())
			.asFloatBuffer();
		
		for(int i=0; i<wires.length; i++)
		{			
			nioWires.put(wires[i].getCoordinates(grid));			
		}

		// Create edge shapes directly, don't make it appear in graph
		LineArray la = new NioLineArray(wires.length * 2,
			GeometryArray.COORDINATES | GeometryArray.COLOR_3);
		la.setCoordRefBuffer(new J3DBuffer(nioWires));
		int colSize = wires.length * 2 * 3;
		FloatBuffer nioWireColors = ByteBuffer.allocateDirect(colSize * 4)
			.order(ByteOrder.nativeOrder()).asFloatBuffer();
		float[] colors = getColorForOrder(-2, 0);
		for (int i = 0; i < colSize; i += 3)
			nioWireColors.put(colors);
		la.setColorRefBuffer(new J3DBuffer(nioWireColors));
		la.setUserData(new int[]{-2, 0});
		Appearance a = new Appearance();
		//PolygonAttributes pa = new
		// PolygonAttributes(PolygonAttributes.POLYGON_LINE,
		// PolygonAttributes.CULL_NONE, 0);
		//pa.setPolygonOffset(2);
		//pa.setPolygonOffsetFactor(2);
		//a.setPolygonAttributes(pa);
		/*
		 * LineAttributes lat = new LineAttributes(); lat.setLineWidth(2.0f);
		 * a.setLineAttributes(lat);
		 */
		Shape3D s3d = new Shape3D(la, a);
		PickTool.setCapabilities(s3d, PickTool.INTERSECT_FULL);
		s3d.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
		s3d.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		s3d.setCapability(Node.ALLOW_PICKABLE_READ);
		s3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
		s3d.setPickable(true); // by default, can be changed with actions
		s3d.setUserData(this); // this object will handle edges
		addChild(s3d);
		allEdgeShapes.add(s3d);
	}
}
