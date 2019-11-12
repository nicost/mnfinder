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
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.Studio;
import org.micromanager.data.Image;
import org.micromanager.micronuclei.analysisinterface.AnalysisException;
import org.micromanager.micronuclei.analysisinterface.AnalysisModule;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;
import org.micromanager.micronuclei.analysisinterface.ResultRois;

/**
 *
 * @author nico
 */
public class JustNucleiModule extends AnalysisModule {

   private final String UINAME = "Zap Some Nuclei";
   private final String DESCRIPTION
           = "<html>Simple module that finds nuclei in the first channel, <br>"
           + "and identifies a user-defined percentage of these<br>"
           + "as hits";

   private final AnalysisProperty skipWellsWithEdges_;
   private final AnalysisProperty percentageOfNuclei_;
   // private final AnalysisProperty maxStdDev_;
   // private final AnalysisProperty maxMeanIntensity_;
   private final AnalysisProperty minSizeN_;
   private final AnalysisProperty maxSizeN_;
   private final AnalysisProperty sizeFilter_;
   private final AnalysisProperty maxCirc_;
   private final AnalysisProperty minCirc_;
   private final AnalysisProperty sdFilter_;
   
   private final EdgeDetectorSubModule edgeDetector_;
   private RoiManager roiManager_;

   public JustNucleiModule() {
      // note: the type of the value when creating the AnalysisProperty determines
      // the allowed type, and can create problems when the user enters something
      // different
      
      skipWellsWithEdges_ = new AnalysisProperty(this.getClass(),
              "<html>Skip wells with edges</html",
              "Skips wells with edges when checked",
              true,
              null);
      percentageOfNuclei_ = new AnalysisProperty(this.getClass(),
              "Percentage of nuclei to be converted", null, 10.0, null);
      /*
      maxStdDev_ = new AnalysisProperty(this.getClass(),
              "Maximum Std. Dev.",
              "<html>Std. Dev. of grayscale values of original image<br>"
              + "Used to exclude images with edges</html>", 12500.0, null);
      maxMeanIntensity_ = new AnalysisProperty(this.getClass(),
              "Maximum Mean Int.",
              "<html>If the average intensity of the image is higher<br>"
              + "than this number, the image will be skipped", 20000.0, null);
      */
      minSizeN_ = new AnalysisProperty(this.getClass(),
              "<html>Minimum nuclear size (&micro;m<sup>2</sup>)</html>",
              "<html>Smallest size of putative nucleus in "
              + "&micro;m<sup>2</sup></html>", 300.0, null);
      maxSizeN_ = new AnalysisProperty(this.getClass(),
              "<html>Maximum nuclear size (&micro;m<sup>2</sup>)</html>",
              "<html>Largest size of putative nucleus in "
              + "&micro;m<sup>2</sup></html>", 1800.0, null);
      sizeFilter_ = new AnalysisProperty(this.getClass(),
              "<html>Minimum size of clustered cells (&micro;m<sup>2</sup>)</html>",
              "<html>Nuclear size threshold in "
              + "&micro;m<sup>2</sup></html><br> +"
              + "Used to filter pre-watershed mask", 1250.0, null);
      maxCirc_= new AnalysisProperty(this.getClass(),
              "<html>Maximum circ </html>",
              "<html>circ upper limit <br>"
              + "Used to filter selected nucleus", 0.90, null);
      minCirc_= new AnalysisProperty(this.getClass(),
              "<html>Minimum circ </html>",
              "<html>circ lower limit <br>"
              + "Used to filter selected nucleus", 0.55, null);
      sdFilter_= new AnalysisProperty(this.getClass(),
              "<html>sd filter </html>",
              "<html>upper limit of stdev in nucleus channel <br>"
              + "Used to filter selected nucleus", 7000.0, null);
          
      edgeDetector_ = new EdgeDetectorSubModule();

      List<AnalysisProperty> apl = new ArrayList<AnalysisProperty>();
      edgeDetector_.getAnalysisProperties().forEach((ap) -> {
         apl.add(ap);
      });
      apl.add(skipWellsWithEdges_);
      apl.add(percentageOfNuclei_);
      // apl.add(maxStdDev_);
      // apl.add(maxMeanIntensity_);
      apl.add(minSizeN_);
      apl.add(maxSizeN_);
      apl.add(sizeFilter_);
      apl.add(maxCirc_);
      apl.add(minCirc_);
      apl.add(sdFilter_);

      setAnalysisProperties(apl);

      // the ImageJ roiManager	
      roiManager_ = RoiManager.getInstance();
      if (roiManager_ == null) {
         roiManager_ = new RoiManager();
      }

   }

