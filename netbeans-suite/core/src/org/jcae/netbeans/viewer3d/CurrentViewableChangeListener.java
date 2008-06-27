/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jcae.netbeans.viewer3d;

import org.jcae.vtk.Viewable;

/**
 * Be carefull, a viewable == null can be sent
 * @author ibarz
 */
public interface CurrentViewableChangeListener {
	public void currentViewableChanged(Viewable interactor);
}
