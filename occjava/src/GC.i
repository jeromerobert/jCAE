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
 *
 * @author Jens Schmidt
 *
 */
 
 %{
 #include <GC_MakeArcOfCircle.hxx>
 #include <GC_MakeSegment.hxx>
 #include <GCE2d_Root.hxx>
 #include <GCE2d_MakeSegment.hxx>
 %}
 
 class GC_MakeArcOfCircle {
   %rename(value) Value;
   public:
   GC_MakeArcOfCircle(const gp_Pnt& P1,const gp_Pnt& P2,const gp_Pnt& P3);
   GC_MakeArcOfCircle(const gp_Circ& Circ,const gp_Pnt& P1,const gp_Pnt& P2,const Standard_Boolean Sense);
   const Handle_Geom_TrimmedCurve& Value() const;
 };
 
 class GC_MakeSegment {
	%rename(value) Value;
    public:
    GC_MakeSegment(const gp_Pnt& P1, const gp_Pnt& P2);
 	const Handle_Geom_TrimmedCurve& Value() const;
 };
 
 class  GCE2d_Root {
	Handle_Geom_Curve()=0;	
 };

class GCE2d_MakeSegment  : public GCE2d_Root
{
	%rename(value) Value;
	public:
  	GCE2d_MakeSegment(const gp_Pnt2d& P1, const gp_Pnt2d& P2);
	Handle_Geom2d_TrimmedCurve& Value () const;
};


 