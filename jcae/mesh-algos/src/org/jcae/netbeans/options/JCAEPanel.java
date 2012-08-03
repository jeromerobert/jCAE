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

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openide.explorer.propertysheet.PropertySheet;
import org.openide.modules.InstalledFileLocator;
import org.openide.nodes.*;
import org.openide.util.Exceptions;

/**
 *
 * @author Mohit Garg
 *
 */
public final class JCAEPanel extends PropertySheet {

	private static JCAEPanel instance;

	static public JCAEPanel getDefault() {
		if (instance == null) {
			instance = new JCAEPanel();
		}

		return instance;
	}

	public JCAEPanel() {
		//initComponents();
		// TODO listen to changes in form fields and call controller.changed()
		AbstractNode node = new AbstractNode(Children.LEAF) {
			@Override
			public PropertySet[] getPropertySets() {
				return createPropertySet();
			}
		};
		setNodes(new Node[]{node});
		setDescriptionAreaVisible(true);
		setPreferredSize(new Dimension(0, 250));
	}
	private String maximumMemory = "1000m";
	private String[] customJVMParameters = new String[0];
	private String[] customMesherParameters = new String[0];
	private boolean runInSameJVM = Boolean.getBoolean(
		"jcae.netbeans.mesh.samejvm");
	private Double polygonOffset = 20.0;
	private Double polygonOffsetFactor = 2.0;
	private int amibeARThreshold = 400000;

	/**
	 * @return the polygonOffset
	 */
	public Double getPolygonOffset() {
		return polygonOffset;
	}

	/**
	 * @param polygonOffset the polygonOffset to set
	 */
	public void setPolygonOffset(Double polygonOffset) {
		this.polygonOffset = polygonOffset;
	}

	/**
	 * @return the polygonOffsetFactor
	 */
	public Double getPolygonOffsetFactor() {
		return polygonOffsetFactor;
	}

	/**
	 * @param polygonOffsetFactor the polygonOffsetFactor to set
	 */
	public void setPolygonOffsetFactor(Double polygonOffsetFactor) {
		this.polygonOffsetFactor = polygonOffsetFactor;
	}

	/**
	 * @return the amibeARThreshold
	 */
	public int getAmibeARThreshold() {
		return amibeARThreshold;
	}

	/**
	 * @param amibeARThreshold the amibeARThreshold to set
	 */
	public void setAmibeARThreshold(int AmibeARThreshold) {
		this.amibeARThreshold = AmibeARThreshold;
	}

	public Object getOption(String prefName, Class moduleClass) {
		try {
			PrefProperty p = new PrefProperty(prefName, prefName, this,
				moduleClass);
			return p.getValue();
		} catch (NoSuchMethodException ex) {
			Exceptions.printStackTrace(ex);
		}
		return null;
	}

	private Sheet.Set[] createPropertySet() {
		Sheet.Set[] r = new Sheet.Set[3];
		r[0] = new Sheet.Set();
		r[0].setName("Mesher Settings");
		try {
			PrefProperty p1 = new PrefProperty("MaximumMemory", "MaximumMemory",
				this, String.class);
			r[0].put(p1);

			PrefProperty p2 = new PrefProperty("CustomJVMParameters",
				"CustomJVMParameters", this, String[].class);
			r[0].put(p2);

			PrefProperty p3 = new PrefProperty("CustomMesherParameters",
				"CustomMesherParameters", this, String[].class);
			r[0].put(p3);

			PrefProperty p4 = new PrefProperty("RunInSameJVM", "RunInSameJVM",
				this, boolean.class);
			r[0].put(p4);

			PrefProperty p = new PrefProperty("CommandLineAlgo",
				"CommandLineAlgo", this, String[].class);
			r[0].put(p);

		} catch (Exception ex) {
			Exceptions.printStackTrace(ex);
		}

		r[1] = new Sheet.Set();
		r[1].setName("Viewer 3D Settings");
		try {
			PrefProperty p1 = new PrefProperty("PolygonOffset", "PolygonOffset",
				this, Double.class);
			r[1].put(p1);

			PrefProperty p2 = new PrefProperty("PolygonOffsetFactor",
				"PolygonOffsetFactor", this, Double.class);
			r[1].put(p2);

		} catch (Exception ex) {
			Exceptions.printStackTrace(ex);
		}

		r[2] = new Sheet.Set();
		r[2].setName("Amibe Settings");
		try {
			PrefProperty p1 = new PrefProperty("AmibeARThreshold",
				"AmibeARThreshold", this, int.class);
			r[2].put(p1);

		} catch (Exception ex) {
			Exceptions.printStackTrace(ex);
		}
		return r;
	}

	/**
	 * Return a command line to execute other algo in the jcae.jar archive
	 */
	public String[] getCommandLineAlgo() {
		String javaExe = new File(new File(System.getProperty("java.home"),
			"bin"), "java").getPath();
		List<String> toReturn = null;
		toReturn = newParameters();
		toReturn.add(0, javaExe);
		return toReturn.toArray(new String[toReturn.size()]);

	}

	//dummy method for Bean behavior
	public void setCommandLineAlgo(String[] dummy) {
	}

	public List<String> parameters() {
		ArrayList<String> toReturn = new ArrayList<String>();
		toReturn.add("-Xmx" + maximumMemory);
		String logPath = InstalledFileLocator.getDefault().
			locate("etc/logging.properties", "org.jcae.netbeans.mesh", false).
			getAbsolutePath();
		toReturn.add("-Djava.util.logging.config.file=" + logPath);
		toReturn.addAll(Arrays.asList(getCustomJVMParameters()));
		return toReturn;
	}

	public List<String> newParameters() {
		ArrayList<String> toReturn = new ArrayList<String>();
		toReturn.add(
			"-Xmx" + getOption("MaximumMemory", String.class).toString());
		String logPath = InstalledFileLocator.getDefault().
			locate("etc/logging.properties", "org.jcae.netbeans.mesh", false).
			getAbsolutePath();
		toReturn.add("-Djava.util.logging.config.file=" + logPath);
		toReturn.addAll(Arrays.asList((String[]) getOption("CustomJVMParameters",
			String[].class)));
		return toReturn;
	}

	public String getMaximumMemory() {
		return maximumMemory;
	}

	public void setMaximumMemory(String maximumMemory) {
		this.maximumMemory = maximumMemory;
	}

	public boolean isRunInSameJVM() {
		return runInSameJVM;
	}

	public void setRunInSameJVM(boolean runInSameJVM) {
		this.runInSameJVM = runInSameJVM;
	}

	/**
	 * @return Returns the customJVMParameters.
	 */
	public String[] getCustomJVMParameters() {
		return customJVMParameters;
	}

	/**
	 * @param customJVMParameters The customJVMParameters to set.
	 */
	public void setCustomJVMParameters(String[] customJVMParameters) {
		this.customJVMParameters = customJVMParameters;
	}

	/**
	 * @return Returns the customMesherParameters.
	 */
	public String[] getCustomMesherParameters() {
		return customMesherParameters;
	}

	/**
	 * @param customMesherParameters The customMesherParameters to set.
	 */
	public void setCustomMesherParameters(String[] customMesherParameters) {
		this.customMesherParameters = customMesherParameters;
	}
}
