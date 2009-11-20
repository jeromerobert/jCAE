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
 * (C) Copyright 2005-2009, by EADS France
 */
package org.jcae.netbeans.mesh;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import javax.swing.SwingUtilities;
import org.jcae.mesh.JCAEFormatter;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.nodes.Node;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Utilities;
import org.openide.util.actions.CookieAction;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.python.util.PythonInterpreter;

/**
 *
 * @author Jerome Robert
 */
public abstract class AlgoAction extends CookieAction {

	protected static class Redirector extends Thread {

		private final BufferedReader in;
		private final PrintWriter out;

		public Redirector(InputStream in, PrintWriter out) {
			this.in = new BufferedReader(new InputStreamReader(in));
			this.out = out;
		}

		@Override
		public void run() {
			try {
				String line = in.readLine();
				while (line != null) {
					out.println(line);
					line = in.readLine();
				}
			} catch (IOException ex) {
				Exceptions.printStackTrace(ex);
			}
		}
	}

	@Override
	protected int mode() {
		return CookieAction.MODE_EXACTLY_ONE;
	}

	private void runProcess(ProcessBuilder pb, final InputOutput io) throws IOException {
		final Process p = pb.start();
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				io.select();
			}
		});
		new Redirector(p.getInputStream(), io.getOut()).start();
		new Redirector(p.getErrorStream(), io.getErr()).start();
		final ProgressHandle ph = ProgressHandleFactory.createHandle(getName(),
				new Cancellable() {

					@Override
					public boolean cancel() {
						p.destroy();
						return true;
					}
				});
		ph.start();
		try {
			p.waitFor();
		} catch (InterruptedException ex) {
			Exceptions.printStackTrace(ex);
		} finally {
			ph.finish();
		}
	}

	@Override
	protected void performAction(Node[] activatedNodes) {		
		List<String> args = getArguments(activatedNodes[0]);
		if (args != null) {
			File pyFile = InstalledFileLocator.getDefault().locate(
					"amibe-python/" + getCommand() + ".py",
					"org.jcae.netbeans", false);
			InputOutput io = IOProvider.getDefault().getIO(getName(), true);
			if (Settings.getDefault().isRunInSameJVM())
				runInBackground(args, pyFile, io);
			else
				runInOtherVM(activatedNodes[0], args, pyFile, io);
		}
	}

	private void runInBackground(final List<String> args, final File pyFile, final InputOutput io) {
		
		
		try {
			runInSameVM(args, pyFile, io);
		} finally {
			
		}
	}

	private void runInSameVM(List<String> args, File pyFile, final InputOutput io) {
		final ProgressHandle ph = ProgressHandleFactory.createHandle(getName());
		Logger root = Logger.getLogger("org.jcae.mesh");
		final JCAEFormatter jcaeFormatter = new JCAEFormatter();
		//getting and redirecting logs from Bora mesher

		root.setLevel(Level.INFO);
		Handler redirector = new Handler() {

			@Override
			public void publish(LogRecord record) {
				io.getOut().print(getFormatter().format(record));
			}

			@Override
			public void close() throws SecurityException {
			}

			@Override
			public void flush() {
			}
		};
		try
		{
			root.addHandler(redirector);
			for (Handler h : root.getHandlers()) {
				h.setFormatter(jcaeFormatter);
			}
			PythonInterpreter interp = new PythonInterpreter();
			interp.setOut(io.getOut());
			interp.setIn(io.getIn());
			interp.setErr(io.getErr());
			interp.set("args", args);
			interp.exec("import sys");
			interp.exec("sys.argv.extend(args)");
			ph.start();
			interp.execfile(pyFile.getPath());
		}
		finally
		{
			root.removeHandler(redirector);
			ph.finish();
		}
	}

	private void runInOtherVM(Node node, List<String> args, File pyFile, InputOutput io) {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			String ext = Utilities.isWindows() ? ".bat" : "";
			File f = InstalledFileLocator.getDefault().locate(
					"jython/bin/jython" + ext, "org.jcae.netbeans", false);
			pb.command().add(f.getPath());
			for (String s : Settings.getDefault().parameters()) {
				if (s.startsWith("-")) {
					s = "-J" + s;
				}
				pb.command().add(s);
			}
			pb.command().add(pyFile.getPath());
			pb.command().addAll(args);
			pb.environment().put("JAVA_HOME", Settings.getDefault().getJavaVirtualMachine());
			customizeProcessBuilder(node, pb);
			runProcess(pb, io);
		} catch (IOException ex) {
			Exceptions.printStackTrace(ex);
		}
	}

	@Override
	public HelpCtx getHelpCtx() {
		return HelpCtx.DEFAULT_HELP;
	}

	protected abstract String getCommand();

	/** Show a dialog and return a list of argument or null to cancel */
	protected abstract List<String> getArguments(Node node);

	@Override
	protected boolean asynchronous() {
		return true;
	}

	protected void customizeProcessBuilder(Node node, ProcessBuilder pb) {
		AmibeNode n = node.getLookup().lookup(AmibeNode.class);
		if( n != null )
		{
			File f = FileUtil.toFile(n.getDataObject().getPrimaryFile().getParent());
			pb.directory(f);
		}
	}

	@Override
	protected Class<?>[] cookieClasses() {
		return new Class[] { AmibeNode.class };
	}
}
