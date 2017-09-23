/*
 * Project Info:  http://jcae.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2017, by Airbus S.A.S
 */

package org.jcae.mesh.amibe.metrics;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Metric define by values on a point cloud.
 * There is no interpolation, the closest point is found using a kd-tree.
 * @author Jerome Robert
 */
public class PointCloudMetric extends MetricSupport.AnalyticMetric {
    private static class Point extends Location {
        /** The metric value at this point */
        public final double value;

        public Point(double x, double y, double z) {
            this(x, y, z, 0);
        }

        public Point(double x, double y, double z, double value) {
            super(x, y, z);
            this.value = value;
        }
    }

    /** The metric to use while searching in the kd-tree */
    private final Metric metric = new EuclidianMetric3D();
    private double defaultValue = 1;
    private KdTree<Point> kdTree;

    /**
     * Add a point cloud from a binary file.
     * The file is a native endian double float array with {x, y, z, metric}
     * records.
     * @param filename
     */
    public void readPointsFromFile(String filename) throws IOException {
        FileChannel fc = null;
        try {
            fc = new FileInputStream(filename).getChannel();
            // double is 8 bytes, there are 4 double by point
            int pointNumber = (int) (fc.size() / 4 / 8);
            kdTree = null;
            ArrayList<Point> points = new ArrayList<Point>(pointNumber);
            ByteBuffer bb = ByteBuffer.allocate((int) fc.size());
            bb.order(ByteOrder.nativeOrder());
            fc.read(bb);
            for(int i = 0; i < pointNumber; i++) {
                points.add(new Point(bb.getDouble(), bb.getDouble(),
                    bb.getDouble(), bb.getDouble()));
            }
            initKdTree(points);
        } finally {
            if(fc != null)
                fc.close();
        }
    }

    /** Lazy initializtion of the kdTree */
    private void initKdTree(Collection<Point> points) {
        double[] bbox = new double[6];
        for(int i = 0; i < 3; i++) {
            bbox[i] = Double.MAX_VALUE;
            bbox[i + 3] = Double.MIN_VALUE;
        }
        for(Point p: points) {
            bbox[0] = p.getX() < bbox[0] ? p.getX() : bbox[0];
            bbox[1] = p.getY() < bbox[1] ? p.getY() : bbox[1];
            bbox[2] = p.getZ() < bbox[2] ? p.getZ() : bbox[2];
            bbox[3] = p.getX() > bbox[3] ? p.getX() : bbox[3];
            bbox[4] = p.getY() > bbox[4] ? p.getY() : bbox[4];
            bbox[5] = p.getZ() > bbox[5] ? p.getZ() : bbox[5];
        }
        kdTree = new KdTree<Point>(bbox);
        for(Point p: points) {
            kdTree.add(p);
        }
    }

    /** Set the metric value to use if no support points can be found */
    public void setDefaultValue(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public double getTargetSize(double x, double y, double z, int groupId) {
        double r = defaultValue;
        if(kdTree != null) {
            Point l = kdTree.getNearVertex(metric, new Point(x, y, z));
            r = l == null ? r : l.value;
        }
        return r;
    }
}
