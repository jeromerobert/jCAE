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

public class OEMMParameters extends SimpleBeanInfo
{
	private static final int PROPERTY_level = 0;
	private static final int PROPERTY_triangle = 1;

	private int level = 6;

	private int triangle = 50000;

	public int getLevel()
	{
		return this.level;
	}

	public BeanDescriptor getBeanDescriptor()
	{
		BeanDescriptor toReturn=new BeanDescriptor(OEMMParameters.class);
		toReturn.setDisplayName("OEMM Parameters");
		return toReturn;
	}

	public PropertyDescriptor[] getPropertyDescriptors()
	{
		PropertyDescriptor[] properties = new PropertyDescriptor[2];
		try
		{
			properties[PROPERTY_level] = new PropertyDescriptor("level", OEMMParameters.class);
			properties[PROPERTY_level].setPreferred(true);
			properties[PROPERTY_level].setDisplayName("Octree depth");
			properties[PROPERTY_level].setShortDescription("Octree depth");
			properties[PROPERTY_level].setConstrained(true);
			properties[PROPERTY_triangle] = new PropertyDescriptor("triangle", OEMMParameters.class);
			properties[PROPERTY_triangle].setPreferred(true);
			properties[PROPERTY_triangle].setDisplayName("Number of triangle by octree node");
			properties[PROPERTY_triangle].setShortDescription("Number of triangle by octree node");
			properties[PROPERTY_triangle].setConstrained(true);
		}  catch (IntrospectionException e)
		{
			e.printStackTrace();
		}
		return properties;
	}
	
	public int getTriangle()
	{
		return this.triangle;
	}
	
	public void setLevel(int level) throws PropertyVetoException
	{
		if(level<=0)
			throw new PropertyVetoException(
				"Must be > 0",
				new PropertyChangeEvent(this, null, null, null));
		this.level = level;
	}

	public void setTriangle(int triangle) throws PropertyVetoException
	{
		if(level<=0)
			throw new PropertyVetoException(
				"Must be > 0",
				new PropertyChangeEvent(this, null, null, null));

		this.triangle = triangle;
	}
}