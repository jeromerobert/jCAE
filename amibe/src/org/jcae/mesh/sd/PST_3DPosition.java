package org.jcae.mesh.sd;
import org.jcae.opencascade.jni.*;

/**
 * @author cb
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PST_3DPosition extends PST_Position 
{
	double x;
	double y;
	double z;


    public PST_3DPosition(double xx,double yy, double zz) 
    {
        this.x = xx;
        this.y = yy;
        this.z = zz;
    }
	
	public void setXYZ(double xx,double yy, double zz)
	{
		this.x = xx;
		this.y = yy;
		this.z = zz;
	}

	public TopoDS_Shape getShape() {return null;	}
}
