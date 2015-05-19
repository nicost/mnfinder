package org.micromanager.micronuclei.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.frame.RoiManager;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mmcorej.TaggedImage;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.MMStudio;
import org.micromanager.api.ScriptInterface;
import org.micromanager.micronuclei.AnalysisModule;
import org.micromanager.utils.ImageUtils;
import org.micromanager.utils.MMScriptException;

/**
 *
 * @author nico
 */


public class MicroNucleiAnalysisModule implements AnalysisModule {
   private int nucleiCount_ = 0;
   private int zappedNucleiCount_ = 0;

   @Override
   public Roi[] analyze(TaggedImage tImg, JSONObject parms) throws MMScriptException {
      
      boolean showMasks = parms.optBoolean(SHOWMASKS, false);
      nucleiCount_ = parms.optInt(CELLCOUNT, 0);
      zappedNucleiCount_ = parms.optInt(OBJECTCOUNT, 0);
      
      ScriptInterface gui_ = MMStudio.getInstance();

      long startTime = System.currentTimeMillis();
      
      // microNuclei allowed sizes
      final double mnMinSize = 0.5;
      final double mnMaxSize = 800.0;
      // nuclei allowed sized
      final double nMinSize = 80;
      final double nMaxSize = 900;
      // max distance a micronucleus can be separated from a nucleus
      final double maxDistance = 80;
      // min distance a micronucleus should be from the edge of the image
      final double minEdgeDistance = 10.0; // in microns
      // minimum number of "micronuclei" we want per nucleus to score as a hit
      final double minNumMNperNucleus = 4;
      // do not analyze images whose stdev is above this value
      // Use this to remove images showing well edges
      final double maxStdDev = 7000;

      double pixelSize; // not sure why, but imp.getCalibration is unreliable

      // start of the main code
      List<Point2D.Double> microNuclei = new ArrayList<Point2D.Double>();
      Map<Point2D.Double, Roi> microNucleiROIs = new HashMap<Point2D.Double, Roi>();
      Map<Point2D.Double, ArrayList<Point2D.Double> > nuclei = 
              new HashMap<Point2D.Double, ArrayList<Point2D.Double> >();
      //nucleiContents = new ArrayList();
      Map<Point2D.Double, Roi> nucleiRois = new HashMap<Point2D.Double, Roi>();
      List<Point2D.Double> zapNuclei = new ArrayList<Point2D.Double>();

      // clean results table	
      ResultsTable res = ij.measure.ResultsTable.getResultsTable();
      res.reset();

      ImagePlus imp = new ImagePlus ("tmp", ImageUtils.makeProcessor(tImg));
      Calibration cal = imp.getCalibration();
      try {
         cal.pixelWidth = tImg.tags.getDouble("PixelSize_um"); 
         cal.pixelHeight = cal.pixelWidth;
      } catch(JSONException je) {
         throw new MMScriptException ("Failed to find pixelsize in the metadata");
      }
      // remove images that have the well edge in them
      double stdDev = imp.getStatistics().stdDev;
      if (stdDev > maxStdDev) {
         return null;
      }

      int width = imp.getProcessor().getWidth();
      int height = imp.getProcessor().getHeight();
      double widthUm = cal.getX(width);
      double heightUm = cal.getY(height);
      pixelSize = cal.getX(1.0);

      ImagePlus imp2 = (new Duplicator()).run(imp, 1, 1);

      // find micronuclei by sharpening, segmentation using Otsu, and Watershed
      ImagePlus microNucleiImp = imp2.duplicate();
      IJ.run(microNucleiImp, "16-bit", "");
      IJ.run(microNucleiImp, "Sharpen", "");
//IJ.run(microNucleiImp, "Thresholded Blur", "radius=5 threshold=800 softness=0.50 strength=3");
//IJ.run(microNucleiImp, "Unsharp Mask...", "radius=3 mask=0.60");
//IJ.run(microNucleiImp, "Kuwahara Filter", "sampling=3");
      IJ.setAutoThreshold(microNucleiImp, "Otsu dark");
      ij.Prefs.blackBackground = true;
      IJ.run(microNucleiImp, "Convert to Mask", "");
      IJ.run(microNucleiImp, "Close-", "");
      IJ.run(microNucleiImp, "Watershed", "");
      IJ.run("Set Measurements...", "area center decimal=2");
      IJ.run(microNucleiImp, "Analyze Particles...", "size=" + mnMinSize + "-" + mnMaxSize
              + "  show clear add");

      // Build up a list of potential micronuclei
      RoiManager rm = RoiManager.getInstance2();
      if (rm == null) {
         rm = new RoiManager();
      }
      for (Roi roi  : rm.getRoisAsArray()) {
         // approximate microNuclear positions as the center of the bounding box
         Rectangle rc = roi.getBounds();
         double xc = rc.x + 0.5 * rc.width;
         double yc = rc.y + 0.5 * rc.height;
         xc *= pixelSize;
         yc *= pixelSize;
         Point2D.Double pt = new java.awt.geom.Point2D.Double(xc, yc);
         microNuclei.add(pt);
         microNucleiROIs.put(pt, roi);
      }

      // find nuclei by smoothing and gaussian filtering, followed by Otsu segmentation and watershed
      ImagePlus nucleiImp = imp2.duplicate();
      IJ.run(nucleiImp, "Smooth", "");
      IJ.run(nucleiImp, "Gaussian Blur...", "sigma=5.0");
      IJ.setAutoThreshold(nucleiImp, "Otsu dark");
      ij.Prefs.blackBackground = true;
      IJ.run(nucleiImp, "Convert to Mask", "");
      IJ.run(nucleiImp, "Dilate", "");
      IJ.run(nucleiImp, "Erode", "");
      IJ.run(nucleiImp, "Watershed", "");
//IJ.run("Set Measurements...", "area centroid center bounding fit shape redirect=None decimal=2");
      IJ.run("Set Measurements...", "area center decimal=2");
      IJ.run(nucleiImp, "Analyze Particles...", "size=" + nMinSize + "-" + nMaxSize
              + "  clear add");

      // add nuclei to our list of nuclei:
      rm = RoiManager.getInstance2();
      for (Roi roi  : rm.getRoisAsArray()) {
         // approximate nuclear positions as the center of the bounding box
         Rectangle rc = roi.getBounds();
         double xc = rc.x + 0.5 * rc.width;
         double yc = rc.y + 0.5 * rc.height;
         xc *= pixelSize;
         yc *= pixelSize;
         // gui.message("pt: " + xc + ", " + yc);
         Point2D.Double pt = new java.awt.geom.Point2D.Double(xc, yc);
         nucleiRois.put(pt, roi);
         ArrayList<Point2D.Double> containedMNs = new ArrayList<Point2D.Double>();
         nuclei.put(pt, containedMNs);
      }

      // close the ImagePlus as we no longer need it (we could leave this to the GC)
      nucleiImp.changes = false;
      if (showMasks) {
         nucleiImp.show();
      } else {
         nucleiImp.close();
      }

      // no longer need the microNuclei imp
      microNucleiImp.changes = false;
      if (showMasks) {
         microNucleiImp.show();
      } else {
         microNucleiImp.close();
      }

      imp2.changes = false;
      if (showMasks) {
         imp2.show();
      } else {
         imp2.close();
      }

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
      double roiMinSize = pixelSize * pixelSize * nMinSize * 10;
      for (Point2D.Double p  : nuclei.keySet()) {
         res.incrementCounter();
         res.addValue("X", p.x);
         res.addValue("Y", p.y);
         ArrayList<Point2D.Double> mnList = nuclei.get(p);
         res.addValue("# mN", mnList.size());
         int zapit = 0;
         if (nuclei.get(p).size() >= minNumMNperNucleus) {
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
         res.addValue("Zap", zapit);
      }

      res.show("Results");

      nucleiCount_ += nuclei.size();
      zappedNucleiCount_ += zapNuclei.size();
      try {
         parms.put(CELLCOUNT, nucleiCount_);
         parms.put(OBJECTCOUNT, zappedNucleiCount_);
      } catch (JSONException ex) {
         gui_.message("MicroNucleiAnalysis.java: This should never happen!!!");
      }
      
      long endTime = System.currentTimeMillis();
      gui_.message("Analysis took: " + (endTime - startTime) + " millisec");
      

      // get a list with rois that we want to zap
      ArrayList<Roi> zapRois = new ArrayList<Roi>();
      for (Point2D.Double p  : zapNuclei) {
         Roi roi = nucleiRois.get(p);
         zapRois.add(roi);
      }

      gui_.message("mn: " + microNuclei.size() + ", n: " + nuclei.size() + 
                 ", zap: " + zapRois.size());
      
      return zapRois.toArray(new Roi[zapRois.size()]);
   }

        
   /**
    * Calculate the size of an ImageJ ROI
    */
   private long roiSize(Roi r) {
      return r.getBounds().width * r.getBounds().height;
   }

   
}
