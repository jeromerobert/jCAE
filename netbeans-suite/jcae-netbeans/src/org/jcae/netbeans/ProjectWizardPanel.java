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

import java.awt.Component;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.HelpCtx;

/**
 * Panel just asking for basic info.
 */
public class ProjectWizardPanel
	implements
		WizardDescriptor.Panel,
		WizardDescriptor.ValidatingPanel,
		WizardDescriptor.FinishablePanel
{
	private WizardDescriptor wizardDescriptor;
	private ProjectWizardPanelVisual component;

	/** Creates a new instance of templateWizardPanel */
	public ProjectWizardPanel()
	{
	}

	public Component getComponent()
	{
		if (component == null)
		{
			component = new ProjectWizardPanelVisual(this);
		}
		return component;
	}

	public HelpCtx getHelp()
	{
		return new HelpCtx(ProjectWizardPanel.class);
	}

	public boolean isValid()
	{
		getComponent();
		return component.valid(wizardDescriptor);
	}
	private final Set/*<ChangeListener>*/listeners = new HashSet(1);

	public final void addChangeListener(ChangeListener l)
	{
		synchronized (listeners)
		{
			listeners.add(l);
		}
	}

	public final void removeChangeListener(ChangeListener l)
	{
		synchronized (listeners)
		{
			listeners.remove(l);
		}
	}

	protected final void fireChangeEvent()
	{
		Iterator it;
		synchronized (listeners)
		{
			it = new HashSet(listeners).iterator();
		}
		ChangeEvent ev = new ChangeEvent(this);
		while (it.hasNext())
		{
			((ChangeListener) it.next()).stateChanged(ev);
		}
	}

	public void readSettings(Object settings)
	{
		wizardDescriptor = (WizardDescriptor) settings;
		component.read(wizardDescriptor);
	}

	public void storeSettings(Object settings)
	{
		WizardDescriptor d = (WizardDescriptor) settings;
		component.store(d);
	}

	public boolean isFinishPanel()
	{
		return true;
	}

	public void validate() throws WizardValidationException
	{
		getComponent();
		component.validate(wizardDescriptor);
	}
}
