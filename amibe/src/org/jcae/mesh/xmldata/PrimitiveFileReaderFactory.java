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
import java.io.IOException;
import java.io.FileNotFoundException;

public class PrimitiveFileReaderFactory
{
	private static interface FactoryInterface
	{
		IntFileReader getIntFileReader(File file) throws FileNotFoundException, IOException;
		DoubleFileReader getDoubleFileReader(File file) throws FileNotFoundException, IOException;
	}

	private final static FactoryInterface DIRECT_BUFFER = new FactoryByDirectBuffer();
	private final static FactoryInterface MMAP = new FactoryByMmap();

	private static class FactoryByDirectBuffer implements FactoryInterface
	{
		public IntFileReader getIntFileReader(File file) throws FileNotFoundException, IOException
		{
			return new IntFileReaderByDirectBuffer(file);
		}

		public DoubleFileReader getDoubleFileReader(File file) throws FileNotFoundException, IOException
		{
			return new DoubleFileReaderByDirectBuffer(file);
		}
	}

	private static class FactoryByMmap implements FactoryInterface
	{
		public IntFileReader getIntFileReader(File file) throws FileNotFoundException, IOException
		{
			return new IntFileReaderByMmap(file);
		}

		public DoubleFileReader getDoubleFileReader(File file) throws FileNotFoundException, IOException
		{
			return new DoubleFileReaderByMmap(file);
		}
	}

	private final FactoryInterface instance;

	public PrimitiveFileReaderFactory()
	{
		this("directBuffer");
	}

	public PrimitiveFileReaderFactory(String type)
	{
		if ("directBuffer".equals(type))
			instance = DIRECT_BUFFER;
		else if ("mmap".equals(type))
			instance = MMAP;
		else
			throw new IllegalArgumentException("Unsupported argument: \""+type+"\"; valid values are \"directBuffer\" and \"mmap\"");
	}

	/**
	 * Return an IntFileReader instance.
	 *
	 * @param file  a file name containing only integers
	 * @return an IntFileReader instance
	 */
	public IntFileReader getIntReader(File file) throws FileNotFoundException, IOException
	{
		return instance.getIntFileReader(file);
	}

	/**
	 * Return a DoubleFileReader instance.
	 *
	 * @param file  a file name containing only double values
	 * @return a DoubleFileReader instance
	 */
	public DoubleFileReader getDoubleReader(File file) throws FileNotFoundException, IOException
	{
		return instance.getDoubleFileReader(file);
	}

}