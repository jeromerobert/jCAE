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

/*
 * TextComponentOutputStream.java
 *
 * Created on 26 juillet 2002, 14:16
 */

package org.jcae.util;
import javax.swing.*;
import javax.swing.text.*;
import java.io.*;
/**
 *
 * @author  jerome
 */
public class TextComponentOutputStream extends OutputStream
{
	private String cr;	
	JTextComponent textComponent;
	String buffer;
	
	/** Creates a new instance of TextComponentOutputStream */
	public TextComponentOutputStream(JTextComponent tc)
	{
		textComponent=tc;
		cr=System.getProperty("line.separator");
		buffer=new String();
	}
	
	/** Writes the specified byte to this output stream. The general
	 * contract for <code>write</code> is that one byte is written
	 * to the output stream. The byte to be written is the eight
	 * low-order bits of the argument <code>b</code>. The 24
	 * high-order bits of <code>b</code> are ignored.
	 * <p>
	 * Subclasses of <code>OutputStream</code> must provide an
	 * implementation for this method.
	 *
	 * @param      b   the <code>byte</code>.
	 * @exception  IOException  if an I/O error occurs. In particular,
	 *             an <code>IOException</code> may be thrown if the
	 *             output stream has been closed.
	 */
	public void write(int b) throws IOException
	{
	    char cbuf[] = new char[1];
	    cbuf[0] = (char) b;
		buffer=buffer+new String(cbuf);
		if(buffer.endsWith(cr))
		{
		    //buffer=buffer.substring(0,buffer.length()-cr.length());
			String s=textComponent.getText()+buffer;
			if(s.length()>200000) s=s.substring(s.indexOf(cr)+1);
			textComponent.setText(s);
			buffer="";
		}
	}
	
}
