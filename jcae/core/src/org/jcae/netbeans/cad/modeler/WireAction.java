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
 * (C) Copyright 2009, by EADS France
 */

package org.jcae.netbeans.cad.modeler;

import java.awt.BorderLayout;
import org.jcae.netbeans.cad.*;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jerome Robert
 */
public class WireAction extends CookieAction{
	private static class WireComponent extends TopComponent
	{
		private WirePanel panel = new WirePanel();
		public WireComponent()
		{
			setLayout(new BorderLayout());
			add(panel, BorderLayout.CENTER);
		}
		@Override
		public String getDisplayName() {
			return "Wire creation";
		}

		@Override
		public int getPersistenceType() {
			return TopComponent.PERSISTENCE_NEVER;
		}

		@Override
		protected String preferredID() {
			return WireAction.class.getName();
		}

		public WirePanel getPanel()
		{
			return panel;
		}
	};
	
	public static class NewType extends org.openide.util.datatransfer.NewType
	{
		@Override
		public void create() {
			WireComponent wc = null;
			for(TopComponent tc:TopComponent.getRegistry().getOpened())
				if(tc instanceof WireComponent)
					wc = (WireComponent) tc;

			if(wc == null)
			{
				wc = new WireComponent();
				Mode m = WindowManager.getDefault().findMode("properties");
				if(m == null)
					m = WindowManager.getDefault().findMode("explorer");
				m.dockInto(wc);
				wc.open();
			}
			
			wc.requestActive();
			//TODO set BRepNode in the WirePanel
		}

		@Override
		public String getName() {
			return "Wire";
		}
	}
	
	@Override
	protected int mode() {
		return CookieAction.MODE_EXACTLY_ONE;
	}

	@Override
	protected Class<?>[] cookieClasses() {
		return new Class[] { BrepNode.class };
	}

	@Override
	protected void performAction(Node[] activatedNodes) {
		NbShape s = GeomUtils.getShape(activatedNodes[0]);
		new NewType().create();
	}

	@Override
	public String getName() {
		return "Create wire";
	}

	@Override
	public HelpCtx getHelpCtx() {
		return HelpCtx.DEFAULT_HELP;
	}
}
