///////////////////////////////////////////////////////////////////////////////
//PROJECT:       MicroNuclei detection project
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nico Stuurman
//
// COPYRIGHT:    Regents of the University of California 2015
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

import org.micromanager.micronuclei.utilities.Distance;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import org.micromanager.Studio;
import org.micromanager.data.Image;
import org.micromanager.internal.utils.NumberUtils;

import org.micromanager.micronuclei.analysisinterface.AnalysisModule;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;
import org.micromanager.micronuclei.analysisinterface.PropertyException;
import org.micromanager.micronuclei.analysisinterface.AnalysisException;
import org.micromanager.micronuclei.analysisinterface.ResultRois;



/**
 * Actual micro-nuclei detection code 
 * 
 * @author nico
 */
public class MicroNucleiAnalysisModule extends AnalysisModule {
   private int nucleiCount_ = 0;
   private int zappedNucleiCount_ = 0;
   AnalysisProperty minSizeMN_, maxSizeMN_, minSizeN_, maxSizeN_,
           maxDistance_, minNMNPerNucleus_, maxStdDev_, maxNumberOfNuclei_,
           maxNumberOfZaps_, checkInSmallerImage_, minEdgeDistance_,
           maxMeanIntensity_; 
   private final String UINAME = "MicroNucleiAnalysis";
      private final String DESCRIPTION = 
           "<html>Identifies cells with micro-nuclei by comparing<br>" +
           "two masks, one obtained after smoothing the image, <br>" +
           "the other after sharpening the image.";
   
   
   
   public MicroNucleiAnalysisModule()  {
      try {
         // note: the type of the value when creating the AnalysisProperty determines
         // the allowed type, and can create problems when the user enters something
         // different
         maxStdDev_ = new AnalysisProperty(this.getClass(),
                 "Maximum Std. Dev.", 
                 "<html>Std. Dev. of grayscale values of original image<br>" +
                          "Used to exclude images with edges</html>", 12500.0);
         maxMeanIntensity_ = new AnalysisProperty(this.getClass(),
                 "Maximum Mean Int.", 
                 "<html>If the average intensity of the image is higher<br>" + 
                          "than this number, the image will be skipped", 20000.0);
         minSizeMN_ = new AnalysisProperty(this.getClass(),
                 "<html>Minimum micronuclear size (&micro;m<sup>2</sup>)</html>", 
                 null, 20.0 );
         maxSizeMN_ = new AnalysisProperty(this.getClass(),
                 "<html>Maximum micronuclear size (&micro;m<sup>2</sup>)</html>", 
                 null, 800.0 );
         minSizeN_ = new AnalysisProperty(this.getClass(),
                  "<html>Minimum nuclear size (&micro;m<sup>2</sup>)</html>", null, 80.0);
         maxSizeN_ = new AnalysisProperty(this.getClass(),
                  "<html>Maximum nuclear size (&micro;m<sup>2</sup>)</html>", null, 800.0);
         maxDistance_ = new AnalysisProperty(this.getClass(),
                  "<html>Maximum distance (&micro;m)</html>", 
                 " <html>Maximum distance (&micro;m)</html> between center of micronucleu and center of nucleus" , 25.0);
         minNMNPerNucleus_ = new AnalysisProperty(this.getClass(),
                  "Minimum number of micronuclei", 
                 "Minimum number of micronuclei per \"nucleus\"", 3);
         minEdgeDistance_ = new AnalysisProperty(this.getClass(),
                  "<html>Minimum distance from the edge (&micro;m<sup>2</sup>)</html>", 
                 null, 10.0);
         maxNumberOfNuclei_ = new AnalysisProperty(this.getClass(), 
                 "Maximum number of nuclei per image", 
                 "Do not include images that have more than this number of nuclei", 250);
         maxNumberOfZaps_ = new AnalysisProperty(this.getClass(),
                 "Skip image if more than this number should be zapped", null, 15);
         checkInSmallerImage_ = new AnalysisProperty(this.getClass(), 
                  "Check again in subregion", "When checked, will re-analyze the image, only looking \n" +
                          "at a local, small area.  This can improve accuracy at the cost of speed", true);
         List<AnalysisProperty> apl = new ArrayList<AnalysisProperty>();
         apl.add(maxStdDev_);
         apl.add(maxMeanIntensity_);
         apl.add(minSizeMN_);
         apl.add(maxSizeMN_);
         apl.add(minSizeN_);
         apl.add(maxSizeN_);
         apl.add(minNMNPerNucleus_);
         apl.add(maxDistance_);
         apl.add(maxNumberOfNuclei_);
         apl.add(maxNumberOfZaps_);
         apl.add(checkInSmallerImage_);
         apl.add(minEdgeDistance_);
         
         setAnalysisProperties(apl);
      } catch (PropertyException ex) {
         // todo: handle error}
      }
   }
   
