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
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.netbeans.mesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.log4j.*;
import org.jcae.mesh.Mesher;
import org.jcae.netbeans.Utilities;
import org.openide.ErrorManager;
import org.openide.util.Cancellable;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;


public class JCAEMesher implements Runnable, Cancellable
{	
	/**
	 * Thread to do a pipe between an InputStream and an OutputWriter
	 */
	private static class Redirector implements Runnable
	{
		private OutputWriter ow;
		private BufferedReader reader;

		/**
		 * @param inputStream
		 * @param ow
		 */
		public Redirector(InputStream inputStream, OutputWriter ow)
		{
			this.reader=new BufferedReader(new InputStreamReader(inputStream));
			this.ow=ow;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			System.out.println("Starting "+reader+" "+ow);
			try
			{
				String buffer=reader.readLine();
				while(buffer!=null)
				{
					ow.println(buffer);
					System.out.println(buffer);
					buffer=reader.readLine();
				}
			}
			catch(IOException ex)
			{
				ErrorManager.getDefault().notify(ex);
			}
		}
	}
	private static transient int ioProviderCounter=1;

	private Mesh mesh;
	private Process process;
	private String reference;
	private Thread thread;
	
	public JCAEMesher(String referenceDir, Mesh mesh)
	{
		reference=referenceDir;
		this.mesh=mesh;
	}
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */		
	public void run()
	{
		try
		{
			InputOutput io=IOProvider.getDefault().getIO("jCAE Mesher "+ioProviderCounter, true);				
			ioProviderCounter++;
			
			final String brepName=Utilities.absoluteFileName(mesh.getGeometryFile(), reference);
			String xmlDir=Utilities.absoluteFileName(mesh.getMeshFile(), reference);
			
			final OutputWriter ow=io.getOut();
			if(Settings.getDefault().isRunInSameJVM())
			{				
				final Appender a=new WriterAppender(new PatternLayout("%-4r [%-5p] %c- %m%n"), ow);
				BasicConfigurator.configure(a);
				Logger.getRootLogger().setLevel(Level.INFO);
				thread=Thread.currentThread();
				Mesher.main(new String[]{brepName, xmlDir, ""+mesh.getEdgeLength(), ""+mesh.getDeflection()});
				thread=null;
				a.close();
			}
			else
			{											
				ArrayList array=new ArrayList();
				array.addAll(Arrays.asList(Settings.getDefault().getCommandLine()));
				array.add(brepName);
				array.add(xmlDir);
				array.add(""+mesh.getEdgeLength());
				array.add(""+mesh.getDeflection());
				array.addAll(Arrays.asList(Settings.getDefault().getCustomMesherParameters()));
				String[] cmdLine = (String[]) array.toArray(new String[array.size()]);
				System.err.println("jcae-netbeans-mesh: Running command line "+array);					
				process=Runtime.getRuntime().exec(cmdLine);										
				new Thread(new Redirector(process.getInputStream(), ow)).start();
				new Thread(new Redirector(process.getErrorStream(), ow)).start();
				process.waitFor();
				process=null;
			}
			
			ow.close();			
		}
		catch(Exception ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
	}			
	
	public String toString()
	{
		return "jCAE triangle mesher";
	}

	public boolean cancel()
	{
		try
		{
			if(process!=null)
			{
				process.destroy();
				return true;
			}
			
			if(thread!=null)
			{
				thread.interrupt();
			}			
		}
		catch(Exception ex)
		{
			ErrorManager.getDefault().notify(ex);
			return false;
		}
		return false;
	}
}
