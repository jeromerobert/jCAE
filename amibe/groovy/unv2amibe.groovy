/**
 * Convert a UNV file into amibe format.
 */
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.amibe.util.UNVReader;

Mesh mesh = new Mesh(new MeshTraitsBuilder());
UNVReader.readMesh(mesh, args[0]);
MeshWriter.writeObject3D(mesh, args[1], "jcae3d", ".", "dummy.brep");
