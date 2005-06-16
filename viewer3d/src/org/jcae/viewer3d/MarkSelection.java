package org.jcae.viewer3d;

/**
 * @author Jerome Robert
 *
 */
public interface MarkSelection
{
	Object[] getMarks();
	int[] getMarksID(Object marks);
	int getDomainID();
}
