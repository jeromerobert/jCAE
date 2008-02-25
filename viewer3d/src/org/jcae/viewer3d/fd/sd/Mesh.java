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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d.fd.sd;
import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;
import org.jcae.viewer3d.Palette;
/**
 *
 * @author Jerome Robert
 */
public class Mesh
{
	private static Logger logger=Logger.getLogger(Mesh.class.getName());
	private float[][] grid;
	private HashMap<Integer, ArrayList<Plate>> plates;
	private HashMap<Integer, Wire> wires;
	public int texturingThreshold=150;

	//used to keep the order of the plates in the plate file. It's used when
	//reading the jf02 file
	private ArrayList<Plate> orderedPlates;

	// for preprocessing rendering
	private HashMap<Integer, Color> colors;
	// for post processsing rendering
	private HashMap<Integer, ArrayList<Plate>> texturedPlates;
	private HashMap<Integer, ColoredPlateSet> coloredPlates;
	public float minValue, maxValue;

	public void logarithm()
	{
		float delta=Math.abs(minValue);
		float localMax=-Float.MAX_VALUE,localMin=-Float.MIN_VALUE;
		if(delta<=2*Float.MIN_VALUE) delta=Math.abs(maxValue)/1000;
		float cst=(float)Math.log(maxValue-minValue+delta);
		for(int i=0;i<orderedPlates.size();i++)
		{
			Plate p=orderedPlates.get(i);
			for(int j=0;j<p.values.length;j++)
			{
				if(p.values[j]>maxValue) p.values[j]=maxValue;
				if(p.values[j]<minValue) p.values[j]=minValue;
				p.values[j]=(float)Math.log(p.values[j]-minValue+delta);
				if(p.values[j]>localMax) localMax=p.values[j];
				if(p.values[j]<localMin) localMin=p.values[j];				
			}
		}
		maxValue=localMax;
		minValue=localMin;
	}
	/** Creates a new instance of Mesh */
	public Mesh()
	{
	}

	public int[] getGroupsIDs()
	{
		int[] ids=new int[plates.size()];
		Iterator<Integer> it=plates.keySet().iterator();
		for(int i=0;it.hasNext();i++)
		{
			ids[i]=it.next().intValue();
		}
		return ids;
	}
	
	public int getNumberOfTexturedPlates(int groupId)
	{
		ArrayList a=(texturedPlates.get(new Integer(groupId)));
		return a.size();
	}

	public int[] getTexture(int groupId, int plateID)
	{
		ArrayList a=(texturedPlates.get(new Integer(groupId)));
		Plate plate=(Plate)(a.get(plateID));
		float[] values=plate.values;
		int[] texture=new int[values.length+2];
		texture[0]=plate.getWidth();
		texture[1]=plate.getHeight();
		for(int i=0;i<values.length;i++)
		{
			texture[i+2]=Color.HSBtoRGB((maxValue-values[i])/(maxValue-minValue)*170f/255f,1.0f,1.0f);
			//logger.fine("texture["+(i+2)+"]="+Integer.toHexString(texture[i+2]));
		}
		return texture;
	}
	
	/**Used to improve speed execution of getCoordinates and getCoordinateIndices*/
	private PreprocessPlateSet currentPreprocessPlateSet=null;
	private int currentPreprocessPlateSetId=-1;
	
	/*
	 * If getCoordinates and getCoordinateIndices are called successively there is
	 * no computation for the second call
	 */
	public float[] getCoordinates(int groupId)
	{
		if((currentPreprocessPlateSet==null)||(currentPreprocessPlateSetId!=groupId))
		{
			ArrayList a=(plates.get(new Integer(groupId)));
			currentPreprocessPlateSet=new PreprocessPlateSet(a, grid);
			currentPreprocessPlateSetId=groupId;
		}		
		return currentPreprocessPlateSet.getCoordinates();		
		
	}

	/*
	 * If getCoordinates and getCoordinateIndices are called successively there is
	 * no computation for the second call
	 */
	public int[] getCoordinateIndices(int groupId)
	{
		if((currentPreprocessPlateSet==null)||(currentPreprocessPlateSetId!=groupId))
		{
			ArrayList a=(plates.get(new Integer(groupId)));
			currentPreprocessPlateSet=new PreprocessPlateSet(a, grid);
			currentPreprocessPlateSetId=groupId;
		}		
		return currentPreprocessPlateSet.getCoordinateIndices();
	}
	
	public float[] getTexturedPlateCoordinates(int groupId, int plateID)
	{
		ArrayList a=(texturedPlates.get(new Integer(groupId)));
		return ((Plate)a.get(plateID)).getCoordinates(grid);
	}

