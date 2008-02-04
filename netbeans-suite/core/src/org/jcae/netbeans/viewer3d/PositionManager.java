package org.jcae.netbeans.viewer3d;

import java.util.Vector;
import org.jcae.viewer3d.View;

public class PositionManager
{		
	private static PositionManager singleton=new PositionManager();
	Vector shots=new Vector();
	
	public void savePosition(View view)
	{
		ScreenShotPosition ssp=new ScreenShotPosition(view,view.where());
		shots.insertElementAt(ssp,0);
		ssp.shot();
	}
	
	public void goToPosition(View view, int index){
		ScreenShotPosition ssp=(ScreenShotPosition)shots.get(index);
		view.move(ssp.getPosition());
	}
	
	public int getPositionCount(){
		return shots.size();
	}
	
	public ScreenShotPosition getPosition(int index){
		return (ScreenShotPosition)shots.get(index);
	}
	
	public void removePosition(int index){
		shots.remove(index);
	}
		
	static public PositionManager getDefault()
	{
		return singleton;
	}

	public void goTo(View view)
	{
		if(shots.size()>0)
			new ViewList(this, view).setVisible(true);
	}
}
