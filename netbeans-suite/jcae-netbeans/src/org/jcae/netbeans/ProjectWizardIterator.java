package org.jcae.netbeans;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public class ProjectWizardIterator
	implements
		WizardDescriptor.InstantiatingIterator
{
	private static final long serialVersionUID = 1L;
	private transient int index;
	private transient WizardDescriptor.Panel[] panels;
	private transient WizardDescriptor wiz;

	public ProjectWizardIterator()
	{
	}

	public static ProjectWizardIterator createIterator()
	{
		return new ProjectWizardIterator();
	}

	private WizardDescriptor.Panel[] createPanels()
	{
		return new WizardDescriptor.Panel[]{new ProjectWizardPanel(),};
	}

	private String[] createSteps()
	{
		return new String[]{"Create a new jCAE Project"};
	}

	public Set instantiate() throws IOException
	{
		Set resultSet = new LinkedHashSet();
		File dirF = FileUtil.normalizeFile((File) wiz.getProperty("projdir"));
		dirF.mkdirs();
		//String name = (String) wiz.getProperty("name");
		FileObject template = Templates.getTemplate(wiz);
		FileObject dir = FileUtil.toFileObject(dirF);
		template.copy(dir, template.getName(), template.getExt());
		// Always open top dir as a project:
		resultSet.add(dir);
		// Look for nested projects to open as well:
		Enumeration e = dir.getFolders(true);
		while (e.hasMoreElements())
		{
			FileObject subfolder = (FileObject) e.nextElement();
			if (ProjectManager.getDefault().isProject(subfolder))
			{
				resultSet.add(subfolder);
			}
		}
		File parent = dirF.getParentFile();
		if (parent != null && parent.exists())
		{
			ProjectChooser.setProjectsFolder(parent);
		}
		return resultSet;
	}

	public void initialize(WizardDescriptor wiz)
	{
		this.wiz = wiz;
		index = 0;
		panels = createPanels();
		// Make sure list of steps is accurate.
		String[] steps = createSteps();
		for (int i = 0; i < panels.length; i++)
		{
			Component c = panels[i].getComponent();
			if (steps[i] == null)
			{
				// Default step name to component name of panel.
				// Mainly useful for getting the name of the target
				// chooser to appear in the list of steps.
				steps[i] = c.getName();
			}
			if (c instanceof JComponent)
			{ // assume Swing components
				JComponent jc = (JComponent) c;
				// Step #.
				jc.putClientProperty("WizardPanel_contentSelectedIndex",
					new Integer(i)); // NOI18N
				// Step name (actually the whole list for reference).
				jc.putClientProperty("WizardPanel_contentData", steps); // NOI18N
			}
		}
	}

	public void uninitialize(WizardDescriptor wiz)
	{
		this.wiz.putProperty("projdir", null); //NOI18N
		this.wiz.putProperty("name", null); //NOI18N
		this.wiz = null;
		panels = null;
	}

	public String name()
	{
		return MessageFormat.format("{0} of {1}",
			new Object[]{new Integer(index + 1), new Integer(panels.length)});
	}

	public boolean hasNext()
	{
		return index < panels.length - 1;
	}

	public boolean hasPrevious()
	{
		return index > 0;
	}

	public void nextPanel()
	{
		if (!hasNext()) throw new NoSuchElementException();
		index++;
	}

	public void previousPanel()
	{
		if (!hasPrevious()) throw new NoSuchElementException();
		index--;
	}

	public WizardDescriptor.Panel current()
	{
		return panels[index];
	}

	// If nothing unusual changes in the middle of the wizard, simply:
	public final void addChangeListener(ChangeListener l)
	{
	}

	public final void removeChangeListener(ChangeListener l)
	{
	}
}
