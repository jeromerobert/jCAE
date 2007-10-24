package org.jcae.viewer3d.fe.amibe;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.viewer3d.Domain;
import org.jcae.viewer3d.fe.FEDomainAdaptor;
import org.jcae.viewer3d.fe.FEProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AmibeOverlayProvider implements FEProvider
{
	private File directory;	
	private long lastUpdateTime;
	private File jcae3d;
	public static String FREE_EDGE="FreeEdges";
	public static String MULTI_EDGE="MultiEdges";
	public static Color FREE_EDGE_COLOR=Color.RED;
	public static Color MULTI_EDGE_COLOR=Color.MAGENTA;

	private String flag;
	private Element subMesh;
	
	/**
	 * @param directory The directory containing the jcae3d file
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public AmibeOverlayProvider(File directory, String flag) 
		throws ParserConfigurationException, SAXException, IOException
	{
		this.directory=directory;
		this.flag=flag;
		jcae3d = new File(directory, "jcae3d");
		load();
	}
	
	private Element getSubMeshElement(Document document)
	{
		NodeList nl=document.getElementsByTagName("submesh");
		for(int i=0; i<nl.getLength(); i++)
		{
			Element e=(Element) nl.item(i);
			NodeList nl1=e.getElementsByTagName("flag");
			if(nl1.getLength()>0)
			{
				Element flagElement=(Element) nl1.item(0);
				if(flagElement.getAttribute("value").equals(flag))
				{
					return e;
				}
			}				
		}
		return null;		
	}
	
	private void load()
		throws ParserConfigurationException, SAXException, IOException
	{				
		lastUpdateTime=jcae3d.lastModified();
		Document document = AmibeProvider.parseXML(jcae3d);
		subMesh=getSubMeshElement(document);
	}
	
	/* (non-Javadoc)
	 * @see jcae.viewer3d.DomainProvider#getDomainIDs()
	 */
	public int[] getDomainIDs()
	{
		if(subMesh==null)
			return new int[0];
		return new int[]{0};
	}

	
	private Color getColor()
	{
		if(MULTI_EDGE==flag)
			return MULTI_EDGE_COLOR;
		return FREE_EDGE_COLOR;
	}
	/* (non-Javadoc)
	 * @see jcae.viewer3d.DomainProvider#getDomain(int)
	 */
	public Domain getDomain(int id)
	{
		try
		{
			if(jcae3d.lastModified()!=lastUpdateTime)
				load();
			return new AmibeBeanDomain(directory, subMesh, getColor());
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return new FEDomainAdaptor();		
		}
		catch(ParserConfigurationException ex)
		{
			ex.printStackTrace();
			return new FEDomainAdaptor();
		}
		catch (SAXException e)
		{
			e.printStackTrace();
			return new FEDomainAdaptor();
		}
	}
}

