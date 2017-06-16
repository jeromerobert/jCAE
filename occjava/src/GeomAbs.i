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
 * (C) Copyright 2009, by EADS France
 */

/**
 * GeomAbs_Shape
 */
// rename GeomAbs_Shape to make it more Java friendly
%rename(C0) GeomAbs_C0;
%rename(G1) GeomAbs_G1;
%rename(C1) GeomAbs_C1;
%rename(G2) GeomAbs_G2;
%rename(C2) GeomAbs_C2;
%rename(C3) GeomAbs_C3;
%rename(CN) GeomAbs_CN;

enum GeomAbs_Shape
{
    GeomAbs_C0,
    GeomAbs_G1,
    GeomAbs_C1,
    GeomAbs_G2,
    GeomAbs_C2,
    GeomAbs_C3,
    GeomAbs_CN
};

enum GeomAbs_JoinType
{
    GeomAbs_Arc,
    GeomAbs_Tangent,
    GeomAbs_Intersection
};
