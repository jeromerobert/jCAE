package org.jcae.opencascade.jni;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.jcae.opencascade.Utilities;

/** Test native stream */
public class NativeStream
{
	@Test public void reader()
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
			System.out.println("read");
			assertTrue(BRepTools.read(s, new FileInputStream(f).getChannel(),new BRep_Builder()));
			Utilities.dumpTopology(s);
			
			//Test reading an empty brep file
			s=new TopoDS_Shape();
			f=File.createTempFile("occjava", "brep");
			f.deleteOnExit();
			c = new FileInputStream(f).getChannel();
			assertFalse(BRepTools.read(s, c, new BRep_Builder()));
			c.close();			
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
}
