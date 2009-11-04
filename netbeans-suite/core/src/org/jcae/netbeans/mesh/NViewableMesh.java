/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jcae.netbeans.mesh;

import org.jcae.vtk.ViewableMesh;
import org.openide.nodes.Node;

/**
 *
 * @author Jerome Robert
 */
public class NViewableMesh extends ViewableMesh {
	private final Node node;

	public NViewableMesh(org.jcae.vtk.Mesh mesh, Node node) {
		super(mesh);
		this.node = node;
	}

	public Node getNode()
	{
		return node;
	}
}
