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

import java.util.ArrayList;
import java.util.List;
import org.jcae.netbeans.Utilities;
import org.openide.nodes.Node;

public final class SmoothAction extends AlgoAction
{	
	public String getName()
	{
		return "Smooth";
	}

	@Override
	protected String getCommand() {
		return "smooth3dBg";
	}

	@Override
	protected List<String> getArguments(Node node) {
		String meshDirectory = node.getLookup().lookup(AmibeNode.class).getMeshDirectory();
		SmoothParameters bean=new SmoothParameters();
		if(Utilities.showEditBeanDialog(bean))
		{
			ArrayList<String> l = new ArrayList<String>();
			l.add("--iterations");
			l.add(Integer.toString(bean.getIterationNumber()));
			l.add("--size");
			l.add(Double.toString(bean.getElementSize()));
			l.add(meshDirectory);
			l.add(meshDirectory);
			return l;
		}
		else return null;
	}
}

