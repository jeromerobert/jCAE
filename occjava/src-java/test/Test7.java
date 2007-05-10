package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.jcae.opencascade.jni.*;

/** Test native stream */
public class Test7
{
	public static void main(String[] args)
	{
		try
		{
			TopoDS_Shape shape=new BRepPrimAPI_MakeSphere(new double[]{1,1,1}, 1).shape();
			File f=File.createTempFile("occjava", "brep");
			f.deleteOnExit();
			FileChannel c = new FileOutputStream(f).getChannel();
			BRepTools.write(shape, c);
			c.close();
			TopoDS_Shape s=new TopoDS_Shape();
			BRepTools.read(new TopoDS_Shape(),
				new FileInputStream(f).getChannel(),new BRep_Builder());
			TopoDS_Solid solid=(TopoDS_Solid)TopoDS_Shape.downcast(s);
			System.out.println(solid);
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
}
