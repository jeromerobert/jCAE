package org.jcae.viewer3d.fe.unv;

import gnu.trove.TFloatArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class UNVParser
{
	private static final int TETRA4_MASK  = 0x10000000;
	private static final int HEXA8_MASK   = 0x20000000;

	private static final int TRIA3_MASK   = 0x10000000;
	private static final int TRIA6_MASK   = 0x20000000;
	private static final int QUAD4_MASK   = 0x40000000;

	private float[] nodesCoordinates;
	private TIntIntHashMap nodesIndicesMap;
	private boolean hasBeam2, hasTria3, hasTria6, hasQuad4, hasTetra4, hasHexa8;
	private ArrayList<String> surfaceGroupNames=new ArrayList<String>();
	private ArrayList<int[]> surfaceGroups=new ArrayList<int[]>();
	private TIntArrayList beamIndices=new TIntArrayList();
	private TIntArrayList surfaceIndices=new TIntArrayList();
	private TIntArrayList volumeIndices=new TIntArrayList();
	private TIntIntHashMap elementBeamIndicesMap, elementSurfaceIndicesMap, elementVolumeIndicesMap;

	public float[] getNodesCoordinates()
	{
		return nodesCoordinates;
	}

	public String[] getTria3GroupNames()
	{
		return surfaceGroupNames.toArray(new String[0]);
	}

	public int[] getTria3FromGroup(int groupId)
	{
		int[] elids=surfaceGroups.get(groupId);
		int cnt = 0;
		for(int val: elids)
		{
			if ((val & TRIA3_MASK) != 0)
				cnt++;
		}
		int[] toReturn=new int[cnt*3];
		cnt = 0;
		for(int val: elids)
		{
			if ((val & TRIA3_MASK) == 0)
				continue;
			int iid=(val & ~TRIA3_MASK);
			toReturn[cnt++]=surfaceIndices.get(iid++);
			toReturn[cnt++]=surfaceIndices.get(iid++);
			toReturn[cnt++]=surfaceIndices.get(iid++);
		}
		return toReturn;
	}
	
	public int[] getQuad4FromGroup(int groupId)
	{
		int[] elids=surfaceGroups.get(groupId);
		int cnt = 0;
		for(int val: elids)
		{
			if ((val & QUAD4_MASK) != 0)
				cnt++;
		}
		int[] toReturn=new int[cnt*4];
		cnt = 0;
		for(int val: elids)
		{
			if ((val & QUAD4_MASK) == 0)
				continue;
			int iid=(val & ~QUAD4_MASK);
			toReturn[cnt++]=surfaceIndices.get(iid++);
			toReturn[cnt++]=surfaceIndices.get(iid++);
			toReturn[cnt++]=surfaceIndices.get(iid++);
			toReturn[cnt++]=surfaceIndices.get(iid++);
		}
		return toReturn;
	}
	
	public int[] getBeam2FromGroup(int groupId)
	{
		if (groupId == UNVProvider.OTHERS_GROUP)
			return beamIndices.toNativeArray();
		return new int[0];
	}

	public int[] getTria6FromGroup(int groupId)
	{
		int[] elids=surfaceGroups.get(groupId);
		int cnt = 0;
		for(int val: elids)
		{
			if ((val & TRIA6_MASK) != 0)
				cnt++;
		}
		int[] toReturn=new int[cnt*6];
		cnt = 0;
		for(int val: elids)
		{
			if ((val & TRIA6_MASK) == 0)
				continue;
			int iid=(val & ~TRIA6_MASK);
			toReturn[cnt++]=surfaceIndices.get(iid++);
			toReturn[cnt++]=surfaceIndices.get(iid++);
			toReturn[cnt++]=surfaceIndices.get(iid++);
			toReturn[cnt++]=surfaceIndices.get(iid++);
			toReturn[cnt++]=surfaceIndices.get(iid++);
			toReturn[cnt++]=surfaceIndices.get(iid++);
		}
		return toReturn;
	}

	public boolean hasBeam2()
	{
		return hasBeam2;
	}

	public boolean hasTria6()
	{
		return hasTria6;
	}

	public void parse(BufferedReader rd) throws IOException
	{
		double unit = 1.0;
		String line;
		
		elementBeamIndicesMap=new TIntIntHashMap();
		elementSurfaceIndicesMap=new TIntIntHashMap();
		elementVolumeIndicesMap=new TIntIntHashMap();
		nodesIndicesMap=new TIntIntHashMap();
		
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
					readGroup(rd, blockID);
					break;
				case 790:
					readLoadSets(rd);
					break;					
				default:
					while (!(line = rd.readLine()).equals("    -1"));
			}
		}
		
		//If there are triangles but no groups
		if(surfaceGroupNames.size()==0 && surfaceIndices.size()>0)
		{
			surfaceGroupNames.add("");
			int[] group=new int[surfaceIndices.size()];
			int i = 0;
			for(int val: elementSurfaceIndicesMap.getValues())
			{
				group[i]=val;
				i++;
			}
			surfaceGroups.add(group);
		}
		
		//free indices maps.
		nodesIndicesMap=null;
		elementBeamIndicesMap=null;
		elementSurfaceIndicesMap=null;
		elementVolumeIndicesMap=null;
	}
	
	private void readFace(BufferedReader rd) throws IOException
	{			
		String line;

		while (!(line = rd.readLine().trim()).equals("-1"))
		{
			// first line: type of object
			StringTokenizer st = new StringTokenizer(line);
			int ind = Integer.parseInt(st.nextToken());
			int type = Integer.parseInt(st.nextToken());

			line = rd.readLine(); //RECORD 2			
			
			switch(type)
			{
				case 21: // Linear beam
					line=rd.readLine(); //skip          0         1         1
					st = new StringTokenizer(line);	
					elementBeamIndicesMap.put(ind, beamIndices.size());
					for(int i=0; i<2; i++)
						beamIndices.add(nodesIndicesMap.get(Integer.parseInt(st.nextToken())));
					hasBeam2 = true;
					break;
				case 74:  // Membrane Linear Triangle
				case 91:  // Thin Shell Linear Triangle
				case 41:  // Plane Stress Linear Triangle
					st = new StringTokenizer(line);	
					elementSurfaceIndicesMap.put(ind, TRIA3_MASK | surfaceIndices.size());
					for(int i=0; i<3; i++)
						surfaceIndices.add(nodesIndicesMap.get(Integer.parseInt(st.nextToken()))); 									
					hasTria3 = true;
					break;
				case 92: // Thin Shell Parabolic Triangle
					st = new StringTokenizer(line);	
					elementSurfaceIndicesMap.put(ind, TRIA6_MASK | surfaceIndices.size());
					for(int i=0; i<3; i++)
					{
						surfaceIndices.add(nodesIndicesMap.get(Integer.parseInt(st.nextToken())));
						st.nextToken(); //keep only vertex nodes
					}
					hasTria6 = true;
					break;
				case 94: // Thin Shell Linear Quadrilateral
					st = new StringTokenizer(line);	
					elementSurfaceIndicesMap.put(ind, QUAD4_MASK | surfaceIndices.size());
					for(int i=0; i<4; i++)
						surfaceIndices.add(nodesIndicesMap.get(Integer.parseInt(st.nextToken())));
					hasQuad4 = true;
					break;
				case 111: // Solid Linear Tetrahedron
					st = new StringTokenizer(line);	
					elementVolumeIndicesMap.put(ind, TETRA4_MASK | volumeIndices.size());
					for(int i=0; i<4; i++)
						volumeIndices.add(nodesIndicesMap.get(Integer.parseInt(st.nextToken())));   
					hasTetra4 = true;
					break;
				case 115: // Solid Linear Brick
					st = new StringTokenizer(line);	
					elementVolumeIndicesMap.put(ind, HEXA8_MASK | volumeIndices.size());
					for(int i=0; i<8; i++)
						volumeIndices.add(nodesIndicesMap.get(Integer.parseInt(st.nextToken())));   
					hasHexa8 = true;
					break;
			}
		}
	}
	
	private void readGroup(BufferedReader rd, int blockID) throws IOException
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
			surfaceGroupNames.add(rd.readLine().trim());
			
			TIntArrayList facelist = new TIntArrayList();
			while ((line = rd.readLine().trim()).startsWith("8"))
			{
				st = new StringTokenizer(line);
				
				// read one element over two, the first one doesnt matter
				while (st.hasMoreTokens()) {
					st.nextToken();
					String index = st.nextToken();
					int id=elementSurfaceIndicesMap.get(Integer.parseInt(index));
					facelist.add(id);
					nbelem--;
					if (blockID == 2435)
					{
						st.nextToken();
						st.nextToken();
					}
				}
				if  (nbelem <= 0)
				{
					line = rd.readLine();
					break;
				}
			}
			surfaceGroups.add(facelist.toNativeArray());			
		}
	}
	
	private void readTetra4LoadSet(int element, int faceId, TIntArrayList group)
	{
		int p1=volumeIndices.get(element++);
		int p2=volumeIndices.get(element++);
		int p3=volumeIndices.get(element++);
		int p4=volumeIndices.get(element++);
		
		group.add(surfaceIndices.size());
		switch(faceId)
		{
			case 1:
				surfaceIndices.add(p1);
				surfaceIndices.add(p2);
				surfaceIndices.add(p3);
				break;
			case 2:
				surfaceIndices.add(p1);
				surfaceIndices.add(p2);
				surfaceIndices.add(p4);
				break;
			case 3:
				surfaceIndices.add(p2);
				surfaceIndices.add(p3);
				surfaceIndices.add(p4);
				break;
			case 4:
				surfaceIndices.add(p1);
				surfaceIndices.add(p3);
				surfaceIndices.add(p4);
				break;
			default:
				throw new IllegalStateException("Face ID should be 1,2,3 or 4");
		}			
	}

	private void readHexa8LoadSet(int element, int faceId, TIntArrayList group)
	{
		int p1=volumeIndices.get(element++);
		int p2=volumeIndices.get(element++);
		int p3=volumeIndices.get(element++);
		int p4=volumeIndices.get(element++);
		int p5=volumeIndices.get(element++);
		int p6=volumeIndices.get(element++);
		int p7=volumeIndices.get(element++);
		int p8=volumeIndices.get(element++);
		
		group.add(surfaceIndices.size());
		switch(faceId)
		{
			case 1:
				surfaceIndices.add(p1);
				surfaceIndices.add(p2);
				surfaceIndices.add(p3);
				surfaceIndices.add(p4);
				break;
			case 2:
				surfaceIndices.add(p5);
				surfaceIndices.add(p6);
				surfaceIndices.add(p7);
				surfaceIndices.add(p8);
				break;
			case 3:
				surfaceIndices.add(p1);
				surfaceIndices.add(p2);
				surfaceIndices.add(p6);
				surfaceIndices.add(p5);
				break;
			case 4:
				surfaceIndices.add(p2);
				surfaceIndices.add(p3);
				surfaceIndices.add(p7);
				surfaceIndices.add(p6);
				break;
			case 5:
				surfaceIndices.add(p3);
				surfaceIndices.add(p4);
				surfaceIndices.add(p8);
				surfaceIndices.add(p7);
				break;
			case 6:
				surfaceIndices.add(p1);
				surfaceIndices.add(p4);
				surfaceIndices.add(p8);
				surfaceIndices.add(p5);
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
			int element=elementVolumeIndicesMap.get(Integer.parseInt(st.nextToken()));

			int faceId=Integer.parseInt(st.nextToken());
			if((element & TETRA4_MASK) != 0)
			{
				readTetra4LoadSet(element & (~TETRA4_MASK), faceId, groupTetra4);
			}
			else if((element & HEXA8_MASK) != 0)
			{
				readHexa8LoadSet(element & (~HEXA8_MASK), faceId, groupHexa8);
			}
				
			rd.readLine(); //RECORD 4
			rd.readLine(); //RECORD 5
		}
		
		if(!groupTetra4.isEmpty())
		{
			surfaceGroupNames.add(name);
			surfaceGroups.add(groupTetra4.toNativeArray());
		}

		if(!groupHexa8.isEmpty())
		{
			surfaceGroupNames.add(name);
			surfaceGroups.add(groupHexa8.toNativeArray());
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
			System.out.println(unvp.getQuad4FromGroup(0).length);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
