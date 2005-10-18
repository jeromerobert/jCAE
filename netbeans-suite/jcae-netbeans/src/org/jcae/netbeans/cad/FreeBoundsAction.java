package org.jcae.netbeans.cad;

import java.awt.Color;
import org.jcae.netbeans.viewer3d.View3D;
import org.jcae.opencascade.jni.BRep_Builder;
import org.jcae.opencascade.jni.ShapeAnalysis_FreeBounds;
import org.jcae.opencascade.jni.TopoDS_Compound;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

public class FreeBoundsAction extends CookieAction
{
	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected Class[] cookieClasses()
	{
		return new Class[]{ShapeCookie.class};
	}

	protected void performAction(Node[] arg0)
	{
		BRep_Builder bb = new BRep_Builder();
		TopoDS_Compound tc=new TopoDS_Compound();
		bb.makeCompound(tc);		
		for(int i=0; i<arg0.length; i++)
		{
			TopoDS_Shape shape = GeomUtils.getShape(arg0[i]);
			ShapeAnalysis_FreeBounds safb=new ShapeAnalysis_FreeBounds(shape);
			bb.add(tc, safb.getClosedWires());
			bb.add(tc, safb.getOpenWires());
		}
		View3D v=View3D.getView3D();
		OCCProvider occp=new OCCProvider(tc);
		occp.setEdgeColor(Color.GREEN);
		ViewableCAD viewable = new ViewableCAD(occp);
		viewable.setName(arg0[0].getName()+" free edges");
		viewable.setLineWidth(3f);
		v.getView().add(viewable);
	}

	public String getName()
	{
		return "Free edges";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}
	
	protected boolean asynchronous()
	{
		return false;
	}
}
