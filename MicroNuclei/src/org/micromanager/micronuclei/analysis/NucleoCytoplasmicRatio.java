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

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.alg.misc.GPixelMath;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.alg.nn.KdTreePoint2D_F32;
import boofcv.alg.segmentation.watershed.WatershedVincentSoille1991;
import boofcv.core.image.ConvertImage;
import boofcv.factory.segmentation.FactorySegmentationAlg;
import boofcv.struct.ConfigLength;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.Studio;
import org.micromanager.data.Image;
import org.micromanager.micronuclei.analysisinterface.AnalysisException;
import org.micromanager.micronuclei.analysisinterface.AnalysisModule;
import static org.micromanager.micronuclei.analysisinterface.AnalysisModule.CELLCOUNT;
import static org.micromanager.micronuclei.analysisinterface.AnalysisModule.OBJECTCOUNT;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;
import org.micromanager.micronuclei.analysisinterface.ResultRois;
import org.micromanager.micronuclei.utilities.BinaryListOps;
import org.micromanager.internal.utils.imageanalysis.BoofCVImageConverter;

/**
 *
 * @author nico
 */
public class NucleoCytoplasmicRatio extends AnalysisModule {

   private final String UINAME = "Nuclear-Cytoplasmic Ratio";
   private final String DESCRIPTION
           = "<html>Locate nuclei based on nuclear channel, <br>"
           + "calculate nucl/cytoplasmic ration in another.";

   private final AnalysisProperty skipWellsWithEdges_;
   private final AnalysisProperty nuclearChannel_;
   private final AnalysisProperty testChannel_;
   private final AnalysisProperty minSizeN_;
   private final AnalysisProperty maxSizeN_;
   
   private final EdgeDetectorSubModule edgeDetector_;
   private RoiManager roiManager_;

   public NucleoCytoplasmicRatio() {
      // note: the type of the value when creating the AnalysisProperty determines
      // the allowed type, and can create problems when the user enters something
      // different
      nuclearChannel_ = new AnalysisProperty(this.getClass(), 
              "<html>Channel nr. for nuclei</html>",
              "Channel nr for nuclei",
              1, null);
      testChannel_ = new AnalysisProperty(this.getClass(), 
              "<html>Channel nr. for nucleo-cytoplasmic ratio</html>",
              "Channel nr. for nucleo-cytoplasmic ratio",
              2, null);
      skipWellsWithEdges_ = new AnalysisProperty(this.getClass(),
              "<html>Skip wells with edges</html",
              "Skips wells with edges when checked",
              true,
              null);
      minSizeN_ = new AnalysisProperty(this.getClass(),
              "<html>Minimum nuclear size (&micro;m<sup>2</sup>)</html>",
              "<html>Smallest size of putative nucleus in "
              + "&micro;m<sup>2</sup></html>", 300.0, null);
      maxSizeN_ = new AnalysisProperty(this.getClass(),
              "<html>Maximum nuclear size (&micro;m<sup>2</sup>)</html>",
              "<html>Largest size of putative nucleus in "
              + "&micro;m<sup>2</sup></html>", 1800.0, null);
          
      edgeDetector_ = new EdgeDetectorSubModule();

      
      List<AnalysisProperty> apl = new ArrayList<>();
      
      apl.add(nuclearChannel_);
      apl.add(testChannel_);
      apl.add(minSizeN_);
      apl.add(maxSizeN_);
      for (AnalysisProperty ap : edgeDetector_.getAnalysisProperties()) {
         apl.add(ap);
      }
      apl.add(skipWellsWithEdges_);

      setAnalysisProperties(apl);

      // the ImageJ roiManager	
      roiManager_ = RoiManager.getInstance();
      if (roiManager_ == null) {
         roiManager_ = new RoiManager();
      }

   }

