/*  Implementation in Java of Shewchuk's predicates.                         */
/*  November 2003   Denis Barbier                                            */
/*  This code is released under the GNU General Public License               */
/*  Copyright of the original C file follows:                                */
/*===========================================================================*/
/*                                                                           */
/*  Routines for Arbitrary Precision Floating-point Arithmetic               */
/*  and Fast Robust Geometric Predicates                                     */
/*  (predicates.c)                                                           */
/*                                                                           */
/*  May 18, 1996                                                             */
/*                                                                           */
/*  Placed in the public domain by                                           */
/*  Jonathan Richard Shewchuk                                                */
/*  School of Computer Science                                               */
/*  Carnegie Mellon University                                               */
/*  5000 Forbes Avenue                                                       */
/*  Pittsburgh, Pennsylvania  15213-3891                                     */
/*  jrs@cs.cmu.edu                                                           */
/*                                                                           */
/*  This file contains C implementation of algorithms for exact addition     */
/*    and multiplication of floating-point numbers, and predicates for       */
/*    robustly performing the orientation and incircle tests used in         */
/*    computational geometry.  The algorithms and underlying theory are      */
/*    described in Jonathan Richard Shewchuk.  "Adaptive Precision Floating- */
/*    Point Arithmetic and Fast Robust Geometric Predicates."  Technical     */
/*    Report CMU-CS-96-140, School of Computer Science, Carnegie Mellon      */
/*    University, Pittsburgh, Pennsylvania, May 1996.  (Submitted to         */
/*    Discrete & Computational Geometry.)                                    */
/*                                                                           */
/*  Original file, the paper listed above, and other information are         */
/*    available from the Web page http://www.cs.cmu.edu/~quake/robust.html . */
/*===========================================================================*/

package org.jcae.mesh.util;

public class Predicates {
	private static double epsilon = -1.0;
	private static double splitter = -1.0;
	private static double resulterrbound;
	private static double ccwerrboundA, ccwerrboundB, ccwerrboundC;
	private static double iccerrboundA, iccerrboundB, iccerrboundC;

