
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge, Vertex
from org.jcae.mesh.amibe.algos3d import *
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.amibe.metrics import EuclidianMetric3D, DistanceMetric
from org.jcae.mesh.xmldata import MeshReader, MeshWriter, Amibe2VTK
from org.jcae.mesh.amibe.algos3d import SmoothNodes3DBg, RemeshPolyline

# Java
from java.util import HashMap
from java.util import ArrayList
from java.util import LinkedHashMap

# GNU trove
from gnu.trove import TIntArrayList

# Python
import sys
from math import sqrt
from optparse import OptionParser
import tempfile
import subprocess
import os

def read_groups(file_name):
    f=open(file_name)
    r=f.read().split()
    f.close()
    return r

debug_write_counter=1
def writeVTK(liaison):
    global debug_write_counter
    """MeshWriter.writeObject3D(liaison.mesh, "/tmp/tmp.amibe", "")
    Amibe2VTK("/tmp/tmp.amibe").write("/tmp/m%i.vtp" % debug_write_counter);"""
    debug_write_counter=debug_write_counter+1

def read_mesh(path):
    mtb = MeshTraitsBuilder.getDefault3D()
    mtb.addNodeSet()
    mesh = Mesh(mtb)
    MeshReader.readObject3D(mesh, path)
    return mesh

def afront_debug(afront_path, liaison, tmp_dir, mesh_dir, size):
    from org.jcae.mesh.xmldata import Amibe2OFF, AFront2Amibe, AmibeReader
    """ Same as afront but with temporary files to help debugging """
    ar = AmibeReader.Dim3(mesh_dir)
    opts = HashMap()
    opts.put("size", str(size))
    opts.put("minCosAfterSwap", "0.3")
    opts.put("coplanarity", "-2")
    remesh = Remesh(liaison, opts)
    inserted_vertices = ArrayList()
    number_of_groups = liaison.mesh.getNumberOfGroups()
    for g_id in xrange(1, number_of_groups+1):
        g_name = liaison.mesh.getGroupName(g_id)
        print g_name
        off_fn = tmp_dir+"/"+g_name+".off"
        m_fn = tmp_dir+"/"+g_name+".m"
        amibe_fn = tmp_dir+"/"+g_name+".amibe"
        vtk_fn = tmp_dir+"/"+g_name+".vtp"
        Amibe2OFF(ar).write(off_fn, g_name)
        cmd = [afront_path, '-nogui', off_fn, '-failsafe','false', '-target_size',
            str(size), '-lf_progress', 'true', '-stop_every', '1000',
			'-quiet', 'true', '-outname', m_fn, '-tri_mesh']
        print " ".join(cmd)
        subprocess.call(cmd)
        if os.path.isfile(m_fn):
            AFront2Amibe(amibe_fn).read(m_fn)
            Amibe2VTK(amibe_fn).write(vtk_fn)
            vertices = read_mesh(amibe_fn).nodes
            inserted_vertices.addAll(remesh.insertNodes(vertices, g_id, size, size/100.0))
    return inserted_vertices

def afront(afront_path, tmp_dir, mesh_dir, size):
    from org.jcae.mesh.xmldata import AmibeReader, MultiDoubleFileReader
    """ Run afront and return a MultiDoubleFileReader allowing to read created
    nodes """
    ar = AmibeReader.Dim3(mesh_dir)
    sm = ar.submeshes[0]
    nodes_file = os.path.join(tmp_dir, "nodes.bin")
    for g in sm.groups:
        if g.numberOfTrias == 0:
            f = open(nodes_file, 'ab')
            f.write('\0'*4)
            f.close()
            continue
        cmd = [afront_path, '-nogui', ':stdin', '-failsafe','false', '-target_size',
            str(size), '-lf_progress', 'true', '-stop_every', '1000',
            '-quiet', 'true', '-outname', nodes_file, '-tri_mesh']
        print " ".join(cmd)
        p = subprocess.Popen(cmd, stdin = subprocess.PIPE, cwd = tmp_dir)
        sm.readGroup(g, p.stdin.fileno().channel)
        p.stdin.flush()
        return_code = p.wait()
        if return_code != 0:
            print "Exit code: "+str(return_code)
    return MultiDoubleFileReader(nodes_file)

def afront_insert(liaison, nodes_reader, size):
    opts = HashMap()
    opts.put("size", str(size))
    opts.put("minCosAfterSwap", "0.3")
    opts.put("coplanarity", "-2")
    remesh = Remesh(liaison, opts)
    inserted_vertices = ArrayList()
    for g_id in xrange(1, liaison.mesh.getNumberOfGroups()+1):
        vs = nodes_reader.next()
        inserted_vertices.addAll(remesh.insertNodes(vs, g_id, size, size/100.0))
    return inserted_vertices

