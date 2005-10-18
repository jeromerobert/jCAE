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