	public Predicates()
	{
		if (epsilon < 0.0)
			init();
	}

/* Many of the operations are broken up into two pieces, a main part that    */
/*   performs an approximate operation, and a "tail" that computes the       */
/*   roundoff error of that operation.                                       */
/*                                                                           */
/* The operations Fast_Two_Sum(), Fast_Two_Diff(), Two_Sum(), Two_Diff(),    */
/*   Split(), and Two_Product() are all implemented as described in the      */
/*   reference.  Each of these macros requires certain variables to be       */
/*   defined in the calling routine.  The variables `bvirt', `c', `abig',    */
/*   `_i', `_j', `_k', `_l', `_m', and `_n' are declared `INEXACT' because   */
/*   they store the result of an operation that may incur roundoff error.    */
/*   The input parameter `x' (or the highest numbered `x_' parameter) must   */
/*   also be declared `INEXACT'.                                             */

/*
#define Fast_Two_Sum_Tail(a, b, x, y) \
  bvirt = x - a; \
  y = b - bvirt

#define Fast_Two_Sum(a, b, x, y) \
  x = (REAL) (a + b); \
  Fast_Two_Sum_Tail(a, b, x, y)
*/
	private static double [] Fast_Two_Sum(double a, double b)
	{
		double [] res = new double[2];
		res[0] = (double) (a + b);
		double bvirt = res[0] - a;
		res[1] = b - bvirt;
		return res;
	}

/*
#define Fast_Two_Diff_Tail(a, b, x, y) \
  bvirt = a - x; \
  y = bvirt - b

#define Fast_Two_Diff(a, b, x, y) \
  x = (REAL) (a - b); \
  Fast_Two_Diff_Tail(a, b, x, y)
*/
	private static double [] Fast_Two_Diff(double a, double b)
	{
		double [] res = new double[2];
		res[0] = (double) (a - b);
		double bvirt = a - res[0];
		res[1] = bvirt - b;
		return res;
	}

/*
#define Two_Sum_Tail(a, b, x, y) \
  bvirt = (REAL) (x - a); \
  avirt = x - bvirt; \
  bround = b - bvirt; \
  around = a - avirt; \
  y = around + bround

#define Two_Sum(a, b, x, y) \
  x = (REAL) (a + b); \
  Two_Sum_Tail(a, b, x, y)
*/
	private static double [] Two_Sum(double a, double b)
	{
		double [] res = new double[2];
		res[0] = (double) (a + b);
		double bvirt = (double) (res[0] - a);
		double avirt = res[0] - bvirt;
		double bround = b - bvirt;
		double around = a - avirt;
		res[1] = around + bround;
		return res;
	}

/*
#define Two_Diff_Tail(a, b, x, y) \
  bvirt = (REAL) (a - x); \
  avirt = x + bvirt; \
  bround = bvirt - b; \
  around = a - avirt; \
  y = around + bround
*/
	private static double Two_Diff_Tail(double a, double b, double x)
	{
		double bvirt = (double) (a - x);
		double avirt = x + bvirt;
		double bround = bvirt - b;
		double around = a - avirt;
		double y = around + bround;
		return y;
	}

/*
#define Two_Diff(a, b, x, y) \
  x = (REAL) (a - b); \
  Two_Diff_Tail(a, b, x, y)
*/
	private static double [] Two_Diff(double a, double b)
	{
		double [] res = new double[2];
		double x = (double) (a - b);
		res[0] = x;
		res[1] = Two_Diff_Tail(a, b, x);
		return res;
	}

/*
#define Split(a, ahi, alo) \
  c = (REAL) (splitter * a); \
  abig = (REAL) (c - a); \
  ahi = c - abig; \
  alo = a - ahi
*/
	private static double [] Split(double a)
	{
		double [] res = new double[2];
		double c = (double) (splitter * a);
		double abig = (double) (c - a);
		res[0] = c - abig;
		res[1] = a - res[0];
		return res;
	}

/*
#define Two_Product_Tail(a, b, x, y) \
  Split(a, ahi, alo); \
  Split(b, bhi, blo); \
  err1 = x - (ahi * bhi); \
  err2 = err1 - (alo * bhi); \
  err3 = err2 - (ahi * blo); \
  y = (alo * blo) - err3

#define Two_Product(a, b, x, y) \
  x = (REAL) (a * b); \
  Two_Product_Tail(a, b, x, y)
*/
	private static double [] Two_Product(double a, double b)
	{
		double [] res = new double[2];
		res[0] = (double) (a * b);
		double [] spa = Split(a); //  spa = (ahi,alo)
		double [] spb = Split(b); //  spb = (bhi,blo)
		double err1 = res[0] - (spa[0] * spb[0]);
		double err2 = err1 - (spa[1] * spb[0]);
		double err3 = err2 - (spa[0] * spb[1]);
		res[1] = (spa[1] * spb[1]) - err3;
		return res;
	}

/* Two_Product_Presplit() is Two_Product() where one of the inputs has       */
/*   already been split.  Avoids redundant splitting.                        */
/*
#define Two_Product_Presplit(a, b, bhi, blo, x, y) \
  x = (REAL) (a * b); \
  Split(a, ahi, alo); \
  err1 = x - (ahi * bhi); \
  err2 = err1 - (alo * bhi); \
  err3 = err2 - (ahi * blo); \
  y = (alo * blo) - err3
*/
	private static double [] Two_Product_Presplit(double a, double b, double bhi, double blo)
	{
		double [] res = new double[2];
		res[0] = (double) (a * b);
		double [] spa = Split(a); //  spa = (ahi,alo)
		double err1 = res[0] - (spa[0] * bhi);
		double err2 = err1 - (spa[1] * bhi);
		double err3 = err2 - (spa[0] * blo);
		res[1] = (spa[1] * blo) - err3;
		return res;
	}

/* Two_Product_2Presplit() is Two_Product() where both of the inputs have    */
/*   already been split.  Avoids redundant splitting.                        */
/*
#define Two_Product_2Presplit(a, ahi, alo, b, bhi, blo, x, y) \
  x = (REAL) (a * b); \
  err1 = x - (ahi * bhi); \
  err2 = err1 - (alo * bhi); \
  err3 = err2 - (ahi * blo); \
  y = (alo * blo) - err3
*/
	private static double [] Two_Product_2Presplit(double a, double ahi, double alo, double b, double bhi, double blo)
	{
		double [] res = new double[2];
		res[0] = (double) (a * b);
		double err1 = res[0] - (ahi * bhi);
		double err2 = err1 - (alo * bhi);
		double err3 = err2 - (ahi * blo);
		res[1] = (alo * blo) - err3;
		return res;
	}

/* Square() can be done more quickly than Two_Product().                     */
/*
#define Square_Tail(a, x, y) \
  Split(a, ahi, alo); \
  err1 = x - (ahi * ahi); \
  err3 = err1 - ((ahi + ahi) * alo); \
  y = (alo * alo) - err3

#define Square(a, x, y) \
  x = (REAL) (a * a); \
  Square_Tail(a, x, y)
*/
	private static double [] Square(double a)
	{
		double [] res = new double[2];
		res[0] = (double) (a * a);
		double [] spa = Split(a); //  spa = (ahi,alo)
		double err1 = res[0] - (spa[0] * spa[0]);
		double err3 = err1 - ((spa[0]+spa[0]) * spa[1]);
		res[1] = (spa[1] * spa[1]) - err3;
		return res;
	}

/* Macros for summing expansions of various fixed lengths.  These are all    */
/*   unrolled versions of Expansion_Sum().                                   */
/*
#define Two_One_Sum(a1, a0, b, x2, x1, x0) \
  Two_Sum(a0, b , _i, x0); \
  Two_Sum(a1, _i, x2, x1)

#define Two_Two_Sum(a1, a0, b1, b0, x3, x2, x1, x0) \
  Two_One_Sum(a1, a0, b0, _j, _0, x0); \
  Two_One_Sum(_j, _0, b1, x3, x2, x1)

//  DB: which is equivalent to
//  #define Two_Two_Sum(a1, a0, b1, b0, x3, x2, x1, x0) \
//    Two_Sum(a0, b0, _i, x0); \
//    Two_Sum(a1, _i, _j, _k); \
//    Two_Sum(_k, b1, _i, x1); \
//    Two_Sum(_j, _i, x3, x2);
*/
	private static void Two_Two_Sum(double a1, double a0, double b1, double b0, double [] res)
	{
		double [] tmp1 = Two_Sum(a0, b0);
		res[0] = tmp1[1];
		double [] tmp2 = Two_Sum(a1, tmp1[0]);
		tmp1 = Two_Sum(tmp2[1], b1);
		res[1] = tmp1[1];
		double [] tmp3 = Two_Sum(tmp2[0], tmp1[0]);
		res[2] = tmp3[1];
		res[3] = tmp3[0];
	}

/*
#define Two_One_Diff(a1, a0, b, x2, x1, x0) \
  Two_Diff(a0, b , _i, x0); \
  Two_Sum( a1, _i, x2, x1)

#define Two_Two_Diff(a1, a0, b1, b0, x3, x2, x1, x0) \
  Two_One_Diff(a1, a0, b0, _j, _0, x0); \
  Two_One_Diff(_j, _0, b1, x3, x2, x1)

//  DB: which is equivalent to
//  #define Two_Two_Diff(a1, a0, b1, b0, x3, x2, x1, x0) \
//    Two_Diff(a0, b0, _i, x0); \
//    Two_Sum(a1, _i, _j, _k); \
//    Two_Diff(_k, b1, _i, x1); \
//    Two_Sum(_j, _i, x3, x2);
*/
	private static void Two_Two_Diff(double a1, double a0, double b1, double b0, double [] x)
	{
		double [] tmp1 = Two_Diff(a0, b0);
		x[0] = tmp1[1];
		double [] tmp2 = Two_Sum(a1, tmp1[0]);
		tmp1 = Two_Diff(tmp2[1], b1);
		x[1] = tmp1[1];
		double [] tmp3 = Two_Sum(tmp2[0], tmp1[0]);
		x[2] = tmp3[1];
		x[3] = tmp3[0];
	}


/* Macros for multiplying expansions of various fixed lengths.               */
/*
#define Two_Two_Product(a1, a0, b1, b0, x7, x6, x5, x4, x3, x2, x1, x0) \
  Split(a0, a0hi, a0lo); \
  Split(b0, bhi, blo); \
  Two_Product_2Presplit(a0, a0hi, a0lo, b0, bhi, blo, _i, x0); \
  Split(a1, a1hi, a1lo); \
  Two_Product_2Presplit(a1, a1hi, a1lo, b0, bhi, blo, _j, _0); \
  Two_Sum(_i, _0, _k, _1); \
  Fast_Two_Sum(_j, _k, _l, _2); \
  Split(b1, bhi, blo); \
  Two_Product_2Presplit(a0, a0hi, a0lo, b1, bhi, blo, _i, _0); \
  Two_Sum(_1, _0, _k, x1); \
  Two_Sum(_2, _k, _j, _1); \
  Two_Sum(_l, _j, _m, _2); \
  Two_Product_2Presplit(a1, a1hi, a1lo, b1, bhi, blo, _j, _0); \
  Two_Sum(_i, _0, _n, _0); \
  Two_Sum(_1, _0, _i, x2); \
  Two_Sum(_2, _i, _k, _1); \
  Two_Sum(_m, _k, _l, _2); \
  Two_Sum(_j, _n, _k, _0); \
  Two_Sum(_1, _0, _j, x3); \
  Two_Sum(_2, _j, _i, _1); \
  Two_Sum(_l, _i, _m, _2); \
  Two_Sum(_1, _k, _i, x4); \
  Two_Sum(_2, _i, _k, x5); \
  Two_Sum(_m, _k, x7, x6)
*/
	private static double [] Two_Two_Product(double a1, double a0, double b1, double b0)
	{
		double [] res = new double[8];
		double [] a0split, a1split, bsplit, tmp1, tmp2, tmp3, tmp4;

//  Split(a0, a0hi, a0lo);
		a0split = Split(a0);
//  Split(b0, bhi, blo);
		bsplit = Split(b0);
//  Two_Product_2Presplit(a0, a0hi, a0lo, b0, bhi, blo, _i, x0);
		tmp1 = Two_Product_2Presplit(a0, a0split[0], a0split[1], b0, bsplit[0], bsplit[1]);
		res[0] = tmp1[1];
//  Split(a1, a1hi, a1lo);
		a1split = Split(a1);
//  Two_Product_2Presplit(a1, a1hi, a1lo, b0, bhi, blo, _j, _0);
		tmp2 = Two_Product_2Presplit(a1, a1split[0], a1split[1], b0, bsplit[0], bsplit[1]);
//  Two_Sum(_i, _0, _k, _1);
		tmp3 = Two_Sum(tmp1[0], tmp2[1]);
//  Fast_Two_Sum(_j, _k, _l, _2);
		tmp1 = Fast_Two_Sum(tmp2[0], tmp3[0]);
//  Split(b1, bhi, blo);
		bsplit = Split(b1);
//  Two_Product_2Presplit(a0, a0hi, a0lo, b1, bhi, blo, _i, _0);
		tmp2 = Two_Product_2Presplit(a0, a0split[0], a0split[1], b1, bsplit[0], bsplit[1]);
//  Two_Sum(_1, _0, _k, x1);
		tmp3 = Two_Sum(tmp3[1], tmp2[1]);
		res[1] = tmp3[1];
//  Two_Sum(_2, _k, _j, _1);
		tmp4 = Two_Sum(tmp1[1], tmp3[0]);
//  Two_Sum(_l, _j, _m, _2);
		tmp1 = Two_Sum(tmp1[0], tmp4[0]);
//  Two_Product_2Presplit(a1, a1hi, a1lo, b1, bhi, blo, _j, _0);
		tmp3 = Two_Product_2Presplit(a1, a1split[0], a1split[1], b1, bsplit[0], bsplit[1]);
//  Two_Sum(_i, _0, _n, _0);
		tmp2 = Two_Sum(tmp2[0], tmp3[1]);
//  Two_Sum(_1, _0, _i, x2);
		tmp4 = Two_Sum(tmp4[1], tmp2[1]);
		res[2] = tmp4[1];
//  Two_Sum(_2, _i, _k, _1);
		tmp4 = Two_Sum(tmp1[1], tmp4[0]);
//  Two_Sum(_m, _k, _l, _2);
		tmp1 = Two_Sum(tmp1[0], tmp4[0]);
//  Two_Sum(_j, _n, _k, _0);
		tmp2 = Two_Sum(tmp3[0], tmp2[0]);
//  Two_Sum(_1, _0, _j, x3);
		tmp3 = Two_Sum(tmp4[1], tmp2[1]);
		res[3] = tmp3[1];
//  Two_Sum(_2, _j, _i, _1);
		tmp4 = Two_Sum(tmp1[1], tmp3[0]);
//  Two_Sum(_l, _i, _m, _2);
		tmp1 = Two_Sum(tmp1[0], tmp4[0]);
//  Two_Sum(_1, _k, _i, x4);
		tmp2 = Two_Sum(tmp4[1], tmp2[0]);
		res[4] = tmp2[1];
//  Two_Sum(_2, _i, _k, x5);
		tmp3 = Two_Sum(tmp1[1], tmp2[0]);
		res[5] = tmp3[1];
//  Two_Sum(_m, _k, x7, x6)
		tmp2 = Two_Sum(tmp1[0], tmp3[0]);
		res[6] = tmp2[1];
		res[7] = tmp2[0];
		return res;
	}

