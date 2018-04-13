
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
   private final AnalysisProperty edgeMinMean_;
   private final AnalysisProperty edgeNrPixelDilation_;
   
   public EdgeDetectorSubModule() {

      edgeDetectionChannel_ = new AnalysisProperty(this.getClass(), 
              "Channel # for plate edge Detection",
              "<html>Channel used to detect edge of the plate</html>", 
              1, 
              null);
      edgeMinMean_ = new AnalysisProperty(this.getClass(), 
              "Min. Mean Int. of edge",
              "<html>Minimum intensity to accept given area as an edge</html>", 
              20000.0, 
              null);
      edgeNrPixelDilation_ = new AnalysisProperty(this.getClass(), 
              "Expand edge with # of pixels",
              "<html>Nr of pixels to expand the edge with</html>", 
              36,
              null);
      List<AnalysisProperty> aps = new ArrayList<AnalysisProperty>();
      
      aps.add(edgeDetectionChannel_);
      aps.add(edgeMinMean_);
      aps.add(edgeNrPixelDilation_);
      super.setAnalysisProperties(aps);
      
   }
   
   @Override
   public Roi analyze(Studio mm, Image[] imgs) throws AnalysisException {
      IJ.setBackgroundColor(0,0,0); 
      IJ.setForegroundColor(255,255,255);
      Roi roi = null;
      RoiManager roiManager = RoiManager.getInstance();
      if (roiManager == null) {
         roiManager = new RoiManager();
      }
      
      // "translate" from 1-based user display to 0-based channel storage
      int channelNr = (Integer) edgeDetectionChannel_.get() - 1;
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
      // Fill holes
      IJ.run(ip, "Convert to Mask", "");
      IJ.run(ip, "Options...", "iterations=1 count=1 black edm=Overwrite do=Close");
      IJ.run(ip, "Options...", "iterations=1 count=1 black edm=Overwrite do=Fill Holes");
      // Expand the edge a bit to avoid weird things next to the edge
      IJ.run(ip, "Options...", "iterations=" + (Integer) edgeNrPixelDilation_.get() + " count=1 black edm=Overwrite do=Dilate");
      
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
         if (val > (Double) edgeMinMean_.get() ) {
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
