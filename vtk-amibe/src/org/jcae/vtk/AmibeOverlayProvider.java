package org.jcae.vtk;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * @author Julian Ibarz
 */
public class AmibeOverlayProvider
{
	public static final String FREE_EDGE = "FreeEdges";
	public static final String MULTI_EDGE = "MultiEdges";
	public static final Color FREE_EDGE_COLOR = Color.RED;
	public static final Color MULTI_EDGE_COLOR = Color.MAGENTA;

	private final File directory;
	private final String flag;
	private final Element subMesh;
	private static Document parseXML(File file)
		throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		DocumentBuilder builder=factory.newDocumentBuilder();

		builder.setEntityResolver(new ClassPathEntityResolver());
		Document document=builder.parse(file);
		document.normalize();

		return document;
	}
	/**
	 * @param directory The directory containing the jcae3d file
	 * @param flag 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public AmibeOverlayProvider(File directory, String flag)
			throws ParserConfigurationException, SAXException, IOException
	{
		this.directory = directory;
		this.flag = flag;
		File jcae3d = new File(directory, "jcae3d");
		Document document = parseXML(jcae3d);
		subMesh = getSubMeshElement(document);
	}

	private Element getSubMeshElement(Document document)
	{
		NodeList nl = document.getElementsByTagName("submesh");
		for (int i = 0; i < nl.getLength(); i++)
		{
			Element e = (Element) nl.item(i);
			NodeList nl1 = e.getElementsByTagName("flag");
			if (nl1.getLength() > 0)
			{
				Element flagElement = (Element) nl1.item(0);
				if (flagElement.getAttribute("value").equals(flag))
					return e;
			}
		}
		return null;
	}

	public File getDirectory()
	{
		return directory;
	}

	public Element getSubMesh()
	{
		return subMesh;
	}
}

