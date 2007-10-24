package org.jcae.viewer3d.fd;



public class GridUtil {
	final static int X_DIR=0;
	final static int Y_DIR=1;
	final static int Z_DIR=2;
	
	private FDProvider aFDProvider;
	
	public GridUtil(FDProvider aFDProvider){
		this.aFDProvider=aFDProvider;
	}
	
	/*
	 * returns the cell index of point p.
	 * The index is ranged between 0 and gridCount-2;
	 * The returned index is set to -1 if the point is out of the grid
	 */
	public int getXCellIndex(double p){
		int[] range=new int[2];
		range[0]=0;
		range[1]=getCellCount(X_DIR)-1;
		if(!checkInside(X_DIR,p)) return -1;
		getCell(X_DIR,range,p);
		return range[0];
	}
	
	public int getYCellIndex(double p){
		int[] range=new int[2];
		range[0]=0;
		range[1]=getCellCount(Y_DIR)-1;
		if(!checkInside(Y_DIR,p)) return -1;
		getCell(Y_DIR,range,p);
		return range[0];
	}
	
	public int getZCellIndex(double p){
		int[] range=new int[2];
		range[0]=0;
		range[1]=getCellCount(Z_DIR)-1;
		if(!checkInside(Z_DIR,p)) return -1;
		getCell(Z_DIR,range,p);
		return range[0];
	}

	/*
	 * Checks if the point is in the min/max interval.
	 */
	
	private boolean checkInside(int type,double p){
		double max=getCellValue(getCellCount(type)-1,type);
		double min=getCellValue(0,type);
		return (min<=p && max>=p);
	}
	/*
	 * Returns the cell number of the point p
	 * the returned index is ranged between min and max-1
	 * where min is stored in range[0] and max in range[1]
	 */
	private void getCell(int type,int[] range,double p){

		int posi=(int)Math.floor((range[0]+range[1])/2.);		
		double posd=getCellValue(posi,type);
		 
		if(p<posd) range[1]=posi;
		else if(p>posd) range[0]=posi;
		else {
			range[0]=posi;
			range[1]=posi;
		}
		
		if((range[1]-range[0])<=1) return;
		getCell(type,range,p);
		return;
	}
	
	private double getCellValue(int index,int type){
		switch(type){
		case X_DIR : return aFDProvider.getXGrid(index);
		case Y_DIR : return aFDProvider.getYGrid(index);
		case Z_DIR : return aFDProvider.getZGrid(index);
		}
		return 0.;
	}
	
	private int getCellCount(int type){
		switch(type){
		case X_DIR : return aFDProvider.getXGridCount();
		case Y_DIR : return aFDProvider.getYGridCount();
		case Z_DIR : return aFDProvider.getZGridCount();
		}
		return 0;
	}
	
}
