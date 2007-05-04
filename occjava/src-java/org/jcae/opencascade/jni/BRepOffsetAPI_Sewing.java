package org.jcae.opencascade.jni;

/**
 * This class seems be deprecated in Opencascade 6.2
 * @deprecated use BRepBuilderAPI_Sewing
 */
public class BRepOffsetAPI_Sewing extends BRepBuilderAPI_Sewing
{
	/**
	 * @deprecated use BRepBuilderAPI_Sewing
	 */
	public BRepOffsetAPI_Sewing(double tolerance, boolean option, boolean cutting, boolean nonmanifold) {
		super(tolerance, option, cutting, nonmanifold);
	}

	/**
	 * @deprecated use BRepBuilderAPI_Sewing
	 */
	public BRepOffsetAPI_Sewing(double tolerance, boolean option, boolean cutting) {
		super(tolerance, option, cutting);
	}

	/**
	 * @deprecated use BRepBuilderAPI_Sewing
	 */
	public BRepOffsetAPI_Sewing(double tolerance, boolean option) {
		super(tolerance, option);
	}

	/**
	 * @deprecated use BRepBuilderAPI_Sewing
	 */
	public BRepOffsetAPI_Sewing(double tolerance) {
		super(tolerance);
	}

	/**
	 * @deprecated use BRepBuilderAPI_Sewing
	 */
	public BRepOffsetAPI_Sewing() {
	}
}
