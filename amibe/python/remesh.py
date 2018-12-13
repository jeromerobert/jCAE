
# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge, Vertex
from org.jcae.mesh.amibe.algos3d import *
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.amibe.metrics import EuclidianMetric3D, DistanceMetric, SingularMetric, AbstractDistanceMetric
from org.jcae.mesh.xmldata import MeshReader, MeshWriter, Amibe2VTK

# Java
from java.util import HashMap
from java.util import ArrayList
from java.util import LinkedHashMap
from java.lang import String

# Python
import sys
from math import sqrt
from optparse import OptionParser
import tempfile
import subprocess
import os
import shutil

def read_groups(file_name):
    f=open(file_name)
    r=f.read().split()
    f.close()
    return r

def check_metric_type(file_name):
    """Read a first data line of a point metric file and return corresponding
    format. Returns 'distance', 'singular', 'empty', 'unknown'
    """
    ftype = 'empty'
    with open(file_name, 'r') as fid:
        line = fid.readline()
        while line.startswith('#'):
            line = fid.readline()
        if not line.startswith('#'):
            data = line.strip().split()
            ndata = len(data)
            if ndata == 7 or ndata == 12:
                ftype = 'distance'
            elif ndata == 8 or ndata == 13:
                ftype = 'singular'
            else:
                ftype = 'unknown'
        return ftype

# Set to 1 to enable writing mesh at each stage
debug_write_counter=0
def writeVTK(liaison):
    global debug_write_counter
    if debug_write_counter:
        MeshWriter.writeObject3D(liaison.mesh, "/tmp/tmp.amibe", "")
        Amibe2VTK("/tmp/tmp.amibe").write("/tmp/m%i.vtp" % (debug_write_counter-1));
        debug_write_counter=debug_write_counter+1

def read_mesh(path):
    mtb = MeshTraitsBuilder.getDefault3D()
    mtb.addNodeSet()
    mesh = Mesh(mtb)
    MeshReader.readObject3D(mesh, path)
    return mesh

def afront_debug(afront_path, tmp_dir, mesh, size, point_metric, immutable_groups,
		afront_stderr = None, custom_options=None):
    from org.jcae.mesh.xmldata import Amibe2OFF, AFront2Amibe, AmibeReader, MultiDoubleFileReader
    """ Same as afront but with temporary files to help debugging """
    if custom_options is None:
        custom_options = []
    mesh_dir = os.path.join(tmp_dir, "mesh.amibe")
    if isinstance(point_metric, AbstractDistanceMetric):
        metric_file = os.path.join(tmp_dir, "metric.bin")
        point_metric.save(metric_file)
        g_id = 1
    else:
        metric_file = None
    MeshWriter.writeObject3D(mesh, mesh_dir, "")
    ar = AmibeReader.Dim3(mesh_dir)
    sm = ar.submeshes[0]
    nodes_file = os.path.join(tmp_dir, "nodes.bin")
    for g in sm.groups:
        if g.numberOfTrias == 0 or g.name in immutable_groups:
            f = open(nodes_file, 'ab')
            f.write('\0'*4)
            f.close()
            continue
        off_fn = tmp_dir+"/"+g.name+".off"
        m_fn = tmp_dir+"/"+g.name+".m"
        amibe_fn = tmp_dir+"/"+g.name+".amibe"
        vtk_fn = tmp_dir+"/"+g.name+".vtp"
        Amibe2OFF(ar).write(off_fn, g.name)
        cmd = [afront_path, '-nogui', off_fn, '-failsafe','false',
            '-resamp_bounds', 'false', '-lf_progress', 'true',
            '-stop_every', '10000', '-quiet', 'true', '-outname', nodes_file,
            '-idealNumThreads', '1'] + ([] if custom_options is None else custom_options)
        if metric_file:
            cmd.extend(['-target_size', str(point_metric.getSize(g_id)),
                '-metric_file', metric_file])
            g_id = g_id + 1
        else:
            cmd.extend(['-target_size', str(size)])
        cmd.append('-tri_mesh')
        logstd = sys.stdout if afront_stderr is subprocess.STDOUT else sys.stderr
        logstd.write("meshing %s\n" % g.name)
        logstd.write(" ".join(cmd)+"\n")
        return_code = subprocess.call(cmd, cwd = tmp_dir, stderr = afront_stderr)
        if return_code != 0:
            print "Exit code: "+str(return_code)
    return MultiDoubleFileReader(nodes_file)

