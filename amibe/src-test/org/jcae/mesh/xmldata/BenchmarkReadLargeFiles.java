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

import org.junit.Test;
import static org.junit.Assert.*;

public class BenchmarkReadLargeFiles
{	

	private File file = new File("/home/db/Projects/jcae/amibe/src-test/org/jcae/mesh/xmldata/triangles3d.bin");
	private long checkSum = 1546946509L;

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

	@Test public void mmap() throws FileNotFoundException, IOException
	{
		assertEquals(computeSum(new IntFileReaderByMmap(file)), checkSum);
	}

	@Test public void direct() throws FileNotFoundException, IOException
	{
		assertEquals(computeSum(new IntFileReaderByDirectBuffer(file)), checkSum);
	}

	@Test public void blockMmap() throws FileNotFoundException, IOException
	{
		assertEquals(computeSumByBlock(new IntFileReaderByMmap(file)), checkSum);
	}

	@Test public void blockdirect() throws FileNotFoundException, IOException
	{
		assertEquals(computeSumByBlock(new IntFileReaderByDirectBuffer(file)), checkSum);
	}

	@Test public void absoluteMmap() throws FileNotFoundException, IOException
	{
		assertEquals(absoluteComputeSum(new IntFileReaderByMmap(file)), checkSum);
	}

	@Test public void absoluteDirect() throws FileNotFoundException, IOException
	{
		assertEquals(absoluteComputeSum(new IntFileReaderByDirectBuffer(file)), checkSum);
	}

	@Test public void absoluteBlockMmap() throws FileNotFoundException, IOException
	{
		assertEquals(absoluteComputeSumByBlock(new IntFileReaderByMmap(file)), checkSum);
	}

	@Test public void absoluteBlockDirect() throws FileNotFoundException, IOException
	{
		assertEquals(absoluteComputeSumByBlock(new IntFileReaderByDirectBuffer(file)), checkSum);
	}
}