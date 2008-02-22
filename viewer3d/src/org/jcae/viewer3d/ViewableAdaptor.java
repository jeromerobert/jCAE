package org.jcae.viewer3d;

import com.sun.j3d.utils.picking.PickResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.media.j3d.Bounds;
import javax.media.j3d.Node;

public abstract class ViewableAdaptor implements Viewable {
	
	private Collection<SelectionListener> listeners=Collections.synchronizedCollection(new ArrayList<SelectionListener>());

	/**Method called by the integrator
	 * This fires the views that the viewable has changed
	 * */
	public void domainsChanged(int[] domainId) {
		View.stopRenderer(this);
		domainsChangedPerform(domainId);
		View.viewableChanged(this);
		View.startRenderer(this);
	}

	/** 
	 * @param domainId The ids (Integer) of domains that changed or null to
	 * specify that even ids may have change
	 */
	public abstract void domainsChangedPerform(int[] domainId);
	
	public abstract DomainProvider getDomainProvider();

	public abstract void setDomainVisible(Map<Integer, Boolean> map);

	public abstract Node getJ3DNode();

	public abstract void pick(PickViewable result);

	public abstract void unselectAll();

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#addSelectionListener(org.jcae.viewer3d.SelectionListener)
	 */
	public void addSelectionListener(SelectionListener listener)
	{
		listeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#removeSelectionListener(org.jcae.viewer3d.SelectionListener)
	 */
	public void removeSelectionListener(SelectionListener listener)
	{
		listeners.add(listener);
	}
	
	protected void fireSelectionChanged()
	{
		Iterator<SelectionListener> it = listeners.iterator();
		while (it.hasNext())
		{
			SelectionListener s = it.next();
			s.selectionChanged();
		}
	}

	/** Default implementation which does nothing */
	public void pickArea(PickResult[] result, Bounds bound) {
		
	}
}
