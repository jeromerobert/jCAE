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

package org.jcae.mesh.gui;

import bsh.BshClassManager;
import bsh.classpath.*;
import java.util.*;
import org.jcae.mesh.MeshProcessor;

/**
 *
 * @author  Jerome Robert
 */
public class MeshProcessorLister
{	
	public static Collection list()
	{	    
	    ArrayList toReturn=new ArrayList();
		BshClassManager classManager=BshClassManager.createClassManager();
		BshClassPath classPath;
		try
		{
			classPath=((ClassManagerImpl)classManager).getClassPath();
		}
		catch(bsh.ClassPathException ex)
		{
			ex.printStackTrace();
			return toReturn;
		}
		
		Collection packages=classPath.getPackagesSet();				
		Iterator it=packages.iterator();
		while(it.hasNext())
		{
			String packag=(String)it.next();
			if(packag.startsWith("org.jcae.mesh"))
			{
				Collection classes=classPath.getClassesForPackage(packag);
				Iterator it2=classes.iterator();
				while(it2.hasNext())
				{
					String className=(String)it2.next();					
					if(isMeshProcessorClass(className))
						toReturn.add(formatName(className));
				}
			}
		}			
		toReturn.remove("MeshProcessor");
		return toReturn;
	}
	
	private static boolean isMeshProcessorClass(String className)
	{
		try
		{
			if(className.endsWith("Impl")) return false;
			if(className.endsWith("Stub")) return false;
			Class clazz=Class.forName(className);
			return MeshProcessor.class.isAssignableFrom(clazz);
		} catch(ClassNotFoundException ex)
		{
			return false;
		} catch(UnsatisfiedLinkError ex)
		{
			return false;
		}
	}
	
	private static String formatName(String in)
	{
		return in.substring(new String("org.jcae.mesh").length()+1);
	}
}
