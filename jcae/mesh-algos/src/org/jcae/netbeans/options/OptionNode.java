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
 * (C) Copyright 2012, by EADS France
 */

package org.jcae.netbeans.options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openide.modules.InstalledFileLocator;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;


/**
 *
 * @author Jerome Robert
 */
public final class OptionNode extends AbstractNode {

	private final static PrefProperty MAX_MEMORY =
		new PrefProperty("maximumMemory", "Maximum memory", "1000m",
		"Maximum Java heap size in the mesher. See -Xmx in Java documentation.");
	private final static PrefProperty JVM_OPTIONS =
		new PrefProperty("jvmOptions", "JVM options", new String[0],
		"Java custom options. See Java documentation.");
	public final static PrefProperty AMIBE_OPTIONS =
		new PrefProperty("amibeOptions", "Mesher options", new String[0],
		"Amibe custom options.");
	public final static PrefProperty SAME_JVM =
		new PrefProperty("runInSameJVM", "Run in same JVM", false);
	public final static PrefProperty REFRESH_THRESHOLD = new PrefProperty(
		"amibeARThreshold", "Mesh auto-refresh threshold", 400000,
		"Mesh whose number of triangles is greater than this value will not "+
		"automatically be refresh in the 3D view");

	public OptionNode() {
		super(Children.LEAF);
	}

	@Override
	public Sheet.Set[] getPropertySets() {
		Sheet.Set mesher = new Sheet.Set();
		mesher.setName("Mesher Settings");
		mesher.put(MAX_MEMORY);
		mesher.put(JVM_OPTIONS);
		mesher.put(AMIBE_OPTIONS);
		mesher.put(SAME_JVM);
		Sheet.Set viewer = new Sheet.Set();
		viewer.setName("Viewer 3D Settings");
		viewer.put(REFRESH_THRESHOLD);
		return new Sheet.Set[]{mesher, viewer};
	}

	public static List<String> getJVMOptions() {
		ArrayList<String> toReturn = new ArrayList<String>();
		toReturn.add("-Xmx" + MAX_MEMORY.getValue());
		String logPath = InstalledFileLocator.getDefault().
			locate("etc/logging.properties", "org.jcae.netbeans.mesh", false).
			getAbsolutePath();
		toReturn.add("-Djava.util.logging.config.file=" + logPath);
		toReturn.addAll(Arrays.asList((String[]) JVM_OPTIONS.getValue()));
		return toReturn;
	}
}
