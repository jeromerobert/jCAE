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
package org.jcae.netbeans.cad;

import java.awt.datatransfer.Transferable;
import org.openide.loaders.LoaderTransfer;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.datatransfer.ExClipboard;
import org.openide.util.datatransfer.ExTransferable;

/**
 *
 * @author Jerome Robert
 */
@org.openide.util.lookup.ServiceProvider(service=
	org.openide.util.datatransfer.ExClipboard.Convertor.class)
public class GeomCConvertor implements ExClipboard.Convertor{	
	public Transferable convert(Transferable t) {
		Node[] ns = NodeTransfer.nodes(t, NodeTransfer.COPY|NodeTransfer.MOVE);
		if (ns != null && ns.length==1)
		{
			final NbShape shape = GeomUtils.getShape(ns[0]);
			if(shape!=null)
			{
				ExTransferable r = ExTransferable.create(t);
				ExTransferable.Single s = LoaderTransfer.transferable(null,
					LoaderTransfer.COPY|LoaderTransfer.MOVE);
				ExTransferable.Single ss = new ExTransferable.Single (s.getTransferDataFlavors()[0]) {
					public Object getData () {
						return BrepDataObject.createInMemory(shape);
					}
				};
				r.put(ss);
				return r;
			}
		}
		return t;
	}
}
