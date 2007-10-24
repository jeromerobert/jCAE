package org.jcae.viewer3d.test;

import java.awt.Color;
import javax.swing.JFrame;
import org.jcae.viewer3d.Domain;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.fe.FEDomainAdaptor;
import org.jcae.viewer3d.fe.FEProvider;
import org.jcae.viewer3d.fe.ViewableFE;

/**
 * This is an example to show how to display quads in jcae-viewe3d 
 */
public class TestFEQuad implements FEProvider
{
	/**
	 * This is a group of quad elements
	 * @see FEDomain
	 */
	private static  class TestDomain extends FEDomainAdaptor
	{
		private float[] nodes;
		private int[] quads;
		private Color color;
		
		/**
		 * 
		 * @param nodes The nodes coordinates {x1, y1, z1, x2, y2, z2 ...}
		 * @param quads The indices of nodes. The ID of the first node is 0.
		 * @param color The color of the domain.
		 */
		public TestDomain(float[] nodes, int[] quads, Color color)
		{
			this.nodes=nodes;
			this.quads=quads;
			this.color=color;
		}
		
		public int getNumberOfNodes()
		{
			return nodes.length/3;
		}
		
		public int getNumberOfQuad4()
		{
			return quads.length/4;
		}
		
		public float[] getNodes()
		{
			return nodes;
		}
		
		public int[] getQuad4()
		{			
			return quads;
		}
		
		public Color getColor()
		{
			return color;
		}
	}


	private Domain[] domains;

	public TestFEQuad()
	{	
		TestDomain domain1=new TestDomain(new float[]{
			// the nodes coordinates
			0,0,0,
			0,1,0,
			1,1,0,
			1,0,0,
			2,0,0,
			2,1,0
		}, new int[]{
			// the quad indices
			0, 1, 2, 3,
			3, 4, 5, 2
		}, Color.WHITE);

		TestDomain domain2=new TestDomain(new float[]{
			// the nodes coordinates
			0,0,0.1f,
			0,1,0.2f,
			1,1,0.3f,
			1,0,0.4f,
			2,0,0.5f,
			2,1,0.6f
		}, new int[]{
			// the quad indices
			0, 1, 2, 3,
			3, 4, 5, 2
			// the color of the domain
		}, Color.RED);

		domains=new Domain[]{domain1, domain2};
	}
	
	public void show()
	{
		JFrame frame=new JFrame();
		View view=new View(frame);
		ViewableFE viewable=new ViewableFE(this);
		viewable.setShowShapeLine(true);
		view.add(viewable);
		view.fitAll();
		view.setOriginAxisVisible(true);
		frame.getContentPane().add(view);
		frame.setVisible(true);	
	}
	
	public static void main(String[] args)
	{
		new TestFEQuad().show();
	}

	public Domain getDomain(int id)
	{
		return domains[id];
	}

	public int[] getDomainIDs()
	{
		// we have 2 domains with ID 0 and 1
		return new int[]{0, 1};
	}
}
