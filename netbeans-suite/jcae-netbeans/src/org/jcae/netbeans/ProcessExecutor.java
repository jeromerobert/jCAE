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
 * (C) Copyright 2006, by EADS CRC
 */

package org.jcae.netbeans;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.ErrorManager;
import org.openide.util.Cancellable;
import org.openide.util.RequestProcessor;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

/**
 *
 * @author jerome
 */
public class ProcessExecutor implements Runnable, Cancellable
{	
	/**
	 * Thread to do a pipe between an InputStream and an OutputWriter
	 */
	private static class Redirector implements Runnable
	{
		private OutputWriter ow;
		private BufferedReader reader;

		public Redirector(InputStream inputStream, OutputWriter ow)
		{
			this.reader=new BufferedReader(new InputStreamReader(inputStream));
			this.ow=ow;
		}

		public void run()
		{
			try
			{
				String buffer=reader.readLine();
				while(buffer!=null)
				{
					ow.println(buffer);					
					buffer=reader.readLine();
				}
			}
			catch(IOException ex)
			{
				ErrorManager.getDefault().notify(ex);
			}
		}
	}
	
	private String name=toString();
	private String[] cmdArray;
	private String[] envp;
	private File directory;
	private transient Process process;
	
	/** Creates a new instance of ProcessExecutor */
	public ProcessExecutor(String[] cmdArray, String[] envp, File dir)
	{
		this.cmdArray=cmdArray;
		this.envp=envp;
		this.directory=dir;
	}

	/** Creates a new instance of ProcessExecutor */
	public ProcessExecutor(String[] cmdArray)
	{
		this(cmdArray, null, null);
	}
	
	public void setName(String name)
	{
		this.name=name;
	}
	
	public void start()
	{
		RequestProcessor.getDefault().post(this);		
	}
	
	public void run()
	{
		try
		{
			ProgressHandle ph = ProgressHandleFactory.createHandle(name, this);
			ph.start();
			// Create a new output window
			InputOutput io=IOProvider.getDefault().getIO(name, true);	

			process=Runtime.getRuntime().exec(cmdArray, envp, directory);

			// Pipe Anadel output to the output window
			new Thread(new Redirector(process.getInputStream(), io.getOut())).start();
			new Thread(new Redirector(process.getErrorStream(), io.getOut())).start();

			process.waitFor();
			ph.finish();
		}
		catch(Exception ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
	}

	public boolean cancel()
	{	
		process.destroy();
		return true;
	}
}
