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

package org.jcae.netbeans.options;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import org.openide.nodes.PropertySupport;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;

/**
 *
 * @author Mohit Garg
 *
 */
public class PrefProperty extends PropertySupport.Reflection {

	private final String prefName;
	private final Object defValue;
	private final Preferences preferences;
	private final String jCAEPrefName;

	public PrefProperty(String prefName, String name, Object defValue,
		String description, Class moduleClass) throws NoSuchMethodException {
		super(defValue, moduleClass, name);
		setName(name);
		this.prefName = prefName;
		this.jCAEPrefName = "jcae." + prefName;
		this.defValue = defValue;
		this.setShortDescription(description);
		preferences = NbPreferences.forModule(defValue.getClass());
	}

	public PrefProperty(String prefName, String name, Object defValue,
		Class moduleClass) throws NoSuchMethodException {
		this(prefName, name, defValue, null, moduleClass);
	}

	@Override
	public boolean canRead() {
		return true;
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	@Override
	public Object getValue() {
		Class cls;
		Method meth;
		Object retobj = null;
		try {
			String metPrefix = "";
			if (getValueType().isAssignableFrom(boolean.class)) {
				metPrefix = "is";
			} else {
				metPrefix = "get";
			}

			char firstChar = prefName.charAt(0);
			String metName = prefName.replaceFirst(String.valueOf(firstChar),
				String.valueOf(firstChar).toUpperCase());
			metName = metPrefix + metName;
			cls = Class.forName(defValue.getClass().getCanonicalName());
			try {
				meth = cls.getMethod(metName, (Class[]) null);
				retobj = meth.invoke(defValue, (Object[]) null);
			} catch (IllegalAccessException ex) {
				Exceptions.printStackTrace(ex);
			} catch (IllegalArgumentException ex) {
				Exceptions.printStackTrace(ex);
			} catch (InvocationTargetException ex) {
				Exceptions.printStackTrace(ex);
			} catch (NoSuchMethodException ex) {
				Exceptions.printStackTrace(ex);
			} catch (SecurityException ex) {
				Exceptions.printStackTrace(ex);
			}
		} catch (ClassNotFoundException ex) {
			Exceptions.printStackTrace(ex);
		}

		if (Double.class.isAssignableFrom(getValueType())) {
			return (Double) preferences.getDouble(jCAEPrefName, (Double) retobj);
		} else if (boolean.class.isAssignableFrom(getValueType())) {
			return (boolean) preferences.getBoolean(jCAEPrefName,
				(Boolean) retobj);
		} else if (Integer.class.isAssignableFrom(getValueType())) {
			return (Integer) preferences.getInt(jCAEPrefName, (Integer) retobj);
		} else if (int.class.isAssignableFrom(getValueType())) {
			return preferences.getInt(jCAEPrefName,
				Integer.parseInt((retobj.toString())));
		} else if (File.class.isAssignableFrom(getValueType())) {
			return new File(preferences.get(jCAEPrefName, retobj.toString()));
		} else if (String.class.isAssignableFrom(getValueType())) {
			return preferences.get(jCAEPrefName, (String) retobj);
		} else if (String[].class.isAssignableFrom(getValueType())) {
			List<String> l = new ArrayList<String>();
			//Check if preference available as prefName.0, prefName.1, ...
			int i = 0;
			while (true) {
				String prefVal = preferences.get(jCAEPrefName + "." + i, "NA");
				if (prefVal.compareTo("NA") != 0) {
					l.add(i++, prefVal);
				} else {
					break;
				}
			}
			if (!l.isEmpty()) {
				return l.toArray(new String[l.size()]);
			} else {
				return (String[]) retobj;
			}
		}

		return null;
	}

	@Override
	public void setValue(Object val) throws IllegalAccessException,
		IllegalArgumentException, InvocationTargetException {
		if (val.getClass().isAssignableFrom(String[].class)) {
			for (int i = 0; i < ((String[]) val).length; i++) {
				preferences.put(jCAEPrefName + "." + i,
					((String[]) val)[i].toString());
			}
		} else {
			preferences.put(jCAEPrefName, val.toString());
		}
	}
}