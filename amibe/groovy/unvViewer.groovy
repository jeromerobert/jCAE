/**
 * Run a viewer to display an I-Deas UNV mesh.
 */
import java.io.File;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.jcae.viewer3d.fe.unv.UNVProvider;
import org.jcae.viewer3d.fe.ViewableFE;
import org.jcae.viewer3d.View;

String unvFile = args[0];

JFrame feFrame=new JFrame("jCAE Demo");
feFrame.setSize(800,600);
feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

View bgView=new View(feFrame);
UNVProvider ap = new UNVProvider(new File(unvFile));
bgView.add(new ViewableFE(ap));
bgView.fitAll();
feFrame.getContentPane().add(bgView);
feFrame.setVisible(true);
bgView.setOriginAxisVisible(true);

