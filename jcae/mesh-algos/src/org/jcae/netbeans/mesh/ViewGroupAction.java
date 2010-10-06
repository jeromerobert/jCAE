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

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.vtk.AmibeToMesh;
import org.jcae.vtk.View;
import org.openide.explorer.ExplorerManager;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.WeakListeners;
import org.xml.sax.SAXException;

public class ViewGroupAction extends AbstractGroupAction
{
	public String getName()
	{
		return "View";
	}

	/**
	 * A viewable which remove it self from the view if the underlying
	 * DataObject is removed
	 */
	private static class AViewable extends AmibeNViewable implements PropertyChangeListener
	{
		private final View view;
		public AViewable(Node n, ExplorerManager em, View view) {
			super(n, em);
			this.view = view;
		}
		public void propertyChange(PropertyChangeEvent evt) {
			if(DataObject.PROP_VALID.equals(evt.getPropertyName()))
			{
				if(!(Boolean)evt.getNewValue())
				{
					//We are in the file system monitor thread so we need to
					//switch to the EDT.
					EventQueue.invokeLater(new Runnable(){
						public void run() {
							view.remove(AViewable.this);
						}
					});
				}
			}
		}
	};
	
	@Override
	protected void processGroups(String[] idGroupsDisplayed, final View view, Node node,
		ExplorerManager em)
		throws ParserConfigurationException, SAXException, IOException
	{
		AmibeNViewable interactor = AmibeNViewable.get(node, view);
		AmibeDataObject ado = node.getLookup().lookup(AmibeDataObject.class);
		if(interactor == null)
		{
			AViewable v = new AViewable(node, em, view);
			v.setName(ado.getName());
			view.add(v);
			// thanks to WeakListerns, the PropertyChangeListener is
			// automatically removed from the AmibeDataObject when the viewable
			// is removed from the view
			ado.addPropertyChangeListener(WeakListeners.propertyChange(v, ado));
			interactor = v;
		}		
		AmibeToMesh reader = new AmibeToMesh(ado.getGroups().getMeshFile(), idGroupsDisplayed);
		interactor.addTriangles(reader.getTriangles());
		interactor.addBeams(reader.getBeams());		
		view.Render();
	}
}
