package org.jcae.viewer3d.post;

import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.media.j3d.*;
import javax.vecmath.*;

import org.jcae.opencascade.Utilities;
import org.jcae.opencascade.jni.BRep_Builder;
import org.jcae.opencascade.jni.BRep_Tool;
import org.jcae.opencascade.jni.GeomAPI_ProjectPointOnSurf;
import org.jcae.opencascade.jni.Geom_Surface;
import org.jcae.opencascade.jni.TopoDS_Compound;
import org.jcae.opencascade.jni.TopoDS_Face;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.viewer3d.PickViewable;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.bg.ViewableBG;
import org.jcae.viewer3d.cad.CADDomain;
import org.jcae.viewer3d.cad.CADProvider;
import org.jcae.viewer3d.cad.CADSelection;
import org.jcae.viewer3d.cad.FaceMesh;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;
import com.sun.j3d.utils.image.TextureLoader;

/**
 * A special View which allows to fit a texture on a given geometry.
 * It helps to compute the transformation which will match the texture
 * on the geometry.
 * The texture is mapped with an orthogonal projection on the geometry.
 * The projection is done by OpenGL. The transformation applied on the
 * texture before being projected is computed using 2 user specified triangles,
 * one in the geometry space (3D) and one in the texture space (3D with z=0).
 * If the specified triangles are not similar (the angles of one are equal
 * to the corresponding angles of the other) the computed transformation
 * is normalized to ensure that the projection is not deformant.
 * @author Jerome Robert
 */
public class TextureFitter extends View
{
	private static final long serialVersionUID = 2147414791584387916L;

	/**
	 * A special ViewableCAD which allows picking points on a surface.
	 * Other kind of picking are not availables (use the ViewableCAD if
	 * you need them).
	 */
	public static class PickViewableCAD extends ViewableCAD
	{
		private Point3d pickingPoint;
		private int faceID;
		private TopoDS_Shape shape;
		public PickViewableCAD(CADProvider provider, TopoDS_Shape shape)
		{		
			super(provider);
			this.shape=shape;
		}
		
		public void pick(PickViewable result)
		{
			Object o=getPickUserData(result);
			if((o instanceof ViewableCAD.FacePickingInfo))
			{
				FacePickingInfo fpi=(FacePickingInfo) o;
				pickingPoint = result.getIntersection().getPointCoordinates();
				faceID=fpi.id;
				fireSelectionChanged();
			}
		}
		
		/** Return the coordinates of the last picked point on a surface */
		public double[] getLastPick()
		{
			if(pickingPoint!=null)
			{
				TopoDS_Face face = Utilities.getFace(shape, faceID);
				Geom_Surface geom = BRep_Tool.surface(face);
				double[] point=new double[3];
				pickingPoint.get(point);
				GeomAPI_ProjectPointOnSurf proj=new GeomAPI_ProjectPointOnSurf(point, geom);
				if(proj.nbPoints()>0)
					return proj.point(1);
				else
					return point;
			}
			else
				return null;
		}
		
		public void setSelectionMode(short mode) {
			throw new UnsupportedOperationException();
		}
		
		public void unselectAll()
		{
			//do nothing, particularly do not fire event about unselecting
		}
	}

	private static Transform3D computeTransform(
		Point3d[] triangle2d, Point3d[] triangle3d, int width, int height, boolean normalize)
	{
		//3D -> 2D matrix
		Transform3D trsf1=normalizeTriangle(triangle2d);
		trsf1.mulInverse(normalizeTriangle(triangle3d));

		//force orthogonal and uniform scale matrix		
		if(normalize)
		{
			trsf1.normalizeCP();		
			trsf1.setScale(trsf1.getScale());
		}

		//bitmap 2D -> 1x1 square
		Matrix4d m2d=new Matrix4d();
		m2d.setM00(1.0/width);
		m2d.setM11(1.0/height);
		m2d.setM22(1);
		m2d.setM33(1);
		
		Transform3D t2d=new Transform3D(m2d);
		t2d.mul(trsf1);
		
		return t2d;		
	}
	
