/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


 */

package org.jcae.mesh;

//import java.util.HashSet;
import org.jcae.mesh.util.HashSet;

public class MeshHypothesis {
    public final static int CONSTRAINT=0;
    public final static int ALGO=1;

    private String name;
    private long id;
    private int type;
    private HashSet compatibleHypothesis;

    public final static int BEAM = 2;
    public final static int TRIA3 = 3;




	/**
	 * Constructor with a name, an Id and a constraint type.
	 * @param name : a String, the name
	 * @param id : a long value, the Id
	 * @param type : an integer value, the constraint value
	 */
    public MeshHypothesis(String name, long id,int type) {
        this.name = name;
        this.id = id;
		this.type=type;
    }

	/**
	 * Get the Id
	 * @return long : the Id
	 */
    public long getId() {
        return id;
    }

	/**
	 * Set the Id.
	 * @param id : a long value, the Id
	 */
    public void setId(long id) {
        this.id = id;
    }

	/**
	 * Get the name.
	 * @return String : the name
	 */
    public String getName() {
        return name;
    }

	/**
	 * Set the name.
	 * @param name : a String, the name
	 */
    public void setName(String name) {
        this.name = name;
    }

	/**
	 * Retrieves the constraint type.
	 * @return int : the constraint type
	 */
    public int getType(){
	return type;
    }
    
	/**
	 * Set the constraint type.
	 * @param type : an integer value, the type of the constraint
	 */
    public void setType(int type)
    {
    	this.type= type;
    }

	/**
	 * Method getCompatibleHypothesis.
	 * @return HashSet 
	 * TODO
	 */
    public HashSet getCompatibleHypothesis() {
        return null;
    }

	/**
	 * Method setCompatibleHypothesis.
	 * @param compatibleHypothesis : a HashSet of hypothsis.
	 * TODO
	 */
    public void setCompatibleHypothesis(HashSet compatibleHypothesis) {
	this.compatibleHypothesis=compatibleHypothesis;
    }

}



