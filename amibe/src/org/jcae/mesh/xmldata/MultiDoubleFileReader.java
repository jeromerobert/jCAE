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
 * (C) Copyright 2012, by EADS France
 */

package org.jcae.mesh.xmldata;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Read a file whose format is the following.
 * [number of double in the block](int), [a sequence of double], ...
 * @author Jerome Robert
 */
public class MultiDoubleFileReader implements Iterable<DoubleBuffer>, Iterator<DoubleBuffer>
{
	protected ReadableByteChannel channel;
	protected ByteBuffer buffer = ByteBuffer.allocate(4);
	public MultiDoubleFileReader(String fileName) throws IOException
	{
		FileChannel fc = new FileInputStream(fileName).getChannel();
		fc.position(0);
		channel = fc;
		buffer.order(ByteOrder.nativeOrder());
	}

	public void close() throws IOException
	{
		channel.close();
	}

	public Iterator<DoubleBuffer> iterator() {
		return this;
	}

	public boolean hasNext() {
		return true;
	}

	public DoubleBuffer next() {
		try {
			((Buffer)buffer).clear();
			int r = channel.read(buffer);
			if(r != 4)
				throw new NoSuchElementException(
					"Enable to read the size of the underlying array. Only "+r+
					" bytes were read while 4 should have been.");
			int size = buffer.getInt(0);
			ByteBuffer toReturn = ByteBuffer.allocate(size * 8);
			toReturn.order(ByteOrder.nativeOrder());
			int n = channel.read(toReturn);
			if(n != size * 8)
				throw new NoSuchElementException("End of file");
			((Buffer)toReturn).rewind();
			return toReturn.asDoubleBuffer();
		} catch (IOException ex) {
			NoSuchElementException ne = new NoSuchElementException(ex.getMessage());
			ne.initCause(ex);
			throw ne;
		}
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
