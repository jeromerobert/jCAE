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
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.netbeans.cad;

import java.io.IOException;
import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

public class RemoveAction extends CookieAction
{
	protected int mode()
	{
		return CookieAction.MODE_ANY;
	}

	protected Class[] cookieClasses()
	{
		return new Class[]{NbShape.class};
	}

	protected void performAction(Node[] nodes)
	{
		for(Node n: nodes)
		{
			GeomUtils.getShape(n).remove();
			GeomUtils.getParentBrep(n).getDataObject().setModified(true);
			try
			{
				n.destroy();
			}
			catch (IOException ex)
			{
				ErrorManager.getDefault().notify(ex);
			}			
		}
	}

	public String getName()
	{
		return "Remove";
	}

	public HelpCtx getHelpCtx()
	{
		return HelpCtx.DEFAULT_HELP;
	}
	
}
