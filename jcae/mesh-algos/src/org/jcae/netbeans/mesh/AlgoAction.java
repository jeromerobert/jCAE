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
 * (C) Copyright 2005-2010, by EADS France
 */
package org.jcae.netbeans.mesh;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.jcae.mesh.JCAEFormatter;
import org.jcae.netbeans.options.JCAEPanel;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.nodes.Node;
import org.openide.util.*;
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
				//the child process has been killed
				out.println("End of stream");
			}
		}
	}

	@Override
	protected int mode() {
		return CookieAction.MODE_ALL;
	}

	private String join(List<String> l)
	{
		StringBuilder sb = new StringBuilder();
		for(String s:l)
		{
			sb.append(s);
			sb.append(' ');
		}
		return sb.toString();
	}

	private void runProcess(ProcessBuilder pb, final InputOutput io) throws IOException {
		io.getOut().println("Running "+join(pb.command()));
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

	/** Check that groups are all from the same mesh */
	private boolean checkGroups(Node[] nodes)
	{
		HashSet<AmibeDataObject> ados = new HashSet<AmibeDataObject>();
		for(Node n:nodes)
			ados.add(amibeDataObject(n));
		if(ados.size() != 1)
		{
			JOptionPane.showMessageDialog(null, "Selected groups must belong to the same mesh.");
			return false;
		}
		else
			return true;
	}

	private String groupNames(Node[] nodes, AmibeDataObject ado)
	{
		StringBuilder sb = new StringBuilder(2*nodes.length);
		boolean first = true;
		assert ado != null;
		if(ado.getGroups() == null)
			return "";
		assert ado.getGroups().getGroups() != null;
		int allGroupsNumber = ado.getGroups().getGroups().length;
		TreeSet<String> selectedGroups = new TreeSet<String>();
		for(Node n:nodes)
		{
			GroupNode gn = n.getLookup().lookup(GroupNode.class);
			if(gn != null)
				selectedGroups.add(gn.getName());
		}

		if(selectedGroups.size() == allGroupsNumber)
			return "";

		for(String s:selectedGroups)
		{
			if(!first)
				sb.append(",");
			sb.append(s);
			first = false;
		}
		return sb.toString();
	}

	private AmibeDataObject amibeDataObject(Node node)
	{
		AmibeDataObject ado = node.getLookup().lookup(AmibeDataObject.class);
		if(ado == null)
		{
			ado = node.getParentNode().getParentNode().getLookup().lookup(AmibeDataObject.class);
		}
		return ado;
	}
	
	@Override
	protected void performAction(Node[] activatedNodes) {
		if(checkGroups(activatedNodes))
		{			
			AmibeDataObject ado = amibeDataObject(activatedNodes[0]);
			String groupNames = groupNames(activatedNodes, ado);
			List<String> args = getArguments(ado);
			if (args != null) {
				String command;
				if(!groupNames.isEmpty())
				{
					args.remove(args.size()-1); //inputdir
					args.remove(args.size()-1); //outputdir
					args.add(0,"--immutable-border");
					args.add(0, getCommand());
					args.add(0, groupNames);
					args.add(0, ado.getMeshDirectory());
					args.add(0, ado.getMeshDirectory());
					command="submesh";
				}
				else
					command=getCommand();
				File pyFile = InstalledFileLocator.getDefault().locate(
						"amibe-python/" + command + ".py",
						"org.jcae.netbeans", false);
				InputOutput io = IOProvider.getDefault().getIO(getName(), true);
				if (NbPreferences.forModule(JCAEPanel.class).getBoolean("runInSameJVM", true))
					runInSameVM(args, pyFile, io);
				else
					runInOtherVM(activatedNodes[0], args, pyFile, io);
				ado.refreshGroups();
			}
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

	private String getClassPath()
	{
		return InstalledFileLocator.getDefault().locate(
			"modules/ext/amibe.jar", "org.jcae.netbeans", false).getPath();
	}
	
	private void runInOtherVM(Node node, List<String> args, File pyFile, InputOutput io) {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			String ext = Utilities.isWindows() ? ".bat" : "";
			File f = InstalledFileLocator.getDefault().locate(
					"modules/jython/bin/jython" + ext, "org.jcae.netbeans.mesh", false);
			pb.command().add(f.getPath());
                        JCAEPanel j = new JCAEPanel(null);
			for (String s : j.parameters()) {
				if (s.startsWith("-") && !s.startsWith("-D")) {
					s = "-J" + s;
				}
				pb.command().add(s);
			}
			String home = System.getProperty("netbeans.user");
			File dir = new File(new File(new File(new File(home), "var"), "cache"), "jython");
			pb.command().add("-Dpython.cachedir="+dir.getPath());
			pb.environment().put("CLASSPATH", getClassPath());			
			pb.command().add(pyFile.getPath());
			pb.command().addAll(args);
			pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
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
	protected abstract List<String> getArguments(AmibeDataObject node);

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
		return new Class<?>[] { AmibeDataObject.class, GroupNode.class };
	}
}