	public static void init ()
	{
  		double half;
  		double check, lastcheck;
  		boolean every_other;

  		every_other = true;
  		half = 0.5;
  		epsilon = 1.0;
  		splitter = 1.0;
  		check = 1.0;
  /* Repeatedly divide `epsilon' by two until it is too small to add to    */
  /*   one without causing roundoff.  (Also check if the sum is equal to   */
  /*   the previous sum, for machines that round up instead of using exact */
  /*   rounding.  Not that this library will work on such machines anyway. */
  		do {
			lastcheck = check;
			epsilon *= half;
			if (every_other)
				splitter *= 2.0;
			every_other = !every_other;
			check = 1.0 + epsilon;
		} while ((check != 1.0) && (check != lastcheck));
  		splitter += 1.0;

  /* Error bounds for orientation and incircle tests. */
		resulterrbound = (3.0 + 8.0 * epsilon) * epsilon;
		ccwerrboundA = (3.0 + 16.0 * epsilon) * epsilon;
		ccwerrboundB = (2.0 + 12.0 * epsilon) * epsilon;
		ccwerrboundC = (9.0 + 64.0 * epsilon) * epsilon * epsilon;
		iccerrboundA = (10.0 + 96.0 * epsilon) * epsilon;
		iccerrboundB = (4.0 + 48.0 * epsilon) * epsilon;
		iccerrboundC = (44.0 + 576.0 * epsilon) * epsilon * epsilon;
	}

	public static void printEpsilon()
	{
		System.out.println(" resulterrbound="+resulterrbound);
		System.out.println("   ccwerrboundA="+ccwerrboundA);
		System.out.println("   ccwerrboundB="+ccwerrboundB);
		System.out.println("   ccwerrboundC="+ccwerrboundC);
		System.out.println("   iccerrboundA="+iccerrboundA);
		System.out.println("   iccerrboundB="+iccerrboundB);
		System.out.println("   iccerrboundC="+iccerrboundC);
	}

/*****************************************************************************/
/*                                                                           */
/*  doubleprint()   Print the bit representation of a double.                */
/*                                                                           */
/*  Useful for debugging exact arithmetic routines.                          */
/*                                                                           */
/*****************************************************************************/

	public static void doubleprint(double number)
	{
        long no = Double.doubleToRawLongBits(number);
		String bitStr = Long.toBinaryString(no);
		for (int i = bitStr.length(); i < 64; i++)
			System.out.print("0");
        System.out.println(bitStr);
	}

/*****************************************************************************/
/*                                                                           */
/*  expansion_print()   Print the bit representation of an expansion.        */
/*                                                                           */
/*  Useful for debugging exact arithmetic routines.                          */
/*                                                                           */
/*****************************************************************************/

	public static void expansion_print(int elen, double [] e)
	{
		for (int i = elen - 1; i >= 0; i--)
		{
			doubleprint(e[i]);
			if (i > 0)
				System.out.print(" +");
			System.out.println("");
		}
	}

/*****************************************************************************/
/*                                                                           */
/*  grow_expansion()   Add a scalar to an expansion.                         */
/*                                                                           */
/*  Sets h = e + b.  See the long version of my paper for details.           */
/*                                                                           */
/*  Maintains the nonoverlapping property.  If round-to-even is used (as     */
/*  with IEEE 754), maintains the strongly nonoverlapping and nonadjacent    */
/*  properties as well.  (That is, if e has one of these properties, so      */
/*  will h.)                                                                 */
/*                                                                           */
/*****************************************************************************/

	/* e and h can be the same. */
	private static double [] grow_expansion(int elen, double [] e, double b)
	{
		double Q;
		int eindex;
		double enow;
		double [] h = new double[elen+1];
	
		Q = b;
		for (eindex = 0; eindex < elen; eindex++) {
			enow = e[eindex];
			double [] s = Two_Sum(Q, enow);
			Q = s[0];
			h[eindex] = s[1];
		}
		h[eindex] = Q;
		return h;
	}

/*****************************************************************************/
/*                                                                           */
/*  grow_expansion_zeroelim()   Add a scalar to an expansion, eliminating    */
/*                              zero components from the output expansion.   */
/*                                                                           */
/*  Sets h = e + b.  See the long version of my paper for details.           */
/*                                                                           */
/*  Maintains the nonoverlapping property.  If round-to-even is used (as     */
/*  with IEEE 754), maintains the strongly nonoverlapping and nonadjacent    */
/*  properties as well.  (That is, if e has one of these properties, so      */
/*  will h.)                                                                 */
/*                                                                           */
/*****************************************************************************/

	private static double [] grow_expansion_zeroelim(int elen, double [] e, double b)
	{
		double Q;
		int eindex, hindex;
		double enow;
		double bvirt;
		double avirt, bround, around;
		double [] h = new double[elen+1];
	
		hindex = 0;
		Q = b;
		for (eindex = 0; eindex < elen; eindex++) {
			enow = e[eindex];
			double [] s = Two_Sum(Q, enow);
			Q = s[0];
			if (s[1] != 0.0) {
				h[hindex++] = s[1];
			}
		}
		if ((Q != 0.0) || (hindex == 0)) {
			h[hindex++] = Q;
		}
		double [] res = new double[hindex];
		System.arraycopy(h, 0, res, 0, hindex);
		return res;
	}

/*****************************************************************************/
/*                                                                           */
/*  expansion_sum()   Sum two expansions.                                    */
/*                                                                           */
/*  Sets h = e + f.  See the long version of my paper for details.           */
/*                                                                           */
/*  Maintains the nonoverlapping property.  If round-to-even is used (as     */
/*  with IEEE 754), maintains the nonadjacent property as well.  (That is,   */
/*  if e has one of these properties, so will h.)  Does NOT maintain the     */
/*  strongly nonoverlapping property.                                        */
/*                                                                           */
/*****************************************************************************/

	/* e and h can be the same, but f and h cannot. */
	private static double [] expansion_sum(int elen, double [] e, int flen, double [] f)
	{
		double Q;
		int findex, hindex, hlast;
		double hnow;
		double [] h = new double[elen+flen];
	
		Q = f[0];
		for (hindex = 0; hindex < elen; hindex++) {
			hnow = e[hindex];
			double [] s =Two_Sum(Q, hnow);
			Q = s[0];
			h[hindex] = s[1];
		}
		h[hindex] = Q;
		hlast = hindex;
		for (findex = 1; findex < flen; findex++) {
			Q = f[findex];
			for (hindex = findex; hindex <= hlast; hindex++) {
				hnow = h[hindex];
				double [] s = Two_Sum(Q, hnow);
				Q = s[0];
				h[hindex] = s[1];
			}
			h[++hlast] = Q;
		}
		return h;
	}

/*****************************************************************************/
/*                                                                           */
/*  fast_expansion_sum_zeroelim()   Sum two expansions, eliminating zero     */
/*                                  components from the output expansion.    */
/*                                                                           */
/*  Sets h = e + f.  See the long version of my paper for details.           */
/*                                                                           */
/*  If round-to-even is used (as with IEEE 754), maintains the strongly      */
/*  nonoverlapping property.  (That is, if e is strongly nonoverlapping, h   */
/*  will be also.)  Does NOT maintain the nonoverlapping or nonadjacent      */
/*  properties.                                                              */
/*                                                                           */
/*****************************************************************************/

	/* h cannot be e or f. */
	private static int fast_expansion_sum_zeroelim(int elen, double [] e, int flen, double [] f, double [] h)
	{
		double Q;
		int eindex, findex, hindex;
		double enow, fnow;
		double [] s;
	
		enow = e[0];
		fnow = f[0];
		eindex = findex = 0;
		if ((fnow > enow) == (fnow > -enow)) {
			Q = enow;
			enow = e[++eindex];
		} else {
			Q = fnow;
			fnow = f[++findex];
		}
		hindex = 0;
		if ((eindex < elen) && (findex < flen)) {
			if ((fnow > enow) == (fnow > -enow)) {
				s = Fast_Two_Sum(enow, Q);
				++eindex;
				if (eindex < elen)
					enow = e[eindex];
			} else {
				s = Fast_Two_Sum(fnow, Q);
				++findex;
				if (findex < flen)
					fnow = f[findex];
			}
			Q = s[0];
			if (s[1] != 0.0) {
				h[hindex++] = s[1];
			}
			while ((eindex < elen) && (findex < flen)) {
				if ((fnow > enow) == (fnow > -enow)) {
					s = Two_Sum(Q, enow);
					++eindex;
					if (eindex < elen)
						enow = e[eindex];
				} else {
					s = Two_Sum(Q, fnow);
					++findex;
					if (findex < flen)
						fnow = f[findex];
				}
				Q = s[0];
				if (s[1] != 0.0) {
					h[hindex++] = s[1];
				}
			}
		}
		while (eindex < elen) {
			s = Two_Sum(Q, enow);
			++eindex;
			if (eindex < elen)
				enow = e[eindex];
			Q = s[0];
			if (s[1] != 0.0) {
				h[hindex++] = s[1];
			}
		}
		while (findex < flen) {
			s = Two_Sum(Q, fnow);
			++findex;
			if (findex < flen)
				fnow = f[findex];
			Q = s[0];
			if (s[1] != 0.0) {
				h[hindex++] = s[1];
			}
		}
		if ((Q != 0.0) || (hindex == 0)) {
			h[hindex++] = Q;
		}
		return hindex;
	}

/*****************************************************************************/
/*                                                                           */
/*  scale_expansion_zeroelim()   Multiply an expansion by a scalar,          */
/*                               eliminating zero components from the        */
/*                               output expansion.                           */
/*                                                                           */
/*  Sets h = be.  See either version of my paper for details.                */
/*                                                                           */
/*  Maintains the nonoverlapping property.  If round-to-even is used (as     */
/*  with IEEE 754), maintains the strongly nonoverlapping and nonadjacent    */
/*  properties as well.  (That is, if e has one of these properties, so      */
/*  will h.)                                                                 */
/*                                                                           */
/*****************************************************************************/

