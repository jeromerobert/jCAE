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

package org.jcae.netbeans.ant;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
	
/**
 *
 * @author Jerome Robert
 */
public class AntVersion extends Task
{
	private String version;
	private String title;
	public void execute() throws BuildException
	{
		try
		{
			File root = getProject().getBaseDir();
			setProperty(root,
				"branding/core/core.jar/org/netbeans/core/startup/Bundle.properties",
				"currentVersion", version);
			setProperty(root,
				"branding/modules/org-netbeans-core.jar/org/netbeans/core/ui/Bundle.properties",
				"LBL_ProductInformation");
			setProperty(root,
				"branding/modules/org-netbeans-core-windows.jar/org/netbeans/core/windows/view/ui/Bundle.properties",
				"CTL_MainWindow_Title");
			setProperty(root,
				"branding/modules/org-netbeans-core-windows.jar/org/netbeans/core/windows/view/ui/Bundle.properties",
				"CTL_MainWindow_Title_No_Project");
			setProperty(root, "nbproject/project.properties", "app.title");
			setManifestVersion(root, "core/manifest.mf");
			setManifestVersion(root, "occjava-nb/manifest.mf");
		}
		catch (IOException ex)
		{
			throw new BuildException(ex);
		}
	}
	
	private void setManifestVersion(File root, String manifest) throws IOException
	{
		File f = new File(root, manifest);
		FileInputStream in = new FileInputStream(f);
		Manifest m = new Manifest(in);
		in.close();
		m.getMainAttributes().put(new Name("OpenIDE-Module-Specification-Version"), version);
		FileOutputStream out = new FileOutputStream(f);
		m.write(out);
		out.close();
	}
	
	private void setProperty(File root, String file, String propName) throws IOException
	{
		setProperty(root, file, propName, title);
	}

	private void setProperty(File root, String file, String propName, String value) throws IOException
	{
		Properties p = new Properties();
		File f = new File(root, file);
		FileReader fr = new FileReader(f);
		p.load(fr);
		fr.close();
		p.setProperty(propName, value);
		FileWriter fw = new FileWriter(f);
		p.store(fw, "");
		fw.close();
	}
	
	public void setVersion(String v)
	{
		version = v;
		title = "jCAE "+version;
	}
}
