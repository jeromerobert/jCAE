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
 */

package org.jcae.netbeans.mesh;

import java.io.*;
import java.text.*;
import java.util.*;

public class Icms2Unv
{
	public static class FormatD25_16 extends DecimalFormat
	{
		private static String PATERN = "0.0000000000000000E00";

		public FormatD25_16()
		{
			super(PATERN);
			DecimalFormatSymbols dfs = getDecimalFormatSymbols();
			dfs.setDecimalSeparator('.');
			setDecimalFormatSymbols(dfs);
		}

		/*
		 * (non-Javadoc)
		 * @see java.text.NumberFormat#format(double, java.lang.StringBuffer,
		 * java.text.FieldPosition)
		 */
		public StringBuffer format(double number, StringBuffer toAppendTo,
			FieldPosition pos)
		{
			StringBuffer sb = super.format(number, toAppendTo, pos);
			int n = sb.length() - 3;
			if (n > 0)
			{
				if (sb.charAt(n) == 'E')
				{
					sb.setCharAt(n, 'D');
					sb.insert(n + 1, '+');
				} else if (sb.charAt(n) == '-')
				{
					sb.setCharAt(n - 1, 'D');
				}
			}
			n = 25 - sb.length();
			if (n > 0)
			{
				char[] c = new char[n];
				Arrays.fill(c, ' ');
				sb.insert(0, c);
			}
			return sb;
		}
	}
	public static class FormatI10 extends NumberFormat
	{
		/*
		 * (non-Javadoc)
		 * @see java.text.NumberFormat#format(double, java.lang.StringBuffer,
		 * java.text.FieldPosition)
		 */
		public StringBuffer format(double number, StringBuffer toAppendTo,
			FieldPosition pos)
		{
			return format((long) number, toAppendTo, pos);
		}

		/*
		 * (non-Javadoc)
		 * @see java.text.NumberFormat#format(long, java.lang.StringBuffer,
		 * java.text.FieldPosition)
		 */
		public StringBuffer format(long number, StringBuffer toAppendTo,
			FieldPosition pos)
		{
			StringBuffer s = new StringBuffer();
			s.append(number);
			int n = 10 - s.length();
			if (n > 0)
			{
				char[] c = new char[n];
				Arrays.fill(c, ' ');
				toAppendTo.append(c);
				toAppendTo.append(s);
			}
			return toAppendTo;
		}

		/*
		 * (non-Javadoc)
		 * @see java.text.NumberFormat#parse(java.lang.String,
		 * java.text.ParsePosition)
		 */
		public Number parse(String source, ParsePosition parsePosition)
		{
			throw new UnsupportedOperationException();
		}
	}
	private final static String CR = System.getProperty("line.separator");
	private final static NumberFormat FORMAT_D25_16 = new FormatD25_16();
	private final static NumberFormat FORMAT_I10 = new FormatI10();

