# jCAE
from org.jcae.mesh.amibe.ds import Mesh, Vertex, AbstractHalfEdge
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.metrics import (
    EuclidianMetric3D,
    DistanceMetric,
    SingularMetric,
)
from org.jcae.mesh.amibe.algos3d import PolylineFactory, RemeshPolyline
from org.jcae.mesh.xmldata import MeshReader, MeshWriter
from org.jcae.mesh.amibe.projection import MeshLiaison

# Java
from java.util import ArrayList
from java.util import LinkedHashMap
from java.lang import String, Math
from java.util import HashMap

# Python
import sys
from optparse import OptionParser
from remesh import check_metric_type


def remesh_beams(liaison, size, rho, immutable_groups, point_metric_file=None):
    # immutable groups
    # wire metric
    metric = None
    if point_metric_file is not None:
        metric_type = check_metric_type(point_metric_file)
        if metric_type == "singular":
            if rho > 1.0:
                # mixed metric
                metric = SingularMetric(size, point_metric_file, rho, True)
            else:
                # analytic metric
                metric = SingularMetric(size, point_metric_file)
        else:
            metric = DistanceMetric(size, point_metric_file)
    polylines = PolylineFactory(liaison.mesh, 135.0, size * 0.2)
    liaison.mesh.resetBeams()
    for entry in polylines.entrySet():
        groupId = entry.key
        for polyline in entry.value:
            if point_metric_file is None:
                metric = ArrayList()
                for v in polyline:
                    metric.add(EuclidianMetric3D(size))
            if liaison.mesh.getGroupName(groupId) in immutable_groups:
                result = polyline
            else:
                result = RemeshPolyline(liaison.mesh, polyline, metric).compute()
            for i in xrange(result.size() - 1):
                liaison.mesh.addBeam(result.get(i), result.get(i + 1), groupId)
    # redefine liaison to remove orphan nodes
    mesh = liaison.getMesh()
    mtb = MeshTraitsBuilder.getDefault3D()
    mtb.addNodeSet()
    liaison = MeshLiaison.create(mesh, mtb)
    return liaison


def remesh(**kwargs):
    """Remesh beams of an existing mesh with a singular analytical metric

    It is necessary to remove J_ and G_ groups for wires.
    """
    # Build background mesh
    try:
        liaison = kwargs["liaison"]
    except KeyError:
        mtb = MeshTraitsBuilder.getDefault3D()
        mtb.addNodeSet()
        mesh = Mesh(mtb)
        MeshReader.readObject3D(mesh, kwargs["in_dir"])
        liaison = MeshLiaison.create(mesh, mtb)
    immutable_groups = list()
    if kwargs["immutable_groups_file"]:
        f = open(kwargs["immutable_groups_file"])
        immutable_groups = f.read().split()
        f.close()
        liaison.mesh.tagGroups(immutable_groups, AbstractHalfEdge.IMMUTABLE)
    liaison = remesh_beams(liaison, kwargs["size"], kwargs["rho"], immutable_groups, kwargs["point_metric_file"])
    # Output
    MeshWriter.writeObject3D(liaison.getMesh(), kwargs["out_dir"], "")


if __name__ == "__main__":
    """
	Recreate an existing wire mesh, remesh around singularities
	"""

    cmd = (
        "remeshBeams",
        "<inputDir> <outputDir> <size>",
        """Remesh an existing mesh around singularities:
		Refine according to pointMetric and size

	   inputDir
			input amibe mesh

	   outputDir
			output amibe mesh

		size
			Target size""",
    )
    parser = OptionParser(
        usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd, prog="remeshBeams"
    )
    parser.add_option(
        "-P",
        "--point-metric",
        metavar="STRING",
        action="store",
        type="string",
        dest="point_metric_file",
        help="A text file containing points which to refine around.",
    )
    parser.add_option(
        "-r",
        "--rho",
        metavar="FLOAT",
        default=2.0,
        action="store",
        type="float",
        dest="rho",
        help="numerical metric ratio (required: rho > 1, default: 2)",
    )
    parser.add_option(
        "-M",
        "--immutable-groups",
        metavar="STRING",
        action="store",
        type="string",
        dest="immutable_groups_file",
        help="A text file containing the list of groups whose "
        "beam elements and nodes must not be modified by this algorithm.",
    )

    (options, args) = parser.parse_args(args=sys.argv[1:])

    if len(args) != 3:
        parser.print_usage()
        sys.exit(1)
    options.in_dir = args[0]
    options.out_dir = args[1]
    options.size = float(args[3])
    remesh(**options.__dict__)
