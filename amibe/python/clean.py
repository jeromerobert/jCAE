
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.algos3d import SwapEdge, ImproveVertexValence, SmoothNodes3DBg
from org.jcae.mesh.amibe.algos3d import RemoveDegeneratedTriangles
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.xmldata import MeshReader, MeshWriter

# Java
from java.util import HashMap
from java.lang import String

# Python
import sys
from math import cos, pi
from optparse import OptionParser


def clean(**kwargs):
    """Clean a mesh
    """
    # Process coplanarity options
    coplanarity = cos(kwargs['coplanarityAngle'] * pi / 180.)
    if kwargs['coplanarity']:
        coplanarity = kwargs['coplanarity']

    safe_coplanarity = kwargs['safe_coplanarity']
    if safe_coplanarity is None:
        safe_coplanarity = 0.8
    safe_coplanarity = str(max(coplanarity, safe_coplanarity))

    # Build background mesh
    try:
        liaison = kwargs['liaison']
    except KeyError:
        mtb = MeshTraitsBuilder.getDefault3D()
        if kwargs['recordFile']:
            mtb.addTraceRecord()
        mtb.addNodeSet()
        mesh = Mesh(mtb)
        if kwargs['recordFile']:
            mesh.getTrace().setDisabled(True)

        MeshReader.readObject3D(mesh, kwargs['in_dir'])
        liaison = MeshLiaison.create(mesh, mtb)

    if kwargs['recordFile']:
        liaison.getMesh().getTrace().setDisabled(False)
        liaison.getMesh().getTrace().setLogFile(kwargs['recordFile'])
        liaison.getMesh().getTrace().createMesh("mesh", liaison.getMesh())
    if kwargs['immutable_border']:
        liaison.mesh.tagFreeEdges(AbstractHalfEdge.IMMUTABLE)
    liaison.getMesh().buildRidges(coplanarity)
    if kwargs['preserveGroups']:
        liaison.getMesh().buildGroupBoundaries()

    immutable_groups = []
    if kwargs['immutable_groups_file']:
        f = open(kwargs['immutable_groups_file'])
        immutable_groups = f.read().split()
        f.close()
        liaison.mesh.tagGroups(immutable_groups, AbstractHalfEdge.IMMUTABLE)

    if kwargs['recordFile']:
        cmds = [ String("assert self.m.checkNoDegeneratedTriangles()"),
                 String("assert self.m.checkNoInvertedTriangles()"),
                 String("assert self.m.checkVertexLinks()"),
                 String("assert self.m.isValid()") ]
        liaison.getMesh().getTrace().setHooks(cmds)

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
    smoothOptions.put("relaxation", str(0.6))
    if (safe_coplanarity >= 0.0):
        smoothOptions.put("coplanarity", str(safe_coplanarity))
    SmoothNodes3DBg(liaison, smoothOptions).compute()

    # Remove Degenerated
    rdOptions = HashMap()
    rdOptions.put("rho", str(kwargs['eratio']))
    RemoveDegeneratedTriangles(liaison, rdOptions).compute()

    # Output
    MeshWriter.writeObject3D(liaison.getMesh(), kwargs['out_dir'], "")
    if kwargs['recordFile']:
        liaison.getMesh().getTrace().finish()


if __name__ == "__main__":
    """
    Clean an existing mesh: swap+valence+smooth+remove degenerated.
    """

    cmd=("clean", "<inputDir> <outputDir>", "Clean an existing mesh")
    parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd, prog="clean")
    parser.add_option("-a", "--angle", metavar="FLOAT", default=15.0,
                      action="store", type="float", dest="coplanarityAngle",
                      help="angle (in degrees) between face normals to detect "
                      "feature edges (default: 15, superseded if -c is defined)")
    parser.add_option("-c", "--coplanarity", metavar="FLOAT",
                      action="store", type="float", dest="coplanarity",
                      help="dot product of face normals to detect feature edges")
    parser.add_option("-s", "--safe-coplanarity", metavar="FLOAT",
                      action="store", type="float", dest="safe_coplanarity", default=0.8,
                      help="dot product of face normals tolerance for algorithms")
    parser.add_option("-g", "--preserveGroups", action="store_true", dest="preserveGroups",
                      help="edges adjacent to two different groups are handled like free edges")
    parser.add_option("-e", "--eratio", metavar="FLOAT", default=50.0,
                      action="store", type="float", dest="eratio",
                      help="remove triangles whose edge ratio is greater than "
                      "tolerance (default: 50.0)")
    parser.add_option("-I", "--immutable-border",
                      action="store_true", dest="immutable_border",
                      help="Tag free edges as immutable")
    parser.add_option("-M", "--immutable-groups", metavar="STRING",
                      action="store", type="string", dest="immutable_groups_file",
                      help="A text file containing the list of groups which whose "
                      "elements and nodes must not be modified by this algorithm.")
    parser.add_option("--record", metavar="PREFIX",
                      action="store", type="string", dest="recordFile",
                      help="record mesh operations in a Python file to replay this "
                      "scenario")

    (options, args) = parser.parse_args(args=sys.argv[1:])

    if len(args) != 2:
        parser.print_usage()
        sys.exit(1)
    options.in_dir = args[0]
    options.out_dir = args[1]
    clean(**options.__dict__)