def remesh(xmlDir, outDir, **kwargs):
    """
    Remesh the amibe mesh in xmlDir and save it to outDir.
    See parser.add_option code at the end of this file for kwargs details
    """
    class ArgsWrapper(object):
        def __init__(self, kwargs):
            self.kwargs = kwargs
        def __getattr__(self, name):
            return self.kwargs.get(name)

    __remesh(xmlDir, outDir, ArgsWrapper(kwargs))

def __remesh(xmlDir, outDir, options):
    afront_nodes_reader = None
    if options.afront_path:
        tmp_dir = tempfile.mkdtemp()
        afront_nodes_reader = afront(options.afront_path, tmp_dir, xmlDir, options.size)

    mtb = MeshTraitsBuilder.getDefault3D()
    if options.recordFile:
        mtb.addTraceRecord()
    mtb.addNodeSet()
    mesh = Mesh(mtb)
    if options.recordFile:
        mesh.getTrace().setDisabled(True)
    MeshReader.readObject3D(mesh, xmlDir)

    liaison = MeshLiaison(mesh, mtb)
    if options.recordFile:
        liaison.getMesh().getTrace().setDisabled(False)
        liaison.getMesh().getTrace().setLogFile(options.recordFile)
        liaison.getMesh().getTrace().createMesh("mesh", liaison.getMesh())
    if options.immutable_border:
        liaison.mesh.tagFreeEdges(AbstractHalfEdge.IMMUTABLE)

    liaison.getMesh().buildRidges(options.coplanarity)
    if options.immutable_border_group:
        liaison.mesh.tagGroupBoundaries(AbstractHalfEdge.IMMUTABLE)
    else:
        if options.preserveGroups:
            liaison.getMesh().buildGroupBoundaries()

    immutable_groups = []
    if options.immutable_groups_file:
        immutable_groups = read_groups(options.immutable_groups_file)
        liaison.mesh.tagGroups(immutable_groups, AbstractHalfEdge.IMMUTABLE)

    if options.point_metric_file:
        point_metric = DistanceMetric(options.size, options.point_metric_file)
    else:
        point_metric = None
    safe_coplanarity = str(max(options.coplanarity, 0.8))

    afront_frozen = None
    if afront_nodes_reader:
        afront_frozen = afront_insert(liaison, afront_nodes_reader, options.size)
        Vertex.setMutable(afront_frozen, False)

    #0
    writeVTK(liaison)

    if options.recordFile:
        cmds = [ String("assert self.m.checkNoDegeneratedTriangles()"), String("assert self.m.checkNoInvertedTriangles()"), String("assert self.m.checkVertexLinks()"), String("assert self.m.isValid()") ]
        liaison.getMesh().getTrace().setHooks(cmds)

    opts = HashMap()
    opts.put("coplanarity", str(options.coplanarity))
    opts.put("size", str(options.size*0.3))
    opts.put("maxlength", str(options.size*sqrt(2)))
    algo = QEMDecimateHalfEdge(liaison, opts)
    if options.point_metric_file:
        algo.analyticMetric = DistanceMetric(options.size*sqrt(2), options.point_metric_file)
    algo.compute()
    #1
    writeVTK(liaison)

    opts.clear()
    opts.put("size", str(options.size))
    opts.put("coplanarity", str(options.coplanarity))
    opts.put("minCosAfterSwap", "0.3")
    opts.put("nearLengthRatio", "0.6")
    algo = Remesh(liaison, opts)
    algo.analyticMetric = point_metric
    algo.compute()

    #2
    writeVTK(liaison)

    opts.clear()
    opts.put("coplanarity", safe_coplanarity)
    SwapEdge(liaison, opts).compute()

    #3
    writeVTK(liaison)

    opts.clear()
    opts.put("coplanarity", str(options.coplanarity))
    opts.put("iterations", "2")
    opts.put("size", str(options.size))
    algo = SmoothNodes3DBg(liaison, opts)
    algo.compute()

    #4
    writeVTK(liaison)

    opts.clear()
    opts.put("coplanarity", str(options.coplanarity))
    opts.put("minCosAfterSwap", "0.3")
    SwapEdge(liaison, opts).compute()

    #5
    writeVTK(liaison)
    if not options.afront_path:
        opts.clear()
        opts.put("size", str(options.size))
        algo = Remesh(liaison, opts)
        algo.analyticMetric = point_metric
        algo.compute()

    #6
    writeVTK(liaison)

    opts.clear()
    opts.put("size", str(options.size*0.3))
    opts.put("freeEdgesOnly", "true")
    algo = LengthDecimateHalfEdge(liaison, opts)
    if options.point_metric_file:
        algo.analyticMetric = DistanceMetric(options.size*0.3, options.point_metric_file)
    algo.compute()

    #7
    writeVTK(liaison)

    opts.clear()
    opts.put("coplanarity", str(options.coplanarity))
    opts.put("size", str(options.size*0.3))
    opts.put("maxlength", str(options.size*sqrt(2)))
    algo = QEMDecimateHalfEdge(liaison, opts)
    if options.point_metric_file:
        algo.analyticMetric = DistanceMetric(options.size*sqrt(2), options.point_metric_file)
    algo.compute()

    #8
    writeVTK(liaison)

    opts.clear()
    opts.put("coplanarity", str(options.coplanarity))
    opts.put("minCosAfterSwap", "0.3")
    SwapEdge(liaison, opts).compute()

    #9
    writeVTK(liaison)

    if afront_frozen:
        Vertex.setMutable(afront_frozen, True)

    opts.clear()
    opts.put("checkNormals", "false")
    ImproveVertexValence(liaison, opts).compute()

    #10
    writeVTK(liaison)

    opts.clear()
    opts.put("coplanarity", safe_coplanarity)
    opts.put("iterations", str(8))
    algo = SmoothNodes3DBg(liaison, opts)
    algo.compute()

    writeVTK(liaison)

    #MeshWriter.writeObject3D(liaison.mesh, outDir, ""
    polylines=PolylineFactory(liaison.mesh, 135.0, options.size*0.2)
    liaison.mesh.resetBeams()
    for entry in polylines.entrySet():
      groupId = entry.key
      for polyline in entry.value:
            listM = ArrayList()
            for v in polyline:
                listM.add(EuclidianMetric3D(options.size))
            #print "Remesh polyline of group "+str(groupId)+"/"+str(polylines.size())+" "+str(polyline.size())+" vertices"
            if liaison.mesh.getGroupName(groupId) in immutable_groups:
                result = polyline
            else:
                result = RemeshPolyline(liaison.mesh, polyline, listM).compute()
            for i in xrange(result.size() - 1):
                liaison.mesh.addBeam(result.get(i), result.get(i+1), groupId)
            #print "  New polyline: "+str(result.size())+" vertices"

    if options.recordFile:
        liaison.getMesh().getTrace().finish()

    if options.convert_junctions:
        JunctionConverter(liaison.mesh).compute()
    MeshWriter.writeObject3D(liaison.mesh, outDir, "")

