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

package org.jcae.netbeans;

import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;

/**
 *
 * @author jerome
 */
public class JCAEProjectFactory implements ProjectFactory
{
	/**
	 * Test whether a given directory probably refers to a project recognized
	 * by this factory without actually trying to create it.
	 */
	public boolean isProject(FileObject projectDirectory)
	{
		return projectDirectory.getFileObject("jcae", "xml") != null;
	}

	/** Create a project that resides on disk. */
	public org.netbeans.api.project.Project loadProject(
		FileObject projectDirectory, ProjectState state)
	{
		return new JCAEProject(projectDirectory, state);
	}

	/** Save a project to disk. */
	public void saveProject(org.netbeans.api.project.Project project)
	{
	}
}
