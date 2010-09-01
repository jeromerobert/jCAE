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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.openide.util.Exceptions;

/**
 *
 * @author Jerome Robert
 */
public class RemeshAction extends AlgoAction {

	@Override
	public String getName() {
		return "Remesh";
	}

	@Override
	protected String getCommand() {
		return "remesh";
	}

	@Override
	protected List<String> getArguments(AmibeDataObject ado) {
		String meshDirectory = ado.getMeshDirectory();		
		RemeshPanel p = new RemeshPanel();
		p.setTargetSize(ado.getMesh().getEdgeLength());
		if(p.showDialog())
		{
			ArrayList<String> l = new ArrayList<String>();
			l.add("--coplanarity");
			l.add(Double.toString(p.getCoplanarity()));
			l.add("--size");
			l.add(Double.toString(p.getTargetSize()));
			if(p.isFeatureOnly())
				l.add("--features");
			if(p.isPreserveGroups())
				l.add("--preserveGroups");
			if(p.isPointMetric())
			{
				try {
					File f = File.createTempFile("jcae", ".txt");
					f.deleteOnExit();
					PrintStream out = new PrintStream(new FileOutputStream(f));
					p.writePointMetric(out);
					out.close();
					l.add("--point-metric");
					l.add(f.getPath());
				} catch (IOException ex) {
					Exceptions.printStackTrace(ex);
				}
			}
			l.add(meshDirectory);
			l.add(meshDirectory);
			return l;
		}
		else return null;
	}
}
