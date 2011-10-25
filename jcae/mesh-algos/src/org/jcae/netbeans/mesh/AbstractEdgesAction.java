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

import java.awt.Color;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.jcae.mesh.xmldata.ComputeEdgesConnectivity;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.AmibeOverlayProvider;
import org.jcae.vtk.AmibeOverlayToMesh;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup.Result;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.actions.BooleanStateAction;
import org.xml.sax.SAXException;

public abstract class AbstractEdgesAction extends BooleanStateAction
{
	private final static Logger LOGGER = Logger.getLogger(
		AbstractEdgesAction.class.getName());
	public abstract String getBranchGroupLabel();
	public abstract String getActionLabel();
	public abstract String getViewSuffix();
	private AmibeDataObject amibeDataObject;
	private boolean lock;
	//Keep it as a field, else it's garbage collected
	//See http://wiki.netbeans.org/DevFaqTrackGlobalSelection or
	//http://emilian-bold.blogspot.com/2006/11/netbeans-platform-lookupresult-garbage.html
	private final Result<AmibeDataObject> currentSelection;

	public AbstractEdgesAction() {
		setBooleanState(false);
		currentSelection =
			org.openide.util.Utilities.actionsGlobalContext().lookupResult(
			AmibeDataObject.class);
		//listen to node selection
		currentSelection.addLookupListener(new LookupListener() {
			public void resultChanged(LookupEvent ev) {
				if(currentSelection.allInstances().size() == 1)
				{
					amibeDataObject = currentSelection.allInstances().iterator().next();
					setEnabled(true);
					lock = true;
					setBooleanState(getAViewable(amibeDataObject)!=null);
					lock = false;
				}
				else
					setEnabled(false);
			}
		});

		//
		addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if(PROP_BOOLEAN_STATE.equals(evt.getPropertyName()))
				{
					if(lock)
						return;
					View view = ViewManager.getDefault().getCurrentView();
					if((Boolean)evt.getNewValue())
						view.add(createViewable(amibeDataObject, view));
					else
						view.remove(getAViewable(amibeDataObject));
				}
			}
		});
	}

	private AViewable getAViewable(AmibeDataObject ado)
	{
		View view = ViewManager.getDefault().getCurrentView();
		for(Viewable v:view.getViewables())
		{
			if(v instanceof AViewable)
			{
				AViewable av = (AViewable) v;
				if(ado.equals(av.amibeDataObject) &&
					getBranchGroupLabel().equals(av.getBranchGroupLabel()))
					return av;
			}
		}
		return null;
	}
	
	private class AViewable extends AmibeOverlayToMesh
	{
		private final AmibeDataObject amibeDataObject;
		private final View view;
		public AViewable(AmibeDataObject ado, String xmlDir, Color color, View view)
			throws ParserConfigurationException, SAXException, IOException
		{
			super(new AmibeOverlayProvider(
				new File(xmlDir),
				AbstractEdgesAction.this.getBranchGroupLabel()), color);
			this.amibeDataObject = ado;
			this.view = view;
			//Remove the viewable when the associated file is removed by the user
			ado.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					if(DataObject.PROP_VALID.equals(evt.getPropertyName()))
					{
						if(!(Boolean)evt.getNewValue())
						{
							//We are in the file system monitor thread so we need to
							//switch to the EDT.
							EventQueue.invokeLater(new Runnable(){
								public void run() {
									AViewable.this.view.remove(AViewable.this);
								}
							});
						}
					}
				}
			});
		}

		public String getBranchGroupLabel()
		{
			return AbstractEdgesAction.this.getBranchGroupLabel();
		}
	}
	
	private AViewable createViewable(AmibeDataObject c, View view)
	{
		try
		{		
			String reference = FileUtil.toFile(
				c.getPrimaryFile().getParent()).getPath();
			String xmlDir=Utilities.absoluteFileName(
				c.getMesh().getMeshFile(), reference);

			String xmlFile = "jcae3d";
			ComputeEdgesConnectivity computeEdgesConnectivity =
				new ComputeEdgesConnectivity(xmlDir, xmlFile);

			computeEdgesConnectivity.compute();	
			String beanType = getBranchGroupLabel();
			Color color;
			if(beanType.equals(AmibeOverlayProvider.FREE_EDGE))
				color = AmibeOverlayProvider.FREE_EDGE_COLOR;
			else
				color = AmibeOverlayProvider.MULTI_EDGE_COLOR;
			
			AViewable mesh = new AViewable(c, xmlDir, color, view);
			mesh.setName(c.getName()+" "+getViewSuffix());
			return mesh;
		}
		catch (XPathExpressionException ex)
		{
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (IOException ex)
		{
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (ParserConfigurationException ex)
		{
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (SAXException ex)
		{
			LOGGER.log(Level.SEVERE, null, ex);
		}
		return null;
	}

	@Override
	public String getName()
	{
		return NbBundle.getMessage(getClass(), getActionLabel());
	}
	
	@Override
	protected void initialize()
	{
		super.initialize();
		// see org.openide.util.actions.SystemAction.iconResource() javadoc for more details
		putValue("noIconInMenu", Boolean.TRUE);
	}

	@Override
	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}
}