   @Override
   public ResultRois analyze(Studio mm, Image[] imgs, Roi userRoi, JSONObject parms) throws AnalysisException {
      int nC = (int) nuclearChannel_.get() - 1;
      if (nC < 0 || nC > imgs.length - 1) {
         String msg = "Channel nr. for nuclei should be between 1 and " + (imgs.length + 1);
         throw new AnalysisException(msg);
      }
      int cC = (int) testChannel_.get() - 1;
      if (cC < 0 || cC > imgs.length - 1) {
          String msg = "Channel nr. for cytoplasm should be between 1 and " + (imgs.length + 1);
         throw new AnalysisException(msg);
      }
            
      Image nuclImg = imgs[nC];
      Image cytoImg = imgs[cC];
      
      ImageProcessor nuclIProcessor = mm.data().ij().createProcessor(nuclImg);
      Rectangle userRoiBounds = null;
      if (userRoi != null) {
         nuclIProcessor.setRoi(userRoi);
         nuclIProcessor = nuclIProcessor.crop();
         userRoiBounds = userRoi.getBounds();
      }
      
      Roi restrictToThisRoi = edgeDetector_.analyze(mm, imgs); 
      
      if (restrictToThisRoi != null && ((Boolean) skipWellsWithEdges_.get()) ) {
         int pos = imgs[0].getCoords().getStagePosition();
         mm.alerts().postAlert("Skip image", JustNucleiModule.class,
                 "Edge detected at position " + pos );
         return new ResultRois(null, null, null, this.getName());
      }

      ImagePlus nuclIp = (new ImagePlus(UINAME, nuclIProcessor.duplicate()));
      if (restrictToThisRoi != null) {
         nuclIp.setRoi(restrictToThisRoi);
         //IJ.run("setBackgroundColor(0, 0, 0)");
         // this will set the pixels outside of the ROI to the backgroundcolor
         // The automatic thresholding will not look at these pixels 
         // (it only analyzes within the ROI)
         IJ.run(nuclIp, "Clear Outside", "");
      }
      
      Calibration calibration = nuclIp.getCalibration();
      calibration.pixelWidth = nuclImg.getMetadata().getPixelSizeUm();
      calibration.pixelHeight = nuclImg.getMetadata().getPixelSizeUm();
      calibration.setUnit("um");
      
      GrayU16 bNuc = (GrayU16) BoofCVImageConverter.mmToBoofCV(nuclImg, false);
      GrayU16 bgNuc = bNuc.createSameShape(GrayU16.class);
      GrayS32 subNuc = bNuc.createSameShape(GrayS32.class);
      GBlurImageOps.gaussian(bNuc, bgNuc, -1, 400, null);
      // subtract the minimum from background to avoid values < 0 after subtraction
      int min = boofcv.alg.misc.ImageStatistics.min(bgNuc);
      PixelMath.minus(bgNuc, min, bgNuc);
      PixelMath.subtract(bNuc, bgNuc, subNuc);
      ConvertImage.convert(subNuc, bNuc);
      GrayU16 gaussNuc = bNuc.createSameShape(GrayU16.class);
      GBlurImageOps.gaussian(bNuc, gaussNuc, -1, 3, null);
      int otsu2 = GThresholdImageOps.computeOtsu2(gaussNuc, boofcv.alg.misc.ImageStatistics.min(gaussNuc),
              boofcv.alg.misc.ImageStatistics.max(gaussNuc));
      GrayU8 thresholdedNuc = gaussNuc.createSameShape(GrayU8.class);
      GrayU8 thresholdedNuc2 = gaussNuc.createSameShape(GrayU8.class);
      
      //GThresholdImageOps.localOtsu(gaussNuc, thresholdedNuc, false, ConfigLength.fixed(250), 0, 1.0, true);
      //GThresholdImageOps.localSauvola(gaussNuc, thresholdedNuc2, ConfigLength.fixed(250), 1.0f, true);
      
      
      GThresholdImageOps.threshold(gaussNuc, thresholdedNuc, otsu2, false);
            
      ImageProcessor d = BoofCVImageConverter.convert(thresholdedNuc, false);
      
      ImagePlus dyMe = new ImagePlus("Boof-thresholded", d);
      dyMe.show();
      IJ.run(dyMe, "Watershed", "");
      IJ.run(dyMe, "Options...", "iterations=2 count=1 black pad edm=Overwrite do=Erode");
      dyMe.show();
      
      //ImageProcessor c = BoofCVImageConverter.convert(thresholdedNuc2, false);
      //ImagePlus cyMe = new ImagePlus("Boof-localSauvola", c);
      //cyMe.show();
      /*
      WatershedVincentSoille1991 watershed = FactorySegmentationAlg.watershed(ConnectRule.FOUR);
      GrayU8 gaussNuc8 = bNuc.createSameShape(GrayU8.class);
      GPixelMath.multiply(gaussNuc, 255.0 / ImageStatistics.max(gaussNuc), gaussNuc);
      ConvertImage.convert(gaussNuc, gaussNuc8);
      watershed.process(gaussNuc8, label);
      int regions = watershed.getTotalRegions();
      GrayS32 ws = watershed.getOutput();
      int max = boofcv.alg.misc.ImageStatistics.max(ws);
      GPixelMath.multiply(ws, 255.0 / max, ws);
      GrayU8 ws8 = ws.createSameShape(GrayU8.class);
      ConvertImage.convert(ws, ws8);
      ImageProcessor c = BoofCVImageConverter.convert(ws8, false);
      ImagePlus cyMe = new ImagePlus("Boof-ws", c);
      cyMe.show();
      BinaryImageOps.erode8(thresholdedNuc, 1, thresholdedNuc2);
      BinaryImageOps.dilate8(thresholdedNuc2, 1, thresholdedNuc);
      */
      
      
      
      /*
      double minValue = GImageStatistics.min(blurred);
      double maxValue = GImageStatistics.max(blurred);      
      double threshold = GThresholdImageOps.computeHuang(blurred, minValue, maxValue);
      GrayU8 cytoMask = new GrayU8(blurred.width, blurred.height);
      GThresholdImageOps.threshold(blurred, cytoMask, threshold, false);
      */
      

      // Even though we are flatfielding, results are much better after
      // background subtraction.  In one test, I get about 2 fold more nuclei
      // when doing this background subtraction
      IJ.run(nuclIp, "Subtract Background...", "rolling=5 sliding");
      // Pre-filter to improve nuclear detection and slightly enlarge the masks
      IJ.run(nuclIp, "Smooth", "");
      IJ.run(nuclIp, "Gaussian Blur...", "sigma=3.0");

      // get the nuclear masks 
      IJ.setAutoThreshold(nuclIp, "Otsu dark");
      // Fill holes and watershed to split large nuclei
      IJ.run(nuclIp, "Convert to Mask", "");
      // Use this instead of erode/dilate or Close since we can pad the edges this way
      // and can still reject nuclei touching the edge (which is not true when 
      // eroding normall)
      IJ.run(nuclIp, "Options...", "iterations=1 count=1 black pad edm=Overwrite do=Close");
      IJ.run(nuclIp, "Watershed", "");
      // debatable: shrinc the nuclei to make sure we don't get cytoplasm
      IJ.run(nuclIp, "Options...", "iterations=1 count=1 black pad edm=Overwrite do=Erode");
      ImageGray igNuc = BoofCVImageConverter.convert(nuclIp.getProcessor(), false);
      ImageGray igCyto = BoofCVImageConverter.mmToBoofCV(cytoImg, false);
      GrayU8 binary = new GrayU8(igNuc.width,igNuc.height);
      GThresholdImageOps.threshold(igNuc, binary, 10, false);
      GrayS32 contourImg = new GrayS32(igNuc.getWidth(), igNuc.getHeight());
      List<Contour> contours = 
                    BinaryImageOps.contour(binary, ConnectRule.FOUR, contourImg);
      List<List<Point2D_I32>> tmpNuclearClusters = 
                    BinaryImageOps.labelToClusters(contourImg, contours.size(), null);
      Map<Integer, List<Point2D_I32>> nuclearClusters = new HashMap<>(tmpNuclearClusters.size());
      Map<Integer, Point2D_F32> centers = new HashMap<>(tmpNuclearClusters.size());
      // cyoplasm "ring" only
      Map<Integer, List<Point2D_I32>> cytoClusters = new HashMap<>(tmpNuclearClusters.size());
      // nucleus plus cytoplasm
      Map<Integer, List<Point2D_I32>> cellClusters = new HashMap<>(tmpNuclearClusters.size());
      int index = 0;
      for (List<Point2D_I32> cluster : tmpNuclearClusters) {
         nuclearClusters.put(index, cluster);
         centers.put(index, center(cluster));
         Set<Point2D_I32> expandedNuclearCluster = BinaryListOps.listToSet(cluster);
         // this defines an "empty" ring between nucleus and cytoplasm
         for (int i = 0; i < 3; i++) {
            expandedNuclearCluster = 
                 BinaryListOps.dilate4_2D_I32(expandedNuclearCluster, igNuc.width, igNuc.height);
         }
         Set<Point2D_I32>cytoCluster = 
                 BinaryListOps.dilate4_2D_I32(expandedNuclearCluster, igNuc.width, igNuc.height);
         // this defines the "thickness" of the cytoplasmic ring
         for (int i = 0; i < 4; i++) {
            cytoCluster = 
                 BinaryListOps.dilate4_2D_I32(cytoCluster, igNuc.width, igNuc.height);
         }
         cellClusters.put(index, BinaryListOps.setToList(cytoCluster));
         cytoCluster = BinaryListOps.subtract(cytoCluster, expandedNuclearCluster);
         cytoClusters.put(index, BinaryListOps.setToList(cytoCluster));
         index++;
      }
      
      // find the n cell masks closest to this one.  Subtract neighboring cellmask from this cytoplasm mask
      NearestNeighbor<Point2D_F32> nn = FactoryNearestNeighbor.kdtree(new KdTreePoint2D_F32());
      List<Point2D_F32> centersList = new ArrayList<>(centers.size());
      for (Map.Entry<Integer, Point2D_F32> entry : centers.entrySet()) {
         centersList.add(entry.getValue());
      }
      nn.setPoints(centersList, true);
      NnData<Point2D_F32> result = new NnData<>();
      // 5 should be enough...
      FastQueue<NnData<Point2D_F32>> fResults = new FastQueue(5, result.getClass(), true);
      for (int i =  0; i < centersList.size(); i++) {
         List<Point2D_I32> cyto = cytoClusters.get(i);
         nn.findNearest(centersList.get(i), -1, 5, fResults);
         for (int j = 0; j < fResults.size(); j++) {
            NnData<Point2D_F32> candidate = fResults.get(j);
            if (i != candidate.index) { // make sure to not compare against ourselves
               List<Point2D_I32> cell = cellClusters.get(candidate.index);
               Iterator itr = cyto.iterator();
               while (itr.hasNext()) {
                  Point2D_I32 next = (Point2D_I32) itr.next();
                  if (cell.contains(next)) {
                     itr.remove();
                  }
               }
            }
         }
      }
      
      // Make cytoplasmic mask to "AND" our little cyto circles
      GrayU16 originalCyto = (GrayU16) igCyto;
      GrayU16 blurred = originalCyto.createSameShape(GrayU16.class);
      GBlurImageOps.gaussian(originalCyto, blurred, -1, 3, null);
      double minValue = GImageStatistics.min(blurred);
      double maxValue = GImageStatistics.max(blurred);      
      double threshold = GThresholdImageOps.computeHuang(blurred, minValue, maxValue);
      GrayU8 cytoMask = new GrayU8(blurred.width, blurred.height);
      GThresholdImageOps.threshold(blurred, cytoMask, threshold, false);
      //ImageProcessor c = BoofCVImageConverter.convert(cytoMask, false);
      //ImagePlus cyMe = new ImagePlus("Boof-CytoMask", c);
      //cyMe.show();
      // now do the logical "AND"
      for (int i = 0; i < cytoClusters.size(); i++) {
         List<Point2D_I32> cyto = cytoClusters.get(i);
         Iterator itr = cyto.iterator();
         while (itr.hasNext()) {
            Point2D_I32 next = (Point2D_I32) itr.next();
            if (cytoMask.get(next.x, next.y) == 0) {
               itr.remove();
            }
         }
      }
      
      
      // Now get average intensities under nuclear mask and cytoplasmic mask
      for (int i = 0; i < nuclearClusters.size() && i < cytoClusters.size(); i++) {
         
         List<Point2D_I32> nucleus = nuclearClusters.get(i);
         double sum = 0.0;
         for (Point2D_I32 p : nucleus) {
            sum += originalCyto.get(p.x, p.y);
         }
         double nAvg = sum / nucleus.size();
         
         List<Point2D_I32> cyto = cytoClusters.get(i);
         sum = 0.0;
         for (Point2D_I32 p : cyto) {
            sum += originalCyto.get(p.x, p.y);
         }
         double cAvg = sum / cyto.size();
         System.out.println("" + i + " x: " + (int) centers.get(i).x + ", y:" 
                 + (int) centers.get(i).y + ", cytoAvg: " + cAvg + 
                 ", ratio: " + nAvg / cAvg);
      }
      
      
      
      /**
       * Uncomment to display the nuclear and cytoplasmic masks
       *
      GrayU8 dispImg = new GrayU8(igNuc.getWidth(), igNuc.getHeight());
      for (int i = 0; i < nuclearClusters.size(); i++) {
         List<Point2D_I32> cluster = nuclearClusters.get(i);
         for (Point2D_I32 p : cluster) {
            dispImg.set(p.x, p.y, dispImg.get(p.x, p.y) + 30);
         }
      }
      for (int i = 0; i < cytoClusters.size(); i++) {
         List<Point2D_I32> cluster = cytoClusters.get(i);
         for (Point2D_I32 p : cluster) {
            dispImg.set(p.x, p.y, dispImg.get(p.x, p.y) + 60);
         }
      }
      ImageProcessor convert = BoofCVImageConverter.convert(dispImg, false);
      ImagePlus showMe = new ImagePlus("Boof", convert);
      showMe.show();
      */
      
      
      // Now measure and store masks in ROI manager
      /*
      IJ.run("Set Measurements...", "area centroid center bounding fit shape redirect=None decimal=2");
      String analyzeParticlesParameters =  "size=" + (Double) minSizeN_.get() + "-" + 
              (Double) maxSizeN_.get() + " exclude clear add";
      // this roiManager reset is needed since Analyze Particles will not take 
      // this action if it does not find any Rois, leading to erronous results
      roiManager_.reset();
      IJ.run(nuclIp, "Analyze Particles...", analyzeParticlesParameters);
      ImagePlus cytoPlusNucIp = new Duplicator().run(nuclIp);
      IJ.run(cytoPlusNucIp, "Options...", "iteration=2 count=1 black pad edm=Overwrite do=Dilate");
      ImagePlus cytoIp = (new ImageCalculator()).run("Subtract create", cytoPlusNucIp, nuclIp);
      cytoIp.show();
   */

      // prepare the masks to be send to the DMD
      Roi[] allNuclei = roiManager_.getRoisAsArray();
      List convertRoiList = new ArrayList();
      for (Roi allNuclei1 : allNuclei) {
         if (userRoiBounds != null) {
            Rectangle r2d = allNuclei1.getBounds();
            allNuclei1.setLocation(r2d.x + userRoiBounds.x, r2d.y + userRoiBounds.y);
         }
         convertRoiList.add(allNuclei1);
      }
      Roi[] convertRois = new Roi[convertRoiList.size()];
      convertRois = (Roi[]) convertRoiList.toArray(convertRois);
      
      try {
         parms.put(CELLCOUNT, allNuclei.length + parms.getInt(CELLCOUNT));
         parms.put(OBJECTCOUNT, convertRois.length + parms.getInt(OBJECTCOUNT));
      } catch (JSONException jse) {
         throw new AnalysisException (jse.getMessage());
      }
      
      ResultRois rrs = new ResultRois(allNuclei, convertRois, null, 
              this.getName());
      rrs.reportOnImg(0);
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
   
   public static Point2D_F32 center(List<Point2D_I32> input) {
      if (input.size() < 1) return null;
      Point2D_F32 tmp = new Point2D_F32();
      for (Point2D_I32 p: input) {
         tmp.x += p.x;
         tmp.y += p.y;
      }
      tmp.x /= input.size();
      tmp.y /= input.size();
      return tmp;
   }

}
