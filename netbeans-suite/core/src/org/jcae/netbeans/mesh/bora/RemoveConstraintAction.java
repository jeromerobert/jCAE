/*
 * This source code is the property of EADS France. No part of it shall
 * be reproduced or transmitted without the express prior written
 * authorization of EADS France, and its contents shall not be disclosed.
 * Copyright EADS France.
 */
package org.jcae.netbeans.mesh.bora;

import org.jcae.netbeans.mesh.bora.BCADGraphNode;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

/**
 *
 * @author Gautam Botrel
 *
 */
public final class RemoveConstraintAction extends CookieAction {

	@Override
	public String getName() {
		return "Remove attached constraint";
	}

	@Override
	protected void performAction(Node[] arg0) {
		for (Node n : arg0) {
			BCADGraphNode.Attributes attributes = (BCADGraphNode.Attributes)n.getValue("ATTRIBUTES");
			if (attributes.hasConstraint()) {
				attributes.deleteAttachedConstraint();
			}
		}
	}


	@Override
	protected boolean asynchronous() {
		 // performAction() should run in event thread for actions that need a rendering of canva
		return false;
	}

	@Override
	protected boolean enable(Node[] arg0) {
		return true;
	}

	@Override
	protected int mode() {
		return MODE_SOME;
	}

	@Override
	protected Class<?>[] cookieClasses() {
		return new Class[] {
			BCADGraphNode.class
		};
	}

	@Override
	public HelpCtx getHelpCtx() {
		return HelpCtx.DEFAULT_HELP;
	}



}
