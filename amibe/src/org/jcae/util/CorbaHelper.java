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
 * CorbaHelper.java
 *
 * Created on 9 octobre 2002, 11:43
 */

package org.jcae.util;
import java.lang.reflect.*;
/**
 *
 * @author  jerome
 */
public class CorbaHelper
{
	private org.omg.CORBA.Object corbaThis;
	/** Creates a new instance of CorbaHelper */
	public CorbaHelper(org.omg.CORBA.Object aCorbaThis)
	{
		corbaThis=aCorbaThis;
	}
	
	public org.omg.CORBA.Object getObject()
	{
		return corbaThis;
	}
	
	public String toString()
	{
		try
		{
			Method m=corbaThis.getClass().getMethod("name",null);		
			return (String)(m.invoke(corbaThis,null));
		} catch(Exception ex)
		{
			return corbaThis.toString();
		}
	}
}
