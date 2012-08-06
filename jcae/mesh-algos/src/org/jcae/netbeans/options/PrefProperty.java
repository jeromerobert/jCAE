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
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.openide.nodes.Node.Property;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;

/**
 *
 * @author Mohit Garg
 */
public class PrefProperty extends Property {

	private final Object defValue;
	private final Preferences preferences;
	private final String prefName;

	public PrefProperty(String prefName, String name, Object defValue,
		String description)
	{
		super(defValue.getClass());
		setName(name);
		this.prefName = "jcae." + prefName;
		this.defValue = defValue;
		this.setShortDescription(description);
		preferences = NbPreferences.forModule(getClass());
	}

	public PrefProperty(String prefName, String name, Object defValue) {
		this(prefName, name, defValue, null);
	}

	@Override
	public boolean canRead() {
		return true;
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	public Preferences getPreferences()
	{
		return preferences;
	}

	public String getPreferenceName()
	{
		return prefName;
	}

	@Override
	public Object getValue() {
		if (Double.class.isAssignableFrom(getValueType())) {
			return Double.valueOf(preferences.getDouble(prefName, (Double) defValue));
		} else if (Boolean.class.isAssignableFrom(getValueType())) {
			return (Boolean) preferences.getBoolean(prefName, (Boolean) defValue);
		} else if (Integer.class.isAssignableFrom(getValueType())) {
			return (Integer) preferences.getInt(prefName, (Integer) defValue);
		} else if (File.class.isAssignableFrom(getValueType())) {
			return new File(preferences.get(prefName, defValue.toString()));
		} else if (String.class.isAssignableFrom(getValueType())) {
			return preferences.get(prefName, (String) defValue);
		} else if (String[].class.isAssignableFrom(getValueType())) {
			List<String> l = new ArrayList<String>();
			//Check if preference available as prefName.0, prefName.1, ...
			int i = 0;
			String prefVal = preferences.get(prefName + "." + i, null);
			while (prefVal != null) {
				l.add(i++, prefVal);
				prefVal = preferences.get(prefName + "." + i, null);
			}
			return l.toArray(new String[l.size()]);
		}

		throw new IllegalStateException("Unhandled class: "+getValueType());
	}

	@Override
	public void setValue(Object val) {
		if (String[].class.isAssignableFrom(val.getClass())) {
			try {
				for(String k: preferences.keys())
					if(k.startsWith(prefName))
						preferences.remove(k);
			} catch (BackingStoreException ex) {
				Exceptions.printStackTrace(ex);
			}
			for (int i = 0; i < ((String[]) val).length; i++) {
				preferences.put(prefName + "." + i, ((String[]) val)[i]);
			}
		} else {
			preferences.put(prefName, val.toString());
		}
	}
}