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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;


public class IntFileReaderByDirectBuffer implements IntFileReader
{
	private final static int BUFFER_SIZE  = 8*1024*1024;
	private final static int ARRAY_SIZE   = 4096;
	private static final int ELEMENT_SIZE = 4;

	private final FileChannel fc;
	private final long numberOfElements;
	private final ByteBuffer bb;
	private final IntBuffer tb;
	private int[] array;

	private int startBufferIndex;
	private int arrayIndex;
	private int remaining;

	public IntFileReaderByDirectBuffer(File file) throws IOException
	{
		fc = new FileInputStream(file).getChannel();
		numberOfElements = fc.size() / ELEMENT_SIZE;
		if (fc.size() > BUFFER_SIZE)
			bb = ByteBuffer.allocateDirect(BUFFER_SIZE);
		else
			bb = ByteBuffer.allocateDirect((int) fc.size());
		tb = bb.asIntBuffer();

		// Copy from file into buffer
		copyFileIntoBuffer();
	}

	public long size()
	{
		return numberOfElements;
	}

	private void moveBufferTo(long newPosition) throws IOException
	{
		fc.position(newPosition);
		// Fill in buffer
		copyFileIntoBuffer();
	}

	public int get() throws IOException
	{
		if (remaining == 0)
			copyBufferIntoArray();
		if (remaining < 0)
			throw new IOException();

		int ret = array[arrayIndex];
		arrayIndex++;
		remaining--;
		// We must check for EOF if all values are exhausted
		if (remaining == 0 && !tb.hasRemaining())
			copyFileIntoBuffer();
		return ret;
	}

	public int get(int index) throws IOException
	{
		int relIndex = index - startBufferIndex;
		if (relIndex < 0 || relIndex >= tb.limit())
		{
			moveBufferTo(index * ELEMENT_SIZE);
			if (remaining < 0)
				throw new IndexOutOfBoundsException();
			relIndex = index - startBufferIndex;
		}
		return tb.get(relIndex);
	}

	public int get(int[] dst) throws IOException
	{
		return get(dst, 0, dst.length);
	}

	public final int get(int[] dst, int pos, int len) throws IOException
	{
		int ret = 0;
		// First copy ints from array
		if (remaining > 0)
		{
			int nr = Math.min(remaining, len);
			System.arraycopy(array, arrayIndex, dst, pos, nr);
			arrayIndex += nr;
			remaining -= nr;
			pos += nr;
			len -= nr;
			ret += nr;
			if (len == 0)
			{
				if (remaining == 0 && !tb.hasRemaining())
					copyFileIntoBuffer();
				return ret;
			}
		}
		// Now copy ints directly from buffer
		while (tb.remaining() < len)
		{
			int nr = tb.remaining();
			tb.get(dst, pos, nr);
			pos += nr;
			len -= nr;
			ret += nr;
			if (!copyFileIntoBuffer())
				return ret;
		}
		tb.get(dst, pos, len);
		ret += len;
		// Discard array
		remaining = 0;

		return ret;
	}

	public int get(int index, int[] dst) throws IOException
	{
		return get(index, dst, 0, dst.length);
	}

	public final int get(int index, int[] dst, int offset, int len) throws IOException
	{
		int relIndex = index - startBufferIndex;
		if (relIndex < 0 || relIndex >= tb.limit())
		{
			moveBufferTo(index * ELEMENT_SIZE);
			if (remaining < 0)
				throw new IndexOutOfBoundsException();
			relIndex = index - startBufferIndex;
		}
		// Change buffer position and discard array
		tb.position(relIndex);
		remaining = 0;
		return get(dst, offset, len);
	}

	private boolean copyFileIntoBuffer() throws IOException
	{
		tb.clear();
		bb.clear();
		int nr;

		if (fc.position() == fc.size())
		{
			remaining = -1;
			return false;
		}

		startBufferIndex = (int) (fc.position() / ELEMENT_SIZE);
		do
		{
			nr = fc.read(bb);
		} while (nr == 0);
		if (nr == -1)
		{
			remaining = -1;
			return false;
		}
		nr /= ELEMENT_SIZE;
		tb.position(0);
		tb.limit(nr);
		// Discard array
		remaining = 0;

		return true;
	}

	private void copyBufferIntoArray() throws IOException
	{
		if (!tb.hasRemaining())
		{
			if (!copyFileIntoBuffer())
				return;
		}
		if (array == null)
			array = new int[Math.min((int) numberOfElements, ARRAY_SIZE)];

		arrayIndex = 0;
		remaining = Math.min(tb.remaining(), array.length);
		tb.get(array, 0, remaining);
	}

	public boolean isEOF()
	{
		return remaining < 0;
	}

	public void close()
	{
		try
		{
			fc.close();
		}
		catch (IOException ex) { /* Do not care */ }
	}

}