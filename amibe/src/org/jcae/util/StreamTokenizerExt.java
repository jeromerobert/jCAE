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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.util;
import java.io.*;

/**
 * @author Jerome Robert
  */

public class StreamTokenizerExt extends StreamTokenizer
{
	public StreamTokenizerExt(File f) throws FileNotFoundException
	{
		super(new BufferedReader(new InputStreamReader(new FileInputStream(f))));
		resetSyntax();
		setSynthax();
	}
	public StreamTokenizerExt(Reader reader)
	{
	    super(reader);
		resetSyntax();
		setSynthax();
	}

	void setSynthax()
	{
		commentChar('#');
		wordChars('a','z');
		wordChars('A','Z');
		wordChars('_','_');
		wordChars(128 + 32, 255);
		wordChars('0','9');
		wordChars('+','+');
		wordChars('-','-');
		wordChars('.','.');
		whitespaceChars(0, ' ');
		whitespaceChars(':',':');
		whitespaceChars('|','|');
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	public String readLine() throws IOException
	{
	    resetSyntax();
		wordChars(' ',255);
		whitespaceChars('\r','\r');
		whitespaceChars('\n','\n');
		nextToken();
		resetSyntax();
		setSynthax();
		if(sval==null) sval="";
		return sval;
	}
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	public int nextToken() throws IOException
	{
		sval="";
		nval=0;
	    return super.nextToken();
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	public float readFloat() throws IOException
	{
	    nextToken();

		String errmsg="Float value expected line ";
		if(ttype!=TT_WORD)
			throw new NumberFormatException(errmsg+String.valueOf(lineno()));
		try
		{
		    sval=sval.replace('D','E');
			sval=sval.replace('+','0');
			return Float.parseFloat(sval);
		} catch(NumberFormatException ex)
		{
		    throw new NumberFormatException(errmsg+String.valueOf(lineno())
				+", found "+sval);
		}
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	private static String readIntegerErrMsg="Integer value expected line ";
	public int readInteger() throws IOException
	{
	    nextToken();
		if(ttype!=TT_WORD)
		{
		    System.out.println(ttype);
			throw new NumberFormatException(readIntegerErrMsg+lineno());
		}
		try
		{
		    return Integer.parseInt(sval);
		} catch(NumberFormatException ex)
		{
		    throw new NumberFormatException(readIntegerErrMsg+lineno()+", found "+sval);
		}
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	
	public int readLineInteger() throws IOException
	{
		String line=readLine().trim();
		try
		{
		    return Integer.parseInt(line);
		} catch(NumberFormatException ex)
		{
		    throw new NumberFormatException(readIntegerErrMsg+lineno()+", found "+line);
		}
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	public String readWord() throws Exception
	{
	    nextToken();
		if(ttype!=TT_WORD) throw new Exception("Word expected line "+
			String.valueOf(lineno()));
	    return sval;
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	public void readWord(String s) throws Exception
	{
	    nextToken();
		String sm=s+" expected line "+String.valueOf(lineno());
		if(ttype==TT_WORD)
		{
			if(!sval.equals(s)) throw new Exception(sm+","+sval+" found");
		} else throw new Exception(sm);
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	public int readWords(String[] s) throws Exception
	{
	    String errmsg;
		errmsg="[";
		int i=0;
		nextToken();
		for(i=0;i<s.length;i++)
		{
		    if(s[i].equals(sval)) return i;
			if(i>0) errmsg=errmsg+"|";
			errmsg=errmsg+s[i];
		}
		errmsg=errmsg+"] expected line "+String.valueOf(lineno())+","+sval+" found";
		throw new Exception(errmsg);
	}
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	boolean findWord(String s) throws IOException
	{
	    boolean ok,end;
		do
		{
		    nextToken();
		    ok=s.equals(sval);
			end=(ttype==TT_EOF);
		} while((!ok)&&(!end));
		return !end;
	}

	boolean findLine(String s) throws IOException
	{
	    String line;
		boolean ok,end;
		do
		{
		    line=readLine();
		    ok=line.trim().equals(s);
			end=(ttype==TT_EOF);
		} while((!ok)&&(!end));
		return !end;
	}

	public boolean findLineInteger(int i) throws IOException
	{
		return findLine(String.valueOf(i));
	}
}