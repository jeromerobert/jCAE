package org.jcae.viewer3d.post;

import java.awt.Color;

public interface ColorMapper
{
	int map(float value);
	byte[] getPalette();
	Color mapColor(float value);
	void mapColor(float value, int[] dst, int index);
}
