///////////////////////////////////////////////////////////////////////////////
//PROJECT:       MicroNuclei detection project
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nico Stuurman
//
// COPYRIGHT:    Regents of the University of California 2016
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
import ij.measure.Measurements;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.Studio;
import org.micromanager.data.Image;
import org.micromanager.internal.utils.NumberUtils;
import org.micromanager.micronuclei.analysisinterface.AnalysisException;
import org.micromanager.micronuclei.analysisinterface.AnalysisModule;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;
import org.micromanager.micronuclei.analysisinterface.PropertyException;
import org.micromanager.micronuclei.analysisinterface.ResultRois;

/**
 *
 * @author nico
 */
public class JustNucleiModule extends AnalysisModule {
   private final String UINAME = "Zap Some Nuclei";
   private final String DESCRIPTION = 
           "<html>Simple module that finds nuclei in the first channel, <br>" +
           "and identifies a user-defined percentage of these<br>" +
           "as hits";
   
   private AnalysisProperty percentageOfNuclei_;
   private AnalysisProperty maxStdDev_;
   private AnalysisProperty maxMeanIntensity_;
   private AnalysisProperty minSizeN_;
   private AnalysisProperty maxSizeN_;
   
   private RoiManager roiManager_;
   
   public JustNucleiModule() {
      try {
         // note: the type of the value when creating the AnalysisProperty determines
         // the allowed type, and can create problems when the user enters something
         // different
         percentageOfNuclei_ = new AnalysisProperty(this.getClass(),
                 "Percentage of nuclei to be converted", null, 10.0 );
         maxStdDev_ = new AnalysisProperty(this.getClass(),
                 "Maximum Std. Dev.", 
                 "<html>Std. Dev. of grayscale values of original image<br>" +
                          "Used to exclude images with edges</html>", 12500.0);
         maxMeanIntensity_ = new AnalysisProperty(this.getClass(),
                 "Maximum Mean Int.", 
                 "<html>If the average intensity of the image is higher<br>" + 
                          "than this number, the image will be skipped", 20000.0);
         minSizeN_ = new AnalysisProperty(this.getClass(),
                  "<html>Minimum nuclear size (&micro;m<sup>2</sup>)</html>", 
                 "<html>Smallest size of putative nucleus in " + 
                          "&micro;m<sup>2</sup></html>", 300.0);
         maxSizeN_ = new AnalysisProperty(this.getClass(),
                  "<html>Maximum nuclear size (&micro;m<sup>2</sup>)</html>", 
                 "<html>Largest size of putative nucleus in " + 
                          "&micro;m<sup>2</sup></html>",1800.0);
         
         List<AnalysisProperty> apl = new ArrayList<AnalysisProperty>();
         apl.add(percentageOfNuclei_);
         apl.add(maxStdDev_);
         apl.add(maxMeanIntensity_);
         apl.add(minSizeN_);
         apl.add(maxSizeN_);
         
         setAnalysisProperties(apl);
         
         // the ImageJ roiManager	
         roiManager_ = RoiManager.getInstance();
         if (roiManager_ == null) {
            roiManager_ = new RoiManager();
         }

      } catch (PropertyException ex) {
         // todo: handle error}
      }
   }
   