	/* e and h cannot be the same. */
	private static int scale_expansion_zeroelim(int elen, double [] e, double b, double [] h)
	{
		double Q, sum;
		int eindex, hindex;
		double enow;
		double c;
	
		double [] bsplit = Split(b);
		double [] tmp1 = Two_Product_Presplit(e[0], b, bsplit[0], bsplit[1]);
		Q = tmp1[0];
		hindex = 0;
		if (tmp1[1] != 0) {
			h[hindex++] = tmp1[1];
		}
		for (eindex = 1; eindex < elen; eindex++) {
			enow = e[eindex];
			double [] product = Two_Product_Presplit(enow, b, bsplit[0], bsplit[1]);
			tmp1 = Two_Sum(Q, product[1]);
			sum = tmp1[0];
			if (tmp1[1] != 0) {
				h[hindex++] = tmp1[1];
			}
			tmp1 = Fast_Two_Sum(product[0], sum);
			Q = tmp1[0];
			if (tmp1[1] != 0) {
				h[hindex++] = tmp1[1];
			}
		}
		if ((Q != 0.0) || (hindex == 0)) {
			h[hindex++] = Q;
		}
		return hindex;
	}

/*****************************************************************************/
/*                                                                           */
/*  estimate()   Produce a one-word estimate of an expansion's value.        */
/*                                                                           */
/*  See either version of my paper for details.                              */
/*                                                                           */
/*****************************************************************************/

	private static double estimate(int elen, double [] e)
	{
		double Q;
		int eindex;
	
		Q = e[0];
		for (eindex = 1; eindex < elen; eindex++) {
			Q += e[eindex];
		}
		return Q;
	}

/*****************************************************************************/
/*                                                                           */
/*  counterclockwise()   Return a positive value if the points pa, pb, and   */
/*                       pc occur in counterclockwise order; a negative      */
/*                       value if they occur in clockwise order; and zero    */
/*                       if they are collinear.  The result is also a rough  */
/*                       approximation of twice the signed area of the       */
/*                       triangle defined by the three points.               */
/*                                                                           */
/*  Uses exact arithmetic if necessary to ensure a correct answer.  The      */
/*  result returned is the determinant of a matrix.  This determinant is     */
/*  computed adaptively, in the sense that exact arithmetic is used only to  */
/*  the degree it is needed to ensure that the returned value has the        */
/*  correct sign.  Hence, this function is usually quite fast, but will run  */
/*  more slowly when the input points are collinear or nearly so.            */
/*                                                                           */
/*  See my Robust Predicates paper for details.                              */
/*                                                                           */
/*****************************************************************************/

	private static double counterclockwiseadapt(double [] pa, double [] pb, double [] pc, double detsum)
	{
		double acx, acy, bcx, bcy;
		double [] acxbcy, acybcx;
		double acxtail, acytail, bcxtail, bcytail;
		double detleft, detright;
		double detlefttail, detrighttail;
		double det, errbound;
		double [] B = new double[4];
		double [] C1 = new double[8];
		double [] C2 = new double[12];
		double [] D = new double[16];
		int C1length, C2length, Dlength;
		double [] u = new double[4];
		double [] s, t;

		double bvirt;
		double avirt, bround, around;
		double c;
		double abig;
		double ahi, alo, bhi, blo;
		double err1, err2, err3;
		double _i, _j;
		double _0;

		acx = (double) (pa[0] - pc[0]);
		bcx = (double) (pb[0] - pc[0]);
		acy = (double) (pa[1] - pc[1]);
		bcy = (double) (pb[1] - pc[1]);

		acxbcy = Two_Product(acx, bcy);
		acybcx = Two_Product(acy, bcx);

		Two_Two_Diff(acxbcy[0], acxbcy[1], acybcx[0], acybcx[1], B);

		det = estimate(4, B);
		errbound = ccwerrboundB * detsum;
		if ((det >= errbound) || (-det >= errbound)) {
		  return det;
		}

		acxtail = Two_Diff_Tail(pa[0], pc[0], acx);
		bcxtail = Two_Diff_Tail(pb[0], pc[0], bcx);
		acytail = Two_Diff_Tail(pa[1], pc[1], acy);
		bcytail = Two_Diff_Tail(pb[1], pc[1], bcy);

		if ((acxtail == 0.0) && (acytail == 0.0)
		    && (bcxtail == 0.0) && (bcytail == 0.0)) {
		  return det;
		}

		errbound = ccwerrboundC * detsum + resulterrbound * Math.abs(det);
		det += (acx * bcytail + bcy * acxtail)
		     - (acy * bcxtail + bcx * acytail);
		if ((det >= errbound) || (-det >= errbound)) {
		  return det;
		}

		s = Two_Product(acxtail, bcy);
		t = Two_Product(acytail, bcx);
		Two_Two_Diff(s[1], s[0], t[1], t[0], u);
		C1length = fast_expansion_sum_zeroelim(4, B, 4, u, C1);

		s = Two_Product(acx, bcytail);
		t = Two_Product(acy, bcxtail);
		Two_Two_Diff(s[1], s[0], t[1], t[0], u);
		C2length = fast_expansion_sum_zeroelim(C1length, C1, 4, u, C2);

		s = Two_Product(acxtail, bcytail);
		t = Two_Product(acytail, bcxtail);
		Two_Two_Diff(s[1], s[0], t[1], t[0], u);
		Dlength = fast_expansion_sum_zeroelim(C2length, C2, 4, u, D);

		return(D[Dlength - 1]);
	}

	public static double counterclockwise(double [] pa, double [] pb, double [] pc)
	{
		double detleft, detright, det;
		double detsum, errbound;

		detleft = (pa[0] - pc[0]) * (pb[1] - pc[1]);
		detright = (pa[1] - pc[1]) * (pb[0] - pc[0]);
		det = detleft - detright;

		if (detleft > 0.0) {
		  if (detright <= 0.0) {
		    return det;
		  } else {
		    detsum = detleft + detright;
		  }
		} else if (detleft < 0.0) {
		  if (detright >= 0.0) {
		    return det;
		  } else {
		    detsum = -detleft - detright;
		  }
		} else {
		  return det;
		}

		errbound = ccwerrboundA * detsum;
		if ((det >= errbound) || (-det >= errbound)) {
		  return det;
		}

		return counterclockwiseadapt(pa, pb, pc, detsum);
	}

/*****************************************************************************/
/*                                                                           */
/*  orient2dfast()   Approximate 2D orientation test.  Nonrobust.            */
/*  orient2dexact()   Exact 2D orientation test.  Robust.                    */
/*  orient2dslow()   Another exact 2D orientation test.  Robust.             */
/*  orient2d()   Adaptive exact 2D orientation test.  Robust.                */
/*                                                                           */
/*               Return a positive value if the points pa, pb, and pc occur  */
/*               in counterclockwise order; a negative value if they occur   */
/*               in clockwise order; and zero if they are collinear.  The    */
/*               result is also a rough approximation of twice the signed    */
/*               area of the triangle defined by the three points.           */
/*                                                                           */
/*  Only the first and last routine should be used; the middle two are for   */
/*  timings.                                                                 */
/*                                                                           */
/*  The last three use exact arithmetic to ensure a correct answer.  The     */
/*  result returned is the determinant of a matrix.  In orient2d() only,     */
/*  this determinant is computed adaptively, in the sense that exact         */
/*  arithmetic is used only to the degree it is needed to ensure that the    */
/*  returned value has the correct sign.  Hence, orient2d() is usually quite */
/*  fast, but will run more slowly when the input points are collinear or    */
/*  nearly so.                                                               */
/*                                                                           */
/*****************************************************************************/

	public static double orient2dfast(double [] pa, double [] pb, double [] pc)
	{
		double acx, bcx, acy, bcy;
	
		acx = pa[0] - pc[0];
		bcx = pb[0] - pc[0];
		acy = pa[1] - pc[1];
		bcy = pb[1] - pc[1];
		return acx * bcy - acy * bcx;
	}
	
