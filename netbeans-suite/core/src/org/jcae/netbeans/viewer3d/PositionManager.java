package org.jcae.netbeans.viewer3d;

import java.util.Vector;
import org.jcae.viewer3d.View;

public class PositionManager
{		
	private static PositionManager singleton=new PositionManager();
	private Vector<ScreenShotPosition> shots=new Vector<ScreenShotPosition>();
	
	public void savePosition(View view)
	{
		ScreenShotPosition ssp=new ScreenShotPosition(view,view.where());
		shots.insertElementAt(ssp,0);
		ssp.shot();
	}
	
	public void goToPosition(View view, int index){
		view.move(shots.get(index).getPosition());
	}
	
	public int getPositionCount(){
		return shots.size();
	}
	
	public ScreenShotPosition getPosition(int index){
		return shots.get(index);
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
		if(!shots.isEmpty())
			new ViewList(this, view).setVisible(true);
	}
}