def afront(afront_path, tmp_dir, mesh, size, point_metric, immutable_groups,
    afront_stderr = None, custom_options=None):
    from org.jcae.mesh.xmldata import AmibeReader, MultiDoubleFileReader
    """ Run afront and return a MultiDoubleFileReader allowing to read created
    nodes """
    if custom_options is None:
        custom_options = []
    mesh_dir = os.path.join(tmp_dir, "mesh.amibe")
    if isinstance(point_metric, AbstractDistanceMetric):
        metric_file = os.path.join(tmp_dir, "metric.bin")
        point_metric.save(metric_file)
        g_id = 1
    else:
        metric_file = None
    MeshWriter.writeObject3D(mesh, mesh_dir, "")
    ar = AmibeReader.Dim3(mesh_dir)
    sm = ar.submeshes[0]
    nodes_file = os.path.join(tmp_dir, "nodes.bin")
    for g in sm.groups:
        if g.numberOfTrias == 0 or g.name in immutable_groups:
            f = open(nodes_file, 'ab')
            f.write('\0'*4)
            f.close()
            continue
        cmd = [afront_path, '-nogui', ':stdin', '-failsafe','false',
            '-resamp_bounds', 'false', '-lf_progress', 'true',
            '-stop_every', '10000', '-quiet', 'true', '-outname', nodes_file,
            '-idealNumThreads', '1'] + ([] if custom_options is None else custom_options)
        if metric_file:
            cmd.extend(['-target_size', str(point_metric.getSize(g_id)),
                '-metric_file', metric_file])
            g_id = g_id + 1
        else:
            cmd.extend(['-target_size', str(size)])
        cmd.append('-tri_mesh')
        logstd = sys.stdout if afront_stderr is subprocess.STDOUT else sys.stderr
        logstd.write("meshing %s\n" % g.name)
        logstd.write(" ".join(cmd)+"\n")
        try:
            p = subprocess.Popen(cmd, stdin = subprocess.PIPE, cwd = tmp_dir,
                 stderr = afront_stderr)
            sm.readGroup(g, p.stdin.fileno().channel)
            p.stdin.flush()
            return_code = p.wait()
            if return_code != 0:
                print "Exit code: "+str(return_code)
        except OSError:
            print "Cannot run afront"
    if os.path.isfile(nodes_file):
        return MultiDoubleFileReader(nodes_file)
    else:
        return

def afront_insert(liaison, nodes_reader, size, point_metric):
    """ Return the list of mutable nodes which have been inserted """
    inserted_vertices = ArrayList()
    if nodes_reader is None:
        return inserted_vertices
    if point_metric:
        remesh = VertexInsertion(liaison, point_metric)
    else:
        remesh = VertexInsertion(liaison, size)
    for g_id in xrange(1, liaison.mesh.getNumberOfGroups()+1):
        vs = nodes_reader.next()
        remesh.insertNodes(vs, g_id)
        inserted_vertices.addAll(remesh.mutableInserted)
    return inserted_vertices

def remesh(**kwargs):
    """
    Remesh the amibe mesh in xmlDir and save it to outDir.
    See parser.add_option code at the end of this file for kwargs details
    """
    class ArgsWrapper(dict):
        def __init__(self, kwargs):
            self.update(kwargs)
        def __getattr__(self, name):
            return self.get(name)

    __remesh(ArgsWrapper(kwargs))

def create_mesh(**kwargs):
    mtb = MeshTraitsBuilder.getDefault3D()
    if kwargs.get('recordFile'):
        mtb.addTraceRecord()
    mtb.addNodeSet()
    mesh = Mesh(mtb)
    if kwargs.get('recordFile'):
        mesh.getTrace().setDisabled(True)
    MeshReader.readObject3D(mesh, kwargs['in_dir'])
    return mesh