   @Override
   public ResultRois analyze(Studio mm, Image[] imgs, Roi userRoi, JSONObject parms) throws AnalysisException {
      Image img = imgs[0];
      ImageProcessor iProcessor = mm.data().ij().createProcessor(img);
      String posName = img.getMetadata().getPositionName("label");
      System.out.println("#-" + posName);
      
      //for BFP analysis
      Image img1 = imgs[1];
      ImageProcessor iProcessor1 = mm.data().ij().createProcessor(img1);

      Rectangle userRoiBounds = null;
      if (userRoi != null) {
         iProcessor.setRoi(userRoi);
         iProcessor = iProcessor.crop();
         userRoiBounds = userRoi.getBounds();
      }
      
      Roi restrictToThisRoi = edgeDetector_.analyze(mm, imgs); 
      
      if (restrictToThisRoi != null && ((Boolean) skipWellsWithEdges_.get()) ) {
         int pos = imgs[0].getCoords().getStagePosition();
         mm.alerts().postAlert("Skip image", JustNucleiModule.class,
                 "Edge detected at position " + pos );
         return new ResultRois(null, null, null, this.getName());
      }

      ImagePlus ip = (new ImagePlus(UINAME, iProcessor.duplicate()));
      Duplicator duppie = new Duplicator();
      ImagePlus originalIp = duppie.run(ip);
      
      //for BFP analysis
      ImagePlus ip1 = (new ImagePlus(UINAME, iProcessor1.duplicate()));
      ImagePlus originalIp1 = duppie.run(ip1);
      
      if (restrictToThisRoi != null) {
         ip.setRoi(restrictToThisRoi);
         //IJ.run("setBackgroundColor(0, 0, 0)");
         // this will set the pixels outside of the ROI to the backgroundcolor
         // The automatic thresholding will not look at these pixels 
         // (it only analyzes within the ROI)
         IJ.run(ip, "Clear Outside", "");
      }
      
      Calibration calibration = ip.getCalibration();
      calibration.pixelWidth = img.getMetadata().getPixelSizeUm();
      calibration.pixelHeight = img.getMetadata().getPixelSizeUm();
      calibration.setUnit("um");

      // check for edges by calculating stdev
      // ImageStatistics stat = ip.getStatistics(Measurements.MEAN+ Measurements.STD_DEV);
      // final double stdDev = stat.stdDev;
      // final double mean = stat.mean;
      // final double maxStdDev = (Double) maxStdDev_.get();
      // final double maxMean = (Double) maxMeanIntensity_.get();
      /*
      int pos = img.getCoords().getStagePosition();
      if (stdDev > maxStdDev) {
         mm.alerts().postAlert("Skip image", JustNucleiModule.class,
                 "Std. Dev. of image at position " + pos + " (" + 
                  NumberUtils.doubleToDisplayString(stdDev) +
                  ") is higher than the limit you set: " + maxStdDev);
         return new ResultRois(null, null, null, this.getName());
      }
      if (mean > maxMean) {
         mm.alerts().postAlert("Skip image", JustNucleiModule.class,
                 "Mean intensity of image at position " + pos + " (" + 
                  NumberUtils.doubleToDisplayString(mean) +
                  ") is higher than the limit you set: " + maxMean);
         return new ResultRois(null, null, null, this.getName());
      }
      */


      // Even though we are flatfielding, results are much better after
      // background subtraction.  In one test, I get about 2 fold more nuclei
      // when doing this background subtraction
      IJ.run(ip, "Subtract Background...", "rolling=5 sliding");
      // Pre-filter to improve nuclear detection and slightly enlarge the masks
      IJ.run(ip, "Smooth", "");
      //IJ.run(ip, "Gaussian Blur...", "sigma=5.0");

      // get the nuclear masks 
      IJ.setAutoThreshold(ip, "Otsu dark");
      // Fill holes and watershed to split large nuclei
      IJ.run(ip, "Convert to Mask", "");
      // Use this instead of erode/dilate or Close since we can pad the edges this way
      // and can still reject nuclei touching the edge (which is not true when 
      // eroding normall)
      
      // added by Xiaowei to exclude clustered cells
      IJ.run("Set Measurements...", "center area integrated redirect=None decimal=2");
      String analyzeMaskParameters =  "size=" + (Double) sizeFilter_.get() + "-Infinity" + "clear display add";
      // do not use "exclude" option since this won't exclude ROI which connects with edge
      roiManager_.reset();
      IJ.run(ip, "Analyze Particles...", analyzeMaskParameters);
      Roi[] clusterMask = roiManager_.getRoisAsArray();
      
      if (clusterMask != null) {
         for (Roi mask : clusterMask) {
            ip.setRoi(mask);
            //IJ.run("setBackgroundColor(0, 0, 0)");
            // this will set the pixels outside of the ROI to the backgroundcolor
            // The automatic thresholding will not look at these pixels 
            // (it only analyzes within the ROI)
            IJ.run(ip, "Clear", "");
         }
      }
      //added done
      
      //IJ.run(ip, "Options...", "iterations=1 count=1 black pad edm=Overwrite do=Close");
      IJ.run(ip, "Fill Holes", "");
      IJ.run(ip, "Watershed", "");

      // Now measure and store masks in ROI manager
      IJ.run("Set Measurements...", "area centroid center bounding fit shape redirect=None decimal=2");
      String analyzeParticlesParameters =  "size=" + (Double) minSizeN_.get() + "-" + 
              (Double) maxSizeN_.get() + "circularity=" + (Double) minCirc_.get() + "-" + (Double) maxCirc_.get() + " exclude clear add";
      // add circularity filter to exclude cells lost focus/ clustered cells
      // this roiManager reset is needed since Analyze Particles will not take 
      // this action if it does not find any Rois, leading to erronous results
      roiManager_.reset();
      IJ.run(ip, "Analyze Particles...", analyzeParticlesParameters);
      Roi[] selectedNuclei = roiManager_.getRoisAsArray();
      
      ResultsTable rt = new ResultsTable();
      Analyzer analyzer = new Analyzer(originalIp, Analyzer.MEAN + Analyzer.STD_DEV + Analyzer.AREA, rt);

      List<Roi> sdFilteredList = new ArrayList<>();
      
      for (Roi nuc : selectedNuclei) {
         originalIp.setRoi(nuc);
         analyzer.measure();
         // IJ.run(cellImgIp2, "Measure", "");
         int counter = rt.getCounter();
         int col = rt.getColumnIndex("Mean");
         double meanVal = rt.getValueAsDouble(col, counter - 1); //all the Area values
         int sdCol = rt.getColumnIndex("StdDev");
         double sdVal = rt.getValueAsDouble(sdCol, counter - 1);
         int sizeCol = rt.getColumnIndex("Area");
         double sizeVal = rt.getValueAsDouble(sizeCol, counter - 1); //all the Area values
         //System.out.println("GFP: " + counter + ", mean: " + meanVal + ", size: " + sizeVal + ", stdDev: " + sdVal);
         //System.out.println("counter: " + counter + meanVal + sdVal + sizeVal);
         if (sdVal < (Double) sdFilter_.get()) {
            sdFilteredList.add(nuc);
         }
      }
      
      // prepare the masks to be send to the DMD
      //Roi[] allNuclei = roiManager_.getRoisAsArray();
      Roi[] allNuclei = sdFilteredList.toArray(new Roi[sdFilteredList.size()]);

      // for BFP analysis
      ResultsTable rt0 = new ResultsTable();
      Analyzer analyzer0 = new Analyzer(originalIp, Analyzer.MEAN+ Analyzer.AREA + Analyzer.STD_DEV, rt0);
      
      for (Roi nuc : allNuclei) {
         originalIp.setRoi(nuc);
         analyzer0.measure();
         // IJ.run(cellImgIp2, "Measure", "");
         int counter0 = rt0.getCounter();
         int col0 = rt0.getColumnIndex("Mean");
         double meanVal0 = rt0.getValueAsDouble(col0, counter0 - 1); //all the Area values
         int sdCol0 = rt0.getColumnIndex("StdDev");
         double sdVal0 = rt0.getValueAsDouble(sdCol0, counter0 - 1);
         int sizeCol0 = rt0.getColumnIndex("Area");
         double sizeVal0 = rt0.getValueAsDouble(sizeCol0, counter0 - 1); //all the Area values
         System.out.println("GFP: " + counter0 + ", mean: " + meanVal0  + ", size: " + sizeVal0 + ", stdDev: " + sdVal0);
         //System.out.println("counter: " + counter1 + meanVal1+ sizeVal1);
      }
      
      ResultsTable rt1 = new ResultsTable();
      Analyzer analyzer1 = new Analyzer(originalIp1, Analyzer.MEAN+ Analyzer.AREA, rt1);
      
      for (Roi nuc : allNuclei) {
         originalIp1.setRoi(nuc);
         analyzer1.measure();
         // IJ.run(cellImgIp2, "Measure", "");
         int counter1 = rt1.getCounter();
         int col1 = rt1.getColumnIndex("Mean");
         double meanVal1 = rt1.getValueAsDouble(col1, counter1 - 1); //all the Area values
         int sizeCol1 = rt1.getColumnIndex("Area");
         double sizeVal1 = rt1.getValueAsDouble(sizeCol1, counter1 - 1); //all the Area values
         System.out.println("BFP: " + counter1 + ", mean: " + meanVal1  + ", size: " + sizeVal1);
         //System.out.println("counter: " + counter1 + meanVal1+ sizeVal1);
      }
      
      //mm.alerts().postAlert(UINAME, JustNucleiModule.class, 
      //        "Found " + allNuclei.length + " nuclei");
      List convertRoiList = new ArrayList();
      List nonConvertRoiList = new ArrayList();
      int nrNucleiToSkip = (int) (1 / ((Double) percentageOfNuclei_.get() / 100.0) );
      for (int i = 0; i < allNuclei.length; i++) {
         if (userRoiBounds != null) {
            Rectangle r2d = allNuclei[i].getBounds();
            allNuclei[i].setLocation(r2d.x + userRoiBounds.x, r2d.y + userRoiBounds.y);
         }
         if (nrNucleiToSkip ==  0 || i % nrNucleiToSkip == 0) {
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
      
      ResultRois rrs = new ResultRois(allNuclei, convertRois, nonConvertRois, 
              this.getName());
      rrs.reportOnImg(0);
      //rrs.reportOnZapChannel(0); // Pre-Zap
      //rrs.reportOnZapChannel(2);  // Post-Zap
      return rrs;
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
