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

import java.util.ArrayList;
import java.util.List;
import org.jcae.netbeans.mesh.AmibeNode;
import org.openide.nodes.Node;

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
	protected List<String> getArguments(Node node) {
		String meshDirectory = node.getLookup().lookup(AmibeDataObject.class).getMeshDirectory();
		RemeshPanel p = new RemeshPanel();
		if(p.showDialog())
		{
			ArrayList<String> l = new ArrayList<String>();
			l.add("--coplanarity");
			l.add(Double.toString(p.getCoplanarity()));
			l.add("--size");
			l.add(Double.toString(p.getTargetSize()));
			l.add(meshDirectory);
			l.add(meshDirectory);
			return l;
		}
		else return null;
	}
}
