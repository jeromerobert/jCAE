package org.jcae.netbeans.viewer3d.actions;

import org.jcae.vtk.CameraManager;


public class BottomAction extends ChangeCameraOrientation
{
	public BottomAction()
	{
		super("Bottom", "v3d-view-change-to-bottom.png", CameraManager.Orientation.BOTTOM);
	}
}