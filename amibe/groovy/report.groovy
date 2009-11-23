/**
 * Sample class to print quality statistics about an amibe mesh.
 * Example: to print histogram about minimum angles:
 *  amibebatch report -c MinAngleFace -s .0174532925199432957 -b 6,12,18,24,30,36,42,48,54
 */
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshExporter;
import org.jcae.mesh.amibe.validation.*;
import java.lang.reflect.Constructor;
import java.io.File;
import gnu.trove.TIntHashSet;
import org.apache.commons.cli.*;

cmd=["report  ", "Print statistics about mesh quality"]
usage="<dir>"

void usage(int rc, Options options)
{
	HelpFormatter formatter = new HelpFormatter();
	formatter.printHelp("amibebatch "+cmd[0].trim()+" [OPTIONS] "+usage, cmd[1], options, "");
	System.exit(rc);
}

Options options = new Options();
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("show this help message and exit")
		.withLongOpt("help")
		.create('h'));
options.addOption(
	OptionBuilder.withArgName("LIST").hasArg()
		.withDescription("comma separated list of values, implies -H")
		.withLongOpt("bounds")
		.create('b'));
options.addOption(
	OptionBuilder.withArgName("CLASS").hasArg()
		.withDescription("criterion (default: MinAngleFace)")
		.withLongOpt("criterion")
		.create('c'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("lists all available criteria")
		.withLongOpt("list-criteria")
		.create('C'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("reports statistics by face")
		.withLongOpt("detailed")
		.create('d'));
options.addOption(
	OptionBuilder.withArgName("NUMBER").hasArg()
		.withDescription("meshing had been started from this patch number")
		.withLongOpt("from-face")
		.create('f'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("prints histogram")
		.withLongOpt("histogram")
		.create('H'));
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("give details about mesh (number of vertices, triangles, etc)")
		.withLongOpt("mesh")
		.create('m'));
options.addOption(
	OptionBuilder.withArgName("BASE").hasArg()
		.withDescription("creates <BASE>.mesh and <BASE>.bb MEDIT files")
		.withLongOpt("output")
		.create('o'));
options.addOption(
	OptionBuilder.withArgName("NUMBER").hasArg()
		.withDescription("scale factor (default: 1.0)")
		.withLongOpt("scale")
		.create('s'));
CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);
if (cmd.hasOption('C'))
{
	String [] listStr = QualityProcedure.getListSubClasses();
	println("List of available criteria for -c option:");
	for (int idx = 0; idx < listStr.length; idx += 2)
	{
		println(" "+listStr[idx]);
		println("   "+listStr[idx+1]);
	}
	System.exit(0);
}

String [] remaining = cmd.getArgs();
if (remaining.length != 1)
	usage(1, options);

String xmlDir = remaining[0]
String outBasename=cmd.getOptionValue('o');
boolean detailed = cmd.hasOption('d');
boolean histogram = cmd.hasOption('H') || cmd.hasOption('b');

String [] sBounds=cmd.getOptionValue('b', "0.2,0.4,0.6,0.8").split(",");
Float [] bounds = new Float[sBounds.length];
for (int i = 0; i < bounds.length; i++)
	bounds[i] = Float.parseFloat(sBounds[i]);

String crit=cmd.getOptionValue('c', "MinAngleFace");
Constructor cons = Class.forName("org.jcae.mesh.amibe.validation."+crit).getConstructor();
QualityProcedure qproc = cons.newInstance();
if (qproc.getType() != QualityProcedure.FACE)
	throw new IllegalArgumentException("amibeReporter only accepts criterion on faces");

float scaleFactor=Float.parseFloat(cmd.getOptionValue('s', "1.0")).floatValue();

MeshTraitsBuilder mtb = qproc.getMeshTraitsBuilder();
if (cmd.hasOption('m'))
{
	if (!mtb.hasNodes())
		mtb.addNodeList();
	TriangleTraitsBuilder ttb = mtb.getTriangleTraitsBuilder();
	if (!ttb.hasHalfEdge() && !ttb.hasVirtualHalfEdge())
		ttb.addHalfEdge();
}
Mesh mesh = new Mesh(mtb);
try
{
	MeshReader.readObject3D(mesh, xmlDir);
}
catch (IOException ex)
{
	println("File "+xmlDir+File.separator+"jcae3d does not exist!");
	usage(0, options);
}
if (cmd.hasOption('m'))
{
	int nrVertices = 0;
	int nrNMVertices = 0;
	int nrEdges = 0;
	int nrNMEdges = 0;
	int nrFreeEdges = 0;
	int nrTriangles = 0;
	AbstractHalfEdge ot = null;
	for (Triangle t : mesh.getTriangles())
	{
		if (!t.isWritable())
			continue;
		ot = t.getAbstractHalfEdge(ot);
		nrTriangles++;
		for (int i = 0; i < 3; i++)
		{
			ot = ot.next();
			nrEdges++;
			if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY))
			{
				nrFreeEdges++;
				nrEdges++;
			}
			if (ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
			{
				nrNMEdges++;
			}
		}
	}
	nrEdges /= 2;

	for (Vertex v : mesh.getNodes())
	{
		if (!v.isWritable())
			continue;
		nrVertices++;
		if (!v.isManifold())
			nrNMVertices++;
	}
	System.out.println("Number of triangles: "+nrTriangles);
	System.out.println("Total number of edges: "+nrEdges);
	if (nrFreeEdges > 0)
		System.out.println("Number of free edges: "+nrFreeEdges);
	if (nrNMEdges > 0)
		System.out.println("Number of non-manifold edges: "+nrNMEdges);
	System.out.println("Total number of vertices: "+nrVertices);
	if (nrNMVertices > 0)
		System.out.println("Number of non-manifold vertices: "+nrNMVertices);
}
// Compute mesh quality
int nrFaces = 1;
int ifacemin = Integer.parseInt(cmd.getOptionValue('f', "1"))
if (detailed)
{
	TIntHashSet groups = new TIntHashSet(mesh.getTriangles().size());
	for (Triangle f: mesh.getTriangles())
	{
		if (f.isWritable())
		{
			int i = f.getGroupId() + 1 - ifacemin;
			if (i >= 0)
				groups.add(i);
		}
	}
	nrFaces = groups.size();
}
QualityFloat [] data = new QualityFloat[nrFaces];
int mean = mesh.getTriangles().size() / nrFaces;
for (int i = 0; i < data.length; i++)
{
	data[i] = new QualityFloat(mean);
	data[i].setQualityProcedure(qproc);
	data[i].setTarget(scaleFactor);
}
for (Triangle f: mesh.getTriangles())
{
	if (f.isWritable())
	{
		int i = f.getGroupId() + 1 - ifacemin;
		if (i < 0 || !detailed)
			i = 0;
		data[i].compute(f);
	}
}
for (int i = 0; i < data.length; i++)
{
	data[i].finish();
	if (detailed)
		println("Face "+(i+1));
	if (histogram)
	{
		// Prints histogram on console
		if (cmd.hasOption('b'))
			data[i].split(bounds);
		else
			data[i].split(10);
		data[i].printLayers();
	}
	else
		data[i].printStatistics();
}
if (null != outBasename)
{
	// Prints triangle quality into a .bb file to be displayed by MEDIT
	if (detailed)
	{
		int [] ids = new int[1];
		for (int i = 0; i < data.length; i++)
		{
			ids[0] = i + ifacemin - 1;
			data[i].printMeshBB(outBasename+"-"+ids[0]+".bb");
			new MeshExporter.MESH(new File(xmlDir), ids).write(outBasename+"-"+ids[0]+".mesh");
		}
	}
	else
	{
		data[0].printMeshBB(outBasename+".bb");
		new MeshExporter.MESH(xmlDir).write(outBasename+".mesh");
	}
}

