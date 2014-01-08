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
 * (C) Copyright 2012, by EADS France
 */

package org.jcae.vtk;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import vtk.vtkJavaGarbageCollector;
import vtk.vtkObject;
import vtk.vtkObjectBase;

/**
 *
 * @author Jerome Robert
 */
final class VTKMemoryManager {
	private final static Logger LOGGER =
		Logger.getLogger(VTKMemoryManager.class.getName());
	/**
	 * vtk.vtkGlobalJavaHash.GarbageCollector if vtk &lg; 5.8,
	 * vtkObject.JAVA_OBJECT_MANAGER.getAutoGarbageCollector() else
	 */
	public final static vtkJavaGarbageCollector GC;
	private final static Method DELETE_ALL_METHOD;
	private final static Object OBJECT_MANAGER;
	private final static Method UNREGISTER_METHOD;
	private final static ConcurrentHashMap<Long, WeakReference<?>> OBJECT_MAP;
	static
	{
		vtkJavaGarbageCollector lGC = null;
		Method lDeleteAll = null, lUnregisterMethod = null;
		Object lObjectManager = null;
		ConcurrentHashMap<Long, WeakReference<?>> lObjectMap = null;
		try
		{
			try {
				Class<?> javaHash = vtkObject.class.forName("vtk.vtkGlobalJavaHash");
				Field f = javaHash.getDeclaredField("GarbageCollector");
				lGC = (vtkJavaGarbageCollector) f.get(null);
				lDeleteAll = javaHash.getDeclaredMethod("GC");
				lObjectMap = (ConcurrentHashMap)
					javaHash.getDeclaredField("PointerToReference").get(null);
			} catch (ClassNotFoundException ex) {
				Field f = vtkObjectBase.class.getDeclaredField("JAVA_OBJECT_MANAGER");
				lObjectManager = f.get(null);
				Method m = lObjectManager.getClass().getDeclaredMethod("getAutoGarbageCollector");
				lGC = (vtkJavaGarbageCollector) m.invoke(lObjectManager);
				lDeleteAll = lObjectManager.getClass().getDeclaredMethod("deleteAll");
				lUnregisterMethod = lObjectManager.getClass().getDeclaredMethod("unRegisterJavaObject", Long.class);
			}
		}
		catch(Exception ex)
		{
			LOGGER.log(Level.SEVERE, null, ex);
		}
		GC = lGC;
		DELETE_ALL_METHOD = lDeleteAll;
		OBJECT_MANAGER = lObjectManager;
		UNREGISTER_METHOD = lUnregisterMethod;
		OBJECT_MAP = lObjectMap;
	}

	private VTKMemoryManager(){}

	/**
	 * vtkGlobalJavaHash.GC() if vtk &lg; 5.8, else
     * vtkObject.JAVA_OBJECT_MANAGER.deleteAll();
	 */
	public static void deleteAll()
	{
		try {
			DELETE_ALL_METHOD.invoke(OBJECT_MANAGER);
		} catch (IllegalAccessException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (IllegalArgumentException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (InvocationTargetException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Tag a vtkObjectBase so it will be deleted at the native level at the
	 * next vtkGlobalJavaHash.GC() call. Such call is normally triggered by
	 * vtkJavaGarbageCollector.
	 * As JVM doesn't monitor the native memory usage, VTK object may never be
	 * deleted if the Java garbage collector is not triggered by other java
	 * object creation. This methods allows to manually tag a vtkObject as
	 * removable without actually removing it.
	 */
	public static void delete(vtkObjectBase o)
	{
		if(OBJECT_MAP != null)
		{
			WeakReference<?> ref = (WeakReference) OBJECT_MAP.get(o.GetVTKId());
			ref.clear();
		}
		else //OBJECT_MANAGER != null
		{
			try {
				UNREGISTER_METHOD.invoke(OBJECT_MANAGER, o.GetVTKId());
			} catch (IllegalAccessException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			} catch (IllegalArgumentException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			} catch (InvocationTargetException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}
}