if __name__ == "__main__":
    """
    Remesh an existing mesh.
    """

    cmd=("remesh  ", "<inputDir> <outputDir>", "Remesh an existing mesh")
    parser = OptionParser(usage="amibebatch %s [OPTIONS] %s\n\n%s" % cmd,
        prog="remesh")
    parser.add_option("-g", "--preserveGroups", action="store_true", dest="preserveGroups",
                      help="edges adjacent to two different groups are handled like free edges")
    parser.add_option("-t", "--size", metavar="FLOAT", default=0.0,
                      action="store", type="float", dest="size",
                      help="target size")
    parser.add_option("-I", "--immutable-border",
                      action="store_true", dest="immutable_border",
                      help="Tag free edges as immutable")
    parser.add_option("-G", "--immutable-border-group",
                      action="store_true", dest="immutable_border_group",
                      help="Tag border group edges as immutable")
    parser.add_option("--record", metavar="PREFIX",
                      action="store", type="string", dest="recordFile",
                      help="record mesh operations in a Python file to replay this scenario")
    parser.add_option("-c", "--coplanarity", metavar="FLOAT",
                      action="store", type="float", dest="coplanarity", default=0.9,
                      help="dot product of face normals to detect feature edges")
    parser.add_option("-P", "--point-metric", metavar="STRING",
                      action="store", type="string", dest="point_metric_file",
                      help="""A CSV file containing points which to refine around. Each line must contains 6 values:
                      - 1
                      - x, y, z
                      - the distance of the source where the target size is defined
                      - the target size at the given distance""")
    parser.add_option("-M", "--immutable-groups", metavar="STRING",
                      action="store", type="string", dest="immutable_groups_file",
                      help="""A text file containing the list of groups which whose
                      elements and nodes must be modified by this algorithm.""")
    parser.add_option("-J", "--convert-junctions", metavar="STRING",
                      action="store_true", dest="convert_junctions",
                      help="""Convert junctions between beams and triangles to group
                      of nodes. The junction vertex is duplicated and a node group
                      is created with the 2 vertices plus the closest one from
                      adjacent triangles.""")
    parser.add_option("--afront", metavar="PATH",
                      action="store", type="string", dest="afront_path",
                      help="Path to the afront (http://afront.sf.net) executable.")
    (options, args) = parser.parse_args(args=sys.argv[1:])

    if len(args) != 2:
        parser.print_usage()
        sys.exit(1)

    xmlDir = args[0]
    outDir = args[1]
    __remesh(xmlDir, outDir, options)