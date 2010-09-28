package org.jcae.vtk;

import gnu.trove.THashMap;
import gnu.trove.TIntArrayList;
import org.jcae.mesh.xmldata.AmibeWriter;
import vtk.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * Converts a VTP mesh to an Amibe mesh.
 * <ul>
 * </ul>
 * @author Ganesh Patil
 */
public class VTP2Amibe
{
    static
    {
        //Load prerequisites : VTK Libraries
        System.loadLibrary("vtkIOJava");
        System.loadLibrary("vtkFilteringJava");
        System.loadLibrary("vtkCommonJava");
    }

    private final static Logger LOGGER = Logger.getLogger(VTP2Amibe.class.getName());
    //Counter to create Amibe Dim2 objects
    private int dim2Count = 0;

    /**
     *
     * Reads XML poly data from VTP files
     * @param inFile
     * @param outDir
     * @throws IOException
     */
    public void convert(String inFile, String outDir) throws IOException
    {
        LOGGER.log(Level.INFO, "Reading {0}", inFile);
        //Read Poly Data
        vtkXMLPolyDataReader polyDataFile = new vtkXMLPolyDataReader();
        polyDataFile.SetFileName(inFile);
        polyDataFile.Update();
        convert(polyDataFile, outDir);
    }

    /**
     *
     * Reads poly data from XML poly data
     * @param polyDataFile
     * @param outDir
     * @throws IOException
     */
    private void convert(vtkXMLPolyDataReader polyDataFile, String outDir) throws IOException
    {
        //Read and process Poly Data
        vtkPolyData plyData;
        plyData = polyDataFile.GetOutput();
        convert(plyData, outDir);
    }

    /**
     *
     * Reads Node, Beam, Triangle, Group data from poly data
     * Writes Amibe files
     * @param plyData
     * @param outDir
     * @throws IOException
     */
    private void convert(vtkPolyData plyData, String outDir) throws IOException
    {
        // Amibe writer object for 3D elements
        AmibeWriter.Dim3 out = new AmibeWriter.Dim3(outDir);

        // AmibeWriter object for 2D elements
        AmibeWriter.Dim2 out2 = new AmibeWriter.Dim2(outDir, dim2Count++);

        //Read Point Data
        convertPointData(plyData, out, out2);

        //Read Line Data
        convertLineData(plyData, out2);

        //Read Triangle Data
        convertTriangleData(plyData, out);

        //Read group data
        convertGroupData(plyData, out, out2);

        //Write Amibe files
        out2.finish();
        out.finish();
    }

    /**
     *
     * Writes Node data in Amibe
     * @param plyData
     * @param out
     * @param out2
     * @throws IOException
     */
    private void convertPointData(vtkPolyData plyData, AmibeWriter.Dim3 out, AmibeWriter.Dim2 out2) throws IOException
    {
        for(int i =0; i < plyData.GetNumberOfPoints(); i++ )
        {
            out.addNode(plyData.GetPoint(i));
            out2.addNode(plyData.GetPoint(i));
        }
    }

    /**
     *
     * Writes edge/beam (2d elements) data in Amibe
     * @param plyData
     * @param out2
     * @throws IOException
     */
    private void convertLineData(vtkPolyData plyData, AmibeWriter.Dim2 out2) throws IOException
    {
        vtkCellArray cellArray  = plyData.GetLines();
        vtkIdTypeArray lineIdArray = cellArray.GetData();
        for(int i = 0; i < cellArray.GetSize(); i=i+2)
        {
            out2.addBeam((int)lineIdArray.GetComponent(i+1,0),(int) lineIdArray.GetComponent(i+2,0));
        }
    }

    /**
     *
     * Writes triangle data in to Amibe
     * @param plyData
     * @param out
     * @throws IOException
     */
    private void convertTriangleData(vtkPolyData plyData, AmibeWriter.Dim3 out) throws IOException
    {
        vtkCellArray polyCellArray = plyData.GetPolys();
        vtkIdTypeArray polyIdArray = polyCellArray.GetData();
        for(int j = 0; j < (polyCellArray.GetSize()-3);j+=4)
        {
            out.addTriangle((int)polyIdArray.GetComponent(j+1,0),(int)polyIdArray.GetComponent(j+2,0),(int)polyIdArray.GetComponent(j+3,0));
        }
    }

