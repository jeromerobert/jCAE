# jCAE
from org.jcae.mesh.amibe.ds import Mesh, AbstractHalfEdge
from org.jcae.mesh.amibe.algos3d import SplitTagged
from org.jcae.mesh.amibe.algos3d import SwapEdge, ImproveVertexValence, SmoothNodes3DBg
from org.jcae.mesh.amibe.traits import MeshTraitsBuilder
from org.jcae.mesh.amibe.projection import MeshLiaison
from org.jcae.mesh.xmldata import MeshReader, MeshWriter

# Java
from java.util import HashMap

# Python
from argparse import ArgumentParser


def split_tagged(**kwargs):
    """
    """
    # Process coplanarity options
    if kwargs['coplanarity']:
        coplanarity = kwargs['coplanarity']
    else:
        coplanarity = -2.0

    safe_coplanarity = kwargs['safe_coplanarity']
    if safe_coplanarity is None:
        safe_coplanarity = 0.8
    safe_coplanarity = str(max(coplanarity, safe_coplanarity))

    # Liaison
    try:
        liaison = kwargs['liaison']
    except KeyError:
        mtb = MeshTraitsBuilder.getDefault3D()
        mtb.addNodeSet()
        mesh = Mesh(mtb)
        MeshReader.readObject3D(mesh, kwargs['in_dir'])
        if kwargs['background']:
            mesh_bg = Mesh(mtb)
            MeshReader.readObject3D(mesh_bg, kwargs['background'])
            liaison = MeshLiaison.create(mesh_bg, mesh, mtb)
        else:
            liaison = MeshLiaison.create(mesh, mtb)

    liaison.getMesh().buildRidges(coplanarity)
    if kwargs['immutable_border']:
        liaison.mesh.tagFreeEdges(AbstractHalfEdge.IMMUTABLE)
    if kwargs['preserveGroups']:
        liaison.mesh.buildGroupBoundaries()
    immutable_groups = []
    if kwargs['immutable_groups_file']:
        f = open(kwargs['immutable_groups_file'])
        immutable_groups = f.read().split()
        f.close()
        liaison.mesh.tagGroups(immutable_groups, AbstractHalfEdge.IMMUTABLE)

    # Split
    algo = SplitTagged(liaison)
    tags = None
    if kwargs['tags'] is not None:
        tags = kwargs['tags']
    elif kwargs['tag_file'] is not None:
        with open(kwargs['tag_file'], 'r') as fid:
            tags = list(map(int, fid.readlines()))
    if tags is not None:
        algo.tagTriangles(tags)
    algo.compute()

    # Regularization
    if kwargs['clean']:
        swapOptions = HashMap()
        swapOptions.put('coplanarity', str(safe_coplanarity))
        swapOptions.put('minCosAfterSwap', '0.3')
        SwapEdge(liaison, swapOptions).compute()

        valenceOptions = HashMap()
        valenceOptions.put('coplanarity', str(safe_coplanarity))
        valenceOptions.put('checkNormals', 'true')
        ImproveVertexValence(liaison, valenceOptions).compute()

        smoothOptions = HashMap()
        smoothOptions.put('iterations', str(8))
        smoothOptions.put('boundaries', 'true')
        smoothOptions.put('check', 'true')
        smoothOptions.put('relaxation', str(0.6))
        smoothOptions.put('coplanarity', str(safe_coplanarity))
        SmoothNodes3DBg(liaison, smoothOptions).compute()

    # Output mesh
    MeshWriter.writeObject3D(liaison.getMesh(), kwargs['out_dir'], '')


if __name__ == '__main__':
    """
    Split tagged elements of a mesh
    """
    parser = ArgumentParser(description='Split tagged elements of a mesh')
    parser.add_argument('in_dir', help='Input amibe mesh')
    parser.add_argument('out_dir', help='Output amibe mesh')
    parser.add_argument('-t', '--tag', action='store', dest='tag_file', default=None,
                        help='file containing a list of triangle ids')
    parser.add_argument('-b', '--background', action='store', dest='background', default=None,
                        help='background amibe file')
    parser.add_argument('-c', '--coplanarity', action='store', type=float, dest='coplanarity', default=None,
                        help='dot product of face normals to detect feature edges')
    parser.add_argument('-s', '--safe-coplanarity', action='store', type=float, dest='safe_coplanarity', default=0.8,
                        help='dot product of face normals tolerance for algorithms')
    parser.add_argument('-r', '--clean', action='store_true', dest='clean', default=False,
                        help='apply mesh regularization')
    parser.add_argument('-g', '--preserveGroups', action='store_true', dest='preserveGroups',
                        help='edges adjacent to two different groups are handled like free edges')
    parser.add_argument('-I', '--immutable-border', action='store_true', dest='immutable_border',
                        help='Tag free edges as immutable')
    parser.add_argument('-M', '--immutable-groups', action='store', dest='immutable_groups_file',
                        help='A text file containing the list of groups which whose '
                        'elements and nodes must not be modified by this algorithm.')

    args = parser.parse_args()

    args.tags = None
    split_tagged(**args.__dict__)
