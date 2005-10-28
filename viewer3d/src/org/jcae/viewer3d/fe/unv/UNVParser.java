package org.jcae.viewer3d.fe.unv;

import gnu.trove.TFloatArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class UNVParser
{
	private float[] nodesCoordinates;
	private TIntIntHashMap nodesIndicesMap;
	private int[] tetra4Indices;
	private TIntIntHashMap tetra4IndicesMap;
	private int[] hexa8Indices;
	private TIntIntHashMap hexa8IndicesMap;	
	private ArrayList tria3GroupNames=new ArrayList();
	private ArrayList tria3Groups=new ArrayList();
	private TIntArrayList tria3Indices=new TIntArrayList();
	private TIntIntHashMap tria3IndicesMap;

	public float[] getNodesCoordinates()
	{
		return nodesCoordinates;
	}

	public String[] getTria3GroupNames()
	{
		return (String[]) tria3GroupNames.toArray(new String[0]);
	}

	public int[][] getTria3Groups()
	{
		return (int[][]) tria3Groups.toArray(new int[0][]);
	}
	
	public int[] getTria3Indices()
	{
		return tria3Indices.toNativeArray();
	}
	
	public void parse(BufferedReader rd) throws IOException
	{
		double unit = 1.0;
		String line;
		
		nodesIndicesMap=new TIntIntHashMap();
		tria3IndicesMap=new TIntIntHashMap();
		tetra4IndicesMap=new TIntIntHashMap();
		while ((line = rd.readLine()) != null)
		{
			line = rd.readLine();
			int blockID=Integer.parseInt(line.trim());
			
			switch(blockID)
			{
				case 2411:
				case 781:
					readNodes(rd, unit);
					break;
				case 2412:
					readFace(rd);
					break;
				case 164:
					unit=readUnit(rd);
					break;
				case 2435:
				case 2430:
					readGroup(rd);
					break;
				case 790:
					readLoadSets(rd);
					break;					
				default:
					while (!(line = rd.readLine()).equals("    -1"));
			}
		}
		
		//If there are triangles but no groups
		if(tria3GroupNames.size()==0 && tria3Indices.size()>0)
		{
			tria3GroupNames.add("");
			int[] group=new int[tria3Indices.size()/3];
			for(int i=0; i<group.length; i++)
			{
				group[i]=i;
			}
			tria3Groups.add(group);
		}
		
		//free indices maps.
		nodesIndicesMap=null;
		tria3IndicesMap=null;
		tetra4IndicesMap=null;
		tetra4Indices=null;
		hexa8Indices=null;
		hexa8IndicesMap=null;
	}
	
	private void readFace(BufferedReader rd) throws IOException
	{			
		TIntArrayList tetra4=new TIntArrayList();
		TIntArrayList hexa8=new TIntArrayList();
		
		String line;

		while (!(line = rd.readLine().trim()).equals("-1")) {
			// first line: type of object
			StringTokenizer st = new StringTokenizer(line);
			String index = st.nextToken();
			String type = st.nextToken();
			int ind = Integer.parseInt(index);		

			line = rd.readLine(); //RECORD 2
			
			if (type.equals("74") || type.equals("91") || type.equals("41"))
			{				
				// triangle
				st = new StringTokenizer(line);	
				tria3IndicesMap.put(ind, tria3Indices.size()/3);
				for(int i=0; i<3; i++)
					tria3Indices.add(nodesIndicesMap.get(Integer.parseInt(st.nextToken()))); 				
			}			
			else if (type.equals("111"))
			{
				st = new StringTokenizer(line);	
				tetra4IndicesMap.put(ind, tetra4.size()/4);
				for(int i=0; i<4; i++)
					tetra4.add(nodesIndicesMap.get(Integer.parseInt(st.nextToken())));   
			}
			else if (type.equals("115"))
			{
				st = new StringTokenizer(line);	
				hexa8IndicesMap.put(ind, hexa8.size()/8);
				for(int i=0; i<4; i++)
					hexa8.add(nodesIndicesMap.get(Integer.parseInt(st.nextToken())));   
			}			
		}
		
		tetra4Indices=tetra4.toNativeArray();
		hexa8Indices=hexa8.toNativeArray();
	}

	private void readGroup(BufferedReader rd) throws IOException
	{		
		String line = rd.readLine();
		while (!line.trim().equals("-1"))
		{
			// read the number of elements to read in the last number of the line
			StringTokenizer st = new StringTokenizer(line);
			String snb = st.nextToken();
			
			while (st.hasMoreTokens())
			{
				snb = st.nextToken();
			}
			int nbelem = Integer.parseInt(snb);			
			// Read group name
			tria3GroupNames.add(rd.readLine().trim());
			
			TIntArrayList facelist = new TIntArrayList();
			while ((line = rd.readLine().trim()).startsWith("8"))
			{
				st = new StringTokenizer(line);
				
				// read one element over two, the first one doesnt matter
				while (st.hasMoreTokens()) {
					st.nextToken();
					String index = st.nextToken();
					int id=tria3IndicesMap.get(Integer.parseInt(index));
					facelist.add(id);
				}
			}
			tria3Groups.add(facelist.toNativeArray());			
		}
	}

	private void readLoadSets(BufferedReader rd) throws IOException
	{
		rd.readLine(); //RECORD 1 (skip)
		String name=rd.readLine(); //RECORD 2 (skip)		
		tria3GroupNames.add(name); 
		
		String line;
		
		TIntArrayList group=new TIntArrayList();
		while (!(line = rd.readLine().trim()).equals("-1")) //RECORD 3 (type 2)
		{
			// first line: type of object
			StringTokenizer st = new StringTokenizer(line);
			st.nextToken(); //skip face pressure load label
			int element=tetra4IndicesMap.get(Integer.parseInt(st.nextToken()));
			element*=4;
			int p1=tetra4Indices[element++];
			int p2=tetra4Indices[element++];
			int p3=tetra4Indices[element++];
			int p4=tetra4Indices[element++];
			int faceId=Integer.parseInt(st.nextToken());
			group.add(tria3Indices.size()/3);
			switch(faceId)
			{
				case 1:
					tria3Indices.add(p1);
					tria3Indices.add(p2);
					tria3Indices.add(p3);
					break;
				case 2:
					tria3Indices.add(p1);
					tria3Indices.add(p2);
					tria3Indices.add(p4);
					break;
				case 3:
					tria3Indices.add(p2);
					tria3Indices.add(p3);
					tria3Indices.add(p4);
					break;
				case 4:
					tria3Indices.add(p1);
					tria3Indices.add(p3);
					tria3Indices.add(p4);
					break;
				default:
					throw new IllegalStateException("Face ID should be 1,2,3 or 4");
			}			
			rd.readLine(); //RECORD 4
			rd.readLine(); //RECORD 5
		}
		tria3Groups.add(group.toNativeArray());
		return;
	}

	private void readNodes(BufferedReader rd, double unit) throws IOException
	{
		TIntIntHashMap indices=new TIntIntHashMap();
		TFloatArrayList coords=new TFloatArrayList();
		float x, y, z;
		String line;
		int k=0;
		while (!(line = rd.readLine().trim()).equals("-1"))
		{
			// First number : the node's id
			StringTokenizer st = new StringTokenizer(line);
			int index = new Integer(st.nextToken()).intValue();
			line = rd.readLine();
			// line contains coord x,y,z
			st = new StringTokenizer(line);
			String x1 = st.nextToken();
			String y1 = st.nextToken();
			String z1;

			if(st.hasMoreTokens())
				z1 = st.nextToken();
			else
				z1 = "0.0";
			x1 = x1.replace('D', 'E');
			y1 = y1.replace('D', 'E');
			z1 = z1.replace('D', 'E');
			x = (float) (Double.parseDouble(x1) / unit);
			y = (float) (Double.parseDouble(y1) / unit);
			z = (float) (Double.parseDouble(z1) / unit);
			indices.put(index, k++);
			coords.add(x);
			coords.add(y);
			coords.add(z);
		}
		this.nodesIndicesMap=indices;
		this.nodesCoordinates=coords.toNativeArray();
	}

	private double readUnit(BufferedReader rd) {
		double unit = 1.0;
		String line;
		try {
			//retrieve the second line
			line = rd.readLine();
			line = rd.readLine();

			// fisrt number : the unit
			StringTokenizer st = new StringTokenizer(line);
			String unite = st.nextToken();
			unite = unite.replace('D', 'E');
			unit = Double.parseDouble(unite);
			while (!(line = rd.readLine().trim()).equals("-1")) {
				// ???
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return unit;
	}	
}