	private static GeometryArray createGeometry(OCCProvider occProvider)
	{	
		int[] ids=occProvider.getDomainIDs();

		int nbint=0;
		int nbfl=0;
		ArrayList meshes=new ArrayList();
		for(int i=0; i<ids.length; i++)
		{
			CADDomain d = (CADDomain) occProvider.getDomain(i);
			Iterator it = d.getFaceIterator();
			if(it!=null)
			while(it.hasNext())
			{
				FaceMesh fm=(FaceMesh) it.next();
				nbint += fm.getMesh().length;
				nbfl+=fm.getNodes().length;
				meshes.add(fm);
			}
		}
		
		int[] trias=new int[nbint];
		float[] nodes=new float[nbfl];
		int destPosInt=0, destPosFl=0;
		for(int i=0; i<meshes.size(); i++)
		{
			FaceMesh fm=(FaceMesh) meshes.get(i);
			int[] m=fm.getMesh();
			float[] n=fm.getNodes();
			System.arraycopy(n, 0, nodes, destPosFl, n.length);
				
			System.arraycopy(m, 0, trias, destPosInt, m.length);
			for(int j=0; j<m.length; j++)
			{
				trias[destPosInt+j]+=destPosFl/3;
			}
			destPosInt+=m.length;
			destPosFl+=n.length;
		}
		
		IndexedTriangleArray ita=new IndexedTriangleArray(
			nodes.length/3,GeometryArray.COORDINATES, trias.length);
		ita.setCoordinates(0, nodes);
		ita.setCoordinateIndices(0, trias);
		return ita;
	}
	/**
	 * Return selected faces in a viewable 
	 * @param viewable The viewable on witch picking has been done
	 * @param shape The shape from which faces are selected. This is the one
	 * used to create the viewable.
	 * @return A compound including all selected faces
	 */
	public static TopoDS_Compound getSelectFaces(ViewableCAD viewable, TopoDS_Shape shape)
	{
		CADSelection[] ss=viewable.getSelection();
		ArrayList faces=new ArrayList();
		for(int i=0; i<ss.length; i++)
		{
			int[] ids=ss[i].getFaceIDs();
			for(int j=0; j<ids.length; j++)
				faces.add(Utilities.getFace(shape, ids[j]));							
		}
		
		if(faces.size()>0)
		{
			BRep_Builder bb=new BRep_Builder();
			TopoDS_Compound compound=new TopoDS_Compound();
			bb.makeCompound(compound);
			
			for(int i=0; i<faces.size(); i++)
				bb.add(compound, (TopoDS_Shape) faces.get(i));
			
			return compound;
		}
		else
			return null;
	}

	/**
	 * Compute the normalized transformation of triangle src to triangle dst
	 * @param dst The 3 transformed points
	 * @param src The 3 points to transform
	 */
	public static Matrix4d getTransform(Point3d[] dst, Point3d[] src)
	{		
		return getTransform(dst, src, true);
	}
	
	/**
	 * Compute the transformation of triangle src to triangle dst
	 * @param dst The 3 transformed points
	 * @param src The 3 points to transform
	 * @param true to normalize the transformation
	 */
	public static Matrix4d getTransform(Point3d[] dst, Point3d[] src, boolean normalize)
	{		
	    Matrix4d m=new Matrix4d();
        Transform3D trsf1=computeTransform(dst, src, 1, 1, normalize);
        trsf1.get(m);
        return m;
	}
	
	/**
	 * Return the scaling factor in the 3 direction for the given
	 * transformation. For texture fitting the 3 values should be equals, so
	 * using this method is a way to control the validity of the input points.
	 * @return a vector containing scaling for x, y and z
	 */
	public static Vector3d getScaling(Matrix4d m)
	{
		Vector3d toReturn=new Vector3d();
		Vector4d v=new Vector4d();
		m.getColumn(0, v);
		toReturn.x=v.length();
		m.getColumn(1, v);
		toReturn.y=v.length();
		m.getColumn(2, v);
		toReturn.z=v.length();
		return toReturn;
	}
	
	/**
	 * Return the sum in absolute value of scalar product of column vector of
	 * the matrix. A value close to 0 means that the input points used to
	 * define the texture fitting are good. A big value means they are not.
	 */
	public static double getOrthogonality(Matrix4d m)
	{
		double toReturn=0;
		Vector4d v4d1=new Vector4d();
		Vector4d v4d2=new Vector4d();
		Vector4d v4d3=new Vector4d();		
		m.getColumn(0, v4d1);
		m.getColumn(1, v4d2);
		m.getColumn(2, v4d3);
		toReturn+=Math.abs(v4d1.dot(v4d2));
		toReturn+=Math.abs(v4d1.dot(v4d3));
		toReturn+=Math.abs(v4d2.dot(v4d3));
		return toReturn;
	}
	
