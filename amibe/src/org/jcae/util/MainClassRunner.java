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
 * MainClassRunner.java
 *
 * Created on 25 septembre 2002, 14:18
 */

package org.jcae.util;
import java.lang.reflect.*;
/**
 *
 * @author  jerome
 */
public class MainClassRunner extends Thread
{
	Class myClass;
	String[] myArgs;

	public void run()
	{
		try
		{
			Class[] ca=new Class[1];
			ca[0]=myArgs.getClass();
			Method mm=myClass.getMethod("main", ca);
			Object o=myClass.newInstance();
			mm.invoke(null,new Object[]{myArgs});
		} catch(Exception ex)
		{
			ex.printStackTrace(System.out);
			throw new RuntimeException(ex.toString());
		}
	}

	/** Creates a new instance of MainClassRunner */
	public MainClassRunner(Class clazz, String[] args)
	{
		myClass=clazz;
		myArgs=args;
		setName(clazz.getName()+".main");
	}
}
