package org.jcae.netbeans.options;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.prefs.Preferences;
import org.openide.nodes.Node.Property;
import org.openide.util.NbPreferences;

/**
 *
 * @author u
 */
public class PrefProperty extends Property {
    private final String prefName;
    private final Object defValue;
    private final Preferences preferences;

    public PrefProperty(String prefName, String name, Object defValue, String description, Class moduleClass)
    {
        super(defValue.getClass());
        setName(name);
        this.prefName = prefName;
        this.defValue = defValue;
        this.setShortDescription(description);
        preferences = NbPreferences.forModule(moduleClass);
    }

    public PrefProperty(String prefName, String name, Object defValue, Class moduleClass)
    {
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
    public Object getValue() throws IllegalAccessException,
        InvocationTargetException {        
        if(Double.class.isAssignableFrom(getValueType()))
            return (Double) preferences.getDouble(prefName, (Double)defValue);
        else if(Boolean.class.isAssignableFrom(getValueType()))
            return (Boolean) preferences.getBoolean(prefName, (Boolean)defValue);
        else if(Integer.class.isAssignableFrom(getValueType()))
            return (Integer) preferences.getInt(prefName, (Integer)defValue);
        else if(File.class.isAssignableFrom(getValueType()))
            return new File(preferences.get(prefName, defValue.toString()));
        else
            return preferences.get(prefName, (String)defValue);
    }

    @Override
    public void setValue(Object val) throws IllegalAccessException,
        IllegalArgumentException, InvocationTargetException {
        preferences.put(prefName, val.toString());
    }
}