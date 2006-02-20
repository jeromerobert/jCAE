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
 * (C) Copyright 2005, by EADS CRC
 */

/**
 * Shape_Enum
 */
// rename ShapeEnum so it match first implementation of libOccJava
%rename(COMPOUND) TopAbs_COMPOUND;
%rename(COMPSOLID) TopAbs_COMPSOLID;
%rename(SOLID) TopAbs_SOLID;
%rename(SHELL) TopAbs_SHELL;
%rename(FACE) TopAbs_FACE;
%rename(WIRE) TopAbs_WIRE;
%rename(EDGE) TopAbs_EDGE;
%rename(VERTEX) TopAbs_VERTEX;
%rename(SHAPE) TopAbs_SHAPE;
	
enum TopAbs_ShapeEnum 
{
	TopAbs_COMPOUND,
	TopAbs_COMPSOLID,
	TopAbs_SOLID,
	TopAbs_SHELL,
	TopAbs_FACE,
	TopAbs_WIRE,
	TopAbs_EDGE,
	TopAbs_VERTEX,
	TopAbs_SHAPE
};

%rename(FORWARD) TopAbs_FORWARD;
%rename(REVERSED) TopAbs_REVERSED;
%rename(INTERNAL) TopAbs_INTERNAL;
%rename(EXTERNAL) TopAbs_EXTERNAL;
enum TopAbs_Orientation
{
	TopAbs_FORWARD,
	TopAbs_REVERSED,
	TopAbs_INTERNAL,
	TopAbs_EXTERNAL
};
