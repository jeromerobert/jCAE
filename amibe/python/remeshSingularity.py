# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.algos3d import RemeshSkeleton, Remesh, QEMDecimateHalfEdge
from org.jcae.mesh.amibe.algos3d import SwapEdge, ImproveVertexValence, SmoothNodes3DBg
from org.jcae.mesh.amibe.algos3d import RemoveDegeneratedTriangles
from org.jcae.mesh.amibe.algos3d import PolylineFactory, RemeshPolyline
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.amibe.metrics import EuclidianMetric3D, SingularMetric
from org.jcae.mesh.xmldata import MeshReader, MeshWriter

# Java
from java.util import HashMap
from java.util import ArrayList
from java.lang import String, Math

# Python
import sys
from math import cos, pi
from optparse import OptionParser
from remeshBeams import remesh_beams


def remesh(**kwargs):
    """
    Remesh an existing mesh with a singular analytical metric
    """
    # Process coplanarity options
    coplanarity = cos(kwargs["coplanarityAngle"] * pi / 180.0)
    if kwargs["coplanarity"]:
        coplanarity = kwargs["coplanarity"]

    safe_coplanarity = kwargs["safe_coplanarity"]
    if safe_coplanarity is None:
        safe_coplanarity = 0.8
    safe_coplanarity = max(coplanarity, safe_coplanarity)

    # Build background mesh
    try:
        liaison = kwargs["liaison"]
    except KeyError:
        mtb = MeshTraitsBuilder.getDefault3D()
        if kwargs["recordFile"]:
            mtb.addTraceRecord()
        mtb.addNodeSet()
        mesh = Mesh(mtb)
        if kwargs["recordFile"]:
            mesh.getTrace().setDisabled(True)

        MeshReader.readObject3D(mesh, kwargs["in_dir"])
        liaison = MeshLiaison.create(mesh, mtb)

    if kwargs["recordFile"]:
        liaison.getMesh().getTrace().setDisabled(False)
        liaison.getMesh().getTrace().setLogFile(kwargs["recordFile"])
        liaison.getMesh().getTrace().createMesh("mesh", liaison.getMesh())
    if kwargs["immutable_border"]:
        liaison.mesh.tagFreeEdges(AbstractHalfEdge.IMMUTABLE)
    liaison.getMesh().buildRidges(coplanarity)
    if kwargs["preserveGroups"]:
        liaison.getMesh().buildGroupBoundaries()

    immutable_groups = []
    if kwargs["immutable_groups_file"]:
        f = open(kwargs["immutable_groups_file"])
        immutable_groups = f.read().split()
        f.close()
        liaison.mesh.tagGroups(immutable_groups, AbstractHalfEdge.IMMUTABLE)

    if kwargs["recordFile"]:
        cmds = [
            String("assert self.m.checkNoDegeneratedTriangles()"),
            String("assert self.m.checkNoInvertedTriangles()"),
            String("assert self.m.checkVertexLinks()"),
            String("assert self.m.isValid()"),
        ]
        liaison.getMesh().getTrace().setHooks(cmds)

    # Decimate
    if kwargs["decimateSize"] or kwargs["decimateTarget"]:
        decimateOptions = HashMap()
        if kwargs["decimateSize"]:
            decimateOptions.put("size", str(kwargs["decimateSize"]))
        elif kwargs["decimateTarget"]:
            decimateOptions.put("maxtriangles", str(kwargs["decimateTarget"]))
        decimateOptions.put("coplanarity", str(safe_coplanarity))
        QEMDecimateHalfEdge(liaison, decimateOptions).compute()
        swapOptions = HashMap()
        swapOptions.put("coplanarity", str(safe_coplanarity))
        SwapEdge(liaison, swapOptions).compute()

    # Metric
    if kwargs["rho"] > 1.0:
        # mixed metric
        metric = SingularMetric(
            kwargs["sizeinf"], kwargs["point_metric_file"], kwargs["rho"], True
        )
    else:
        # analytic metric
        metric = SingularMetric(kwargs["sizeinf"], kwargs["point_metric_file"])

    # Remesh Skeleton
    if kwargs["skeleton"]:
        RemeshSkeleton(liaison, 1.66, metric, 0.01).compute()

    # Remesh
    refineOptions = HashMap()
    refineOptions.put("size", str(kwargs["sizeinf"]))
    refineOptions.put("coplanarity", str(safe_coplanarity))
    refineOptions.put("nearLengthRatio", str(kwargs["nearLengthRatio"]))
    refineOptions.put("project", "false")
    if kwargs["allowNearNodes"]:
        refineOptions.put("allowNearNodes", "true")
    refineAlgo = Remesh(liaison, refineOptions)
    refineAlgo.setAnalyticMetric(metric)
    refineAlgo.compute()

    if not kwargs["noclean"]:
        # Swap
        swapOptions = HashMap()
        swapOptions.put("coplanarity", str(safe_coplanarity))
        swapOptions.put("minCosAfterSwap", "0.3")
        SwapEdge(liaison, swapOptions).compute()

        # Improve valence
        valenceOptions = HashMap()
        valenceOptions.put("coplanarity", str(safe_coplanarity))
        valenceOptions.put("checkNormals", "false")
        ImproveVertexValence(liaison, valenceOptions).compute()

        # Smooth
        smoothOptions = HashMap()
        smoothOptions.put("iterations", str(8))
        smoothOptions.put("check", "true")
        smoothOptions.put("boundaries", "true")
        smoothOptions.put("relaxation", str(0.6))
        if safe_coplanarity >= 0.0:
            smoothOptions.put("coplanarity", str(safe_coplanarity))
        SmoothNodes3DBg(liaison, smoothOptions).compute()

        # Remove Degenerated
        rdOptions = HashMap()
        rdOptions.put("rho", str(kwargs["eratio"]))
        RemoveDegeneratedTriangles(liaison, rdOptions).compute()

    # remesh beams
    if kwargs["wire_size"] > 0.0:
        liaison = remesh_beams(
            liaison=liaison,
            size=kwargs["wire_size"],
            rho=kwargs["rho"],
            immutable_groups=immutable_groups,
            point_metric_file=kwargs["wire_metric_file"],
        )

    # Output
    MeshWriter.writeObject3D(liaison.getMesh(), kwargs["out_dir"], "")
    if kwargs["recordFile"]:
        liaison.getMesh().getTrace().finish()