   /**
    * Simple class just so that we can get information back out of a method
    */
   class MutableInt {
      private int val_;
      MutableInt(int val) {
         val_ = val;
      }
      void set(int newVal){
         val_ = newVal;
      }
      int get() { return val_; }
   }
  
   @Override
   public ResultRois analyze(Studio studio, Image[] imgs, Roi userRoi, JSONObject parms) throws AnalysisException {
      Image image = imgs[0];
      nucleiCount_ = parms.optInt(CELLCOUNT, 0);
      zappedNucleiCount_ = parms.optInt(OBJECTCOUNT, 0);

      long startTime = System.currentTimeMillis();
      
      ImagePlus imp = new ImagePlus ("tmp", studio.data().ij().createProcessor(image));
      Calibration cal = imp.getCalibration();
      cal.pixelWidth = image.getMetadata().getPixelSizeUm();
      cal.pixelHeight = image.getMetadata().getPixelSizeUm();

      // remove images that have the well edge in them
      ImageStatistics statistics = imp.getStatistics(Measurements.MEAN+ Measurements.STD_DEV);
      double stdDev = statistics.stdDev;
      double mean = statistics.mean;
      // do not analyze images whose stdev or mean is higher than user-specified value
      // Use these criteria to remove images showing well edges
      final double maxStdDev = (Double) maxStdDev_.get();
      final double maxMean = (Double) maxMeanIntensity_.get();
      int pos = image.getCoords().getStagePosition();
      if (stdDev > maxStdDev) {
         studio.alerts().postAlert("Skip image", MicroNucleiAnalysisModule.class,
                 "Std. Dev. of image at position " + pos + " (" + 
                  NumberUtils.doubleToDisplayString(stdDev) +
                  ") is higher than the limit you set: " + maxStdDev);
         return null;
      }

      if (mean > maxMean) {
         studio.alerts().postAlert("Skip image", MicroNucleiAnalysisModule.class,
                 "Mean intenisty of image at position " + pos + " (" + 
                  NumberUtils.doubleToDisplayString(mean) +
                  ") is higher than the limit you set: " + maxMean);
         return null;
      }

      MutableInt nrNuclei = new MutableInt(0);
      
      Roi[] hits = analyzeImagePlus(imp, cal, parms, nrNuclei);
      nucleiCount_ += nrNuclei.get();

      
      if ( (Boolean) checkInSmallerImage_.get() ) {
         ArrayList<Roi> cleanedHits = new ArrayList<Roi>();
         // Check all our hits by taking a subregion of the original image 
         // and re-running the analysis
         ij.IJ.log("Running sub-analysis");
         for (Roi roi : hits) {
            ImagePlus region = getRegion (imp, roi, 200);
            Roi[] newHits = analyzeImagePlus(region, cal, parms, nrNuclei);
            if (newHits.length > 0)
               cleanedHits.add(roi);
         }
         hits = new Roi[cleanedHits.size()];
         hits = cleanedHits.toArray(hits);
      }
      
      
      zappedNucleiCount_ += hits.length;
      try {
         parms.put(CELLCOUNT, nucleiCount_);
         parms.put(OBJECTCOUNT, zappedNucleiCount_);
      } catch (JSONException ex) {
         ij.IJ.log("MicroNucleiAnalysis.java: This should never happen!!!");
      }
      
      
      long endTime = System.currentTimeMillis();
      ij.IJ.log("Analysis took: " + (endTime - startTime) + " millisec");
            
      ResultRois rrs = new ResultRois(null, hits, null);
      rrs.reportOnImg(1);
      rrs.reportOnZapChannel(0); // Pre-Zap
      rrs.reportOnZapChannel(2);  // Post-Zap
      return rrs;
   }
   
   
   /**
    * 
    * @param imp
    * @param calibration
    * @param parms
    * @param nrNuclei
    * @return 
    */
   private Roi[] analyzeImagePlus(ImagePlus imp, Calibration calibration, JSONObject parms,
           MutableInt nrNuclei) {
      
      boolean showMasks = false;
      try {
         showMasks = parms.getBoolean(AnalysisModule.SHOWMASKS);
      } catch (JSONException jex) { // do nothing
      }
      
      // microNuclei allowed sizes
      final double microNucleiMinSize = (Double) minSizeMN_.get();
      final double microNucleiMaxSize = (Double) maxSizeMN_.get();
      // nuclei allowed sized
      final double nucleiMinSize = (Double) minSizeN_.get();
      final double nucleiMaxSize = (Double) maxSizeN_.get();
      // max distance a micronucleus can be separated from a nucleus
      final double maxDistance = (Double) maxDistance_.get();
      // min distance a micronucleus should be from the edge of the image
      final double minEdgeDistance = (Double) minEdgeDistance_.get(); // in microns
      // minimum number of "micronuclei" we want per nucleus to score as a hit
      final int minNumMNperNucleus = (Integer) minNMNPerNucleus_.get();

      // if the image has more than this number of nuclei, do not zap
      final int maxNumberOfNuclei = (Integer) maxNumberOfNuclei_.get();
      // if more than this number of nuclei should be zapped, skip zapping altogether
      final int maxNumberOfZaps = (Integer) maxNumberOfZaps_.get();
      
      // start of the main code
      List<Point2D.Double> microNuclei = new ArrayList<Point2D.Double>();
      Map<Point2D.Double, Roi> microNucleiROIs = new HashMap<Point2D.Double, Roi>();
      Map<Point2D.Double, ArrayList<Point2D.Double> > nuclei = 
              new HashMap<Point2D.Double, ArrayList<Point2D.Double> >();
      //nucleiContents = new ArrayList();
      Map<Point2D.Double, Roi> nucleiRois = new HashMap<Point2D.Double, Roi>();
      Map<Point2D.Double, Double> nucleiSizes = new HashMap<Point2D.Double, Double>();
      List<Point2D.Double> zapNuclei = new ArrayList<Point2D.Double>();

      // clean results table	
      ResultsTable res = ij.measure.ResultsTable.getResultsTable();
      res.reset();

      ImagePlus imp2 = (new Duplicator()).run(imp, 1, 1);

      // find micronuclei by sharpening, segmentation using Otsu, and Watershed
      ImagePlus microNucleiImp = imp2.duplicate();
      IJ.run(microNucleiImp, "16-bit", "");
      IJ.run(microNucleiImp, "Sharpen", "");
      IJ.setAutoThreshold(microNucleiImp, "Otsu dark");
      ij.Prefs.blackBackground = true;
      IJ.run(microNucleiImp, "Convert to Mask", "");
      IJ.run(microNucleiImp, "Close-", "");
      IJ.run(microNucleiImp, "Watershed", "");
      IJ.run("Set Measurements...", "area center decimal=2");
      // this roiManager reset is needed since Analyze Particles will not take 
      // this action if it does not find any Rois, leading to erronous results 
      RoiManager rm = RoiManager.getInstance2();
      if (rm == null) {
         rm = new RoiManager();
      }
      rm.reset();
      IJ.run(microNucleiImp, "Analyze Particles...", "size=" + 
              (Double) microNucleiMinSize + "-" + (Double) microNucleiMaxSize
              + "  exclude  clear add");

      // Build up a list of potential micronuclei

      for (Roi roi  : rm.getRoisAsArray()) {
         // approximate microNuclear positions as the center of the bounding box
         Rectangle rc = roi.getBounds();
         double xc = rc.x + 0.5 * rc.width;
         double yc = rc.y + 0.5 * rc.height;
         xc *= calibration.getX(1.0);
         yc *= calibration.getX(1.0);
         Point2D.Double pt = new java.awt.geom.Point2D.Double(xc, yc);
         microNuclei.add(pt);
         microNucleiROIs.put(pt, roi);
      }

      // find nuclei by smoothing and gaussian filtering, 
      // followed by Otsu segmentation and watershed
      ImagePlus nucleiImp = imp2.duplicate();
      ResultsTable rt = Analyzer.getResultsTable();
      IJ.run(nucleiImp, "Smooth", "");
      IJ.run(nucleiImp, "Gaussian Blur...", "sigma=5.0");
      IJ.setAutoThreshold(nucleiImp, "Otsu dark");
      ij.Prefs.blackBackground = true;
      IJ.run(nucleiImp, "Convert to Mask", "");      
      // Use this instead of erode/dilate or Close since we can pad the edges this way
      // and can still reject nuclei touching the edge (which is not true when 
      // eroding normall)
      IJ.run(nucleiImp, "Options...", "iterations=1 count=1 black pad edm=Overwrite do=Close");
      IJ.run(nucleiImp, "Watershed", "");
      IJ.run("Set Measurements...", "area center decimal=2");

      // this roiManager reset is needed since Analyze Particles will not take 
      // this action if it does not find any Rois, leading to erronous results 
      rm.reset();
      rt.reset();
      // include large nuclei here so that we will assign the corresponding microNuclei 
      // correctly.  Weed these out later
      IJ.run(nucleiImp, "Analyze Particles...", "size=" + (Double) nucleiMinSize + "-" +
              (Double) nucleiMaxSize + " exclude clear add");
      rt.updateResults();

      // get nuclei from RoiManager and add to our list of nuclei:
      rm = RoiManager.getInstance2();
      int counter = 0;
      Roi[] roiManagerRois = rm.getRoisAsArray();
      if (roiManagerRois.length == rt.getCounter()) {
         for (Roi roi : roiManagerRois) {
            // approximate nuclear positions as the center of the bounding box
            Rectangle rc = roi.getBounds();
            double xc = rc.x + 0.5 * rc.width;
            double yc = rc.y + 0.5 * rc.height;
            xc *= calibration.getX(1.0);
            yc *= calibration.getX(1.0);
            Point2D.Double pt = new java.awt.geom.Point2D.Double(xc, yc);
            nucleiRois.put(pt, roi);
            ArrayList<Point2D.Double> containedMNs = new ArrayList<Point2D.Double>();
            nuclei.put(pt, containedMNs);
            nucleiSizes.put(pt, rt.getValue("Area", counter));
            counter++;
         }
      } else {
         ij.IJ.log("Number of Rois does not equal number of Particles in results table");
      }

      // either close or show the nuclear mask as desired
      nucleiImp.changes = false;
      if (showMasks) {
         nucleiImp.show();
      } else {
         nucleiImp.close();
      }

      // either close of show the "micro-nuclear" mask as desired
      microNucleiImp.changes = false;
      if (showMasks) {
         microNucleiImp.show();
      } else {
         microNucleiImp.close();
      }

      imp2.changes = false;
      imp2.close();

      // cycle through the list of micronuclei
      // assign each to the nearest by nucleus (not more than maxdistance away)
      for (Point2D.Double mn  : microNuclei) {
         Point2D.Double cn = Distance.closest(mn, nuclei);
         if (cn != null && maxDistance > Distance.distance(mn, cn)) {
            nuclei.get(cn).add(mn);
         }
      }

      // report what we found
      res.reset();

      // this is a bit funky, but seems to work
      double roiMinSize = calibration.getX(1.0) * calibration.getX(1.0) * nucleiMinSize * 10;
      for (Point2D.Double p  : nuclei.keySet()) {
         res.incrementCounter();
         res.addValue("X", p.x);
         res.addValue("Y", p.y);
         ArrayList<Point2D.Double> mnList = nuclei.get(p);
         res.addValue("# mN", mnList.size());
         int zapit = 0;
         if (nuclei.get(p).size() >= minNumMNperNucleus) {
            double nSize = nucleiSizes.get(p);
            // make sure that this nucleus is not too large
            if (nSize < nucleiMaxSize) {
            // add to our target nuclei, except if these happen to be two nuclei that were 
               // lying close together. 
               if (mnList.size() == 2) {
                  Roi r0 = microNucleiROIs.get(mnList.get(0));
                  Roi r1 = microNucleiROIs.get(mnList.get(1));
                  if ((r0 != null && roiSize(r0) < roiMinSize)
                          || (r1 != null && roiSize(r1) < roiMinSize)) {
                     zapNuclei.add(p);
                     zapit = 1;
                  }
               } else {
                  zapNuclei.add(p);
                  zapit = 1;
               }
            }
         }
         res.addValue("Zap", zapit);
      }

      res.show("Results");
     

      // get a list with rois that we want to zap
      ArrayList<Roi> zapRois = new ArrayList<Roi>();
      for (Point2D.Double p  : zapNuclei) {
         Roi roi = nucleiRois.get(p);
         zapRois.add(roi);
      }

      ij.IJ.log("mn: " + microNuclei.size() + ", n: " + nuclei.size() + 
                 ", zap: " + zapRois.size());
      
      // make sure that we do not zap if there are too many nuclei in the image
      if (nuclei.size() > maxNumberOfNuclei) {
         zapRois.clear();
         ij.IJ.log("Not zapping cells since there are too many nuclei per image");
         
      }
      // make sure that we do not zap if there are too many cells to be zapped
      if (zapRois.size() > maxNumberOfZaps) {
         zapRois.clear();
         ij.IJ.log("Not zapping cells since there are too many cells to be zapped");
      }
      
      nrNuclei.set(nuclei.size());
      
      return zapRois.toArray(new Roi[zapRois.size()]);
   }

        
   /**
    * Calculate the size of an ImageJ ROI
    */
   private long roiSize(Roi r) {
      return r.getBounds().width * r.getBounds().height;
   }
   
   /**
    * Returns the center of the given Roi
    */
   private Point getCenter(Roi roi)
   {
      Point center = new Point();
      center.x = roi.getBounds().x + (int) (0.5 * roi.getBounds().width);
      center.y = roi.getBounds().y + (int) (0.5 * roi.getBounds().height);
      return center;
   }
   
   /**
    * Returns an ImagePlus of the region around the Roi
    * @param imp
    * @param roi
    * @return 
    */
   private ImagePlus getRegion (ImagePlus imp, Roi roi, int size) 
   {
      int halfsize = (int) (0.5 * size);
      Point center = getCenter(roi);
      int x = center.x - halfsize;
      if (x < 0)
         x = 0;
      int y = center.y - halfsize;
      if (y < 0)
         y = 0;
      if (x + size > imp.getWidth())
         x = imp.getWidth() - size;
      if (y+ size > imp.getHeight())
         y = imp.getHeight() - size;
      
      imp.setRoi(x, y, size, size);
      
      return imp.duplicate();
   }

   @Override
   public void reset() {
      nucleiCount_ = 0;
      zappedNucleiCount_ = 0; 
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
