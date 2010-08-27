# jCAE
from org.jcae.mesh.xmldata import SubMeshWorker

# Java
from java.lang import String

# Python
import sys
from array import array
"""
apply another command to a subset of mesh group
"""
args=sys.argv[1:]
if len(args) < 4:
    print "Apply another command to a subset of mesh group"
    print "Syntax: submesh <inputDir> <outputDir> <group list> <cmd> [<cmd args>...]"
    sys.exit(1)

input_dir=args[0]
output_dir=args[1]
group_list=array(String, args[2].split(","))
cmd_name=args[3]
submesh_worker = SubMeshWorker(input_dir)
extractedDir = submesh_worker.extractGroups(group_list)
sys.argv=sys.argv[4:]
sys.argv.append(extractedDir)
sys.argv.append(extractedDir)
__import__(cmd_name)
submesh_worker.mergeMeshes(args[1]);

