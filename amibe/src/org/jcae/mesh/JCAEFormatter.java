/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2008, by EADS France
 
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

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class JCAEFormatter extends Formatter
{
	private static long startDate = -1L;
	private static final String lineSep = System.getProperty("line.separator");

	public JCAEFormatter() {
		startDate = -1L;
	}

	@Override
	public final String format(LogRecord record)
	{
		String loggerName = record.getLoggerName();
		if(loggerName == null)
		{
			loggerName = "root";
		}
		if (startDate < 0L)
		{
			startDate = record.getMillis();
		}
		StringBuilder output = new StringBuilder()
			.append(record.getMillis() - startDate)
			.append(" [")
			.append(record.getLevel())
			.append("] ")
			.append(loggerName)
			.append("- ")
			.append(formatMessage(record))
			.append(lineSep);
		return output.toString();
	}
}
