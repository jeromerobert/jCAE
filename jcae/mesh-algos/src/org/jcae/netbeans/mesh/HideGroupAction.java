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
 * (C) Copyright 2010, by EADS France
 */

package org.jcae.netbeans.mesh;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.vtk.LeafNode.DataProvider;
import org.jcae.vtk.View;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.xml.sax.SAXException;

/**
 *
 * @author Jerome Robert
 */
public class HideGroupAction extends AbstractGroupAction {

	@Override
	protected void processGroups(String[] groupsToDisplay, View view, Node node,
		ExplorerManager em)
		throws ParserConfigurationException, SAXException, IOException
	{
		AmibeNViewable interactor = AmibeNViewable.get(node, view);
		if(interactor != null)
		{
			HashSet<String> existing = new HashSet<String>();
			existing.addAll(interactor.getBeamGroups());
			existing.addAll(interactor.getTriaGroups());
			existing.removeAll(Arrays.asList(groupsToDisplay));
			if(existing.isEmpty())
			{
				view.remove(interactor);
			}
			else
			{
				Map<String, DataProvider> toRemove =
					new HashMap<String, DataProvider>(groupsToDisplay.length);
				for(String s:groupsToDisplay)
					toRemove.put(s, null);
				interactor.addTriangles(toRemove);
				interactor.addBeams(toRemove);
			}
			view.Render();
		}
	}

	@Override
	public String getName() {
		return "Hide";
	}

}
