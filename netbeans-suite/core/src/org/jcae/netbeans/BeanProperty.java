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
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.netbeans;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import org.openide.nodes.PropertySupport;

public class BeanProperty extends PropertySupport.Reflection
{
	public BeanProperty(Object bean, String property) throws NoSuchMethodException, IntrospectionException
	{
		super(bean, getClass(bean, property), property);
		setDisplayName(Utilities.pretty(property));
	}

	private static Class getClass(Object bean, String property) throws IntrospectionException
	{
		PropertyDescriptor[] pd = Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors();
		for(int i=0; i<pd.length; i++)
		{
			if(pd[i].getName().equals(property))
			{
				if(pd[i].getReadMethod()!=null)
					return pd[i].getReadMethod().getReturnType();
				else
					return pd[i].getWriteMethod().getParameterTypes()[0];
			}
		}
		return String.class;
	}
}
