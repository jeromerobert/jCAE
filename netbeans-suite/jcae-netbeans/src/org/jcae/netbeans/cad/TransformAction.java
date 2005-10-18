package org.jcae.netbeans.cad;

import javax.swing.JMenu;
import org.jcae.netbeans.Utilities;
import org.jcae.opencascade.jni.BRepBuilderAPI_Transform;
import org.jcae.opencascade.jni.GP_Trsf;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.openide.util.actions.NodeAction;
import org.openide.util.actions.Presenter;
import org.openide.util.actions.SystemAction;

public abstract class TransformAction extends CookieAction
{
	static public class AllActions extends NodeAction
	{
		protected boolean enable(Node[] arg0)
		{
			return true;
		}

		public HelpCtx getHelpCtx()
		{
			return null;
		}

		public javax.swing.JMenuItem getMenuPresenter()
		{
			JMenu toReturn = new JMenu(getName());
			toReturn.add(((Presenter.Menu) SystemAction.get(Translate.class))
				.getMenuPresenter());
			toReturn.add(((Presenter.Menu) SystemAction.get(Rotate.class))
				.getMenuPresenter());
			return toReturn;
		}

		public String getName()
		{
			return "Transform";
		}

		public javax.swing.JMenuItem getPopupPresenter()
		{
			return getMenuPresenter();
		}

		protected void performAction(Node[] arg0)
		{
		}
	}
	static public class Rotate extends TransformAction
	{
		public String getName()
		{
			return "Rotation";
		}

		protected Object getParameters()
		{
			return new Rotation();
		}

		protected GP_Trsf getTrsf(Object parameter)
		{
			Rotation r=(Rotation) parameter;
			GP_Trsf theTransformation = new GP_Trsf();
			double[] axisStruct = new double[]{
				r.getAxisX1(), r.getAxisY1(), r.getAxisZ1(),
				r.getAxisX2(), r.getAxisY2(), r.getAxisZ2()};
			theTransformation.setRotation(axisStruct, r.getAngle());
			return theTransformation;
		}

	}
	
	static public class Translate extends TransformAction
	{
		public String getName()
		{
			return "Translation";
		}

		protected Object getParameters()
		{
			return new Translation();
		}

		protected GP_Trsf getTrsf(Object parameter)
		{
			Translation r=(Translation) parameter;
			GP_Trsf theTransformation = new GP_Trsf();
			theTransformation.setTranslation(new double[]{r.getX(), r.getY(), r.getZ()});
			return theTransformation;
		}
	}



	protected Class[] cookieClasses()
	{
		return new Class[]{ShapeCookie.class};
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}

	protected abstract Object getParameters();
	protected abstract GP_Trsf getTrsf(Object parameter);

	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected void performAction(Node[] arg0)
	{
		Object param = getParameters();
		if (Utilities.showEditBeanDialog(param))
		{
			GP_Trsf trsf = getTrsf(param);
			for (int i = 0; i < arg0.length; i++)
			{
				TopoDS_Shape s = GeomUtils.getShape(arg0[i]);
				BRepBuilderAPI_Transform bt = new BRepBuilderAPI_Transform(s,
					trsf, true);
				TopoDS_Shape newShape=bt.shape();
				GeomUtils.insertShape(newShape, getName(),
					arg0[i].getParentNode());
				GeomUtils.getParentBrep(arg0[i]).getDataObject().setModified(true);
			}
		}
	}
}
