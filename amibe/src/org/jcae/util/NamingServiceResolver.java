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

import org.omg.CORBA.ORB;
import org.omg.CosNaming.*;
import java.lang.reflect.*;
import org.apache.log4j.Logger;
/**
 * @author  Jerome Robert
 */
public class NamingServiceResolver
{
	private NamingContextExt namingContextExt;
	static private Logger logger=Logger.getLogger(NamingServiceResolver.class);
	
	public NamingServiceResolver(ORB orb)
	{
		try
		{
			org.omg.CORBA.Object objRef = 
				orb.resolve_initial_references("NameService");			
			namingContextExt = NamingContextExtHelper.narrow(objRef);
		}
		catch(org.omg.CORBA.ORBPackage.InvalidName ex)
		{
			ex.printStackTrace();			
		}
	}
	
	public NamingServiceResolver(NamingContextExt namingContextExt)
	{
		this.namingContextExt = namingContextExt;
	}
	
	public Class getClass(String pathName)
	{
		try
		{			
			return getClass(namingContextExt.to_name(pathName));
		}catch(org.omg.CosNaming.NamingContextPackage.InvalidName ex)
		{
			logger.error("Invalid Name :"+pathName);
			ex.printStackTrace();
			return null;
		}
	}
	
	public Class getClass(NameComponent[] path)
	{	
		try
		{		
			String className=path[path.length-1].kind;
			logger.debug("getClass("+className+")");
			return Class.forName(className);
		}
		catch(ClassNotFoundException ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
	
	public org.omg.CORBA.Object getObject(NameComponent[] path)
	{		
		try
		{			
			return getObject(namingContextExt.to_string(path));
		}
		catch(org.omg.CosNaming.NamingContextPackage.InvalidName ex)
		{
			ex.printStackTrace();
			return null;
		}		
	}
	
	public org.omg.CORBA.Object getObjectThis(Class classN)
	{
		try
		{
			NameComponent[] path=namingContextExt.to_name("this.Object");			
			org.omg.CORBA.Object o=namingContextExt.resolve(path);
			Class clazz=Class.forName(classN.getName()+"Helper");
			Method narrow=clazz.getMethod("narrow",new Class[]{org.omg.CORBA.Object.class});
			return (org.omg.CORBA.Object)narrow.invoke(null,new Object[]{o});
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return null;
		}		
	}
	
	public org.omg.CORBA.Object getObject(String pathName)
	{		
		try
		{
			NameComponent[] path=namingContextExt.to_name(pathName+"/this.Object");			
			org.omg.CORBA.Object o=namingContextExt.resolve(path);
			Class clazz=Class.forName(getClass(pathName).getName()+"Helper");
			Method narrow=clazz.getMethod("narrow",new Class[]{org.omg.CORBA.Object.class});
			return (org.omg.CORBA.Object)narrow.invoke(null,new Object[]{o});
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
}
