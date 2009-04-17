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
 * (C) Copyright 2009, by EADS France
 */

package org.jcae.mesh.xmldata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class BenchmarkReadLargeFiles
{	

	private static int computeSum(IntFileReader ifr)
	{
		long start = System.currentTimeMillis();
		int sum = 0;
		try
		{
			while (!ifr.isEOF())
				sum += ifr.get();

			ifr.close();
		} catch (IOException ex) {
			Logger.getLogger(BenchmarkReadLargeFiles.class.getName()).log(Level.SEVERE, null, ex);
		}

		System.out.println("processing time: "+(System.currentTimeMillis() - start)+" ms");
		return sum;
	}

	private static int computeSumByBlock(IntFileReader ifr)
	{
		long start = System.currentTimeMillis();
		int sum = 0;
		int [] temp = new int[4096];
		try
		{
			while (!ifr.isEOF())
			{
				for (int i=0, nr=ifr.get(temp); i < nr; i++)
					sum += temp[i];
			}

			ifr.close();
		} catch (IOException ex) {
			Logger.getLogger(BenchmarkReadLargeFiles.class.getName()).log(Level.SEVERE, null, ex);
		}

		System.out.println("processing time: "+(System.currentTimeMillis() - start)+" ms");
		return sum;
	}

	private static int absoluteComputeSum(IntFileReader ifr)
	{
		long start = System.currentTimeMillis();
		int sum = 0;
		try
		{
			for (int i = 0, n = (int) ifr.size(); i < n; i++)
				sum += ifr.get(i);

			ifr.close();
		} catch (IOException ex) {
			Logger.getLogger(BenchmarkReadLargeFiles.class.getName()).log(Level.SEVERE, null, ex);
		}

		System.out.println("processing time: "+(System.currentTimeMillis() - start)+" ms");
		return sum;
	}

	private static int absoluteComputeSumByBlock(IntFileReader ifr)
	{
		long start = System.currentTimeMillis();
		int sum = 0;
		int [] temp = new int[12345];
		long size = ifr.size();
		try
		{
			int i = 0;
			while (i < size)
			{
				int nr = ifr.get(i, temp);
				for (int j = 0; j < nr; j++)
					sum += temp[j];
				i += nr;
			}

			ifr.close();
		} catch (IOException ex) {
			Logger.getLogger(BenchmarkReadLargeFiles.class.getName()).log(Level.SEVERE, null, ex);
		}

		System.out.println("processing time: "+(System.currentTimeMillis() - start)+" ms");
		return sum;
	}

	public static void main(String []args)
	{
		try {
			File file = new File("/home/db/Projects/jcae/amibe/src-test/org/jcae/mesh/xmldata/triangles3d.bin");
			int sum;

			sum = computeSum(new IntFileReaderByMmap(file));
			assert sum == 1546946509;
			System.out.println("mmap sum: "+sum);

			sum = computeSum(new IntFileReaderByDirectBuffer(file));
			assert sum == 1546946509;
			System.out.println("direct sum: "+sum);

			sum = computeSumByBlock(new IntFileReaderByMmap(file));
			assert sum == 1546946509;
			System.out.println("mmap by block, sum: "+sum);

			sum = computeSumByBlock(new IntFileReaderByDirectBuffer(file));
			assert sum == 1546946509;
			System.out.println("direct by block, sum: "+sum);

			sum = absoluteComputeSum(new IntFileReaderByDirectBuffer(file));
			assert sum == 1546946509;
			System.out.println("mmap (abs. get) sum: "+sum);

			sum = absoluteComputeSum(new IntFileReaderByDirectBuffer(file));
			assert sum == 1546946509;
			System.out.println("direct (abs. get) sum: "+sum);

			sum = absoluteComputeSumByBlock(new IntFileReaderByMmap(file));
			assert sum == 1546946509;
			System.out.println("mmap by block (abs. get) sum: "+sum);

			sum = absoluteComputeSumByBlock(new IntFileReaderByDirectBuffer(file));
			assert sum == 1546946509;
			System.out.println("direct by block (abs. get) sum: "+sum);

		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}