   @Override
   public ResultRois analyze(Studio mm, Image img, Roi userRoi, JSONObject parms) throws AnalysisException {
      ImageProcessor iProcessor = mm.data().ij().createProcessor(img);
      Rectangle userRoiBounds = null;
      if (userRoi != null) {
         iProcessor.setRoi(userRoi);
         iProcessor = iProcessor.crop();
         userRoiBounds = userRoi.getBounds();
      }
      ImagePlus ip = (new ImagePlus("tmp", iProcessor)).duplicate();
      Calibration calibration = ip.getCalibration();
      calibration.pixelWidth = img.getMetadata().getPixelSizeUm();
      calibration.pixelHeight = img.getMetadata().getPixelSizeUm();
      calibration.setUnit("um");

      // check for edges by calculating stdev
      ImageStatistics stat = ip.getStatistics(Measurements.MEAN+ Measurements.STD_DEV);
      final double stdDev = stat.stdDev;
      final double mean = stat.mean;
      final double maxStdDev = (Double) maxStdDev_.get();
      final double maxMean = (Double) maxMeanIntensity_.get();
      int pos = img.getCoords().getStagePosition();
      if (stdDev > maxStdDev) {
         mm.alerts().postAlert("Skip image", JustNucleiModule.class,
                 "Std. Dev. of image at position " + pos + " (" + 
                  NumberUtils.doubleToDisplayString(stdDev) +
                  ") is higher than the limit you set: " + maxStdDev);
         return new ResultRois(null, null, null);
      }
      if (mean > maxMean) {
         mm.alerts().postAlert("Skip image", JustNucleiModule.class,
                 "Mean intensity of image at position " + pos + " (" + 
                  NumberUtils.doubleToDisplayString(mean) +
                  ") is higher than the limit you set: " + maxMean);
         return new ResultRois(null, null, null);
      }


      // Even though we are flatfielding, results are much better after
      // background subtraction.  In one test, I get about 2 fold more nuclei
      // when doing this background subtraction
      IJ.run(ip, "Subtract Background...", "rolling=5 sliding");
      // Pre-filter to improve nuclear detection and slightly enlarge the masks
      IJ.run(ip, "Smooth", "");
      IJ.run(ip, "Gaussian Blur...", "sigma=5.0");

      // get the nuclear masks 
      IJ.setAutoThreshold(ip, "Otsu dark");
      // Fill holes and watershed to split large nuclei
      IJ.run(ip, "Convert to Mask", "");
      // Use this instead of erode/dilate or Close since we can pad the edges this way
      // and can still reject nuclei touching the edge (which is not true when 
      // eroding normall)
      IJ.run(ip, "Options...", "iterations=1 count=1 black pad edm=Overwrite do=Close");
      IJ.run(ip, "Watershed", "");

      // Now measure and store masks in ROI manager
      IJ.run("Set Measurements...", "area centroid center bounding fit shape redirect=None decimal=2");
      String analyzeParticlesParameters =  "size=" + (Double) minSizeN_.get() + "-" + 
              (Double) maxSizeN_.get() + " exclude clear add";
      // this roiManager reset is needed since Analyze Particles will not take 
      // this action if it does not find any Rois, leading to erronous results
      roiManager_.reset();
      IJ.run(ip, "Analyze Particles...", analyzeParticlesParameters);

      // prepare the masks to be send to the DMD
      Roi[] allNuclei = roiManager_.getRoisAsArray();
      //mm.alerts().postAlert(UINAME, JustNucleiModule.class, 
      //        "Found " + allNuclei.length + " nuclei");
      List convertRoiList = new ArrayList();
      List nonConvertRoiList = new ArrayList();
      int nrNucleiToSkip = (int) (1 / ((Double) percentageOfNuclei_.get() / 100.0));
      for (int i = 0; i < allNuclei.length; i++) {
         if (userRoiBounds != null) {
            Rectangle r2d = allNuclei[i].getBounds();
            allNuclei[i].setLocation(r2d.x + userRoiBounds.x, r2d.y + userRoiBounds.y);
         }
         if (i % nrNucleiToSkip == 0) {
            convertRoiList.add(allNuclei[i]);
         } else {
            nonConvertRoiList.add(allNuclei[i]);
         }
      }
      Roi[] convertRois = new Roi[convertRoiList.size()];
      convertRois = (Roi[]) convertRoiList.toArray(convertRois);
      Roi[] nonConvertRois = new Roi[nonConvertRoiList.size()];
      nonConvertRois = (Roi[]) nonConvertRoiList.toArray(nonConvertRois);
      
      try {
         parms.put(CELLCOUNT, allNuclei.length + parms.getInt(CELLCOUNT));
         parms.put(OBJECTCOUNT, convertRois.length + parms.getInt(OBJECTCOUNT));
      } catch (JSONException jse) {
         throw new AnalysisException (jse.getMessage());
      }
      
      return new ResultRois(allNuclei, convertRois, nonConvertRois);
   }

   @Override
   public void reset() {
      // Nothing todo
   }

   @Override
   public String getName() {
      return UINAME;
   }
   
   @Override
   public String getDescription() {
      return DESCRIPTION;
   }
   
}
