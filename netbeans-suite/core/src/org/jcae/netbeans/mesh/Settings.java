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
 * (C) Copyright 2008, by EADS France
 */

package org.jcae.netbeans.mesh;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import org.jcae.mesh.Mesher;
import org.openide.ErrorManager;
import org.openide.options.SystemOption;
import org.openide.util.Lookup;

/**
 * @author Jerome Robert
 *
 */
public class Settings extends SystemOption
{
	static final long serialVersionUID = 2437343054662293472L;
	static public Settings getDefault()
	{
		return Lookup.getDefault().lookup(Settings.class);
	}
	private String javaVirtualMachine;
	private String maximumMemory="1500m";	
	private String mesherJar;
	private String[] customJVMParameters=new String[0];
	private String[] customMesherParameters=new String[0];
	private boolean runInSameJVM=Boolean.getBoolean("jcae.netbeans.mesh.samejvm"); 
	
	public Settings()
	{	
		try
		{
			mesherJar=new File(
				Mesher.class.getProtectionDomain().getCodeSource().
				getLocation().toURI()).getPath();
		}
		catch (URISyntaxException e)
		{
			ErrorManager.getDefault().notify(e);
		}
		javaVirtualMachine=System.getProperty("java.home");
		
	}
	
	/* (non-Javadoc)
	 * @see org.openide.options.SystemOption#displayName()
	 */
	public String displayName()
	{
		return "Mesher settings";
	}

	/**
	 * Return a command line to execute the mesher
	 */
	public String[] getCommandLine()
	{		
		String javaExe=new File(new File(javaVirtualMachine, "bin"), "java").getPath();
		ArrayList<String> toReturn=new ArrayList<String>();
		toReturn.add(javaExe);
		toReturn.add("-Xmx"+maximumMemory);
		toReturn.add("-Djava.util.logging.config.file="+System.getProperty("java.util.logging.config.file"));
		toReturn.addAll(Arrays.asList(getCustomJVMParameters()));
		toReturn.add("-jar");
		toReturn.add(mesherJar);
		return toReturn.toArray(new String[toReturn.size()]);
	}

	/**
	 * Return a command line to execute other algo in the jcae.jar archive
	 */
	public String[] getCommandLineAlgo()
	{		
		String javaExe=new File(new File(javaVirtualMachine, "bin"), "java").getPath();
		ArrayList<String> toReturn=new ArrayList<String>();
		toReturn.add(javaExe);
		toReturn.add("-Xmx"+maximumMemory);
		toReturn.add("-Djava.util.logging.config.file="+System.getProperty("java.util.logging.config.file"));
		toReturn.addAll(Arrays.asList(getCustomJVMParameters()));
		toReturn.add("-classpath");
		toReturn.add(mesherJar);
		return toReturn.toArray(new String[toReturn.size()]);
	}
	
	
	public String getJavaVirtualMachine()
	{
		return javaVirtualMachine;
	}
	
	public String getMaximumMemory()
	{
		return maximumMemory;
	}
	
	public File getMesherJar()
	{
		return new File(mesherJar);
	}
	
	
	public void setJavaVirtualMachine(String javaVirtualMachine)
	{
		this.javaVirtualMachine = javaVirtualMachine;
	}
	
	public void setMaximumMemory(String maximumMemory)
	{
		this.maximumMemory = maximumMemory;
	}
	
	public void setMesherJar(File mesherJar)
	{
		this.mesherJar = mesherJar.getPath();
	}
	public boolean isRunInSameJVM()
	{
		return runInSameJVM;
	}
	public void setRunInSameJVM(boolean runInSameJVM)
	{
		this.runInSameJVM = runInSameJVM;
	}
	/**
	 * @return Returns the customJVMParameters.
	 */
	public String[] getCustomJVMParameters()
	{
		return customJVMParameters;
	}
	/**
	 * @param customJVMParameters The customJVMParameters to set.
	 */
	public void setCustomJVMParameters(String[] customJVMParameters)
	{
		this.customJVMParameters = customJVMParameters;
	}
	/**
	 * @return Returns the customMesherParameters.
	 */
	public String[] getCustomMesherParameters()
	{
		return customMesherParameters;
	}
	/**
	 * @param customMesherParameters The customMesherParameters to set.
	 */
	public void setCustomMesherParameters(String[] customMesherParameters)
	{
		this.customMesherParameters = customMesherParameters;
	}
}
