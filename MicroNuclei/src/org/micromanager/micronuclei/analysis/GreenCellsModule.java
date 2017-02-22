///////////////////////////////////////////////////////////////////////////////
//PROJECT:       MicroNuclei detection project
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nico Stuurman
//
// COPYRIGHT:    Regents of the University of California 2017
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
import ij.measure.ResultsTable;
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
import static org.micromanager.micronuclei.analysisinterface.AnalysisModule.CELLCOUNT;
import static org.micromanager.micronuclei.analysisinterface.AnalysisModule.OBJECTCOUNT;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;
import org.micromanager.micronuclei.analysisinterface.PropertyException;
import org.micromanager.micronuclei.analysisinterface.ResultRois;

/**
 *
 * @author nico
 */
public class GreenCellsModule extends AnalysisModule {
   private final String UINAME = "Zap Green Cells";
   private final String DESCRIPTION = 
           "<html>Simple module that finds positive cells in the 2nd channel, <br>" +
           "and identifies these as hits";

   private AnalysisProperty maxStdDev_;
   private AnalysisProperty maxMeanIntensity_;
   private AnalysisProperty minSizeN_;
   private AnalysisProperty maxSizeN_;
   private AnalysisProperty minCellSize_;
   private AnalysisProperty maxCellSize_;
   
   private RoiManager roiManager_;
   
