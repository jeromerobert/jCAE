package org.jcae.opencascade.jni;

/**
 * This class seems be deprecated in Opencascade 6.2
 * @deprected use BRepBuilderAPI_Sewing
 */
public class BRepOffsetAPI_Sewing extends BRepBuilderAPI_Sewing
{
	/**
	 * @deprected use BRepBuilderAPI_Sewing
	 */
	public BRepOffsetAPI_Sewing(double tolerance, boolean option, boolean cutting, boolean nonmanifold) {
		super(tolerance, option, cutting, nonmanifold);
	}

	/**
	 * @deprected use BRepBuilderAPI_Sewing
	 */
	public BRepOffsetAPI_Sewing(double tolerance, boolean option, boolean cutting) {
		super(tolerance, option, cutting);
	}

	/**
	 * @deprected use BRepBuilderAPI_Sewing
	 */
	public BRepOffsetAPI_Sewing(double tolerance, boolean option) {
		super(tolerance, option);
	}

	/**
	 * @deprected use BRepBuilderAPI_Sewing
	 */
	public BRepOffsetAPI_Sewing(double tolerance) {
		super(tolerance);
	}

	/**
	 * @deprected use BRepBuilderAPI_Sewing
	 */
	public BRepOffsetAPI_Sewing() {
	}
}