	public static void displayMatrixInfo(Matrix4d matrix)
	{
		System.out.println("Matrix4d: "+matrix);
		Vector4d vx=new Vector4d();
		Vector4d vy=new Vector4d();
		Vector4d vz=new Vector4d();
		Vector4d vt=new Vector4d();
		
		matrix.getColumn(0, vx);
		System.out.println("X scale: "+vx.length());
		matrix.getColumn(1, vy);
		System.out.println("Y scale: "+vy.length());
		matrix.getColumn(2, vz);
		System.out.println("Z scale: "+vz.length());
		matrix.getColumn(3, vt);
		System.out.println("translation: "+vt);

		System.out.println("x.y : "+vx.dot(vy));
		System.out.println("x.z : "+vx.dot(vz));
		System.out.println("y.z : "+vy.dot(vz));
		
		
		Vector3d eulerAngles=new Vector3d();
		Quat4d rotation=new Quat4d();
		matrix.get(rotation);
		getEulerAngles(eulerAngles, rotation);
		System.out.println("theta: "+eulerAngles.x*180/Math.PI);
		System.out.println("phi: "+eulerAngles.y*180/Math.PI);
		System.out.println("psi: "+eulerAngles.z*180/Math.PI);
	}
	
	private static Transform3D normalized(Transform3D t3d)
	{	
		Matrix4d matrix=new Matrix4d();
		t3d.get(matrix);
		Vector4d v=new Vector4d();
		
		matrix.getColumn(0, v);
		double scaleX=v.length();
		matrix.getColumn(1, v);
		double scaleY=v.length();
		matrix.getColumn(2, v);
		double scaleZ=v.length();

		Vector3d translation=new Vector3d();
		t3d.get(translation);
		Quat4d rotation=new Quat4d();
		matrix.get(rotation);				
		return new Transform3D(rotation, translation, (scaleX+scaleY+scaleZ)/3);
	}
	
	/** 
	 * from
	 * http://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
     */
	
	/*public static void getEulerAngles(Tuple3d angles, Quat4d quat)
	{
		double q0=quat.x;
		double q1=quat.y;
		double q2=quat.z;
		double q3=quat.w;		
		angles.x=Math.atan2(2*(q0*q1+q2*q3), 1-2*(q1*q1+q2+q2));
		angles.y=Math.asin(2*(q0*q2-q3*q1));
		angles.z=Math.atan2(2*(q0*q3+q1*q2), 1-2*(q2*q2+q3*q3));
	}*/
	
	
	/**
	 * from
	 * http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToEuler/
	 */
	public static void getEulerAngles(Tuple3d angles, Quat4d q1)
	{
		double heading, attitude, bank;
		double sqw = q1.w * q1.w;
		double sqx = q1.x * q1.x;
		double sqy = q1.y * q1.y;
		double sqz = q1.z * q1.z;
		double unit = sqx + sqy + sqz + sqw; // if normalised is one,
												// otherwise is correction
												// factor
		double test = q1.x * q1.y + q1.z * q1.w;
		if (test > 0.499 * unit)
		{ // singularity at north pole
			heading = 2 * Math.atan2(q1.x, q1.w);
			attitude = Math.PI / 2;
			bank = 0;
		}
		else if (test < -0.499 * unit)
		{ // singularity at south pole
			heading = -2 * Math.atan2(q1.x, q1.w);
			attitude = -Math.PI / 2;
			bank = 0;
		} else
		{
			heading = Math.atan2(2 * q1.y * q1.w - 2 * q1.x * q1.z, sqx - sqy
				- sqz + sqw);
			attitude = Math.asin(2 * test / unit);
			bank = Math.atan2(2 * q1.x * q1.w - 2 * q1.y * q1.z, -sqx + sqy
				- sqz + sqw);
		}
		angles.x = heading;
		angles.y = attitude;
		angles.z = bank;
	}
	
	/**
	 * Compute the transformation which will transform the triangle (O, x, y) to
	 * (p1, p2, p3)
	 */
	private static Transform3D normalizeTriangle(Point3d[] p)
	{
		Vector3d imgX=new Vector3d();
		Vector3d imgY=new Vector3d();
		Vector3d imgZ=new Vector3d();
		
		imgX.sub(p[1], p[0]);
		imgY.sub(p[2], p[0]);
		imgZ.cross(imgX, imgY);
		imgZ.scale((imgX.length()+imgY.length())/imgZ.length()/2);		
		Matrix3d rotation=new Matrix3d();
		rotation.setColumn(0, imgX);
		rotation.setColumn(1, imgY);
		rotation.setColumn(2, imgZ);
		Transform3D toReturn=new Transform3D();
		toReturn.set(rotation, new Vector3d(p[0]), 1);
		return toReturn;		
	}

	private BufferedImage image;

	private TexCoordGeneration texCoordGeneration;
	
	private ViewableBG textureViewable;
	
	/**
	 * @param frame the window owning the widget
	 */
	public TextureFitter(Window frame)
	{
		super(frame, false, true);
	}
	
