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
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.modules.InstalledFileLocator;
import org.openide.nodes.Node;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Utilities;
import org.openide.util.actions.CookieAction;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

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


//			InputOutput io=IOProvider.getDefault().getIO("jCAE Mesher "+ioProviderCounter, true);
//			ioProviderCounter++;
//
//			final String brepName=Utilities.absoluteFileName(mesh.getGeometryFile(), reference);
//			String xmlDir=Utilities.absoluteFileName(mesh.getMeshFile(), reference);
//
//			final OutputWriter ow=io.getOut();
//			if(Settings.getDefault().isRunInSameJVM())
//			{
//				Logger root = Logger.getLogger("");
//				root.setLevel(Level.INFO);
//				Formatter jcaeFormatter = new JCAEFormatter();
//				for (Handler h: root.getHandlers())
//				{
//					h.setFormatter(jcaeFormatter);
//				}
//				thread=Thread.currentThread();
//
////				Mesher.main(new String[]{brepName, xmlDir, ""+mesh.getEdgeLength(), ""+mesh.getDeflection()});
//				thread=null;
//			}
//			else
//			{
//				ArrayList<String> array=new ArrayList<String>();
//				array.addAll(Arrays.asList(Settings.getDefault().getCommandLine()));
//				array.add(brepName);
//				array.add(xmlDir);
//				array.add(""+mesh.getEdgeLength());
//				array.add(""+mesh.getDeflection());
//				array.addAll(Arrays.asList(Settings.getDefault().getCustomMesherParameters()));
//				String[] cmdLine = array.toArray(new String[array.size()]);
//				System.err.println("jcae-netbeans-mesh: Running command line "+array);
//				process=Runtime.getRuntime().exec(cmdLine);
//				new Thread(new Redirector(process.getInputStream(), ow)).start();
//				new Thread(new Redirector(process.getErrorStream(), ow)).start();
//				process.waitFor();
//				process=null;
//			}
//
//			ow.close();

	private void runProcess(ProcessBuilder pb) throws IOException
	{
        final Process p = pb.start();
        final InputOutput io = IOProvider.getDefault().getIO(getName(), true);
        SwingUtilities.invokeLater(new Runnable(){
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
        new Thread(){
            @Override
            public void run() {
                try {
                    p.waitFor();
                }
                catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                }
                finally
                {
                    ph.finish();
                }
            }
        }.start();
	}
	@Override
	protected void performAction(Node[] activatedNodes) {
		String md = activatedNodes[0].getLookup().lookup(AmibeNode.class).getMeshDirectory();
		List<String> args = getArguments(md);
		if(args != null)
			try {
				ProcessBuilder pb = new ProcessBuilder();
				String ext = Utilities.isWindows() ? ".bat" : "";
				File f = InstalledFileLocator.getDefault().locate(
					"jython/bin/jython" + ext, "org.jcae.netbeans", false);
				pb.command().add(f.getPath());
				for (String s : Settings.getDefault().getJVMParams()) {
					if (s.startsWith("-")) {
						s = "-J" + s;
					}
					pb.command().add(s);
				}
				pb.command().add(getCommand() + ".py");
				pb.command().addAll(args);
				runProcess(pb);
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
	protected abstract List<String> getArguments(String meshDirectory);
	
	protected boolean showDialog(JComponent j)
	{
		return JOptionPane.showConfirmDialog(j, null, getName(),
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) ==
			JOptionPane.OK_OPTION;
	}
}
