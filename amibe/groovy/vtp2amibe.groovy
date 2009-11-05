/**
 * Convert a .vtp file into amibe format.
 */
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.xmldata.MeshWriter;

import org.jcae.vtk.Utils;

import vtk.vtkXMLPolyDataReader as VtkXMLPolyDataReader;
import vtk.vtkPolyData as VtkPolyData;

import org.apache.commons.cli.*;

void usage(int rc, Options options)
{
	HelpFormatter formatter = new HelpFormatter();
	formatter.printHelp("groovy vtp2amibe.groovy [options] vtpFile", options);
	System.exit(rc);
}

Options options = new Options();
options.addOption(
	OptionBuilder.hasArg(false)
		.withDescription("usage information")
		.withLongOpt("help")
		.create('h'));
options.addOption(
	OptionBuilder.withArgName("PATH").hasArg()
		.withDescription("output directory (default is current directory)")
		.withLongOpt("outputdir")
		.create('o'));
CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);

String [] remaining = cmd.getArgs();
if (remaining.length != 1)
	usage(1, options);

String vtpFile = remaining[0];
String outDir = cmd.getOptionValue('o', '.');
Mesh mesh = new Mesh(new MeshTraitsBuilder());

Utils.loadVTKLibraries();
VtkXMLPolyDataReader reader = new VtkXMLPolyDataReader();
reader.SetFileName(vtpFile);
reader.Update();
 
VtkPolyData polydata = reader.GetOutput();

Vertex [] vertices = new Vertex[polydata.GetNumberOfPoints()];
double [] coord = new double[3];
for (int i = 0; i < vertices.length; i++)
{
	polydata.GetPoint(i, coord);
	vertices[i] = mesh.createVertex(coord);
}
 

int [] indices = Utils.getValues(polydata.GetPolys());
for (int i = 0; i < indices.length; i++)
{
	if (indices[i] == 3)
		mesh.add(mesh.createTriangle(
			vertices[indices[i+1]],
			vertices[indices[i+2]],
			vertices[indices[i+3]]));
	i += indices[i];
}

MeshWriter.writeObject3D(mesh, outDir, null);
