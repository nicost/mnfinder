
package org.micromanager.micronuclei.analysis;

import ij.gui.Roi;
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
   private AnalysisProperty edgeDetectionChannel_;
   
   public EdgeDetectorSubModule() {

         edgeDetectionChannel_ = new AnalysisProperty(this.getClass(), 
              "Channel for plate edge Detection",
                      "<html>Channel used to detect edge of the plate", "");
      List<AnalysisProperty> aps = new ArrayList<AnalysisProperty>();
      super.setAnalysisProperties(aps);
   }
   
   @Override
   public Roi analyze(Studio studio, Image[] imgs) throws AnalysisException {
      Roi roi = null;
      
      
      return roi;
   }


   
}
