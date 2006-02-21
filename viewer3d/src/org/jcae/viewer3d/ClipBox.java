package org.jcae.viewer3d;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.LineArray;
import javax.media.j3d.Shape3D;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector4d;

public class ClipBox {
	
	Point3d p1;
	Point3d p2;
	BranchGroup branchGroup;
	

	final static double offsetFactor=0.00001;
	
	public ClipBox(Point3d p1,Point3d p2){
		this.p1=p1;
		this.p2=p2;
	}
	
	private void getMinMax(double[] min,double max[],double extended){
		double[] p1Values=new double[3];
		double[] p2Values=new double[3];
		p1.get(p1Values);
		p2.get(p2Values);
		
		for(int i=0;i<3;i++){
			min[i]=(1-extended)*Math.min(p1Values[i],p2Values[i]);
			max[i]=(1+extended)*Math.max(p1Values[i],p2Values[i]);
		}
		
	}
	
	public BranchGroup getShape(){
		if(branchGroup==null){
			double[][] ptValues=new double[2][3];
			getMinMax(ptValues[0],ptValues[1],0);
			LineArray la=new LineArray(24,LineArray.COORDINATES | LineArray.COLOR_3);
			int[][] w=new int[][]{{0,0,0},{1,1,0},{1,0,1},{0,1,1}};
			Color3f color=new Color3f(1,0,0);
			for(int i=0;i<4;i++){
				int[] v=w[i];
				la.setCoordinate(6*i,new Point3d(ptValues[v[0]][0],ptValues[v[1]][1],ptValues[v[2]][2]));
				la.setColor(6*i,color);
				la.setCoordinate(6*i+1,new Point3d(ptValues[((v[0]+1)%2)][0],ptValues[v[1]][1],ptValues[v[2]][2]));
				la.setColor(6*i+1,color);
				
				la.setCoordinate(6*i+2,new Point3d(ptValues[v[0]][0],ptValues[v[1]][1],ptValues[v[2]][2]));
				la.setColor(6*i+2,color);
				la.setCoordinate(6*i+3,new Point3d(ptValues[v[0]][0],ptValues[((v[1]+1)%2)][1],ptValues[v[2]][2]));
				la.setColor(6*i+3,color);
				
				la.setCoordinate(6*i+4,new Point3d(ptValues[v[0]][0],ptValues[v[1]][1],ptValues[v[2]][2]));
				la.setColor(6*i+4,color);
				la.setCoordinate(6*i+5,new Point3d(ptValues[v[0]][0],ptValues[v[1]][1],ptValues[((v[2]+1)%2)][2]));
				la.setColor(6*i+5,color);
			}
			
			Shape3D s=new Shape3D();
			s.addGeometry(la);
			branchGroup=new BranchGroup();
			branchGroup.addChild(s);
			
		}
		return branchGroup;
	}
	
	public Vector4d[] getClipBoxPlanes(){
		
		Vector4d[] toReturn=new Vector4d[6];
		double[] min=new double[3];
		double[] max=new double[3];
		getMinMax(min,max,offsetFactor);
		double[] plane;
		Vector4d plane4d;
		
		
		//xmin plane 
		plane=new double[]{-1,0,0,min[0]};
		plane4d=new Vector4d(plane);
		toReturn[0]=plane4d;
		
		
		//xmax plane
		plane=new double[]{-1,0,0,max[0]};
		plane4d=new Vector4d(plane);
		plane4d.scale(-1);
		toReturn[1]=plane4d;
		
		//ymin plane 
		plane=new double[]{0,-1,0,min[1]};
		plane4d=new Vector4d(plane);
		toReturn[2]=plane4d;
		//ymax plane
		plane=new double[]{0,-1,0,max[1]};
		plane4d=new Vector4d(plane);
		plane4d.scale(-1);
		toReturn[3]=plane4d;
		
		//zmin plane 
		plane=new double[]{0,0,-1,min[2]};
		plane4d=new Vector4d(plane);
		toReturn[4]=plane4d;
		//zmax plane
		plane=new double[]{0,0,-1,max[2]};
		plane4d=new Vector4d(plane);
		plane4d.scale(-1);
		toReturn[5]=plane4d;
		
		return toReturn;
		
	}
	
}
