/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2008, by EADS France

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.amibe.validation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.Mesh;

/**
 * Factory to retrieve QualityProcedure instances by their class name.
 *
 * @author Denis Barbier
 */
public class QualityProcedureFactory
{
	private final static Logger LOGGER = Logger.getLogger(QualityProcedureFactory.class.getName());

	Constructor<QualityProcedure> cons;
	public QualityProcedureFactory(String className)
	{
		cons = null;
		try
		{
			Class<QualityProcedure> clazz = (Class<QualityProcedure>) Class.forName(className);
			cons = clazz.getConstructor();
		}
		catch (ClassNotFoundException ex)
		{
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (NoSuchMethodException ex)
		{
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	public QualityProcedure buildQualityProcedure()
	{
		if (cons == null)
			return null;
		try
		{
			return cons.newInstance();
		} catch (InstantiationException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (IllegalArgumentException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (InvocationTargetException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		return null;
	}
	
	public Mesh buildMesh()
	{
		QualityProcedure qproc = buildQualityProcedure();
		if (qproc == null)
			return null;
		return new Mesh(qproc.getMeshTraitsBuilder());
	}
}