if __name__ == "__main__":
    """
    Remesh an existing mesh around singularities
    """

    cmd = (
        "remeshSingularity",
        "<inputDir> <outputDir> <pointMetric> <sizeInf>",
        """Remesh an existing mesh around singularities:
        1) Decimate (optional)
        2) Refine according to pointMetric and sizeInf
        3) Swap edges
        4) Improve vertex valence
        5) Smooth mesh (move vertices)

       inputDir
            input amibe mesh

       outputDir
            output amibe mesh

       pointMetric
            A CSV file containing points which to refine around. Each line must
            contains 8 values: 1, x, y, z, s0 (target size at distance d0), d0, d1
            (distance from which point has no influence), alpha (singularity order).
            Target size at distance d of this point is computed like this:
                a) if d <= d0, s0
                b) if d >= d1, global mesh size sInf
                c) if d0 < d < d1, s0 + (sInf - s0) * ((d - d0)/(d1 - d0))^(alpha + 1)

            In addition, the numerical criterion makes sure that the target sizes hi
            and hj of nodes i and j of each edge e_ij are such that hi < rho * hj
            with rho > 1 (default 2.0).

        sizeInf
            Target size sInf""",
    )
    parser = OptionParser(
        usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd, prog="remeshSingularity"
    )
    parser.add_option(
        "-a",
        "--angle",
        metavar="FLOAT",
        default=15.0,
        action="store",
        type="float",
        dest="coplanarityAngle",
        help="angle (in degrees) between face normals to detect "
        "feature edges (default: 15, superseded if -c is defined)",
    )
    parser.add_option(
        "-c",
        "--coplanarity",
        metavar="FLOAT",
        action="store",
        type="float",
        dest="coplanarity",
        help="dot product of face normals to detect feature edges",
    )
    parser.add_option(
        "-s",
        "--safe-coplanarity",
        metavar="FLOAT",
        action="store",
        type="float",
        dest="safe_coplanarity",
        default=0.8,
        help="dot product of face normals tolerance for algorithms",
    )
    parser.add_option(
        "-D",
        "--decimate-target",
        metavar="NUMBER",
        action="store",
        type="int",
        dest="decimateTarget",
        help="decimate mesh before remeshing, keep only NUMBER " "triangles",
    )
    parser.add_option(
        "-d",
        "--decimate",
        metavar="FLOAT",
        action="store",
        type="float",
        dest="decimateSize",
        help="decimate mesh before remeshing, specify tolerance",
    )
    parser.add_option(
        "-g",
        "--preserveGroups",
        action="store_true",
        dest="preserveGroups",
        help="edges adjacent to two different groups are handled like free edges",
    )
    parser.add_option(
        "-k",
        "--skeleton",
        default=False,
        action="store_true",
        dest="skeleton",
        help="remesh skeleton beforehand",
    )
    parser.add_option(
        "-n",
        "--allowNearNodes",
        action="store_true",
        dest="allowNearNodes",
        help="insert vertices even if this creates a small edge",
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
        "-T",
        "--nearLengthRatio",
        metavar="FLOAT",
        default=1.0 / Math.sqrt(2.0),
        action="store",
        type="float",
        dest="nearLengthRatio",
        help="ratio to size target to determine if a vertex is near "
        "an existing point (default: 1/sqrt(2))",
    )
    parser.add_option(
        "-w",
        "--wire",
        metavar="FLOAT",
        default=-1.0,
        action="store",
        type="float",
        dest="wire_size",
        help="remesh beams (default: -1.0: do not remesh)",
    )
    parser.add_option(
        "-f",
        "--wire-metric",
        metavar="FLOAT",
        default=None,
        action="store",
        type="str",
        dest="wire_metric_file",
        help="wire metric file",
    )
    parser.add_option(
        "-e",
        "--eratio",
        metavar="FLOAT",
        default=10.0,
        action="store",
        type="float",
        dest="eratio",
        help="remove triangles whose edge ratio is greater than "
        "tolerance (default: 10.0)",
    )
    parser.add_option(
        "--no-clean",
        default=False,
        action="store_true",
        dest="noclean",
        help="Do not clean after remesh",
    )
    parser.add_option(
        "-I",
        "--immutable-border",
        action="store_true",
        dest="immutable_border",
        help="Tag free edges as immutable",
    )
    parser.add_option(
        "-M",
        "--immutable-groups",
        metavar="STRING",
        action="store",
        type="string",
        dest="immutable_groups_file",
        help="A text file containing the list of groups whose "
        "elements and nodes must not be modified by this algorithm.",
    )
    parser.add_option(
        "--record",
        metavar="PREFIX",
        action="store",
        type="string",
        dest="recordFile",
        help="record mesh operations in a Python file to replay this " "scenario",
    )

    (options, args) = parser.parse_args(args=sys.argv[1:])

    if len(args) != 4:
        parser.print_usage()
        sys.exit(1)
    options.in_dir = args[0]
    options.out_dir = args[1]
    options.point_metric_file = args[2]
    options.sizeinf = float(args[3])
    remesh(**options.__dict__)
