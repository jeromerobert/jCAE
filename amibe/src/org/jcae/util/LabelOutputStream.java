/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

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

package org.jcae.util;

import javax.swing.*;
import java.io.*;

class LabelOutputStream extends OutputStream
{
	JLabel jLabel;
	String buffer;
	private String cr;

	public LabelOutputStream(JLabel l)
	{
		jLabel=l;
	    buffer=new String();
		cr=System.getProperty("line.separator");
	}

	public void write(int b)
	{
	    char cbuf[] = new char[1];
	    cbuf[0] = (char) b;
		buffer=buffer+new String(cbuf);
		if(buffer.endsWith(cr))
		{
		    buffer=buffer.substring(0,buffer.length()-cr.length());
			jLabel.setText(buffer);
			buffer="";
		}
	}
}