   public GreenCellsModule() {
      try {
         // note: the type of the value when creating the AnalysisProperty determines
         // the allowed type, and can create problems when the user enters something
         // different
         maxStdDev_ = new AnalysisProperty(this.getClass(),
                 "Maximum Std. Dev. of Nuclear image", 
                 "<html>Std. Dev. of grayscale values of original image<br>" +
                          "Used to exclude images with edges</html>", 12500.0);
         maxMeanIntensity_ = new AnalysisProperty(this.getClass(),
                 "Maximum Mean Int. of Nuclear Image", 
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
         minCellSize_ = new AnalysisProperty(this.getClass(),
                  "<html>Minimum green cell size (&micro;m<sup>2</sup>)</html>", 
                 "<html>Smallest size of putative cell in " + 
                          "&micro;m<sup>2</sup></html>", 600.0);
         maxCellSize_ = new AnalysisProperty(this.getClass(),
                  "<html>Maximum green cell size (&micro;m<sup>2</sup>)</html>", 
                 "<html>Largest size of putative cell in " + 
                          "&micro;m<sup>2</sup></html>",3600.0);
         
         List<AnalysisProperty> apl = new ArrayList<AnalysisProperty>();
         apl.add(maxStdDev_);
         apl.add(maxMeanIntensity_);
         apl.add(minSizeN_);
         apl.add(maxSizeN_);
         apl.add(minCellSize_);
         apl.add(maxCellSize_);
         
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
   public ResultRois analyze(Studio mm, Image[] imgs, Roi userRoi, JSONObject parms) throws AnalysisException {
        
      if (imgs.length < 2) {
         throw new AnalysisException ("Need at least two channels to find nuclei in green cells");
      }
      boolean showMasks = false;
      try {
         showMasks = parms.getBoolean(AnalysisModule.SHOWMASKS);
      } catch (JSONException jex) { // do nothing
      }
      
      // First locate Nuclei
      ImageProcessor nuclearImgProcessor = mm.data().ij().createProcessor(imgs[0]);
      Rectangle userRoiBounds = null;
      if (userRoi != null) {
         nuclearImgProcessor.setRoi(userRoi);
         nuclearImgProcessor = nuclearImgProcessor.crop();
         userRoiBounds = userRoi.getBounds();
      }
      ImagePlus nuclearImgIp = (new ImagePlus("tmp", nuclearImgProcessor)).duplicate();
      Calibration calibration = nuclearImgIp.getCalibration();
      calibration.pixelWidth = imgs[0].getMetadata().getPixelSizeUm();
      calibration.pixelHeight = imgs[0].getMetadata().getPixelSizeUm();
      calibration.setUnit("um");

      // check for edges by calculating stdev
      ImageStatistics stat = nuclearImgIp.getStatistics(Measurements.MEAN+ Measurements.STD_DEV);
      final double stdDev = stat.stdDev;
      final double mean = stat.mean;
      final double maxStdDev = (Double) maxStdDev_.get();
      final double maxMean = (Double) maxMeanIntensity_.get();
      int pos = imgs[0].getCoords().getStagePosition();
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
      IJ.run(nuclearImgIp, "Subtract Background...", "rolling=5 sliding");
      // Pre-filter to improve nuclear detection and slightly enlarge the masks
      IJ.run(nuclearImgIp, "Smooth", "");
      IJ.run(nuclearImgIp, "Gaussian Blur...", "sigma=5.0");

      // get the nuclear masks 
      IJ.setAutoThreshold(nuclearImgIp, "Otsu dark");
      // Fill holes and watershed to split large nuclei
      IJ.run(nuclearImgIp, "Convert to Mask", "");
      // Use this instead of erode/dilate or Close since we can pad the edges this way
      // and can still reject nuclei touching the edge (which is not true when 
      // eroding normall)
      IJ.run(nuclearImgIp, "Options...", "iterations=1 count=1 black pad edm=Overwrite do=Close");
      IJ.run(nuclearImgIp, "Watershed", "");

      // Now measure and store masks in ROI manager
      IJ.run("Set Measurements...", "area centroid center bounding fit shape redirect=None decimal=2");
      String analyzeParticlesParameters =  "size=" + (Double) minSizeN_.get() + "-" + 
              (Double) maxSizeN_.get() + " exclude clear add";
      // this roiManager reset is needed since Analyze Particles will not take 
      // this action if it does not find any Rois, leading to erronous results
      roiManager_.reset();
      IJ.run(nuclearImgIp, "Analyze Particles...", analyzeParticlesParameters);

      Roi[] allNuclei = roiManager_.getRoisAsArray();
      ResultsTable allNucleiTable = roiManager_.multiMeasure(nuclearImgIp);
      if (showMasks) {
         nuclearImgIp.show();
      } else {
         nuclearImgIp.close();
      }
      
      // Now locate Cells
      ImageProcessor cellImgProcessor = mm.data().ij().createProcessor(imgs[1]);
      if (userRoi != null) {
         cellImgProcessor.setRoi(userRoi);
         cellImgProcessor = nuclearImgProcessor.crop();
         userRoiBounds = userRoi.getBounds();
      }
      ImagePlus cellImgIp = (new ImagePlus("tmp", cellImgProcessor)).duplicate();
      calibration = cellImgIp.getCalibration();
      calibration.pixelWidth = imgs[0].getMetadata().getPixelSizeUm();
      calibration.pixelHeight = imgs[0].getMetadata().getPixelSizeUm();
      calibration.setUnit("um");
     
      // Even though we are flatfielding, results are much better after
      // background subtraction.  In one test, I get about 2 fold more nuclei
      // when doing this background subtraction
      IJ.run(cellImgIp, "Subtract Background...", "rolling=5 sliding");
      // Pre-filter to improve nuclear detection and slightly enlarge the masks
      IJ.run(cellImgIp, "Smooth", "");
      IJ.run(cellImgIp, "Gaussian Blur...", "sigma=3.0");

      // get the cell masks 
      IJ.setAutoThreshold(nuclearImgIp, "Otsu dark");
      // Fill holes and watershed to split large nuclei
      IJ.run(cellImgIp, "Convert to Mask", "");
      // Use this instead of erode/dilate or Close since we can pad the edges this way
      // and can still reject nuclei touching the edge (which is not true when 
      // eroding normall)
      IJ.run(cellImgIp, "Options...", "iterations=1 count=1 black pad edm=Overwrite do=Close");
      IJ.run(cellImgIp, "Watershed", "");

      // Now measure and store masks in ROI manager
      IJ.run("Set Measurements...", "area centroid center bounding fit shape redirect=None decimal=2");
      analyzeParticlesParameters =  "size=" + (Double) minCellSize_.get() + "-" + 
              (Double) maxCellSize_.get() + " exclude clear add";
      // this roiManager reset is needed since Analyze Particles will not take 
      // this action if it does not find any Rois, leading to erronous results
      roiManager_.reset();
      IJ.run(cellImgIp, "Analyze Particles...", analyzeParticlesParameters);
      
      Roi[] allCells = roiManager_.getRoisAsArray();
      if (showMasks) {
         cellImgIp.show();
      } else {
         cellImgIp.close();
      }
 
      
      //mm.alerts().postAlert(UINAME, JustNucleiModule.class, 
      //        "Found " + allNuclei.length + " nuclei");
      
      // for each nucleus, see if it belongs to a cell.
      // if so, add it to the convert list
      List convertRoiList = new ArrayList();
      List nonConvertRoiList = new ArrayList();
      for (Roi nucleus : allNuclei) {
         boolean found = false;
         Rectangle nucRect = nucleus.getBounds();
         int xCenter = nucRect.x + (int) (0.5 * nucRect.getBounds().width);
         int yCenter = nucRect.y + (int) (0.5 * nucRect.getBounds().height);
         for (int j = 0; j < allCells.length && !found; j++) {
            if (allCells[j].contains(xCenter, yCenter)) {
               if (userRoiBounds != null) {
                  Rectangle r2d = nucleus.getBounds();
                  nucleus.setLocation(r2d.x + userRoiBounds.x, r2d.y + userRoiBounds.y);
               }
               convertRoiList.add(nucleus);
               found = true;
            }
         }
         if (!found) {
            if (userRoiBounds != null) {
               Rectangle r2d = nucleus.getBounds();
               nucleus.setLocation(r2d.x + userRoiBounds.x, r2d.y + userRoiBounds.y);
            }
            nonConvertRoiList.add(nucleus);
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