	public float[] getColoredPlateCoordinates(int groupId)
	{
		ColoredPlateSet a=(coloredPlates.get(new Integer(groupId)));
		return a.getColoredPlateCoordinates();
	}

	public int[] getColoredPlateCoordinatesIndices(int groupId)
	{
		ColoredPlateSet a=(coloredPlates.get(new Integer(groupId)));
		return a.getColoredPlateCoordinatesIndices();
	}

	public byte[] getColoredPlateColors(int groupId)
	{
		byte[] colors=new byte[1024*3];
		for(int i=0;i<2*512*3;i+=3)
		{
			Color c=Color.getHSBColor(i/(3f*3f*512f), 1f,1f);
			colors[i]=(byte)c.getRed();
			colors[i+1]=(byte)c.getGreen();
			colors[i+2]=(byte)c.getBlue();
		}
		return colors;
	}

	public int[] getColoredPlateColorsIndices(int groupId)
	{
		ColoredPlateSet a=(coloredPlates.get(new Integer(groupId)));
		return a.getColoredPlateColorsIndices();
	}

	public void prepareForDisplay()
	{		
		int numberOfCells,maxPlateSize=0; //for debugging purpose
		texturedPlates=new HashMap<Integer, ArrayList<Plate>>();
		coloredPlates=new HashMap<Integer, ColoredPlateSet>();
		for(Iterator<Integer> it=plates.keySet().iterator();it.hasNext();)
		{
			int attribut=it.next().intValue();
			ArrayList a=plates.get(new Integer(attribut));
			ArrayList<Plate> texturedPlatesA=new ArrayList<Plate>();
			ArrayList<Plate> coloredPlatesA=new ArrayList<Plate>();
			texturedPlates.put(new Integer(attribut),texturedPlatesA);			
			for(int i=0;i<a.size();i++)
			{
				Plate plate=(Plate)a.get(i);
				numberOfCells=plate.numberOfCells();
				if(numberOfCells>maxPlateSize) maxPlateSize=numberOfCells;
				if(numberOfCells>texturingThreshold)
				{
					//texturing
					texturedPlatesA.add(plate);
				}
				else
				{
					//coloring
					coloredPlatesA.add(plate);
				}
			}
			ColoredPlateSet cps=new ColoredPlateSet(coloredPlatesA, grid);			
			cps.setMaxValue(maxValue);
			cps.setMinValue(minValue);
			coloredPlates.put(new Integer(attribut),cps);
			logger.info("Biggest plate size : "+maxPlateSize);
			logger.info("Number of textured plates : "+texturedPlatesA.size());
			logger.info("Number of colored plates : "+coloredPlatesA.size());
		}		
	}

	public void loadGr02File(File f) throws IOException, FileNotFoundException
	{
		logger.fine("loadGr02File");
		StreamTokenizerExt in=new StreamTokenizerExt(f);
		grid=new float[3][];
		grid[0]=new float[in.readInteger()];
		grid[1]=new float[in.readInteger()];
		grid[2]=new float[in.readInteger()];
		for(int j=0;j<3;j++)
			for(int i=0;i<grid[j].length;i++) grid[j][i]=in.readFloat();
	}

	public void loadJqf02File(File f, int iteration, int valueType) throws IOException
	{
		logger.fine("loadJqf02File");
		Jqf02File infile=new Jqf02File();
		infile.init(f);
		float[] iterations=infile.getIterations();
		infile.readValue(iteration, orderedPlates, valueType);
		minValue=infile.getMinValue();
		maxValue=infile.getMaxValue();
	}

	
	public void loadWi02File(File f) throws Exception
	{
		wires = new HashMap<Integer, Wire>();
		int keyWord;
		StreamTokenizerExt in=new StreamTokenizerExt(f);
		in.readWord("WIRES");

		while(in.readWords(new String[]{"WIRE","END"})==0)
		{
			logger.fine("loadWi02File "+in.lineno());
			int orientation=in.readWords(new String[]{"X","Y","Z"});
			Class clazz=null;

			switch(orientation)
			{
				case 0: clazz=WireX.class; break;
				case 1: clazz=WireY.class; break;
				case 2: clazz=WireZ.class; break;
			}
			Constructor constructor=clazz.getConstructor(new Class[0]);

			while(!in.readWord().equals("END"))
			{
				if(in.lineno()%1000==0) logger.fine("loadWi02File "+in.lineno());
				Wire w=(Wire)constructor.newInstance(new Class[0]);
				int code = in.readInteger();
				in.readWord();
				w.min=in.readInteger();
				in.readWord();
				w.max=in.readInteger();
				in.readWord();
				w.position1=in.readInteger();
				in.readWord();
				w.position2=in.readInteger();
				in.readWord();					
				int att = in.readInteger();
				wires.put(new Integer(att*1000+code),w);
			}// while
		}// while
		logger.info("Number of wires : "+wires.size());

		// TODO : load junction points
		
	}// loadPl02File
	
	
	
