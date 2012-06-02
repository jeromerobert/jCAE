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
 * (C) Copyright 2009-2010, by EADS France
 */

package org.jcae.netbeans.mesh;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.event.ChangeListener;
import org.jcae.mesh.xmldata.AmibeWriter;
import org.jcae.netbeans.mesh.AmibeDataObject;
import org.openide.WizardDescriptor;
import org.openide.WizardDescriptor.Panel;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.TemplateWizard;

/**
 *
 * @author Jerome Robert
 */
public class AmibeNewWizard implements WizardDescriptor.InstantiatingIterator<WizardDescriptor> {
	private TemplateWizard wizard;
	
	public Set<AmibeDataObject> instantiate() throws IOException {
		FileObject fo = wizard.getTargetFolder().getPrimaryFile();
		File f = new File(FileUtil.toFile(fo), wizard.getTargetName() + ".amibe");
		AmibeWriter aw = new AmibeWriter.Dim3(f.getPath());
		aw.nextSubMesh();
		aw.finish();
		AmibeDataObject mdo = (AmibeDataObject) DataObject.find(FileUtil.toFileObject(f));
		mdo.save();
		fo.refresh(true);
		return Collections.singleton(mdo);
	}

	public void initialize(WizardDescriptor wizard) {
		if(wizard instanceof TemplateWizard)
			this.wizard = ((TemplateWizard)wizard);
	}

	public void uninitialize(WizardDescriptor wizard) {
		wizard = null;
	}

	public Panel<WizardDescriptor> current() {
		return wizard.targetChooser();
	}

	public String name() {
		return "";
	}

	public boolean hasNext() {
		return false;
	}

	public boolean hasPrevious() {
		return false;
	}

	public void nextPanel() {
		throw new NoSuchElementException();
	}

	public void previousPanel() {
		throw new NoSuchElementException();
	}

	public void addChangeListener(ChangeListener l) {}

	public void removeChangeListener(ChangeListener l) {}
}
