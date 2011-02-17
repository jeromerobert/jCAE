# jCAE
from org.jcae.mesh.xmldata import SubMeshWorker

# Java
from java.lang import String

# Python
import sys, os
from array import array

"""
extract specified groups from a mesh
"""
args=sys.argv[1:]
if len(args) != 3:
    print "Extract specified groups from a mesh"
    print "Syntax: extract <inputDir> <outputDir> <comma-separated group list>"
    sys.exit(1)

input_dir=args[0]
output_dir=args[1]
group_list=array(String, args[2].split(","))
submesh_worker = SubMeshWorker(input_dir)
extractedDir = submesh_worker.extractGroups(group_list)
os.rename(extractedDir, output_dir)

