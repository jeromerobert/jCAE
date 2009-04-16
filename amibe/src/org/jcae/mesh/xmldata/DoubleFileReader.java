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

import java.io.IOException;

public interface DoubleFileReader
{
	/**
	 * Return the number of elements contained in this file.
	 * 
	 * @return the number of elements contained in this file
	 */
	public long size();

	/**
	 * Relative get method.  Return the next double and increment buffer position.
	 * 
	 * @return the next double
	 * @throws java.io.IOException  If some other I/O error occurs
	 */
	public double get() throws IOException;
	
	/**
	 * Absolute get method.  Return the double found at this position.
	 *
	 * @param index  the index from which value is read
	 * @return the double found at this position
	 * @throws java.io.IOException  If some other I/O error occurs
	 */
	public double get(int index) throws IOException;

	/**
	 * Relative bulk get method.  Read values and copy them into an existing array.  Buffer position is incremented.
	 * 
	 * @param dst destination array
	 * @return the number of doubles written into dst
	 * @throws java.io.IOException  If some other I/O error occurs
	 */
	public int get(double[] dst) throws IOException;

	/**
	 * Relative bulk get method.  Read values and copy them into an existing array.  Buffer position is incremented.
	 *
	 * @param dst destination array
	 * @param offset  offset within dst
	 * @param len   maximal number of doubles to read
	 * @return the number of doubles written into dst
	 * @throws java.io.IOException  If some other I/O error occurs
	 */	public int get(double[] dst, int offset, int len) throws IOException;

	/**
	 * Absolute bulk get method.  Read values and copy them into an existing array.
	 *
	 * @param index  the index from which values are read
	 * @param dst destination array
	 * @return the number of doubles written into dst
	 * @throws java.io.IOException  If some other I/O error occurs
	 */
	public int get(int index, double[] dst) throws IOException;

	/**
	 * Absolute bulk get method.  Read values and copy them into an existing array.
	 *
	 * @param index  the index from which values are read
	 * @param dst destination array
	 * @param offset  offset within dst
	 * @param len   maximal number of doubles to read
	 * @return the number of doubles written into dst
	 * @throws java.io.IOException  If some other I/O error occurs
	 */
	public int get(int index, double[] dst, int offset, int len) throws IOException;

	/**
	 * Tell whether reading is done.
	 * 
	 * @return <code>true</code> if file has been fully read, false otherwise
	 */
	public boolean isEOF();

	/**
	 * Close file.
	 */
	public void close();

}