	public static void main(String[] args)
	{
		if (args.length != 2)
		{
			System.err.println("Usage: java " + Icms2Unv.class.getName()
				+ " <ICMS file> <UNV file>");
			return;
		}
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(args[0]));
			Icms2Unv icms2Unv = new Icms2Unv();
			icms2Unv.readICMS(in);
			PrintStream out = new PrintStream(new FileOutputStream(args[1]));
			icms2Unv.writeUNV(out);
		} catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}

	public static void readICMS(BufferedReader in, Collection<double[]> nodes,
		Collection<int[]> triangles, Collection<String> names) throws IOException
	{
		String st = null;
		int offset = 0;
		while ((st = in.readLine()) != null)
		{
			names.add(st);
			st = in.readLine();
			StringTokenizer tokenizer = new StringTokenizer(st, " ");
			int nPoints = Integer.parseInt(tokenizer.nextToken());
			int nTrias = Integer.parseInt(tokenizer.nextToken());
			nPoints = -nPoints;
			double[] points = new double[nPoints*3];
			st = in.readLine();
			for (int i = 0; i < nPoints; i++)
			{
				st = in.readLine();
				tokenizer = new StringTokenizer(st, " ");
				double x = Double.parseDouble(tokenizer.nextToken());
				double y = Double.parseDouble(tokenizer.nextToken());
				double z = Double.parseDouble(tokenizer.nextToken());
				int idx = i * 3;
				points[idx++] = x;
				points[idx++] = y;
				points[idx] = z;
			}
			nodes.add(points);			
			int[] trias = new int[nTrias*3];
			for (int i = 0; i < nTrias; i++)
			{
				st = in.readLine();
				tokenizer = new StringTokenizer(st, " ");
				int i1 = Integer.parseInt(tokenizer.nextToken());
				int i2 = Integer.parseInt(tokenizer.nextToken());
				int i3 = Integer.parseInt(tokenizer.nextToken());
				int idx = i * 3;
				// convert to global numbering from 0 to n-1
				trias[idx++] = i1 + offset - 1 ;
				trias[idx++] = i2 + offset - 1 ;
				trias[idx] = i3 + offset - 1 ;
			}
			offset += nPoints;
			triangles.add(trias);
		}
	}

	/**
	 * @param out
	 * @param amibeTriaToUNVTria
	 */
	public static void writeGroups(PrintStream out, Collection triangles,
		Collection names)
	{
		out.println("    -1" + CR + "  2430");
		int count = 0;
		Iterator it = triangles.iterator();
		Iterator itNames = names.iterator();
		while (it.hasNext())
		{
			int[] tg = (int[]) it.next();
			int groupSize = tg.length / 3;
			out
				.println("1      0         0         0         0         0         0      "
					+ groupSize);
			out.println(itNames.next());
			int countg = 0;
			for (int j = 0; j < groupSize; j++)
			{				
				out.print("         8" + FORMAT_I10.format(count + 1));
				count++;
				countg++;
				if ((countg % 4) == 0) out.println("");
			}
			if ((countg % 4) != 0) out.println();
		}
		out.println("    -1");
	}

	/**
	 * @param out
	 * @param nodes Collection of double[]{x, y, z, x, z...}
	 * @throws IOException
	 */
	public static void writeNodes(PrintStream out, Collection nodes)
		throws IOException
	{
		out.println("    -1" + CR + "  2411");
		int count = 0;
		double x, y, z;
		Iterator it = nodes.iterator();
		while (it.hasNext())
		{
			double[] nodesID = (double[]) it.next();
			for (int i = 0; i < nodesID.length; i += 3)
			{
				x = nodesID[i];
				y = nodesID[i + 1];
				z = nodesID[i + 2];
				// number from 1 to n
				out.println(FORMAT_I10.format(count + 1)
					+ "         1         1         1");
				out.println(FORMAT_D25_16.format(x) + FORMAT_D25_16.format(y)
					+ FORMAT_D25_16.format(z));
				count++;
			}
		}
		out.println("    -1");
	}

	/**
	 * @param out
	 * @param triangles Collection of int[]{n1, n2, n3...}
	 */
	public static void writeTriangles(PrintStream out, Collection triangles)
	{
		out.println("    -1" + CR + "  2412");
		int count = 0;
		Iterator it = triangles.iterator();
		while (it.hasNext())
		{
			int[] tg = (int[]) it.next();
			for (int i = 0; i < tg.length; i += 3)
			{
				count++;
				out.println(FORMAT_I10.format(count)
					+ "        91         1         1         1         3");
				// +1 to number from 1 to n
				out.println(FORMAT_I10.format(tg[i] + 1)
					+ FORMAT_I10.format(tg[i + 1] + 1)
					+ FORMAT_I10.format(tg[i + 2] + 1));
			}
		}
		out.println("    -1");
	}
	private final Collection<String> names = new ArrayList<String>();
	private final Collection<double[]> nodes = new ArrayList<double[]>();
	private final Collection<int[]> triangles = new ArrayList<int[]>();

	public void readICMS(BufferedReader in) throws IOException
	{
		readICMS(in, nodes, triangles, names);
	}

	public void writeUNV(PrintStream out) throws IOException
	{
		writeNodes(out, nodes);
		writeTriangles(out, triangles);
		writeGroups(out, triangles, names);
	}
}