def __remesh(options):
    afront_stderr = getattr(options, 'afront_stderr', None)
    mesh = getattr(options, 'mesh', None)
    liaison = getattr(options, 'liaison', None)
    if not liaison:
        if not mesh:
            mesh = create_mesh(**options)
        liaison = MeshLiaison.create(mesh)

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
        metric_type = check_metric_type(options.point_metric_file)
        if metric_type == 'singular':
            point_metric = SingularMetric(options.size, options.point_metric_file, 2.0, True)
        else:
            point_metric = DistanceMetric(options.size, options.point_metric_file)
    elif getattr(options, 'point_metric', None):
        point_metric = options.point_metric
    else:
        point_metric = None
    safe_coplanarity = str(max(options.coplanarity, 0.8))

    if options.forced_points:
        if point_metric:
            vi = VertexInsertion(liaison, point_metric)
        else:
            vi = VertexInsertion(liaison, options.size)
        vi.insertNodes(options.forced_points, -1)
        Vertex.setMutable(vi.mutableInserted, False)

    #0
    writeVTK(liaison)
    if options.boundary_angle == None:
        options.boundary_angle = 1.66
    if point_metric:
        point_metric.scaling = 1
        if options.forced_bounds:
            BeamInsertion(liaison.mesh, point_metric).insert(
                options.forced_bounds[0], options.forced_bounds[1])
        RemeshSkeleton(liaison, options.boundary_angle, options.size / 100.0, point_metric).compute()
    else:
        RemeshSkeleton(liaison, options.boundary_angle, options.size / 100.0, options.size).compute()
        if options.forced_bounds:
            BeamInsertion(liaison.mesh, options.size).insert(
                options.forced_bounds[0], options.forced_bounds[1])

    #1
    writeVTK(liaison)
    opts = HashMap()
    opts.put("coplanarity", safe_coplanarity)
    # Swapping here will help QEMDecimateHalfEdge to decimate more and will
    # reduce the risk to have edge not processed by LengthDecimateHalfEdge
    algo = SwapEdge(liaison, opts)
    algo.maxSwapVolume = (options.size / 4.0)**3
    algo.compute()

    #2
    writeVTK(liaison)

    if options.recordFile:
        cmds = [ String("assert self.m.checkNoDegeneratedTriangles()"), String("assert self.m.checkNoInvertedTriangles()"), String("assert self.m.checkVertexLinks()"), String("assert self.m.isValid()") ]
        liaison.getMesh().getTrace().setHooks(cmds)

    opts.clear()
    opts.put("coplanarity", str(options.coplanarity))
    opts.put("size", str(options.size*0.3))
    opts.put("maxlength", str(options.size*sqrt(2)))
    #workaround for a QEMDecimateHalfEdge bug
    opts.put("freezeNonManifold", "true")
    algo = QEMDecimateHalfEdge(liaison, opts)
    if point_metric:
        point_metric.scaling = sqrt(2)
        algo.analyticMetric = point_metric
    algo.compute()

    #3
    # afront call
    writeVTK(liaison)
    afront_nodes_reader = None
    afront_frozen = None
    if options.afront_path:
        tmp_dir = tempfile.mkdtemp()
        afront_nodes_reader = afront(options.afront_path, tmp_dir, liaison.mesh,
            options.size, point_metric, immutable_groups, afront_stderr = afront_stderr,
            custom_options = options.afront_custom_options)
        afront_frozen = afront_insert(liaison, afront_nodes_reader, options.size, point_metric)
        Vertex.setMutable(afront_frozen, False)
        shutil.rmtree(tmp_dir, ignore_errors=True)

    #4
    writeVTK(liaison)
    if options.afront_path:
        opts.clear()
        opts.put("expectInsert", "false")
        opts.put("coplanarity", safe_coplanarity)
        SwapEdge(liaison, opts).compute()

    #5
    writeVTK(liaison)
    opts.clear()
    opts.put("size", str(options.size))
    opts.put("freeEdgesOnly", "true")
    opts.put("coplanarity", "-2")
    algo = LengthDecimateHalfEdge(liaison, opts)
    if point_metric:
        algo.analyticMetric = point_metric
    algo.compute()

    #6
    writeVTK(liaison)
    opts.clear()
    opts.put("size", str(options.size))
    opts.put("coplanarity", str(options.coplanarity))
    opts.put("minCosAfterSwap", "0.3")
    opts.put("nearLengthRatio", "0.6")
    algo = Remesh(liaison, opts)
    if point_metric:
        point_metric.scaling = 1
        algo.analyticMetric = point_metric
    algo.compute()

    #7
    writeVTK(liaison)

    opts.clear()
    opts.put("coplanarity", safe_coplanarity)
    opts.put("expectInsert", "false" if options.afront_path else "true")
    SwapEdge(liaison, opts).compute()

    #8
    writeVTK(liaison)

    opts.clear()
    opts.put("coplanarity", str(options.coplanarity))
    opts.put("iterations", "2")
    opts.put("size", str(options.size))
    algo = SmoothNodes3DBg(liaison, opts)
    algo.compute()

    #9
    writeVTK(liaison)

    opts.clear()
    opts.put("coplanarity", str(options.coplanarity))
    opts.put("expectInsert", "false" if options.afront_path else "true")
    opts.put("minCosAfterSwap", "0.3")
    algo = SwapEdge(liaison, opts)
    algo.angleQualityRatio = 150
    algo.compute()

    #10
    writeVTK(liaison)
    if not options.afront_path:
        opts.clear()
        opts.put("size", str(options.size))
        algo = Remesh(liaison, opts)
        algo.analyticMetric = point_metric
        algo.compute()

    #11
    writeVTK(liaison)

    opts.clear()
    opts.put("coplanarity", str(options.coplanarity))
    opts.put("size", str(options.size*0.3))
    opts.put("maxlength", str(options.size*sqrt(2)))
    #workaround for a QEMDecimateHalfEdge bug
    opts.put("freezeNonManifold", "true")
    algo = QEMDecimateHalfEdge(liaison, opts)
    if point_metric:
        point_metric.scaling = sqrt(2)
        algo.analyticMetric = point_metric
    algo.compute()

    #12
    writeVTK(liaison)

    opts.clear()
    opts.put("coplanarity", str(options.coplanarity))
    opts.put("expectInsert", "false" if options.afront_path else "true")
    opts.put("minCosAfterSwap", "0.3")
    algo = SwapEdge(liaison, opts)
    algo.angleQualityRatio = 150
    algo.compute()

    #13
    writeVTK(liaison)

    if afront_frozen:
        Vertex.setMutable(afront_frozen, True)

    opts.clear()
    opts.put("checkNormals", "true" if options.afront_path else "false")
    ImproveVertexValence(liaison, opts).compute()

    #14
    writeVTK(liaison)

    opts.clear()
    opts.put("coplanarity", safe_coplanarity)
    opts.put("iterations", str(8))
    algo = SmoothNodes3DBg(liaison, opts)
    algo.compute()

    #15
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
            elif point_metric:
                result = RemeshPolyline(liaison.mesh, polyline, point_metric).compute()
            else:
                result = RemeshPolyline(liaison.mesh, polyline, listM).compute()
            for i in xrange(result.size() - 1):
                liaison.mesh.addBeam(result.get(i), result.get(i+1), groupId)
            #print "  New polyline: "+str(result.size())+" vertices"

    if options.recordFile:
        liaison.getMesh().getTrace().finish()

    if options.post_script:
        execfile(options.post_script)
    if options.out_dir:
        MeshWriter.writeObject3D(liaison.mesh, options.out_dir, "")

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
                       1, x, y, z, s0 (target size at distance d0), d0, d1(distance from which point has no influence)
                      Target size at distance d of this point is computed like this:
                        a) if d <= d0, s0.
                        b) if d >= d1, global mesh size sInf.
                        c) if d0 < d < d1, s0 + (sInf - s0) * (d^2 - d0^2)/(d1^2 - d0^2).""")
    parser.add_option("-M", "--immutable-groups", metavar="STRING",
                      action="store", type="string", dest="immutable_groups_file",
                      help="""A text file containing the list of groups which whose
                      elements and nodes must be modified by this algorithm.""")
    parser.add_option("--afront", metavar="PATH",
                      action="store", type="string", dest="afront_path",
                      help="Path to the afront (http://afront.sf.net) executable.")
    parser.add_option("--post-script", metavar="PATH",
                      action="store", type="string", dest="post_script",
                      help="Execute the given script in the context of the __remesh method.")
    (options, args) = parser.parse_args(args=sys.argv[1:])

    if len(args) != 2:
        parser.print_usage()
        sys.exit(1)
    options.in_dir = args[0]
    options.out_dir = args[1]
    remesh(**options.__dict__)