	private static double orient2dadapt(double [] pa, double [] pb, double [] pc, double detsum)
	{
		double acx, acy, bcx, bcy;
		double acxtail, acytail, bcxtail, bcytail;
		double [] detl, detr;
		double det, errbound;
		double [] B = new double[4];
		double [] C1 = new double[8];
		double [] C2 = new double[12];
		double [] D = new double[16];
		int C1length, C2length, Dlength;
		double [] u = new double[4];
		double [] s, t;
	
		acx = (double) (pa[0] - pc[0]);
		bcx = (double) (pb[0] - pc[0]);
		acy = (double) (pa[1] - pc[1]);
		bcy = (double) (pb[1] - pc[1]);
	
		detl = Two_Product(acx, bcy);
		detr = Two_Product(acy, bcx);
	
		Two_Two_Diff(detl[0], detl[1], detr[0], detr[1], B);
	
		det = estimate(4, B);
		errbound = ccwerrboundB * detsum;
		if ((det >= errbound) || (-det >= errbound)) {
			return det;
		}
	
		acxtail = Two_Diff_Tail(pa[0], pc[0], acx);
		bcxtail = Two_Diff_Tail(pb[0], pc[0], bcx);
		acytail = Two_Diff_Tail(pa[1], pc[1], acy);
		bcytail = Two_Diff_Tail(pb[1], pc[1], bcy);
	
		if ((acxtail == 0.0) && (acytail == 0.0)
				&& (bcxtail == 0.0) && (bcytail == 0.0)) {
			return det;
		}
	
		errbound = ccwerrboundC * detsum + resulterrbound * Math.abs(det);
		det += (acx * bcytail + bcy * acxtail)
		     - (acy * bcxtail + bcx * acytail);
		if ((det >= errbound) || (-det >= errbound)) {
			return det;
		}
	
		s = Two_Product(acxtail, bcy);
		t = Two_Product(acytail, bcx);
		Two_Two_Diff(s[0], s[1], t[0], t[1], u);
		C1length = fast_expansion_sum_zeroelim(4, B, 4, u, C1);
	
		s = Two_Product(acx, bcytail);
		t = Two_Product(acy, bcxtail);
		Two_Two_Diff(s[0], s[1], t[0], t[1], u);
		C2length = fast_expansion_sum_zeroelim(C1length, C1, 4, u, C2);
	
		s = Two_Product(acxtail, bcytail);
		t = Two_Product(acytail, bcxtail);
		Two_Two_Diff(s[0], s[1], t[0], t[1], u);
		Dlength = fast_expansion_sum_zeroelim(C2length, C2, 4, u, D);
	
		return(D[Dlength - 1]);
	}
	
	public static double orient2d(double [] pa, double [] pb, double [] pc)
	{
		double detleft, detright, det;
		double detsum, errbound;
	
		detleft = (pa[0] - pc[0]) * (pb[1] - pc[1]);
		detright = (pa[1] - pc[1]) * (pb[0] - pc[0]);
		det = detleft - detright;
	
		if (detleft > 0.0) {
			if (detright <= 0.0) {
				return det;
			} else {
				detsum = detleft + detright;
			}
		} else if (detleft < 0.0) {
			if (detright >= 0.0) {
				return det;
			} else {
				detsum = -detleft - detright;
			}
		} else {
			return det;
		}
	
		errbound = ccwerrboundA * detsum;
		if ((det >= errbound) || (-det >= errbound)) {
			return det;
		}
	
		return orient2dadapt(pa, pb, pc, detsum);
	}

/*****************************************************************************/
/*                                                                           */
/*  incirclefast()   Approximate 2D incircle test.  Nonrobust.               */
/*  incircleexact()   Exact 2D incircle test.  Robust.                       */
/*  incircleslow()   Another exact 2D incircle test.  Robust.                */
/*  incircle()   Adaptive exact 2D incircle test.  Robust.                   */
/*                                                                           */
/*               Return a positive value if the point pd lies inside the     */
/*               circle passing through pa, pb, and pc; a negative value if  */
/*               it lies outside; and zero if the four points are cocircular.*/
/*               The points pa, pb, and pc must be in counterclockwise       */
/*               order, or the sign of the result will be reversed.          */
/*                                                                           */
/*  Only the first and last routine should be used; the middle two are for   */
/*  timings.                                                                 */
/*                                                                           */
/*  The last three use exact arithmetic to ensure a correct answer.  The     */
/*  result returned is the determinant of a matrix.  In incircle() only,     */
/*  this determinant is computed adaptively, in the sense that exact         */
/*  arithmetic is used only to the degree it is needed to ensure that the    */
/*  returned value has the correct sign.  Hence, incircle() is usually quite */
/*  fast, but will run more slowly when the input points are cocircular or   */
/*  nearly so.                                                               */
/*                                                                           */
/*****************************************************************************/

	public static double incirclefast(double [] pa, double [] pb, double [] pc, double [] pd)
	{
		double adx, ady, bdx, bdy, cdx, cdy;
		double abdet, bcdet, cadet;
		double alift, blift, clift;
	
		adx = pa[0] - pd[0];
		ady = pa[1] - pd[1];
		bdx = pb[0] - pd[0];
		bdy = pb[1] - pd[1];
		cdx = pc[0] - pd[0];
		cdy = pc[1] - pd[1];
	
		abdet = adx * bdy - bdx * ady;
		bcdet = bdx * cdy - cdx * bdy;
		cadet = cdx * ady - adx * cdy;
		alift = adx * adx + ady * ady;
		blift = bdx * bdx + bdy * bdy;
		clift = cdx * cdx + cdy * cdy;
	
		return alift * bcdet + blift * cadet + clift * abdet;
	}
	
