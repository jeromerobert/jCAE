package org.jcae.netbeans.cad;

import javax.swing.JMenu;
import org.jcae.opencascade.jni.BRepAlgoAPI_BooleanOperation;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.openide.util.actions.NodeAction;
import org.openide.util.actions.Presenter;
import org.openide.util.actions.SystemAction;

public abstract class BooleanAction extends CookieAction
{
	static public class AllActions extends NodeAction
	{
		public javax.swing.JMenuItem getMenuPresenter() 
		{
			JMenu toReturn=new JMenu(getName());
			toReturn.add(((Presenter.Menu)
				SystemAction.get(Common.class)).getMenuPresenter());
			toReturn.add(((Presenter.Menu)
				SystemAction.get(Fuse.class)).getMenuPresenter());
			toReturn.add(((Presenter.Menu)
				SystemAction.get(Cut.class)).getMenuPresenter());
			toReturn.add(((Presenter.Menu)
				SystemAction.get(ReversedCut.class)).getMenuPresenter());
			toReturn.add(((Presenter.Menu)
				SystemAction.get(Section.class)).getMenuPresenter());
			return toReturn;
		}

	    public javax.swing.JMenuItem getPopupPresenter() {
	    	return getMenuPresenter();
	    }

		protected void performAction(Node[] arg0)
		{
		}

		protected boolean enable(Node[] arg0)
		{
			return true;
		}

		public String getName()
		{
			return "Boolean";
		}

		public HelpCtx getHelpCtx()
		{
			return null;
		}
	}
	
	static public class Common extends BooleanAction
	{

		public String getName()
		{
			return "Common";
		}

		protected short getTransformation()
		{
			return 0;
		}		
	}

	static public class Cut extends BooleanAction
	{

		public String getName()
		{
			return "Cut";
		}

		protected short getTransformation()
		{
			return 2;
		}		
	}

	static public class Fuse extends BooleanAction
	{

		public String getName()
		{
			return "Fuse";
		}

		protected short getTransformation()
		{
			return 1;
		}		
	}

	static public class ReversedCut extends BooleanAction
	{

		public String getName()
		{
			return "Reversed Cut";
		}

		protected short getTransformation()
		{
			return 3;
		}		
	}

	static public class Section extends BooleanAction
	{

		public String getName()
		{
			return "Section";
		}

		protected short getTransformation()
		{
			return 4;
		}		
	}
	

	protected Class[] cookieClasses()
	{
		return new Class[]{ShapeCookie.class};
	}
	
	protected boolean enable(Node[] arg0)
	{
		return arg0.length==2;
	}
	
	public HelpCtx getHelpCtx()
	{
		return null;
	}
	
	abstract protected  short getTransformation();

	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected void performAction(Node[] arg0)
	{
		if(arg0.length==2)
		{
			TopoDS_Shape shape1 = GeomUtils.getShape(arg0[0]);
			TopoDS_Shape shape2 = GeomUtils.getShape(arg0[1]);
			BRepAlgoAPI_BooleanOperation bt=new BRepAlgoAPI_BooleanOperation(shape1, shape2, getTransformation());			
			GeomUtils.insertShape(bt.shape(), getName(),
				arg0[0].getParentNode());
			GeomUtils.getParentBrep(arg0[0]).getDataObject().setModified(true);
		}
	}
	
	
}
