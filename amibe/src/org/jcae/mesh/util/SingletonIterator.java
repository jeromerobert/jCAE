/*
 * SingletonIterator.java
 *
 * Created on October 3, 2003, 5:36 PM
 */

package org.jcae.mesh.util;
import java.util.Iterator;
/**
 *
 * @author  robert
 */
public class SingletonIterator implements Iterator
{
	Object o;
	/** Creates a new instance of SingletonIterator */
	public SingletonIterator(Object object)
	{
		o=object;
	}
	
	public boolean hasNext()
	{
		return o!=null;
	}
	
	public Object next()
	{
		Object oo=o;
		o=null;
		return oo;
	}
	
	public void remove()
	{
	}	
}
