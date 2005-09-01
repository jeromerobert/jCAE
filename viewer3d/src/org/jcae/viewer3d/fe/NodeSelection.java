package org.jcae.viewer3d.fe;

public interface NodeSelection extends Cloneable
{
	int getDomainID();
	
	/** The number of selected nodes */
	int getCount();
	/**
	 * The id of the element owning the node
	 * @param id the id of the node (between 0 and getCound()-1) 
	 */
	int getElementID(int id);

	/**
	 * The id of node in the element
	 * @param id the id of the node (between 0 and getCound()-1)
	 * @return 0,1 or 2 for triangles, 0 or 1 for beams 
	 */
	byte getNodeID(int id);
	
	Object clone();
}
