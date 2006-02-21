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
	private static final int PROPERTY_tolerance = 2;	
    private static final int PROPERTY_triangle = 0;
    private static final int PROPERTY_useTolerance = 1;
	
	private double tolerance = 0.1;
	
	public BeanDescriptor getBeanDescriptor()
	{
		BeanDescriptor toReturn=new BeanDescriptor(DecimateParameter.class);
		toReturn.setDisplayName("Decimation parameters");
		return toReturn;
	}

	public PropertyDescriptor[] getPropertyDescriptors()
	{
		PropertyDescriptor[] properties = new PropertyDescriptor[3];
		try
		{
			properties[PROPERTY_tolerance] = new PropertyDescriptor("tolerance", DecimateParameter.class);			
			properties[PROPERTY_tolerance].setDisplayName("Tolerance");
			properties[PROPERTY_tolerance].setShortDescription("Tolerance");
			properties[PROPERTY_tolerance].setConstrained(true);
			
            properties[PROPERTY_triangle] = new PropertyDescriptor ( "triangle", org.jcae.netbeans.mesh.DecimateParameter.class, "getTriangle", "setTriangle" ); // NOI18N
            properties[PROPERTY_triangle].setPreferred(true);
			properties[PROPERTY_triangle].setDisplayName ( "Desired number of triangles" );
            properties[PROPERTY_triangle].setShortDescription ( "Desired number of triangles" );
            
			properties[PROPERTY_useTolerance] = new PropertyDescriptor ( "useTolerance", org.jcae.netbeans.mesh.DecimateParameter.class, "isUseTolerance", "setUseTolerance" ); // NOI18N
            properties[PROPERTY_useTolerance].setDisplayName ( "Use tolerance" );
            properties[PROPERTY_useTolerance].setShortDescription ( "Use tolerance instead of number of triangles" );
			
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

	/**
	 * Holds value of property triangle.
	 */
	private int triangle=10000;

	/**
	 * Getter for property triangle.
	 * @return Value of property triangle.
	 */
	public int getTriangle()
	{
		return this.triangle;
	}

	/**
	 * Setter for property triangle.
	 * @param triangle New value of property triangle.
	 */
	public void setTriangle(int triangle)
	{
		this.triangle = triangle;
	}

	/**
	 * Holds value of property useTolerance.
	 */
	private boolean useTolerance;

	/**
	 * Getter for property useTolerance.
	 * @return Value of property useTolerance.
	 */
	public boolean isUseTolerance()
	{
		return this.useTolerance;
	}

	/**
	 * Setter for property useTolerance.
	 * @param useTolerance New value of property useTolerance.
	 */
	public void setUseTolerance(boolean useTolerance)
	{
		this.useTolerance = useTolerance;
	}
}