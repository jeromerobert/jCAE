/**
 * Sample class to print quality statistics about an amibe mesh.
 */
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.AbstractTriangle;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshExporter;
import org.jcae.mesh.amibe.validation.MinAngleFace;
import org.jcae.mesh.amibe.validation.QualityFloat;
import java.io.File;

void usage() {
	println("Usage: amibeReporter.groovy xmlDir outBase");
	System.exit(1);
}

if (args.length < 2)
	usage();
String xmlDir = args[0]
String xmlFile = "jcae3d";
String outBasename=args[1];
String MESHName=outBasename+".mesh";
String bbfile=outBasename+".bb";

// Use an empty MeshTraitsBuilder to lower memory usage
MeshTraitsBuilder mtb = new MeshTraitsBuilder();
Mesh mesh3D = new Mesh(mtb);
try
{
	MeshReader.readObject3D(mesh3D, xmlDir, xmlFile);
}
catch (RuntimeException ex)
{
	// Err, MeshReader.readObject3D traps exceptions and raises RuntimeException
	// The most common error is that jcae3d file does not exist
	println("File "+xmlDir+File.separator+"jcae3d does not exist!");
	usage();
}
// Compute mesh quality
MinAngleFace qproc = new MinAngleFace();
QualityFloat data = new QualityFloat(mesh3D.getTriangles().size());
data.setQualityProcedure(qproc);
for (AbstractTriangle f: mesh3D.getTriangles())
{
	if (f.isWritable())
		data.compute(f);
}
data.finish();
float target = (float) Math.PI/3.0f;
data.setTarget(target);
data.split(0.4f, 0.6f, 0.8f, 0.9f, 1.0f);
// Prints histogram on console
data.printLayers();
// Prints triangle quality into bbfile, it can be displayed by medit
data.printMeshBB(bbfile);
new MeshExporter.MESH(xmlDir).write(MESHName);

