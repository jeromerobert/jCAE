package org.jcae.netbeans.mesh;

import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.beans.SimpleBeanInfo;

// This bean is its own BeanInfo
// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4316819
// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4651485

public class DecimateParameter extends SimpleBeanInfo
{
	private static final int PROPERTY_tolerance = 0;	

	private double tolerance = 0.1;

	public BeanDescriptor getBeanDescriptor()
	{
		BeanDescriptor toReturn=new BeanDescriptor(DecimateParameter.class);
		toReturn.setDisplayName("Decimation tolerance");
		return toReturn;
	}

	public PropertyDescriptor[] getPropertyDescriptors()
	{
		PropertyDescriptor[] properties = new PropertyDescriptor[1];
		try
		{
			properties[PROPERTY_tolerance] = new PropertyDescriptor("tolerance", DecimateParameter.class);
			properties[PROPERTY_tolerance].setPreferred(true);
			properties[PROPERTY_tolerance].setDisplayName("Tolerance");
			properties[PROPERTY_tolerance].setShortDescription("Tolerance");
			properties[PROPERTY_tolerance].setConstrained(true);
		} 
		catch (IntrospectionException e)
		{
			e.printStackTrace();
		}
		return properties;
	}
	
	
	public void setTolerance(double level) throws PropertyVetoException
	{
		if(level<=0)
			throw new PropertyVetoException(
				"Must be > 0",
				new PropertyChangeEvent(this, null, null, null));
		this.tolerance = level;
	}
	
	public double getTolerance()
	{
		return tolerance;
	}
}