	private static double incircleadapt(double [] pa, double [] pb, double [] pc, double [] pd, double permanent)
	{
		double adx, bdx, cdx, ady, bdy, cdy;
		double det, errbound;
	
		double [] bdxcdy, cdxbdy, cdxady, adxcdy, adxbdy, bdxady;
		double [] bc = new double[4];
		double [] ca = new double[4];
		double [] ab = new double[4];
		double [] axbc = new double[8];
		double [] axxbc = new double[16];
		double [] aybc = new double[8];
		double [] ayybc = new double[16];
		double [] adet = new double[32];
		int axbclen, axxbclen, aybclen, ayybclen, alen;
		double [] bxca = new double[8];
		double [] bxxca = new double[16];
		double [] byca = new double[8];
		double [] byyca = new double[16];
		double [] bdet = new double[32];
		int bxcalen, bxxcalen, bycalen, byycalen, blen;
		double [] cxab = new double[8];
		double [] cxxab = new double[16];
		double [] cyab = new double[8];
		double [] cyyab = new double[16];
		double [] cdet = new double[32];
		int cxablen, cxxablen, cyablen, cyyablen, clen;
		double [] abdet = new double[64];
		int ablen;
		double [] fin1 = new double[1152];
		double [] fin2 = new double[1152];
		double [] finnow, finother, finswap;
		int finlength;
	
		double adxtail, bdxtail, cdxtail, adytail, bdytail, cdytail;
		double [] adxadx, adyady, bdxbdx, bdybdy, cdxcdx, cdycdy;
		double [] aa = new double[4];
		double [] bb = new double[4];
		double [] cc = new double[4];
		double [] ti, tj;
		double [] u = new double[4];
		double [] v = new double[4];
		double [] temp8 = new double[8];
		double [] temp16a = new double[16];
		double [] temp16b = new double[16];
		double [] temp16c = new double[16];
		double [] temp32a = new double[32];
		double [] temp32b = new double[32];
		double [] temp48 = new double[48];
		double [] temp64 = new double[64];
		int temp8len, temp16alen, temp16blen, temp16clen;
		int temp32alen, temp32blen, temp48len, temp64len;
		double [] axtbb = new double[8];
		double [] axtcc = new double[8];
		double [] aytbb = new double[8];
		double [] aytcc = new double[8];
		int axtbblen, axtcclen, aytbblen, aytcclen;
		double [] bxtaa = new double[8];
		double [] bxtcc = new double[8];
		double [] bytaa = new double[8];
		double [] bytcc = new double[8];
		int bxtaalen, bxtcclen, bytaalen, bytcclen;
		double [] cxtaa = new double[8];
		double [] cxtbb = new double[8];
		double [] cytaa = new double[8];
		double [] cytbb = new double[8];
		int cxtaalen, cxtbblen, cytaalen, cytbblen;
		double [] axtbc = new double[8];
		double [] aytbc = new double[8];
		double [] bxtca = new double[8];
		double [] bytca = new double[8];
		double [] cxtab = new double[8];
		double [] cytab = new double[8];
		int axtbclen = 0, aytbclen = 0, bxtcalen = 0, bytcalen = 0, cxtablen = 0, cytablen = 0;
		double [] axtbct = new double[16];
		double [] aytbct = new double[16];
		double [] bxtcat = new double[16];
		double [] bytcat = new double[16];
		double [] cxtabt = new double[16];
		double [] cytabt = new double[16];
		int axtbctlen, aytbctlen, bxtcatlen, bytcatlen, cxtabtlen, cytabtlen;
		double [] axtbctt = new double[8];
		double [] aytbctt = new double[8];
		double [] bxtcatt = new double[8];
		double [] bytcatt = new double[8];
		double [] cxtabtt = new double[8];
		double [] cytabtt = new double[8];
		int axtbcttlen, aytbcttlen, bxtcattlen, bytcattlen, cxtabttlen, cytabttlen;
		double [] abt = new double[8];
		double [] bct = new double[8];
		double [] cat = new double[8];
		int abtlen, bctlen, catlen;
		double [] abtt = new double[4];
		double [] bctt = new double[4];
		double [] catt = new double[4];
		int abttlen, bcttlen, cattlen;
		double negate;
	
		adx = (double) (pa[0] - pd[0]);
		bdx = (double) (pb[0] - pd[0]);
		cdx = (double) (pc[0] - pd[0]);
		ady = (double) (pa[1] - pd[1]);
		bdy = (double) (pb[1] - pd[1]);
		cdy = (double) (pc[1] - pd[1]);
	
		bdxcdy = Two_Product(bdx, cdy);
		cdxbdy = Two_Product(cdx, bdy);
		Two_Two_Diff(bdxcdy[0], bdxcdy[1], cdxbdy[0], cdxbdy[1], bc);
		axbclen = scale_expansion_zeroelim(4, bc, adx, axbc);
		axxbclen = scale_expansion_zeroelim(axbclen, axbc, adx, axxbc);
		aybclen = scale_expansion_zeroelim(4, bc, ady, aybc);
		ayybclen = scale_expansion_zeroelim(aybclen, aybc, ady, ayybc);
		alen = fast_expansion_sum_zeroelim(axxbclen, axxbc, ayybclen, ayybc, adet);
	
		cdxady = Two_Product(cdx, ady);
		adxcdy = Two_Product(adx, cdy);
		Two_Two_Diff(cdxady[0], cdxady[1], adxcdy[0], adxcdy[1], ca);
		bxcalen = scale_expansion_zeroelim(4, ca, bdx, bxca);
		bxxcalen = scale_expansion_zeroelim(bxcalen, bxca, bdx, bxxca);
		bycalen = scale_expansion_zeroelim(4, ca, bdy, byca);
		byycalen = scale_expansion_zeroelim(bycalen, byca, bdy, byyca);
		blen = fast_expansion_sum_zeroelim(bxxcalen, bxxca, byycalen, byyca, bdet);
	
		adxbdy = Two_Product(adx, bdy);
		bdxady = Two_Product(bdx, ady);
		Two_Two_Diff(adxbdy[0], adxbdy[1], bdxady[0], bdxady[1], ab);
		cxablen = scale_expansion_zeroelim(4, ab, cdx, cxab);
		cxxablen = scale_expansion_zeroelim(cxablen, cxab, cdx, cxxab);
		cyablen = scale_expansion_zeroelim(4, ab, cdy, cyab);
		cyyablen = scale_expansion_zeroelim(cyablen, cyab, cdy, cyyab);
		clen = fast_expansion_sum_zeroelim(cxxablen, cxxab, cyyablen, cyyab, cdet);
	
		ablen = fast_expansion_sum_zeroelim(alen, adet, blen, bdet, abdet);
		finlength = fast_expansion_sum_zeroelim(ablen, abdet, clen, cdet, fin1);
	
		det = estimate(finlength, fin1);
		errbound = iccerrboundB * permanent;
		if ((det >= errbound) || (-det >= errbound)) {
			return det;
		}
	
		adxtail = Two_Diff_Tail(pa[0], pd[0], adx);
		adytail = Two_Diff_Tail(pa[1], pd[1], ady);
		bdxtail = Two_Diff_Tail(pb[0], pd[0], bdx);
		bdytail = Two_Diff_Tail(pb[1], pd[1], bdy);
		cdxtail = Two_Diff_Tail(pc[0], pd[0], cdx);
		cdytail = Two_Diff_Tail(pc[1], pd[1], cdy);
		if ((adxtail == 0.0) && (bdxtail == 0.0) && (cdxtail == 0.0)
		 && (adytail == 0.0) && (bdytail == 0.0) && (cdytail == 0.0)) {
			return det;
		}
	
		errbound = iccerrboundC * permanent + resulterrbound * Math.abs(det);
		det += ((adx * adx + ady * ady) * ((bdx * cdytail + cdy * bdxtail)
					- (bdy * cdxtail + cdx * bdytail))
			+ 2.0 * (adx * adxtail + ady * adytail) * (bdx * cdy - bdy * cdx))
		     + ((bdx * bdx + bdy * bdy) * ((cdx * adytail + ady * cdxtail)
					- (cdy * adxtail + adx * cdytail))
			+ 2.0 * (bdx * bdxtail + bdy * bdytail) * (cdx * ady - cdy * adx))
		     + ((cdx * cdx + cdy * cdy) * ((adx * bdytail + bdy * adxtail)
					- (ady * bdxtail + bdx * adytail))
			+ 2.0 * (cdx * cdxtail + cdy * cdytail) * (adx * bdy - ady * bdx));
		if ((det >= errbound) || (-det >= errbound)) {
			return det;
		}
	
		finnow = fin1;
		finother = fin2;
	
		if ((bdxtail != 0.0) || (bdytail != 0.0)
		 || (cdxtail != 0.0) || (cdytail != 0.0)) {
			adxadx = Square(adx);
			adyady = Square(ady);
			Two_Two_Sum(adxadx[0], adxadx[1], adyady[0], adyady[1], aa);
		}
		if ((cdxtail != 0.0) || (cdytail != 0.0)
		 || (adxtail != 0.0) || (adytail != 0.0)) {
			bdxbdx = Square(bdx);
			bdybdy = Square(bdy);
			Two_Two_Sum(bdxbdx[0], bdxbdx[1], bdybdy[0], bdybdy[1], bb);
		}
		if ((adxtail != 0.0) || (adytail != 0.0)
		 || (bdxtail != 0.0) || (bdytail != 0.0)) {
			cdxcdx = Square(cdx);
			cdycdy = Square(cdy);
			Two_Two_Sum(cdxcdx[0], cdxcdx[1], cdycdy[0], cdycdy[1], cc);
		}
	
		if (adxtail != 0.0) {
			axtbclen = scale_expansion_zeroelim(4, bc, adxtail, axtbc);
			temp16alen = scale_expansion_zeroelim(axtbclen, axtbc, 2.0 * adx, temp16a);
	
			axtcclen = scale_expansion_zeroelim(4, cc, adxtail, axtcc);
			temp16blen = scale_expansion_zeroelim(axtcclen, axtcc, bdy, temp16b);
	
			axtbblen = scale_expansion_zeroelim(4, bb, adxtail, axtbb);
			temp16clen = scale_expansion_zeroelim(axtbblen, axtbb, -cdy, temp16c);
	
			temp32alen = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp16blen, temp16b, temp32a);
			temp48len = fast_expansion_sum_zeroelim(temp16clen, temp16c, temp32alen, temp32a, temp48);
			finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len, temp48, finother);
			finswap = finnow; finnow = finother; finother = finswap;
		}
		if (adytail != 0.0) {
			aytbclen = scale_expansion_zeroelim(4, bc, adytail, aytbc);
			temp16alen = scale_expansion_zeroelim(aytbclen, aytbc, 2.0 * ady, temp16a);
	
			aytbblen = scale_expansion_zeroelim(4, bb, adytail, aytbb);
			temp16blen = scale_expansion_zeroelim(aytbblen, aytbb, cdx, temp16b);
	
			aytcclen = scale_expansion_zeroelim(4, cc, adytail, aytcc);
			temp16clen = scale_expansion_zeroelim(aytcclen, aytcc, -bdx, temp16c);
	
			temp32alen = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp16blen, temp16b, temp32a);
			temp48len = fast_expansion_sum_zeroelim(temp16clen, temp16c, temp32alen, temp32a, temp48);
			finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len, temp48, finother);
			finswap = finnow; finnow = finother; finother = finswap;
		}
		if (bdxtail != 0.0) {
			bxtcalen = scale_expansion_zeroelim(4, ca, bdxtail, bxtca);
			temp16alen = scale_expansion_zeroelim(bxtcalen, bxtca, 2.0 * bdx, temp16a);
	
			bxtaalen = scale_expansion_zeroelim(4, aa, bdxtail, bxtaa);
			temp16blen = scale_expansion_zeroelim(bxtaalen, bxtaa, cdy, temp16b);
	
			bxtcclen = scale_expansion_zeroelim(4, cc, bdxtail, bxtcc);
			temp16clen = scale_expansion_zeroelim(bxtcclen, bxtcc, -ady, temp16c);
	
			temp32alen = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp16blen, temp16b, temp32a);
			temp48len = fast_expansion_sum_zeroelim(temp16clen, temp16c, temp32alen, temp32a, temp48);
			finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len, temp48, finother);
			finswap = finnow; finnow = finother; finother = finswap;
		}
		if (bdytail != 0.0) {
			bytcalen = scale_expansion_zeroelim(4, ca, bdytail, bytca);
			temp16alen = scale_expansion_zeroelim(bytcalen, bytca, 2.0 * bdy, temp16a);
	
			bytcclen = scale_expansion_zeroelim(4, cc, bdytail, bytcc);
			temp16blen = scale_expansion_zeroelim(bytcclen, bytcc, adx, temp16b);
	
			bytaalen = scale_expansion_zeroelim(4, aa, bdytail, bytaa);
			temp16clen = scale_expansion_zeroelim(bytaalen, bytaa, -cdx, temp16c);
	
			temp32alen = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp16blen, temp16b, temp32a);
			temp48len = fast_expansion_sum_zeroelim(temp16clen, temp16c, temp32alen, temp32a, temp48);
			finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len, temp48, finother);
			finswap = finnow; finnow = finother; finother = finswap;
		}
		if (cdxtail != 0.0) {
			cxtablen = scale_expansion_zeroelim(4, ab, cdxtail, cxtab);
			temp16alen = scale_expansion_zeroelim(cxtablen, cxtab, 2.0 * cdx, temp16a);
	
			cxtbblen = scale_expansion_zeroelim(4, bb, cdxtail, cxtbb);
			temp16blen = scale_expansion_zeroelim(cxtbblen, cxtbb, ady, temp16b);
	
			cxtaalen = scale_expansion_zeroelim(4, aa, cdxtail, cxtaa);
			temp16clen = scale_expansion_zeroelim(cxtaalen, cxtaa, -bdy, temp16c);
	
			temp32alen = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp16blen, temp16b, temp32a);
			temp48len = fast_expansion_sum_zeroelim(temp16clen, temp16c, temp32alen, temp32a, temp48);
			finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len, temp48, finother);
			finswap = finnow; finnow = finother; finother = finswap;
		}
		if (cdytail != 0.0) {
			cytablen = scale_expansion_zeroelim(4, ab, cdytail, cytab);
			temp16alen = scale_expansion_zeroelim(cytablen, cytab, 2.0 * cdy, temp16a);
	
			cytaalen = scale_expansion_zeroelim(4, aa, cdytail, cytaa);
			temp16blen = scale_expansion_zeroelim(cytaalen, cytaa, bdx, temp16b);
	
			cytbblen = scale_expansion_zeroelim(4, bb, cdytail, cytbb);
			temp16clen = scale_expansion_zeroelim(cytbblen, cytbb, -adx, temp16c);
	
			temp32alen = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp16blen, temp16b, temp32a);
			temp48len = fast_expansion_sum_zeroelim(temp16clen, temp16c, temp32alen, temp32a, temp48);
			finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len, temp48, finother);
			finswap = finnow; finnow = finother; finother = finswap;
		}
	
		if ((adxtail != 0.0) || (adytail != 0.0)) {
			if ((bdxtail != 0.0) || (bdytail != 0.0)
			 || (cdxtail != 0.0) || (cdytail != 0.0)) {
				ti = Two_Product(bdxtail, cdy);
				tj = Two_Product(bdx, cdytail);
				Two_Two_Sum(ti[0], ti[1], tj[0], tj[1], u);
				negate = -bdy;
				ti = Two_Product(cdxtail, negate);
				negate = -bdytail;
				tj = Two_Product(cdx, negate);
				Two_Two_Sum(ti[0], ti[1], tj[0], tj[1], v);
				bctlen = fast_expansion_sum_zeroelim(4, u, 4, v, bct);
	
				ti = Two_Product(bdxtail, cdytail);
				tj = Two_Product(cdxtail, bdytail);
				Two_Two_Diff(ti[0], ti[1], tj[0], tj[1], bctt);
				bcttlen = 4;
			} else {
				bct[0] = 0.0;
				bctlen = 1;
				bctt[0] = 0.0;
				bcttlen = 1;
			}
	
			if (adxtail != 0.0) {
				temp16alen = scale_expansion_zeroelim(axtbclen, axtbc, adxtail, temp16a);
				axtbctlen = scale_expansion_zeroelim(bctlen, bct, adxtail, axtbct);
				temp32alen = scale_expansion_zeroelim(axtbctlen, axtbct, 2.0 * adx, temp32a);
				temp48len = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp32alen, temp32a, temp48);
				finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len, temp48, finother);
				finswap = finnow; finnow = finother; finother = finswap;
				if (bdytail != 0.0) {
					temp8len = scale_expansion_zeroelim(4, cc, adxtail, temp8);
					temp16alen = scale_expansion_zeroelim(temp8len, temp8, bdytail, temp16a);
					finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp16alen, temp16a, finother);
					finswap = finnow; finnow = finother; finother = finswap;
				}
				if (cdytail != 0.0) {
					temp8len = scale_expansion_zeroelim(4, bb, -adxtail, temp8);
					temp16alen = scale_expansion_zeroelim(temp8len, temp8, cdytail, temp16a);
					finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp16alen, temp16a, finother);
					finswap = finnow; finnow = finother; finother = finswap;
				}
	
				temp32alen = scale_expansion_zeroelim(axtbctlen, axtbct, adxtail, temp32a);
				axtbcttlen = scale_expansion_zeroelim(bcttlen, bctt, adxtail, axtbctt);
				temp16alen = scale_expansion_zeroelim(axtbcttlen, axtbctt, 2.0 * adx, temp16a);
				temp16blen = scale_expansion_zeroelim(axtbcttlen, axtbctt, adxtail, temp16b);
				temp32blen = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp16blen, temp16b, temp32b);
				temp64len = fast_expansion_sum_zeroelim(temp32alen, temp32a, temp32blen, temp32b, temp64);
				finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp64len, temp64, finother);
				finswap = finnow; finnow = finother; finother = finswap;
			}
			if (adytail != 0.0) {
				temp16alen = scale_expansion_zeroelim(aytbclen, aytbc, adytail, temp16a);
				aytbctlen = scale_expansion_zeroelim(bctlen, bct, adytail, aytbct);
				temp32alen = scale_expansion_zeroelim(aytbctlen, aytbct, 2.0 * ady, temp32a);
				temp48len = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp32alen, temp32a, temp48);
				finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len, temp48, finother);
				finswap = finnow; finnow = finother; finother = finswap;
	
	
				temp32alen = scale_expansion_zeroelim(aytbctlen, aytbct, adytail, temp32a);
				aytbcttlen = scale_expansion_zeroelim(bcttlen, bctt, adytail, aytbctt);
				temp16alen = scale_expansion_zeroelim(aytbcttlen, aytbctt, 2.0 * ady, temp16a);
				temp16blen = scale_expansion_zeroelim(aytbcttlen, aytbctt, adytail, temp16b);
				temp32blen = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp16blen, temp16b, temp32b);
				temp64len = fast_expansion_sum_zeroelim(temp32alen, temp32a, temp32blen, temp32b, temp64);
				finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp64len, temp64, finother);
				finswap = finnow; finnow = finother; finother = finswap;
			}
		}
		if ((bdxtail != 0.0) || (bdytail != 0.0)) {
			if ((cdxtail != 0.0) || (cdytail != 0.0)
			 || (adxtail != 0.0) || (adytail != 0.0)) {
				ti = Two_Product(cdxtail, ady);
				tj = Two_Product(cdx, adytail);
				Two_Two_Sum(ti[0], ti[1], tj[0], tj[1], u);
				negate = -cdy;
				ti = Two_Product(adxtail, negate);
				negate = -cdytail;
				tj = Two_Product(adx, negate);
				Two_Two_Sum(ti[0], ti[1], tj[0], tj[1], v);
				catlen = fast_expansion_sum_zeroelim(4, u, 4, v, cat);
	
				ti = Two_Product(cdxtail, adytail);
				tj = Two_Product(adxtail, cdytail);
				Two_Two_Diff(ti[0], ti[1], tj[0], tj[1], catt);
				cattlen = 4;
			} else {
				cat[0] = 0.0;
				catlen = 1;
				catt[0] = 0.0;
				cattlen = 1;
			}
	
			if (bdxtail != 0.0) {
				temp16alen = scale_expansion_zeroelim(bxtcalen, bxtca, bdxtail, temp16a);
				bxtcatlen = scale_expansion_zeroelim(catlen, cat, bdxtail, bxtcat);
				temp32alen = scale_expansion_zeroelim(bxtcatlen, bxtcat, 2.0 * bdx, temp32a);
				temp48len = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp32alen, temp32a, temp48);
				finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len, temp48, finother);
				finswap = finnow; finnow = finother; finother = finswap;
				if (cdytail != 0.0) {
					temp8len = scale_expansion_zeroelim(4, aa, bdxtail, temp8);
					temp16alen = scale_expansion_zeroelim(temp8len, temp8, cdytail, temp16a);
					finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp16alen, temp16a, finother);
					finswap = finnow; finnow = finother; finother = finswap;
				}
				if (adytail != 0.0) {
					temp8len = scale_expansion_zeroelim(4, cc, -bdxtail, temp8);
					temp16alen = scale_expansion_zeroelim(temp8len, temp8, adytail, temp16a);
					finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp16alen, temp16a, finother);
					finswap = finnow; finnow = finother; finother = finswap;
				}
	
				temp32alen = scale_expansion_zeroelim(bxtcatlen, bxtcat, bdxtail, temp32a);
				bxtcattlen = scale_expansion_zeroelim(cattlen, catt, bdxtail, bxtcatt);
				temp16alen = scale_expansion_zeroelim(bxtcattlen, bxtcatt, 2.0 * bdx, temp16a);
				temp16blen = scale_expansion_zeroelim(bxtcattlen, bxtcatt, bdxtail, temp16b);
				temp32blen = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp16blen, temp16b, temp32b);
				temp64len = fast_expansion_sum_zeroelim(temp32alen, temp32a, temp32blen, temp32b, temp64);
				finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp64len, temp64, finother);
				finswap = finnow; finnow = finother; finother = finswap;
			}
			if (bdytail != 0.0) {
				temp16alen = scale_expansion_zeroelim(bytcalen, bytca, bdytail, temp16a);
				bytcatlen = scale_expansion_zeroelim(catlen, cat, bdytail, bytcat);
				temp32alen = scale_expansion_zeroelim(bytcatlen, bytcat, 2.0 * bdy, temp32a);
				temp48len = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp32alen, temp32a, temp48);
				finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len, temp48, finother);
				finswap = finnow; finnow = finother; finother = finswap;
	
	
				temp32alen = scale_expansion_zeroelim(bytcatlen, bytcat, bdytail, temp32a);
				bytcattlen = scale_expansion_zeroelim(cattlen, catt, bdytail, bytcatt);
				temp16alen = scale_expansion_zeroelim(bytcattlen, bytcatt, 2.0 * bdy, temp16a);
				temp16blen = scale_expansion_zeroelim(bytcattlen, bytcatt, bdytail, temp16b);
				temp32blen = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp16blen, temp16b, temp32b);
				temp64len = fast_expansion_sum_zeroelim(temp32alen, temp32a, temp32blen, temp32b, temp64);
				finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp64len, temp64, finother);
				finswap = finnow; finnow = finother; finother = finswap;
			}
		}
		if ((cdxtail != 0.0) || (cdytail != 0.0)) {
			if ((adxtail != 0.0) || (adytail != 0.0)
			 || (bdxtail != 0.0) || (bdytail != 0.0)) {
				ti = Two_Product(adxtail, bdy);
				tj = Two_Product(adx, bdytail);
				Two_Two_Sum(ti[0], ti[1], tj[0], tj[1], u);
				negate = -ady;
				ti = Two_Product(bdxtail, negate);
				negate = -adytail;
				tj = Two_Product(bdx, negate);
				Two_Two_Sum(ti[0], ti[1], tj[0], tj[1], v);
				abtlen = fast_expansion_sum_zeroelim(4, u, 4, v, abt);
	
				ti = Two_Product(adxtail, bdytail);
				tj = Two_Product(bdxtail, adytail);
				Two_Two_Diff(ti[0], ti[1], tj[0], tj[1], abtt);
				abttlen = 4;
			} else {
				abt[0] = 0.0;
				abtlen = 1;
				abtt[0] = 0.0;
				abttlen = 1;
			}
	
			if (cdxtail != 0.0) {
				temp16alen = scale_expansion_zeroelim(cxtablen, cxtab, cdxtail, temp16a);
				cxtabtlen = scale_expansion_zeroelim(abtlen, abt, cdxtail, cxtabt);
				temp32alen = scale_expansion_zeroelim(cxtabtlen, cxtabt, 2.0 * cdx, temp32a);
				temp48len = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp32alen, temp32a, temp48);
				finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len, temp48, finother);
				finswap = finnow; finnow = finother; finother = finswap;
				if (adytail != 0.0) {
					temp8len = scale_expansion_zeroelim(4, bb, cdxtail, temp8);
					temp16alen = scale_expansion_zeroelim(temp8len, temp8, adytail, temp16a);
					finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp16alen, temp16a, finother);
					finswap = finnow; finnow = finother; finother = finswap;
				}
				if (bdytail != 0.0) {
					temp8len = scale_expansion_zeroelim(4, aa, -cdxtail, temp8);
					temp16alen = scale_expansion_zeroelim(temp8len, temp8, bdytail, temp16a);
					finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp16alen, temp16a, finother);
					finswap = finnow; finnow = finother; finother = finswap;
				}
	
				temp32alen = scale_expansion_zeroelim(cxtabtlen, cxtabt, cdxtail, temp32a);
				cxtabttlen = scale_expansion_zeroelim(abttlen, abtt, cdxtail, cxtabtt);
				temp16alen = scale_expansion_zeroelim(cxtabttlen, cxtabtt, 2.0 * cdx, temp16a);
				temp16blen = scale_expansion_zeroelim(cxtabttlen, cxtabtt, cdxtail, temp16b);
				temp32blen = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp16blen, temp16b, temp32b);
				temp64len = fast_expansion_sum_zeroelim(temp32alen, temp32a, temp32blen, temp32b, temp64);
				finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp64len, temp64, finother);
				finswap = finnow; finnow = finother; finother = finswap;
			}
			if (cdytail != 0.0) {
				temp16alen = scale_expansion_zeroelim(cytablen, cytab, cdytail, temp16a);
				cytabtlen = scale_expansion_zeroelim(abtlen, abt, cdytail, cytabt);
				temp32alen = scale_expansion_zeroelim(cytabtlen, cytabt, 2.0 * cdy, temp32a);
				temp48len = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp32alen, temp32a, temp48);
				finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp48len, temp48, finother);
				finswap = finnow; finnow = finother; finother = finswap;
	
	
				temp32alen = scale_expansion_zeroelim(cytabtlen, cytabt, cdytail, temp32a);
				cytabttlen = scale_expansion_zeroelim(abttlen, abtt, cdytail, cytabtt);
				temp16alen = scale_expansion_zeroelim(cytabttlen, cytabtt, 2.0 * cdy, temp16a);
				temp16blen = scale_expansion_zeroelim(cytabttlen, cytabtt, cdytail, temp16b);
				temp32blen = fast_expansion_sum_zeroelim(temp16alen, temp16a, temp16blen, temp16b, temp32b);
				temp64len = fast_expansion_sum_zeroelim(temp32alen, temp32a, temp32blen, temp32b, temp64);
				finlength = fast_expansion_sum_zeroelim(finlength, finnow, temp64len, temp64, finother);
				finswap = finnow; finnow = finother; finother = finswap;
			}
		}
	
		return finnow[finlength - 1];
	}
	
	public static double incircle(double [] pa, double [] pb, double [] pc, double [] pd)
	{
		double adx, bdx, cdx, ady, bdy, cdy;
		double bdxcdy, cdxbdy, cdxady, adxcdy, adxbdy, bdxady;
		double alift, blift, clift;
		double det;
		double permanent, errbound;
	
		adx = pa[0] - pd[0];
		bdx = pb[0] - pd[0];
		cdx = pc[0] - pd[0];
		ady = pa[1] - pd[1];
		bdy = pb[1] - pd[1];
		cdy = pc[1] - pd[1];
	
		bdxcdy = bdx * cdy;
		cdxbdy = cdx * bdy;
		alift = adx * adx + ady * ady;
	
		cdxady = cdx * ady;
		adxcdy = adx * cdy;
		blift = bdx * bdx + bdy * bdy;
	
		adxbdy = adx * bdy;
		bdxady = bdx * ady;
		clift = cdx * cdx + cdy * cdy;
	
		det = alift * (bdxcdy - cdxbdy)
		    + blift * (cdxady - adxcdy)
		    + clift * (adxbdy - bdxady);
	
		permanent = (Math.abs(bdxcdy) + Math.abs(cdxbdy)) * alift
		          + (Math.abs(cdxady) + Math.abs(adxcdy)) * blift
		          + (Math.abs(adxbdy) + Math.abs(bdxady)) * clift;
		errbound = iccerrboundA * permanent;
		if ((det > errbound) || (-det > errbound)) {
			return det;
		}
	
		return incircleadapt(pa, pb, pc, pd, permanent);
	}

	public static void main(String args[])
	{
		Predicates p = new Predicates();
		printEpsilon();
	}
}
