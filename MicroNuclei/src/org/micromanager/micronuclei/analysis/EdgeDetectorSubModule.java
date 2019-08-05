///////////////////////////////////////////////////////////////////////////////
//PROJECT:       MicroNuclei detection project
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nico Stuurman
//
// COPYRIGHT:    Regents of the University of California 2018
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES. 

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
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
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
   private final CLIJ clij_;
   
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
      
      clij_ = CLIJ.getInstance();
      
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
      
      ClearCLBuffer src = clij_.push(ip);
      ClearCLBuffer dst = clij_.create(src);
      clij_.op().automaticThreshold(src, dst, "Huang");
      ip = clij_.pull(dst);
      
      // IJ.setAutoThreshold(ip, "Huang dark");
      
      // Fill holes
      IJ.run(ip, "Convert to Mask", "");
      IJ.run(ip, "Options...", "iterations=1 count=1 black edm=Overwrite do=Close");
      IJ.run(ip, "Options...", "iterations=1 count=1 black edm=Overwrite do=Fill Holes");
      // Expand the edge a bit to avoid weird things next to the edge
      IJ.run(ip, "Options...", "iterations=" + (Integer) edgeNrPixelDilation_.get() + " count=1 black edm=Overwrite do=Dilate");
      
      // Run particle analysis....
      // get the largest selection, 
      
      // Now measure and store masks in ROI manager
      String analyzeParticlesParameters =  "size=" + (Double) (0.1 * imageArea) + "-" + 
               imageArea + " clear add";
      // this roiManager reset is needed since Analyze Particles will not take 
      // this action if it does not find any Rois, leading to erronous results
      roiManager.reset();
      IJ.run(ip, "Analyze Particles...", analyzeParticlesParameters);
      final Roi[] candidates = roiManager.getRoisAsArray();
      if (candidates.length >= 1) {
         ip2.setRoi(candidates[0]);
         //ip2.show();
         ResultsTable rt = new ResultsTable();
         Analyzer analyzer = new Analyzer(ip2, Analyzer.MEAN, rt);
         analyzer.measure();
         double val = rt.getValue("Mean", 0);
         // cutoff to make sure this is a real edge
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
   
   /* Wayne Rasband on ImageJ Forum
   img1 = IJ.openImage("http://wsr.imagej.net/images/blobs.gif");
   IJ.setAutoThreshold(img1, "Default");
   IJ.run(img1, "Analyze Particles...", "size=400 show=Overlay exclude");
   img2 = IJ.createImage("Untitled", "8-bit ramp", 256, 254, 1);
   overlay = img1.getOverlay();
   img2.setOverlay(overlay);
   IJ.run("Set Measurements...", "area mean centroid decimal=3");
   rt = new ResultsTable();
   measurements = Analyzer.getMeasurements();
   for (i=0; i<overlay.size(); i++) {
      roi = overlay.get(i);
      img2.setRoi(roi);
      analyzer = new Analyzer(img2, measurements, rt);
      analyzer.measure();
   }
   path = IJ.getDir("temp")+"Results.xls";
   rt.saveAs(path);
   rt2 = ResultsTable.open(path);
   //rt2.show("Results");
   */


   
}