    /**
     * Reads group data info from VTP
     * @param plyData
     * @param out
     * @param out2
     * @throws IOException
     */
    private void convertGroupData(vtkPolyData plyData, AmibeWriter.Dim3 out, AmibeWriter.Dim2 out2) throws IOException
    {
        //Read Group Data i.e. Cell Data
        vtkCellData groupCellData = plyData.GetCellData();
        for(int i =0; i < groupCellData.GetNumberOfArrays(); i++ )
        {
            vtkDataArray dtArray = groupCellData.GetArray(i);
            String arrayName = dtArray.GetName();
            if(arrayName.equalsIgnoreCase("groups"))            //we know that the group name in VTP will always be "groups"
            {
                //create list of group names using tuples
                ArrayList<String>  groupNameList = new ArrayList<String>();
                createGroupNameArray(groupNameList, dtArray);

                //Check the Tuple ids and assign groups to them.
                convertGroups(groupNameList, dtArray, plyData, out , out2);
            }
        }
    }

    /**
     * Creates array of group names.
     * as VTP does not provide these names ; they are created using tuple values.
     * @param groupNameList
     * @param dtArray
     */

    private void createGroupNameArray(ArrayList<String> groupNameList, vtkDataArray dtArray)
    {
        //create group names to fill in Amibe group information.
        for(int k = 0;  k < dtArray.GetNumberOfTuples(); k++)
        {
            int id = (int)dtArray.GetTuple1(k);
            String groupName = "";

            if(id < 10)    groupName = "group0"+ String.valueOf(id);          //this is solution for naming convention.If no.of groups > 99 then need to modify this logic.
            else    groupName = "group"+ String.valueOf(id);

            for(int l = 0; l <= groupNameList.size(); l++)
            {
                //If the group name does not exists in the groupNameList ; add it.
                if(!groupNameList.contains(groupName))
                    groupNameList.add(groupName);
            }
        }
    }

    /**
     * Write group data in Amibe
     * @param groupNameList
     * @param dtArray
     * @param plyData
     * @param out
     * @param out2
     * @throws IOException
     */
    private void convertGroups(ArrayList<String> groupNameList, vtkDataArray dtArray,  vtkPolyData plyData, AmibeWriter.Dim3 out, AmibeWriter.Dim2 out2) throws IOException
    {
        //create array of group identifiers which is always of type int.
        int groupIdentifiers [] = new int[groupNameList.size()];
        for(int i = 0; i < groupNameList.size(); i++)
        {
            String groupName = groupNameList.get(i) ;
            String idVal = groupName.substring(5);
            int _idVal = Integer.valueOf(idVal);
            groupIdentifiers[i] = _idVal;           //as the group names are created using group indices from VTP ; they can be used as identifiers.
        }

        //Using group identifiers ; find out indices of triangles
        // in a group and create separate array for each group.

        for(int i = 0; i < groupIdentifiers.length; i++)
        {
            TIntArrayList idVals = new TIntArrayList();
            for(int j = 0;  j < dtArray.GetNumberOfTuples(); j++)
            {
                if(groupIdentifiers[i] == (int)dtArray.GetTuple1(j))
                    idVals.add(j);
            }
        }

        THashMap groupData = new THashMap();
        for(int i = 0; i < groupIdentifiers.length; i++)
        {
            TIntArrayList idValues = new TIntArrayList();
            for(int j = 0;  j < dtArray.GetNumberOfTuples(); j++)
            {
                if(groupIdentifiers[i] == (int)dtArray.GetTuple1(j))
                {
                    idValues.add(j);
                }
            }
            groupData.put(i, idValues);
        }

        // create groups in amibe using groupNameList and
        // fill the indices of triangles using groupData
        for(int i = 0; i < groupNameList.size(); i++)
        {
            out.nextGroup(groupNameList.get(i));
            out2.nextGroup(groupNameList.get(i));

            Object obj = groupData.get(i);
            TIntArrayList indices = (TIntArrayList)obj;
            for(int k = 0; k < indices.size(); k++)
            {
                int id = indices.get(k);
                if(5 == plyData.GetCellType(id))   //5 = VTK_TRIANGLE
                {
                    out.addTriaToGroup(id);
                }else
                if(3 == plyData.GetCellType(id))    //3 = VTK_LINE
                {
                    out.addTriaToGroup(id);
                }
            }
        }
    }

    /**
     *
     * @param args
     * @throws IOException
     */
    public static void main (String [] args) throws IOException
    {
        try
        {
            VTP2Amibe vtpAmibe = new VTP2Amibe();
            String inFile = "";
            String outDir = "C:\\Home\\GAPATIL857B\\Amibe";
            if (args.length > 0)
               inFile = args[0];
            if (args.length > 1)
               outDir = args[1];

            vtpAmibe.convert(inFile, outDir);
        }catch (IOException e)
        {
             LOGGER.log(Level.SEVERE, null, e);
        }
    }
}
