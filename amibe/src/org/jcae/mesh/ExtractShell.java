/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
	Copyright (C) 2005 Jerome Robert <jeromerobert@users.sourceforge.net>
 
	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.
 
	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.
 
	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh;

import java.io.*;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Iterator;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.NumberFormat;
import java.util.Arrays;
import gnu.trove.TIntHashSet;
import org.apache.log4j.Logger;


public class ExtractShell
{
	private static Logger logger=Logger.getLogger(ExtractShell.class);
	private static int lineno = 0;
	private static String CR = System.getProperty("line.separator");
	private final static NumberFormat FORMAT_I10=new FormatI10();
	
	public static void main(String args[])
	{
		if (args.length != 2)
		{
			System.out.println("Usage : ExtractShell inputfile outputfile");
			System.exit(1);
		}
		String infile = args[0];
		String outfile = args[1];
		String facesfile = "faces.inc";
		TIntHashSet inodes = null;
		String line = new String();
		try
		{
			FileInputStream in = new FileInputStream(infile);
			PrintStream outNodes = new PrintStream(new BufferedOutputStream(new FileOutputStream(outfile)));
			PrintStream outFaces = new PrintStream(new BufferedOutputStream(new FileOutputStream(facesfile)));
			BufferedReader rd=new BufferedReader(new InputStreamReader(in));
			while ((line=rd.readLine())!=null)
			{
				lineno++;
				if (line.trim().equals("-1"))
				{
					line = rd.readLine();
					lineno++;
					if (line.trim().equals("2412"))
					{
						// read faces
						outFaces.println("    -1"+CR+line);
						inodes = readFace(rd, outFaces);
						outFaces.println("    -1");
					}
					else
					{
						// default group
						// read end of group
						while (!(line=rd.readLine().trim()).equals("-1"))
							lineno++;
						lineno++;
					}
				}
			}
			in.close();
			in = new FileInputStream(infile);
			rd = new BufferedReader(new InputStreamReader(in));
			lineno = 0;
			while ((line=rd.readLine())!=null)
			{
				lineno++;
				if (line.trim().equals("-1"))
				{
					line = rd.readLine();
					lineno++;
					if (line.trim().equals("2411") || line.trim().equals("781"))
					{
						// read nodes
						outNodes.println("    -1"+CR+line);
						readNodes(rd, outNodes, inodes);
						outNodes.println("    -1");
					}
					else if (line.trim().equals("164"))
					{
						// read unit
						outNodes.println("    -1"+CR+line);
						readUnit(rd, outNodes);
						outNodes.println("    -1");
					}
					else
					{
						// default group
						// read end of group
						while (!(line=rd.readLine().trim()).equals("-1"))
							lineno++;
						lineno++;
					}
				}
			}
			in.close();
			outNodes.close();
			outFaces.close();
			//  Append faces.inc to output file
			outNodes = new PrintStream(new BufferedOutputStream(new FileOutputStream(outfile, true)));
			in = new FileInputStream(facesfile);
			rd = new BufferedReader(new InputStreamReader(in));
			while ((line=rd.readLine())!=null)
				outNodes.println(line);
			in.close();
			outNodes.close();
		}
		catch(Exception e)
		{
				e.printStackTrace();
				System.exit(1);
		}
	}

