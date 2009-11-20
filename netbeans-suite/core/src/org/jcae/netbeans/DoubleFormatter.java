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
 * (C) Copyright 2005-2009, by EADS France
 */


package org.jcae.netbeans;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.ParseException;
import java.util.Locale;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;

/**
 *
 * @author Jerome Robert
 */

public class DoubleFormatter extends DecimalFormat
{
	private static final DecimalFormat f1 = new DecimalFormat("0.#####E0");
	private static final DecimalFormat f2 = new DecimalFormat("####.####");
	private static final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
	static
	{
		symbols.setGroupingSeparator(' ');
		f1.setDecimalFormatSymbols(symbols);
		f2.setDecimalFormatSymbols(symbols);
	}
	public DoubleFormatter()
	{
		setDecimalFormatSymbols(symbols);
	}

	@Override
	public StringBuffer format(double number, StringBuffer toAppendTo,
		FieldPosition pos)
	{
		if(number == 0)
		{
			toAppendTo.append("0");
		}
		else
		{
			String s1 = f1.format(number);
			String s2 = f2.format(number);
			if (s1.length() >= s2.length() && !"0".equals(s2))
				toAppendTo.append(s2);
			else
				toAppendTo.append(s1);
		}
		return toAppendTo;
	}
	public final static AbstractFormatterFactory FACTORY = new AbstractFormatterFactory()
	{
		@Override
		public AbstractFormatter getFormatter(JFormattedTextField tf)
		{
			return new AbstractFormatter()
			{
				private final DoubleFormatter formatter = new DoubleFormatter();
				@Override
				public Object stringToValue(String text) throws ParseException
				{
					try
					{
						return Double.valueOf(text);
					}
					catch (NumberFormatException ex)
					{
						throw new ParseException(ex.getMessage(), 0);
					}
				}

				@Override
				public String valueToString(Object value) throws ParseException
				{
					if (value == null)
						return "NaN";
					else
						return formatter.format(value);
				}
			};
		}
	};
}