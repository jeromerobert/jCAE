/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2019, by Airbus S.A.S.

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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.amibe.projection;

import org.jcae.mesh.amibe.algos3d.Skeleton;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;

import java.util.Collection;
import java.util.List;

/** Project points on the skeleton of a mesh */
public class SkeletonProjector {
    private final Skeleton skeleton;

    public SkeletonProjector(Mesh mesh) {
        skeleton = new Skeleton(mesh, 0);
    }

    public boolean moveToClosestEdge(Vertex v, int[] groups) {
        Collection<List<AbstractHalfEdge>> polylines;
        if(groups.length == 0)
            polylines = skeleton.getPolylines();
        else
            polylines = skeleton.getPolylines(groups);
        double minDistSqr = Double.POSITIVE_INFINITY;
        Location projection = new Location();
        Location bestProjection = new Location();
        boolean found = false;
        // TODO: This is O(n) for each vertex so O(n^2) to move all vertices. If this is too slow we should rework
        // TriangleKdTree to ObjectKdTree<Triangle or AbstractHalfEdge> and use it to find the closest edge.
        for(List<AbstractHalfEdge> p: polylines) {
            for(AbstractHalfEdge edge: p) {
                double distSqr = v.sqrDistance3D(edge.origin(), edge.destination(), projection);
                if(distSqr < minDistSqr) {
                    found = true;
                    bestProjection.moveTo(projection);
                    minDistSqr = distSqr;
                }
            }
        }
        v.moveTo(bestProjection);
        return found;
    }
}
