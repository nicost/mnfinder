
package org.micromanager.micronuclei.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.List;
import org.micromanager.Studio;
import org.micromanager.data.Image;
import org.micromanager.micronuclei.analysisinterface.AnalysisException;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;
import org.micromanager.micronuclei.analysisinterface.AnalysisSubModule;


/**
 *
 * @author nico
 */
public class EdgeDetectorSubModule extends AnalysisSubModule {  
   private final AnalysisProperty edgeDetectionChannel_;
   
   public EdgeDetectorSubModule() {

      edgeDetectionChannel_ = new AnalysisProperty(this.getClass(), 
              "Nr. of Channel for plate edge Detection",
                      "<html>Channel used to detect edge of the plate", 1);
      List<AnalysisProperty> aps = new ArrayList<AnalysisProperty>();
      aps.add(edgeDetectionChannel_);
      super.setAnalysisProperties(aps);
   }
   
   @Override
   public Roi analyze(Studio mm, Image[] imgs) throws AnalysisException {
      Roi roi;
      
      int channelNr = (Integer) edgeDetectionChannel_.get();
      if (imgs.length < channelNr) {
         // TODO: report problem back to the user
         return null;
      }
      Image img = imgs[channelNr];
      ImageProcessor iProcessor = mm.data().ij().createProcessor(img);
      ImagePlus ip = (new ImagePlus("tmp", iProcessor)).duplicate();
      
      IJ.setAutoThreshold(ip, "Huang");
      // Fill holes and watershed to split large nuclei
      IJ.run(ip, "Convert to Mask", "");
      IJ.run(ip, "Options...", "iterations=1 count=1 black edm=Overwrite do=Close");
      IJ.run(ip, "Options...", "iterations=1 count=1 black edm=Overwrite do=Fill Holes");
      
      // Run particle analysis....
      // get the largest selection, 
      
      IJ.run (ip, "Make Inverse", "");
      roi = ip.getRoi();
        
      
      return roi;
   }


   
}