	public void loadPl22File(File f) throws Exception 
	{
		plates=new HashMap<Integer, ArrayList<Plate>>();
		orderedPlates=new ArrayList<Plate>();
		int keyWord;
		StreamTokenizerExt in=new StreamTokenizerExt(f);
		in.readWord("PLATES");

		while(in.readWords(new String[]{"PLATE","END"})==0)
		{
			logger.fine("loadPl22File "+in.lineno());
			int orientation=in.readWords(new String[]{"X","Y","Z"});
			Class clazz=null;

			switch(orientation)
			{
				case 0: clazz=PlateX.class; break;
				case 1: clazz=PlateY.class; break;
				case 2: clazz=PlateZ.class; break;
			}
			Constructor constructor=clazz.getConstructor(new Class[0]);

			while(!in.readWord().equals("END"))
			{
				if(in.lineno()%1000==0) logger.fine("loadPl22File "+in.lineno());
				Plate p=(Plate)constructor.newInstance(new Class[0]);
				p.min1=in.readInteger();
				in.readWord();
				p.max1=in.readInteger();
				in.readWord();
				p.min2=in.readInteger();
				in.readWord();
				p.max2=in.readInteger();
				in.readWord();
				p.position=in.readInteger();
				in.readWord();					
				Integer att=new Integer(in.readInteger());
				ArrayList<Plate> a=plates.get(att);
				if(a==null)
				{
					a=new ArrayList<Plate>();
					plates.put(att, a);
				}
				a.add(p);
				orderedPlates.add(p);
			}// while
		}// while
		logger.info("Number of plates : "+orderedPlates.size());
		assignGroupColors();
	}// loadPl02File
	
	public void loadPl02File(File f) throws Exception
	{
		plates=new HashMap<Integer, ArrayList<Plate>>();
		orderedPlates=new ArrayList<Plate>();
		StreamTokenizerExt in=new StreamTokenizerExt(f);
		
		int[] numberOfPlate=new int[3];

		numberOfPlate[0]=in.readInteger();
		numberOfPlate[1]=in.readInteger();
		numberOfPlate[2]=in.readInteger();
		Class clazz=null;

		for(int orientation=0;orientation<3;orientation++)
		{	
			switch(orientation)
			{
				case 0: clazz=PlateX.class; break;
				case 1: clazz=PlateY.class; break;
				case 2: clazz=PlateZ.class; break;
			}
			Constructor constructor=clazz.getConstructor(new Class[0]);
			for(int i=0;i<numberOfPlate[orientation];i++)
			{
				if(in.lineno()%1000==0) logger.fine("loadPl02File "+in.lineno());
				Plate p=(Plate)constructor.newInstance(new Class[0]);
				p.min1=in.readInteger();
				p.max1=in.readInteger();
				p.min2=in.readInteger();
				p.max2=in.readInteger();
				p.position=in.readInteger();
				Integer att=new Integer(in.readInteger());
				ArrayList<Plate> a=plates.get(att);
				if(a==null)
				{
					a=new ArrayList<Plate>();
					plates.put(att, a);
				}
				a.add(p);
				orderedPlates.add(p);
			}// for
		}// for
		assignGroupColors();
	}// loadPl02File
	
	private void assignGroupColors()
	{
		colors=new HashMap<Integer, Color>();
		Palette p=new Palette();
		p.addColor(Color.RED);
		p.addColor(Color.GREEN);
		p.addColor(Color.BLUE);		
		p.computeNewColors(plates.size());
		Iterator<Integer> i=plates.keySet().iterator();
		int j=0;
		while(i.hasNext())
		{
			colors.put(i.next(), p.getColor(j));
			j++;
		}
	}
	
	public static void main(String [] args)
	{
		try
		{			
			Mesh mesh=new Mesh();
			mesh.loadGr02File(new File(args[0]));
			mesh.loadPl02File(new File(args[1]));
			mesh.loadJqf02File(new File(args[2]),0,0);
		} catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public ArrayList<Plate> getPlates(int groupId) {
		return plates.get(new Integer(groupId));
	}

	public Plate getPlate(int groupId, int plateId) {
		return plates.get(new Integer(groupId)).get(plateId);
	}
	
	public float[][] getGrid() {
		return grid;
	}
	
	public HashMap<Integer, Wire> getWires() {
		return wires;
	}
}
