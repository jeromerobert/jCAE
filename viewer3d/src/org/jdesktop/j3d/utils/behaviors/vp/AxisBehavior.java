/*
 * $RCSfile$
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 *
 * $Revision$
 * $Date$
 * $State$
 */

package org.jdesktop.j3d.utils.behaviors.vp;

import java.util.Enumeration;
import javax.media.j3d.*;
import javax.vecmath.*;

/**
 * Behavior class to extract the rotation component of the view
 * platform transform, and set the axis transform to the inverse of
 * that rotation. The axis should be added as a child of the platform
 * geometry. Note that this behavior must run after the view platform
 * behavior.
 */
public class AxisBehavior extends Behavior {
    // Axis transform group (target of behavior)
    private TransformGroup axisTG;

    // View platform transform group (source)
    private TransformGroup viewPlatformTG;

    // Wake up every frame (passively)
    private WakeupOnElapsedFrames w = new WakeupOnElapsedFrames(0, true);

    // Cached value of last view platform transform
    private Transform3D lastTransform = new Transform3D();

    // Temporary transform
    private Transform3D t1 = new Transform3D();

    // Temporary rotation matrix
    private Matrix3d rotMat = new Matrix3d();


    /**
     * Constructs a new AxisBehavior from the specified view
     * platform transform group and axis transform group.
     */
    public AxisBehavior(TransformGroup axisTG, TransformGroup viewPlatformTG) {
	// Save references to source and target transform groups
	this.axisTG = axisTG;
	this.viewPlatformTG = viewPlatformTG;

	// Run this behavior in the last scheduling interval
	setSchedulingInterval(Behavior.getNumSchedulingIntervals() - 1);
    }

    /**
     * Initialize local variables and set the initial wakeup
     * condition. Called when the behavior is first made live.
     */
    public void initialize() {
	// Initiialize to identity (no rotation)
	lastTransform.setIdentity();
	t1.setIdentity();
	axisTG.setTransform(t1);

	// Set the initial wakeup condition
	wakeupOn(w);
    }

    /**
     * Extract the rotation from the view platform transform (if it has
     * changed) and update the target transform with its inverse.
     */
    public void processStimulus(Enumeration criteria) {
	viewPlatformTG.getTransform(t1);

	// Compute the new axis transform if the viewPlatformTransform
	// has changed
	if (!lastTransform.equals(t1)) {
	    lastTransform.set(t1);

	    t1.get(rotMat);
	    t1.setIdentity();
	    t1.set(rotMat);
	    t1.invert();
	    axisTG.setTransform(t1);
	}

	// Reset the wakeup condition so we will wakeup next frame
	wakeupOn(w);
    }
}
