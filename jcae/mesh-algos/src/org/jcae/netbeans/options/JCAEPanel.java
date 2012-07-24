/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jcae.netbeans.options;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.openide.explorer.propertysheet.PropertySheet;
import org.openide.modules.InstalledFileLocator;
import org.openide.nodes.*;
import org.openide.util.Exceptions;

public final class JCAEPanel extends PropertySheet {

    private final JCAEOptionsPanelController controller;

    public JCAEPanel(JCAEOptionsPanelController controller) {
        this.controller = controller;
        //initComponents();
        // TODO listen to changes in form fields and call controller.changed()
        AbstractNode node = new AbstractNode(Children.LEAF)
            {
                    @Override
                    public PropertySet[] getPropertySets() {
                            return createPropertySet();
                    }
            };
        setNodes(new Node[]{node});
        setDescriptionAreaVisible(true);
        setPreferredSize(new Dimension(0, 250));
    }

    private String maximumMemory="1000m";	
    private String customJVMParameters=new String();
    private String customMesherParameters=new String();
    private boolean runInSameJVM=Boolean.getBoolean("jcae.netbeans.mesh.samejvm");

    private String polygonOffset = "20.0f";
    private String polygonOffsetFactor = "2.0f";

    /**
     * @return the polygonOffset
     */
    public String getPolygonOffset() {
        return polygonOffset;
    }

    /**
     * @param polygonOffset the polygonOffset to set
     */
    public void setPolygonOffset(String polygonOffset) {
        this.polygonOffset = polygonOffset;
    }

    /**
     * @return the polygonOffsetFactor
     */
    public String getPolygonOffsetFactor() {
        return polygonOffsetFactor;
    }

    /**
     * @param polygonOffsetFactor the polygonOffsetFactor to set
     */
    public void setPolygonOffsetFactor(String polygonOffsetFactor) {
        this.polygonOffsetFactor = polygonOffsetFactor;
    }

	
    private class MyProperty<T> extends PropertySupport.Reflection<T>
    {
            public MyProperty(Class<T> type, String property, String name)
                    throws NoSuchMethodException
            {
                    this(type, property, name, name);
            }
            public MyProperty(Class<T> type, String property, String name, String description)
                    throws NoSuchMethodException
            {
                    super(JCAEPanel.this, type, property);
                    setName(name);
                    setShortDescription(description);
            }
    }
    
    private Sheet.Set[] createPropertySet()
    {
            Sheet.Set[] r = new Sheet.Set[2];
            r[0] = new Sheet.Set();
            r[0].setName("Mesher Settings");
            try {
                    PrefProperty p1 = new PrefProperty("jcae.MaximumMemory", "maximumMemory", maximumMemory, JCAEPanel.class);
                    r[0].put(p1);
                    
                    PrefProperty p2 = new PrefProperty("jcae.CustomJVMParameters", "customJVMParameters", customJVMParameters, JCAEPanel.class);
                    r[0].put(p2);
                    
                    PrefProperty p3 = new PrefProperty("jcae.CustomMesherParameters", "customMesherParameters", customMesherParameters, JCAEPanel.class);
                    r[0].put(p3);
                    
                    PrefProperty p4 = new PrefProperty("jcae.RunInSameJVM", "runInSameJVM", runInSameJVM, JCAEPanel.class);
                    r[0].put(p4);
                    
                    String[] cmd = getCommandLineAlgo();
                    for(int i=0; i<cmd.length; i++)
                    {
                        PrefProperty p = new PrefProperty("jcae.CommandLineAlgo"+"."+(i+1), "commandLineAlgo"+"."+(i+1), cmd[i], JCAEPanel.class);
                        r[0].put(p);
                    }
                                                     
            } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
            }
            
            r[1] = new Sheet.Set();
            r[1].setName("Viewer 3D Settings");
            try {
                    PrefProperty p1 = new PrefProperty("jcae.PolygonOffset", "polygonOffsetFactor", getPolygonOffset(), JCAEPanel.class);
                    r[1].put(p1);
                    
                    PrefProperty p2 = new PrefProperty("jcae.PolygonOffsetFactor", "polygonOffset", getPolygonOffsetFactor(), JCAEPanel.class);
                    r[1].put(p2);
 
            } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
            }
            
            return r;
    }

    public String[] getCommandLineAlgo()
    {
            String javaExe=new File(new File(System.getProperty("java.home"), "bin"), "java").getPath();
            List<String> toReturn=parameters();
            toReturn.add(0, javaExe);
            return toReturn.toArray(new String[toReturn.size()]);
            
    }

    public List<String> parameters()
    {
            ArrayList<String> toReturn=new ArrayList<String>();
            toReturn.add("-Xmx"+maximumMemory);
            String logPath = InstalledFileLocator.getDefault().
                    locate("etc/logging.properties", "org.jcae.netbeans.mesh", false).
                    getAbsolutePath();
            toReturn.add("-Djava.util.logging.config.file="+logPath);
            toReturn.addAll(Arrays.asList(getCustomJVMParameters()));
            return toReturn;
    }
        
    public String getMaximumMemory()
    {
            return maximumMemory;
    }

    public void setMaximumMemory(String maximumMemory)
    {
            this.maximumMemory = maximumMemory;
    }

    public boolean isRunInSameJVM()
    {
            return runInSameJVM;
    }
    public void setRunInSameJVM(boolean runInSameJVM)
    {
            this.runInSameJVM = runInSameJVM;
    }
    /**
        * @return Returns the customJVMParameters.
        */
    public String getCustomJVMParameters()
    {
            return customJVMParameters;
    }
    /**
        * @param customJVMParameters The customJVMParameters to set.
        */
    public void setCustomJVMParameters(String customJVMParameters)
    {
            this.customJVMParameters = customJVMParameters;
    }
    /**
        * @return Returns the customMesherParameters.
        */
    public String getCustomMesherParameters()
    {
            return customMesherParameters;
    }
    /**
        * @param customMesherParameters The customMesherParameters to set.
        */
    public void setCustomMesherParameters(String customMesherParameters)
    {
            this.customMesherParameters = customMesherParameters;
    }

}