	private Appearance createAppearance(Point3d[] triangle2d, Point3d[] triangle3d,
		boolean normalize)
	{		
		Appearance toReturn=new Appearance();
		Texture theTexture = createTexture(image);				
		texCoordGeneration = new TexCoordGeneration(
        	TexCoordGeneration.EYE_LINEAR,
        	TexCoordGeneration.TEXTURE_COORDINATE_2);
		texCoordGeneration.setCapability(TexCoordGeneration.ALLOW_PLANE_WRITE);
		updateTexture(triangle2d, triangle3d, normalize);
        toReturn.setTexture(theTexture);
        toReturn.setTexCoordGeneration(texCoordGeneration);                
		return toReturn;
	}
	
	private Texture createTexture(BufferedImage image)
	{
		TextureLoader tl=new TextureLoader(image, TextureLoader.ALLOW_NON_POWER_OF_TWO);
		Map map=queryProperties();
		int textureWidthMax=((Integer)map.get("textureWidthMax")).intValue();
		int textureHeightMax=((Integer)map.get("textureHeightMax")).intValue();
		boolean textureNonPowerOfTwoAvailable=
			((Boolean)map.get("textureNonPowerOfTwoAvailable")).booleanValue();
		
		while(true)
		{
			ImageComponent2D img=tl.getImage();
			if(img.getWidth()<=textureWidthMax && img.getHeight()<=textureHeightMax)
				break;
			tl=new TextureLoader(tl.getScaledImage(0.5f, 0.5f).getImage(),
				TextureLoader.ALLOW_NON_POWER_OF_TWO);
		}
		
		int flags=TextureLoader.GENERATE_MIPMAP;
		if(textureNonPowerOfTwoAvailable)
			flags=flags|TextureLoader.ALLOW_NON_POWER_OF_TWO;
		
		tl=new TextureLoader(tl.getImage().getImage(), flags);
				
		return tl.getTexture();
	}
	
	/** 
	 * Display the texture with a normalized projection
	 * @param shape The shape on which the texture must be displayed
	 * @param triangle2d The 2D points (z=0) picked on the bitmap
	 * @param triangle3d The 3D points picked on the geometry
	 * @param image The image to display
	 * @param true to normalize
	 */
	public void displayTexture(TopoDS_Shape shape,
		Point3d[] triangle2d, Point3d[] triangle3d, BufferedImage image)
	{
		displayTexture(shape, triangle2d, triangle3d, image, true);
	}
	
	/** 
	 * @param shape The shape on which the texture must be displayed
	 * @param triangle2d The 2D points (z=0) picked on the bitmap
	 * @param triangle3d The 3D points picked on the geometry
	 * @param image The image to display
	 * @param true to normalize
	 */
	public void displayTexture(TopoDS_Shape shape,
		Point3d[] triangle2d, Point3d[] triangle3d, BufferedImage image,
		boolean normalize)
	{
		this.image=image;
		OCCProvider occProvider=new OCCProvider(shape);
		Shape3D shape3D=new Shape3D(createGeometry(occProvider));
		shape3D.setAppearance(createAppearance(triangle2d, triangle3d, normalize));
		BranchGroup bg=new BranchGroup();
		bg.addChild(shape3D);
		textureViewable=new ViewableBG(bg);
		remove(textureViewable);
		add(textureViewable);
	}
	
	/** 
	 * Move the texture. The projection is normalized.
	 * @param triangle2d The 2D points (z=0) picked on the bitmap
	 * @param triangle3d The 3D points picked on the geometry
	 */
	public void updateTexture(Point3d[] triangle2d, Point3d[] triangle3d)
	{
		updateTexture(triangle2d, triangle3d, true);
	}

	/** 
	 * Move the texture
	 * @param triangle2d The 2D points (z=0) picked on the bitmap
	 * @param triangle3d The 3D points picked on the geometry
	 * @param true to normalize
	 */
	public void updateTexture(Point3d[] triangle2d, Point3d[] triangle3d,
		boolean normalize)
	{
        
		Matrix4f m=new Matrix4f();
        Transform3D trsf1=computeTransform(triangle2d, triangle3d,
        	image.getWidth(), -image.getHeight(), normalize);
        trsf1.get(m);
        Vector4f vS=new Vector4f();
        Vector4f vT=new Vector4f();
        m.getRow(0, vS);
        m.getRow(1, vT);
        texCoordGeneration.setPlaneS(vS);
        texCoordGeneration.setPlaneT(vT);
	}
	
	/**
	 * Display 3D properties for debugging purpose
	 * It use Canvas3D.queryProperties
	 * @param out The PrintStream where to print (i.e. System.out)
	 */
	public void print3DProperties(PrintStream out)
	{
		Map map=queryProperties();
		Iterator it=map.entrySet().iterator();
		while(it.hasNext())
		{
			Entry e = (Entry)it.next();
			System.out.println(e.getKey()+" "+e.getValue());
		}
	}	
}
