package org.jcae.viewer3d;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.Node;
import javax.media.j3d.SceneGraphPath;

import com.sun.j3d.utils.picking.PickIntersection;
import com.sun.j3d.utils.picking.PickResult;

public class PickViewable {

	PickResult pickResult;
	int pickId;
	
	public PickViewable(PickResult pickResult,int pickId){
		this.pickResult=pickResult;
		this.pickId=pickId;
	}
	
	public GeometryArray getGeometryArray(){
		return pickResult.getGeometryArray();
	}
	
	public PickIntersection getIntersection(){
		return pickResult.getIntersection(pickId);
	}
	
	public Node getObject(){
		return pickResult.getObject();
	}
	
	public SceneGraphPath getSceneGraphPath(){
		return pickResult.getSceneGraphPath();
	}
	
	public java.lang.String toString(){
		return " >PickResult\n"+pickResult.toString()
		+" >PickIntersection\n"+getIntersection().toString();
	}

}
