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
package org.jcae.netbeans.tweakui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

	@Override
	public void restored() {
		try {
			//Show favorite tab by default
			//layer.xml cannot be used here because the layer order cannot be
			//controled and the one of the favorite module may override ours
			FileObject f = FileUtil.getConfigRoot().getFileObject("Windows2/Modes/explorer/favorites.wstcref");
			OutputStream out = f.getOutputStream();
			InputStream in = getClass().getResourceAsStream("favoritesWstcref.xml");
			FileUtil.copy(in, out);
			in.close();
			out.close();
			Properties p = new Properties();
			p.load(getClass().getResourceAsStream("version.properties"));
			System.setProperty("netbeans.buildnumber", p.getProperty("version"));
		} catch (IOException ex) {
			Exceptions.printStackTrace(ex);
		}
	}
}
