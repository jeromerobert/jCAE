/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.vtk;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
/**
 * @author Jerome Robert
 */
public class ClassPathEntityResolver implements EntityResolver
{		
	public InputSource resolveEntity(String publicId, String systemId)	
	{
		try
		{
			URI uri=new URI(systemId);
			if(uri.getScheme().equals("classpath"))
			{				
				String path=uri.getPath();
				//remove leading "/"
				path=path.substring(1);
				Logger.getLogger(ClassPathEntityResolver.class.getName()).fine("resolve "+systemId+" from CLASSPATH at "+path);
				InputStream in= ClassLoader.getSystemResourceAsStream(path);
				if(in==null)
					return new InputSource(new StringReader(""));
				return new InputSource(in);
			}
			return null;
		}
		catch(URISyntaxException ex)
		{
			ex.printStackTrace();
			return null;
		}
	}	
}
