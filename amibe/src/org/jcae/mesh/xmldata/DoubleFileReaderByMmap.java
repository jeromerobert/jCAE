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
import java.nio.DoubleBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class DoubleFileReaderByMmap implements DoubleFileReader
{
	private final FileChannel fc;
	private final long numberOfElements;
	private final DoubleBuffer tb;
	private final MappedByteBuffer bb;

	public DoubleFileReaderByMmap(File file) throws IOException
	{
		fc = new FileInputStream(file).getChannel();
		numberOfElements = fc.size() / 8;
		bb = fc.map(FileChannel.MapMode.READ_ONLY, 0L, fc.size());
		tb = bb.asDoubleBuffer();
	}

	public long size()
	{
		return numberOfElements;
	}

	public double get() throws IOException
	{
		return tb.get();
	}

	public double get(int index) throws IOException
	{
		return tb.get(index);
	}

	public int get(double[] dst) throws IOException
	{
		return get(dst, 0, dst.length);
	}

	public int get(double[] dst, int offset, int len) throws IOException
	{
		if (tb.remaining() < len)
			len = tb.remaining();
		tb.get(dst, offset, len);
		return len;
	}

	public int get(int index, double[] dst) throws IOException
	{
		return get(index, dst, 0, dst.length);
	}

	public int get(int index, double[] dst, int offset, int len) throws IOException
	{
		fc.position(index * 4L);
		return get(dst, offset, len);
	}

	public boolean isEOF()
	{
		return !tb.hasRemaining();
	}

	public void close()
	{
		try
		{
			fc.close();
		} catch (IOException ex) {/* Do not care */}
		IntFileReaderByMmap.clean(bb);
	}

}