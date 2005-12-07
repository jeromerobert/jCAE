package org.jcae.viewer3d.fe.unv;

import gnu.trove.TFloatArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class UNVParser
{
	private static final int TETRA4_MASK = 0x20000000;
	private static final int HEXA8_MASK = 0x40000000;
	private static final int ELEMENT_MASK = 0x70000000;
	
	private float[] nodesCoordinates;
	private TIntIntHashMap nodesIndicesMap;
	private int[] tetra4Indices;
	private TIntIntHashMap volumeIndicesMap;
	private int[] hexa8Indices;	
	private ArrayList tria3GroupNames=new ArrayList();
	private ArrayList tria3Groups=new ArrayList();
	private ArrayList quad4GroupNames=new ArrayList();
	private ArrayList quad4Groups=new ArrayList();
	private TIntArrayList tria3Indices=new TIntArrayList();
	private TIntArrayList quad4Indices=new TIntArrayList();
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

	public String[] getQuad4GroupNames()
	{
		return (String[]) quad4GroupNames.toArray(new String[0]);
	}
	
	public int[][] getQuad4Groups()
	{
		return (int[][]) quad4Groups.toArray(new int[0][]);
	}
	
	public int[] getQuad4Indices()
	{
		return quad4Indices.toNativeArray();
	}
	
	public void parse(BufferedReader rd) throws IOException
	{
		double unit = 1.0;
		String line;
		
		nodesIndicesMap=new TIntIntHashMap();
		tria3IndicesMap=new TIntIntHashMap();
		volumeIndicesMap=new TIntIntHashMap();
		
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
		volumeIndicesMap=null;
		tetra4Indices=null;
		hexa8Indices=null;
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
				volumeIndicesMap.put(ind, TETRA4_MASK | (tetra4.size()/4) );
				for(int i=0; i<4; i++)
					tetra4.add(nodesIndicesMap.get(Integer.parseInt(st.nextToken())));   
			}
			else if (type.equals("115"))
			{
				st = new StringTokenizer(line);	
				volumeIndicesMap.put(ind, HEXA8_MASK | (hexa8.size()/8) );				
				for(int i=0; i<8; i++)
					hexa8.add(nodesIndicesMap.get(Integer.parseInt(st.nextToken())));   
			}			
		}
		
		tetra4Indices=tetra4.toNativeArray();
		hexa8Indices=hexa8.toNativeArray();
		
		/*PrintStream ps=new PrintStream(new FileOutputStream("/tmp/proutzob"));
		System.out.println("hexa8: "+(hexa8Indices.length/8.0));
		for(int i=0; i<hexa8Indices.length; i++)
		{
			ps.print(hexa8Indices[i]+" ");
			if((i+1)%8 == 0)
				ps.println();
		}
		ps.close();*/
	}
	
	private boolean isTetra4(int id)
	{
		return (id & ELEMENT_MASK) == TETRA4_MASK;
	}
	
	private boolean isHexa8(int id)
	{
		return (id & ELEMENT_MASK) == HEXA8_MASK;
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
	
	private void readTetra4LoadSet(int element, int faceId, TIntArrayList group)
	{
		element*=4;
		int p1=tetra4Indices[element++];
		int p2=tetra4Indices[element++];
		int p3=tetra4Indices[element++];
		int p4=tetra4Indices[element++];
		
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
	}

	private void readHexa8LoadSet(int element, int faceId, TIntArrayList group)
	{
		element*=8;
		int p1=hexa8Indices[element++];
		int p2=hexa8Indices[element++];
		int p3=hexa8Indices[element++];
		int p4=hexa8Indices[element++];
		int p5=hexa8Indices[element++];
		int p6=hexa8Indices[element++];
		int p7=hexa8Indices[element++];
		int p8=hexa8Indices[element++];
		
		group.add(quad4Indices.size()/4);
		switch(faceId)
		{
			case 1:
				quad4Indices.add(p1);
				quad4Indices.add(p2);
				quad4Indices.add(p3);
				quad4Indices.add(p4);
				break;
			case 2:
				quad4Indices.add(p5);
				quad4Indices.add(p6);
				quad4Indices.add(p7);
				quad4Indices.add(p8);
				break;
			case 3:
				quad4Indices.add(p1);
				quad4Indices.add(p2);
				quad4Indices.add(p6);
				quad4Indices.add(p5);
				break;
			case 4:
				quad4Indices.add(p2);
				quad4Indices.add(p3);
				quad4Indices.add(p7);
				quad4Indices.add(p6);
				break;
			case 5:
				quad4Indices.add(p3);
				quad4Indices.add(p4);
				quad4Indices.add(p8);
				quad4Indices.add(p7);
				break;
			case 6:
				quad4Indices.add(p1);
				quad4Indices.add(p4);
				quad4Indices.add(p8);
				quad4Indices.add(p5);
				break;
			default:
				throw new IllegalStateException("Face ID should be 1,2,3,4,5 or 6");
		}			
	}
	
	private void readLoadSets(BufferedReader rd) throws IOException
	{
		rd.readLine(); //RECORD 1 (skip)
		String name=rd.readLine(); //RECORD 2 (skip)		
		 		
		String line;
		
		TIntArrayList groupTetra4=new TIntArrayList();
		TIntArrayList groupHexa8=new TIntArrayList();
		while (!(line = rd.readLine().trim()).equals("-1")) //RECORD 3 (type 2)
		{
			// first line: type of object
			StringTokenizer st = new StringTokenizer(line);
			st.nextToken(); //skip face pressure load label
			int element=volumeIndicesMap.get(Integer.parseInt(st.nextToken()));

			int faceId=Integer.parseInt(st.nextToken());
			if(isTetra4(element))
			{
				readTetra4LoadSet(element & (~ELEMENT_MASK), faceId, groupTetra4);
			}
			else if(isHexa8(element))
			{
				//System.out.println(element+" : "+(element & (~ELEMENT_MASK)) );
				readHexa8LoadSet(element & (~ELEMENT_MASK), faceId, groupHexa8);
			}
				
			rd.readLine(); //RECORD 4
			rd.readLine(); //RECORD 5
		}
		
		if(!groupTetra4.isEmpty())
		{
			tria3GroupNames.add(name);
			tria3Groups.add(groupTetra4.toNativeArray());
		}

		if(!groupHexa8.isEmpty())
		{
			quad4GroupNames.add(name);
			quad4Groups.add(groupHexa8.toNativeArray());
		}
		
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
	
	/**
	 * Debug
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			UNVParser unvp=new UNVParser();
			unvp.parse(new BufferedReader(new FileReader("/home/jerome/cassiope/resources/example/tecplot50x50x50.unv")));
			System.out.println(unvp.getQuad4Indices().length);			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
