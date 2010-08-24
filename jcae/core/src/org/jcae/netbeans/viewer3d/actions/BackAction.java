package org.jcae.netbeans.viewer3d.actions;

import org.jcae.vtk.CameraManager;


public class BackAction extends ChangeCameraOrientation
{
	public BackAction()
	{
		super("Back", "v3d-view-change-to-back.png", CameraManager.Orientation.BACK);
	}
}	