	private static void readUnit(BufferedReader rd, PrintStream outNodes)
	{
		String line = new String();
		try
		{
			//retrieve the second line
			line = rd.readLine();
			outNodes.println(line);
			line = rd.readLine();
			lineno += 2;
			outNodes.println(line);
			while(!(line = rd.readLine()).trim().equals("-1"))
			{
				lineno++;
				outNodes.println(line);
			}
			lineno++;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void readNodes(BufferedReader rd, PrintStream outNodes, TIntHashSet inodes)
	{
		logger.debug("Reading nodes");
		double x,y,z;
		String line = new String();
		int count = 0;
		int inner = 0;
		try
		{
			while(!(line=rd.readLine()).trim().equals("-1"))
			{
				lineno++;
				//First number : the node's id
				StringTokenizer st = new StringTokenizer(line);
				int index = new Integer(st.nextToken()).intValue();
				boolean found = inodes.contains(index);
				if (found)
					count++;
				else
					inner++;
				if (found)
					outNodes.println(line);
				line = rd.readLine();
				lineno++;
				if (found)
					outNodes.println(line);
			}
			lineno++;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		logger.info("Found "+count+" nodes on shell and "+inner+" internal nodes");
	}

	private static TIntHashSet readFace(BufferedReader rd, PrintStream outFaces)
	{
		TIntHashSet inodes = new TIntHashSet();
		logger.debug("Reading triangles");
		HashMap facemap = new HashMap();
		String line = new String();
		
		try
		{
			int index = 0;
			int tetra118 = 0;
			int inner = 0;
			Face [] f = new Face[4];
			while (!(line=rd.readLine()).trim().equals("-1"))
			{
				lineno++;
				// first line: type of object
				StringTokenizer st = new StringTokenizer(line);
				// Volume label is not needed
				st.nextToken();
				String type = st.nextToken();
				int p1 = 0, p2 = 0, p3 = 0, p4 = 0;
				if (type.equals("118"))
				{
					tetra118++;
					line=rd.readLine();
					lineno++;
					// P2 tetrahedron
					// Vertices are in position 1 3 5 10
					st = new StringTokenizer(line);
					p1 = Integer.valueOf(st.nextToken()).intValue();
					st.nextToken();
					p2 = Integer.valueOf(st.nextToken()).intValue();
					st.nextToken();
					p3 = Integer.valueOf(st.nextToken()).intValue();
					st.nextToken();
					st.nextToken();
					st.nextToken();
					line=rd.readLine().trim();
					lineno++;
					st = new StringTokenizer(line);
					st.nextToken();
					p4 = Integer.valueOf(st.nextToken()).intValue();
					f[0] = new Face(p1, p2, p3);
					f[1] = new Face(p1, p2, p4);
					f[2] = new Face(p1, p3, p4);
					f[3] = new Face(p2, p3, p4);
					for (int i = 0; i < 4; i++)
					{
						if (!facemap.containsKey(f[i]))
						{
							index++;
							f[i].label = index;
							facemap.put(f[i], f[i]);
						}
						else
						{
							inner++;
							f[i] = (Face) facemap.get(f[i]);
						}
						f[i].count++;
					}
				}
				else if (type.equals("21") || type.equals("22") || type.equals("23")  || type.equals("24"))
				{
					rd.readLine();
					rd.readLine();
					lineno += 2;
				}
				else
				{
					System.out.println("Unknown field type: "+type+" ...ignored");
					st.nextToken();
					st.nextToken();
					st.nextToken();
					int nr = Integer.valueOf(st.nextToken()).intValue();
					for (int i = nr; i > 0; i -= 8)
					{
						rd.readLine();
						lineno++;
					}
				}
			}
			logger.info("Found "+tetra118+" quadratic tetrahedron, with "+inner+" inner triangles");
			lineno++;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		int index = 0;
		for (Iterator it = facemap.values().iterator(); it.hasNext(); )
		{
			Face f = (Face) it.next();
			if (f.count != 1)
				continue;
			index++;
			outFaces.println(FORMAT_I10.format(index)+"        91         1         1         1         3");
			outFaces.println(FORMAT_I10.format(f.v[0])+FORMAT_I10.format(f.v[1])+FORMAT_I10.format(f.v[2]));
			inodes.add(f.v[0]);
			inodes.add(f.v[1]);
			inodes.add(f.v[2]);
		}
		logger.info("Found "+index+" triangles");
		return inodes;
	}
	
	private static class Face
	{
		public int [] v = new int[3];
		public int count = 0;
		public int label = 0;
		public Face(int i, int j, int k)
		{
			int t;
			v[0] = i;
			v[1] = j;
			v[2] = k;
			//  Sort vertices to speed up this.equals
			if (v[0] > v[1])
			{
				t = v[0];
				v[0] = v[1];
				v[1] = t;
			}
			if (v[1] > v[2])
			{
				t = v[1];
				v[1] = v[2];
				v[2] = t;
			}
			if (v[0] > v[1])
			{
				t = v[0];
				v[0] = v[1];
				v[1] = t;
			}
			assert v[0] < v[1] && v[1] < v[2];
		}
		public final int hashCode()
		{
			return v[0] + v[1] + v[2];
		}
		
		public final boolean equals(Object o)
		{
			if (o == null)
				return false;
			Face that = (Face) o;
			return (that.v[0] == v[0] && that.v[1] == v[1] && that.v[2] == v[2]);
		}
	}
	public static class FormatI10 extends NumberFormat
	{

		/* (non-Javadoc)
		 * @see java.text.NumberFormat#format(double, java.lang.StringBuffer, java.text.FieldPosition)
		 */
		public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos)
		{
			return format((long)number, toAppendTo, pos);
		}

		/* (non-Javadoc)
		 * @see java.text.NumberFormat#format(long, java.lang.StringBuffer, java.text.FieldPosition)
		 */
		public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos)
		{
			StringBuffer s=new StringBuffer();
			s.append(number);
			int n=10-s.length();
			if(n>0)
			{
				char[] c=new char[n];
				Arrays.fill(c, ' ');
				toAppendTo.append(c);
				toAppendTo.append(s);
			}
			return toAppendTo;
		}
		/* (non-Javadoc)
		 * @see java.text.NumberFormat#parse(java.lang.String, java.text.ParsePosition)
		 */
		public Number parse(String source, ParsePosition parsePosition)
		{
			throw new UnsupportedOperationException();
		}
	}
}
