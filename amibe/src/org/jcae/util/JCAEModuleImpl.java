package org.jcae.util;

import java.lang.reflect.Method;
import org.apache.log4j.Logger;
import org.omg.PortableServer.Servant;

import org.omg.CosNaming.*;
import org.omg.PortableServer.POA;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;
import java.util.*;
/**
 *
 * @author  Jerome Robert
 */
public abstract class JCAEModuleImpl implements JCAEModuleOperations
{
	static private Logger logger=Logger.getLogger(JCAEModuleImpl.class);
	protected POA _poa;
	private ORB _orb;
	java.util.Map objectToNameMap=new java.util.HashMap();
	
	public JCAEModuleImpl(ORB orb)
	{
		_orb=orb;
		try
		{
			_poa = (POA)orb.resolve_initial_references("RootPOA");
		} catch(org.omg.CORBA.ORBPackage.InvalidName ex)
		{
			ex.printStackTrace();
			logger.fatal("Unable to find POA");
		}
	}
	
	static public void bind(JCAEModule module)
	{		
		ObjectImpl oi=(ObjectImpl)module;
		org.omg.CORBA.portable.ServantObject _so = oi._servant_preinvoke( "",java.lang.Object.class);
		java.lang.Object opoatie=_so.servant;
		try
		{
			Method m=opoatie.getClass().getMethod("_delegate",null);
			JCAEModuleImpl mi=(JCAEModuleImpl)(m.invoke(opoatie, null));
			mi.bind(module,""+mi);
		} 
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public org.omg.CORBA.Object resolve(String pathName)
	{
		return new NamingServiceResolver(_orb).getObject(pathName);
	}
	
	public String getPath(java.lang.Object object)
	{
		return (String)objectToNameMap.get(object);
	}
	
	public String getObjectName(java.lang.Object object)
	{
		try
		{
			org.omg.CORBA.Object objRef = 
				_orb.resolve_initial_references("NameService");			
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);		
			NameComponent[] path=ncRef.to_name(getPath(object));
			String name=path[path.length-1].id;
			//add escape to name
			NameComponent[] epath=new NameComponent[1];
			epath[0]=new NameComponent(name,"");
			return ncRef.to_string(epath);
		}
		catch(UserException ex)
		{
			ex.printStackTrace();
			return null;
		}	
	}
	
	public void unbind(String pathName)
	{
		try
		{
			org.omg.CORBA.Object objRef = 
				_orb.resolve_initial_references("NameService");			
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

			NameComponent[] path = ncRef.to_name(pathName+"/this.Object");
			ncRef.unbind(path);
			path = ncRef.to_name(pathName);
			ncRef.unbind(path);
		}
		catch(UserException ex)
		{
			ex.printStackTrace();
		}	
	}
	
	/** Change the name of an object on the name server
	 * @param currentName Full pathname of the object (ex : parent.type/parent2.type/objet.type)
	 * @param newName New name of the object. This string <B>do not</B> the path name and the kind of
	 * the object
	 */	
	public void rename(String currentName, String newName)
	{
		logger.debug("rename("+currentName+","+newName+")");
		org.omg.CORBA.Object o=resolve(currentName);
		unbind(currentName);
		String newPath=renameInPath(currentName,newName);
		bind(o, newPath);
		Iterator it=objectToNameMap.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry e=(Map.Entry)it.next();
			String s=(String)e.getValue();
			if(s.equals(currentName))
			{
				e.setValue(newPath);
				break;
			}
		}
	}

	/** Change the name of an object in a given path end return the modified path
	 * @param currentName Full pathname of the object (ex : parent.type/parent2.type/objet.type)
	 * @param newName New name of the object. This string <B>do not</B> the path name and the kind of
	 * the object
	 * @return The modified path
	 */		
	public String renameInPath(String currentName, String newName)
	{
		try
		{
			org.omg.CORBA.Object objRef = 
				_orb.resolve_initial_references("NameService");			
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
			NameComponent[] path=ncRef.to_name(currentName);
			path[path.length-1].id=newName;
			return ncRef.to_string(path);
		}
		catch(UserException ex)
		{
			ex.printStackTrace();
			return null;
		}		
	}
	
	public void bind(org.omg.CORBA.Object object, String pathName)
	{
		logger.info("bind "+pathName);
		try
		{
			org.omg.CORBA.Object objRef = 
				_orb.resolve_initial_references("NameService");			
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

			NameComponent[] path = ncRef.to_name(pathName);
			org.omg.CosNaming.NamingContext newNamingContext;
			try
			{
				newNamingContext=ncRef.bind_new_context(path);
			} catch(org.omg.CosNaming.NamingContextPackage.AlreadyBound ex)
			{
				ncRef.unbind(path);
				newNamingContext=ncRef.bind_new_context(path);
			}
			
			path = ncRef.to_name("this.Object");
			newNamingContext.rebind(path, object);		
		}
		catch(UserException ex)
		{
			ex.printStackTrace();
		}
	}
	
	/** Wrap an xxxObjectOperation into a POATie, Activate it and bind it in a the
	 * naming service.
	 * @param o The Object to wrap
	 * @param name The name used to bind the Object. If null the object will not be bound.
	 * @return The CORBA object wrapping the given object.
	 */	
	public Object activateObject(java.lang.Object o, String name)
	{
		try
		{
			org.omg.CORBA.Object s=createPOATie(o);			
			if(name!=null)
			{
				bind(s,name);
				objectToNameMap.put(o, name);
			}
			return s;
		} catch(Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex.toString());
		}
	}
	
	private org.omg.CORBA.Object createPOATie(java.lang.Object object)
	{
		try
		{
			String cn=object.getClass().getName();
			cn=cn.substring(0,cn.length()-4);
			Class clazz=Class.forName(cn+"POATie");
			java.lang.reflect.Constructor constr=clazz.getConstructor(new Class[]{Class.forName(cn+"Operations")});
			Servant s=(Servant)
				constr.newInstance(new java.lang.Object[]{object});
			// activate servant in POA
			_poa.activate_object(s);
			//narrow the object
			clazz=Class.forName(cn+"Helper");
			Method narrow=clazz.getMethod("narrow",new Class[]{org.omg.CORBA.Object.class});
			return (org.omg.CORBA.Object)narrow.invoke(null,new java.lang.Object[]{s._this_object()});						
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
	
	/*public String getJavaClassName()
	{
	}*/
	public String toString()
	{
		return getName()+"."+dotEscapedNoImplClassName(getClass());
	}
	
	public static String dotEscapedNoImplClassName(Class clazz)
	{
		String cs=clazz.getName();
		String toReturn="";
		for(int i=0;i<cs.length();i++)
		{
			char c=cs.charAt(i);
			if(c=='.') toReturn+='\\';
			toReturn+=c;
		}
		if(toReturn.endsWith("Impl"))
		{
			toReturn=toReturn.substring(0,toReturn.length()-4);
		}
		return toReturn;
	}
	
	
	public void destroy(org.omg.CORBA.Object obj)
	{
		/** @TODO */
	}
	
}
