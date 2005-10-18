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
