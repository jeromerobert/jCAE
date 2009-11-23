import org.jcae.mesh.MeshOEMMIndex
import org.jcae.mesh.oemm.OEMM
import org.jcae.mesh.oemm.MeshReader
import org.jcae.mesh.oemm.Storage
import org.jcae.mesh.amibe.ds.Mesh
import org.jcae.mesh.xmldata.MeshWriter

import org.apache.commons.cli.*

 cmd=["index    ", "Convert a triangle soup into an Amibe mesh"]
usage="<xmlDir> <outputDir>"

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
CommandLineParser parser = new GnuParser();
CommandLine cmd = parser.parse(options, args, true);
if (cmd.hasOption('h'))
	usage(0, options);

String [] remaining = cmd.getArgs();
if (remaining.length != 2)
	usage(1, options);

String xmlDir = remaining[0]

File tempFile = File.createTempFile("oemm", "dir")
//File tempFile = File.createTempFile("oemm", "dir", new File("."))
String tmpdir = tempFile.getAbsolutePath()
tempFile.delete()
File tempDir = new File(tmpdir)
tempDir.mkdir();
String [] mainArgs = new String[4]
mainArgs[0] = remaining[0]
mainArgs[1] = tmpdir
mainArgs[2] = "4"
mainArgs[3] = "50000"

MeshOEMMIndex.main(mainArgs)
OEMM oemm = Storage.readOEMMStructure(mainArgs[1]);
MeshReader mr = new MeshReader(oemm);
Mesh mesh = mr.buildWholeMesh()
MeshWriter.writeObject3D(mesh, remaining[1], null)

