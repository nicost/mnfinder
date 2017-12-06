
package org.micromanager.micronuclei.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
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
      Roi roi = null;
      RoiManager roiManager = RoiManager.getInstance();
      if (roiManager == null) {
         roiManager = new RoiManager();
      }
      
      int channelNr = (Integer) edgeDetectionChannel_.get();
      if (imgs.length < channelNr || channelNr < 0) {
         // TODO: report problem back to the user
         return null;
      }
      Image img = imgs[channelNr];
      ImageProcessor iProcessor = mm.data().ij().createProcessor(img);
      ImagePlus ip = (new ImagePlus("tmp", iProcessor)).duplicate();
      ImagePlus ip2 = ip.duplicate();
      Calibration calibration = ip.getCalibration();
      calibration.pixelWidth = img.getMetadata().getPixelSizeUm();
      calibration.pixelHeight = img.getMetadata().getPixelSizeUm();
      calibration.setUnit("um");
      double imageArea = ip.getWidth() * calibration.pixelWidth * 
              ip.getHeight() * calibration.pixelHeight;
      
      IJ.setAutoThreshold(ip, "Huang dark");
      // Fill holes and watershed to split large nuclei
      IJ.run(ip, "Convert to Mask", "");
      IJ.run(ip, "Options...", "iterations=1 count=1 black edm=Overwrite do=Close");
      IJ.run(ip, "Options...", "iterations=1 count=1 black edm=Overwrite do=Fill Holes");
      
      // Run particle analysis....
      // get the largest selection, 
      
      // Now measure and store masks in ROI manager
      //IJ.run("Set Measurements...", "area redirect=None decimal=2");
      String analyzeParticlesParameters =  "size=" + (Double) (0.1 * imageArea) + "-" + 
               imageArea + " clear add";
      // this roiManager reset is needed since Analyze Particles will not take 
      // this action if it does not find any Rois, leading to erronous results
      roiManager.reset();
      IJ.run(ip, "Analyze Particles...", analyzeParticlesParameters);
      final Roi[] candidates = roiManager.getRoisAsArray();
      if (candidates.length == 1) {
         ip2.setRoi(candidates[0]);
         //ip2.show();
         ResultsTable rt = Analyzer.getResultsTable();
         rt.reset();
         IJ.run("Set Measurements...", "mean redirect=None decimal=2");
         IJ.run(ip2, "Measure", "");
         rt.updateResults();
         double val = rt.getValue("Mean", 0);
         // arbitrary cutoff to make sure this is a real edge
         if (val > 20000) {
            ip.setRoi(candidates[0]);
            IJ.run (ip, "Make Inverse", "");
            roi = ip.getRoi();
            // ip.show();
         }
      }  else { // either 0 or more than 1 roi...   well, that is a conundrum...
         return null;
      }
      
      return roi;
   }


   
}
