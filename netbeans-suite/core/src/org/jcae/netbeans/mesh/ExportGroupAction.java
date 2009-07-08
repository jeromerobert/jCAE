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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.netbeans.mesh;

import java.io.*;
import java.util.HashSet;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.mesh.xmldata.MeshExporter;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.xml.sax.SAXException;

public class ExportGroupAction extends CookieAction
{
	public static class ChooseUnitPanel extends JPanel
	{	
		private JRadioButton meters=new JRadioButton("Meters");
		public ChooseUnitPanel()
		{	
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			JRadioButton mm=new JRadioButton("Millimeters");
			ButtonGroup bg=new ButtonGroup();
			bg.add(meters);
			bg.add(mm);
			meters.setSelected(true);
			add(meters);
			add(Box.createVerticalStrut(5));
			add(mm);
		}
		
		public boolean isMeters()
		{
			return meters.isSelected();
		}
	}	
	
	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected Class[] cookieClasses()
	{
		return new Class[]{GroupNode.class};
	}

	@Override
	protected void performAction(Node[] arg0)
	{
		try
		{
			HashSet<String> set=new HashSet<String>();
			MeshNode meshNode=null;
			for(int i=0; i<arg0.length; i++)
			{
				meshNode=arg0[i].getParentNode().getParentNode()
					.getCookie(MeshNode.class);
				set.add(meshNode.getMeshDirectory());
			}
			
			if(set.size()>1)
			{
				JOptionPane.showMessageDialog(null,
					"UNV export can only work with groups from the same mesh");
				return;
			}
			
			String meshDir=set.toArray()[0].toString();
			FileObject meshDirFile=
				meshNode.getDataObject().getPrimaryFile().getParent();
			
			JFileChooser jfc=new JFileChooser();
			ChooseUnitPanel unitPanel=new ChooseUnitPanel();
			jfc.setAccessory(unitPanel);
			jfc.setCurrentDirectory(FileUtil.toFile(meshDirFile));
			if(jfc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
			{
				int[] ids=new int[arg0.length];
				for(int i=0; i<arg0.length; i++)
				{
					GroupNode n=arg0[i].getCookie(GroupNode.class);
					ids[i]=n.getGroup().getId();
				}			
				String unvFile=jfc.getSelectedFile().getPath();
				
				if(!unvFile.endsWith(".unv"))
					unvFile+=".unv";
				
				PrintStream stream=new PrintStream(new BufferedOutputStream(
					new FileOutputStream(unvFile)));				
				MeshExporter.UNV exporter=new MeshExporter.UNV(new File(meshDir), ids);				
				if(unitPanel.isMeters())
					exporter.setUnit(MeshExporter.UNV.Unit.METER);
				else
					exporter.setUnit(MeshExporter.UNV.Unit.MM);
				exporter.write(stream);				
				stream.close();				
			}
		}
		catch(IOException ex)
		{
			ErrorManager.getDefault().notify(ex);
		}
		catch (ParserConfigurationException e)
		{
			ErrorManager.getDefault().notify(e);
		}
		catch (SAXException e)
		{
			ErrorManager.getDefault().notify(e);
		}
	}

	public String getName()
	{
		return "Export as UNV";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}

}
