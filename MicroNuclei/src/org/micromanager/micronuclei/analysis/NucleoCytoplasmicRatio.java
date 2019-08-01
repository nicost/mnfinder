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
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.alg.nn.KdTreePoint2D_F32;
import boofcv.core.image.ConvertImage;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import clearcl.ClearCL;
import clearcl.ClearCLBuffer;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.backend.ClearCLBackends;
import clearcl.ops.kernels.CLKernelException;
import clearcl.ops.kernels.CLKernelExecutor;
import clearcl.ops.kernels.Kernels;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.EDM;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.text.TextWindow;
import java.awt.Rectangle;
import java.io.IOException;
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
import org.micromanager.data.internal.DefaultImage;
import org.micromanager.internal.utils.ReportingUtils;
import org.micromanager.micronuclei.analysisinterface.AnalysisException;
import org.micromanager.micronuclei.analysisinterface.AnalysisModule;
import static org.micromanager.micronuclei.analysisinterface.AnalysisModule.CELLCOUNT;
import static org.micromanager.micronuclei.analysisinterface.AnalysisModule.OBJECTCOUNT;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;
import org.micromanager.micronuclei.analysisinterface.ResultRois;
import org.micromanager.micronuclei.utilities.BinaryListOps;
import org.micromanager.internal.utils.imageanalysis.BoofCVImageConverter;
import org.micromanager.internal.utils.imageanalysis.ClearCLNioConverters;

/**
 *
 * @author nico
 */
public class NucleoCytoplasmicRatio extends AnalysisModule {

   private final String UINAME = "Nuclear-Cytoplasmic Ratio";
   private final String DESCRIPTION
           = "<html>Locate nuclei based on nuclear channel, <br>"
           + "calculate nucl/cytoplasmic ration in another.";

   private CLKernelExecutor clke_ = null;
   private ClearCLContext cclContext_;
   
   private final AnalysisProperty skipWellsWithEdges_;
   private final AnalysisProperty nuclearChannel_;
   private final AnalysisProperty testChannel_;
   private final AnalysisProperty minSizeN_;
   private final AnalysisProperty maxSizeN_;
   private final AnalysisProperty showMasks_;
   
   private final EdgeDetectorSubModule edgeDetector_;
   private RoiManager roiManager_;
   private TextWindow textWindow_;
   
   // Intermediate BoofCV images.  Keep references for efficiency
   private GrayU16 nuclearImgBackground_;
   private GrayS32 backgroundSubtractedNuclearImg;
   private GrayU16 gaussNuc_;
   private GrayS32 contourImg_;
   private GrayU16 cytoBlurred_;
   private GrayU8 cytoMask_;

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
      showMasks_ = new AnalysisProperty(this.getClass(), 
               "<html>Show binary masks</html>",
               "Show binary masks",
               false, null);
          
      edgeDetector_ = new EdgeDetectorSubModule();

      List<AnalysisProperty> apl = new ArrayList<>();

      apl.add(nuclearChannel_);
      apl.add(testChannel_);
      apl.add(minSizeN_);
      apl.add(maxSizeN_);
      apl.add(showMasks_);
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

      ClearCL mClearCL = new ClearCL(ClearCLBackends.getBestBackend());
      ClearCLDevice mClearCLDevice = mClearCL.getBestGPUDevice();

      if (mClearCLDevice == null) {
         ReportingUtils.logDebugMessage("Warning: GPU device determination failed.");
         return;
      }

      ReportingUtils.logDebugMessage("Using OpenCL device: " + mClearCLDevice.getName());

      cclContext_ = mClearCLDevice.createContext();

      try {
         clke_ = new CLKernelExecutor(
                 cclContext_,
                 clearcl.ocllib.OCLlib.class);
      } catch (IOException ioe) {
         ReportingUtils.showError("Failed to find GPU code");
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

      Roi restrictToThisRoi;
      restrictToThisRoi = edgeDetector_.analyze(mm, imgs);

      if (restrictToThisRoi != null && ((Boolean) skipWellsWithEdges_.get())) {
         int pos = imgs[0].getCoords().getStagePosition();
         mm.alerts().postAlert("Skip image", JustNucleiModule.class,
                 "Edge detected at position " + pos);
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

      final double pixelSurface = nuclImg.getMetadata().getPixelSizeUm()
              * nuclImg.getMetadata().getPixelSizeUm();

      if (clke_ != null) {
         try {
            long[] dimensions = {nuclImg.getWidth(), nuclImg.getHeight()};
            int totalPixels = nuclImg.getWidth() * nuclImg.getHeight();
            DefaultImage dNuclImg = (DefaultImage) nuclImg;
            ClearCLBuffer clNuclImg = ClearCLNioConverters.convertNioTiClearCLBuffer(cclContext_,
                    dNuclImg.getPixelBuffer(), dimensions);
            ClearCLBuffer clNuclearImgBackground = clke_.createCLBuffer(clNuclImg);
            ClearCLBuffer clScratch2 = clke_.createCLBuffer(clNuclImg);
            ClearCLBuffer clScratch3 = clke_.createCLBuffer(clNuclImg);
            // blur to create backgroundimage
            Kernels.blur(clke_, clNuclImg, clNuclearImgBackground, 400.0f, 400.0f);
            // subtract, but ensure there is no clipping
            float[] minMax = Kernels.minMax(clke_, clNuclearImgBackground, 32);
            Kernels.addImageAndScalar(clke_, clNuclearImgBackground, clScratch2, -minMax[0]);
            Kernels.subtractImages(clke_, clNuclImg, clScratch2, clScratch3);
            Kernels.blur(clke_, clScratch3, clScratch2, 2.0f, 2.0f);
            //otsu thresholding
            minMax = Kernels.minMax(clke_, clScratch2, 32);
            int[] hist = new int[256];
            Kernels.histogram(clke_, clScratch2, hist, minMax[0], minMax[1]);
            int otsu = GThresholdImageOps.computeOtsu(hist, 256, totalPixels);
            otsu = (int) (minMax[0] + otsu / 256f * (minMax[1] - minMax[0])) ;
            Kernels.threshold(clke_, clScratch2, clScratch3, (float) otsu);
            // close using dilate/erode (2x each)
            Kernels.dilateBox(clke_, clScratch3, clScratch2);
            Kernels.dilateBox(clke_, clScratch2, clScratch3);            
            Kernels.erodeBox(clke_, clScratch3, clScratch2);
            Kernels.erodeBox(clke_, clScratch2, clScratch3);
            
            

            
            clScratch3.writeTo(dNuclImg.getPixelBuffer(), true);
            
            clNuclImg.close();
            clNuclearImgBackground.close();
            clScratch2.close();
            clScratch3.close();
            
         } catch (CLKernelException  clKexc) {
            ReportingUtils.logError(clKexc);
         }
         
      }

      GrayU16 bNuc = (GrayU16) BoofCVImageConverter.mmToBoofCV(nuclImg, false);
      ImageGray igCyto = BoofCVImageConverter.mmToBoofCV(cytoImg, false);
      nuclearImgBackground_ = (GrayU16) createSameShapeIfNeeded(bNuc,
              nuclearImgBackground_, GrayU16.class);
      backgroundSubtractedNuclearImg = (GrayS32) createSameShapeIfNeeded(bNuc,
              backgroundSubtractedNuclearImg, GrayS32.class);
      gaussNuc_ = (GrayU16) createSameShapeIfNeeded(bNuc, gaussNuc_, GrayU16.class);
      contourImg_ = (GrayS32) createSameShapeIfNeeded(bNuc, contourImg_, GrayS32.class);

      // Heavy blur to create a "background" image
      GBlurImageOps.gaussian(bNuc, nuclearImgBackground_, -1, 400, null);
      // subtract the minimum from background to avoid values < 0 after subtraction
      final int nuclearBackgroundMinimum = ImageStatistics.min(nuclearImgBackground_);
      PixelMath.minus(nuclearImgBackground_, nuclearBackgroundMinimum, nuclearImgBackground_);
      // Subtract background from nuclear image
      PixelMath.subtract(bNuc, nuclearImgBackground_, backgroundSubtractedNuclearImg);
      ConvertImage.convert(backgroundSubtractedNuclearImg, bNuc);
      GBlurImageOps.gaussian(bNuc, gaussNuc_, -1, 3, null);

      int otsu2 = GThresholdImageOps.computeOtsu2(gaussNuc_, ImageStatistics.min(gaussNuc_),
              ImageStatistics.max(gaussNuc_));
      GrayU8 thresholdedNuc = gaussNuc_.createSameShape(GrayU8.class);
      GrayU8 thresholdedNuc2 = gaussNuc_.createSameShape(GrayU8.class);

      GThresholdImageOps.threshold(gaussNuc_, thresholdedNuc, otsu2, false);

      // close using dilate/erode
      BinaryImageOps.dilate4(thresholdedNuc, 2, thresholdedNuc2);
      BinaryImageOps.erode4(thresholdedNuc2, 2, thresholdedNuc);

      // Use ImageJ Watershed code.  Worth porting to BoofCV?
      ImageProcessor d = BoofCVImageConverter.convert(thresholdedNuc, false);
      EDM edm = new EDM();
      edm.toWatershed(d);
      // note: d shares pixels with thesholdedNuc, so we can erode thresholdedNuc directly!
      BinaryImageOps.erode4(thresholdedNuc, 2, thresholdedNuc2);


      /*
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
      */

      List<Contour> contours = 
                    BinaryImageOps.contour(thresholdedNuc2, ConnectRule.FOUR, contourImg_);
      List<List<Point2D_I32>> tmpNuclearClusters = 
                    BinaryImageOps.labelToClusters(contourImg_, contours.size(), null);
      Map<Integer, List<Point2D_I32>> nuclearClusters = new HashMap<>(tmpNuclearClusters.size());
      Map<Integer, Point2D_F32> centers = new HashMap<>(tmpNuclearClusters.size());
      // cyoplasm "ring" only
      Map<Integer, List<Point2D_I32>> cytoClusters = new HashMap<>(tmpNuclearClusters.size());
      // nucleus plus cytoplasm
      Map<Integer, Set<Point2D_I32>> cellClusters = new HashMap<>(tmpNuclearClusters.size());
      int index = 0;
      for (List<Point2D_I32> cluster : tmpNuclearClusters) {
         nuclearClusters.put(index, cluster);
         centers.put(index, center(cluster));
         Set<Point2D_I32> expandedNuclearCluster = BinaryListOps.listToSet(cluster);
         // this defines an "empty" ring between nucleus and cytoplasm
         for (int i = 0; i < 3; i++) {
            expandedNuclearCluster = 
                 BinaryListOps.dilate4_2D_I32(expandedNuclearCluster, thresholdedNuc2.width, thresholdedNuc2.height);
         }
         Set<Point2D_I32>cytoCluster = 
                 BinaryListOps.dilate4_2D_I32(expandedNuclearCluster, thresholdedNuc2.width, thresholdedNuc2.height);
         // this defines the "thickness" of the cytoplasmic ring
         for (int i = 0; i < 4; i++) {
            cytoCluster = 
                 BinaryListOps.dilate4_2D_I32(cytoCluster, thresholdedNuc2.width, thresholdedNuc2.height);
         }
         cellClusters.put(index, cytoCluster);
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
      // 4 should be enough...
      FastQueue<NnData<Point2D_F32>> fResults = new FastQueue(4, result.getClass(), true);
      for (int i =  0; i < centersList.size(); i++) {
         List<Point2D_I32> cyto = cytoClusters.get(i);
         nn.findNearest(centersList.get(i), -1, 5, fResults);
         for (int j = 0; j < fResults.size(); j++) {
            NnData<Point2D_F32> candidate = fResults.get(j);
            if (i != candidate.index) { // make sure to not compare against ourselves
               Set<Point2D_I32> cell = cellClusters.get(candidate.index);
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
      cytoBlurred_ = (GrayU16) createSameShapeIfNeeded(originalCyto, cytoBlurred_, GrayU16.class);
      cytoMask_ = (GrayU8) createSameShapeIfNeeded(originalCyto, cytoMask_, GrayU8.class);
      
      GBlurImageOps.gaussian(originalCyto, cytoBlurred_, -1, 3, null);
      double minValue = GImageStatistics.min(cytoBlurred_);
      double maxValue = GImageStatistics.max(cytoBlurred_);      
      double threshold = GThresholdImageOps.computeHuang(cytoBlurred_, minValue, maxValue);
      GThresholdImageOps.threshold(cytoBlurred_, cytoMask_, threshold, false);
      // Do the logical "AND"
      for (int i = 0; i < cytoClusters.size(); i++) {
         List<Point2D_I32> cyto = cytoClusters.get(i);
         Iterator itr = cyto.iterator();
         while (itr.hasNext()) {
            Point2D_I32 next = (Point2D_I32) itr.next();
            if (cytoMask_.get(next.x, next.y) == 0) {
               itr.remove();
            }
         }
      }
      
      
      // Get average intensities under nuclear mask and cytoplasmic mask
      if (textWindow_ == null || !textWindow_.isVisible()) {
         textWindow_ = new TextWindow("NucleoCytoplasmic Ratio", 
                 "#\tx\ty\tnucl. Size\tnucl. Avg(nucCh.)\tnucl. Avg(cytoCh.)\tcyto. Size \tcyto. Avg\tn/c ratio", 400, 250);
      }
      textWindow_.setVisible(true);
      
      String pos = "" + nuclImg.getCoords().getP();
      //outStream.println("#\tx\ty\tnucl. Size\tnucl. Avg\tcyto Avg\tratio");
      int counter = 0;
      for (int i = 0; i < nuclearClusters.size() && i < cytoClusters.size(); i++) {
         
         List<Point2D_I32> nucleus = nuclearClusters.get(i);
         double sum = 0.0;
         for (Point2D_I32 p : nucleus) {
            sum += bNuc.get(p.x, p.y);
         }
         final double nuclearAvg = sum / nucleus.size();
         
         sum = 0.0;
         for (Point2D_I32 p : nucleus) {
            sum += originalCyto.get(p.x, p.y);
         }
         final double nAvg = sum / nucleus.size();
         
         List<Point2D_I32> cyto = cytoClusters.get(i);
         sum = 0.0;
         for (Point2D_I32 p : cyto) {
            sum += originalCyto.get(p.x, p.y);
         }
         final double cAvg = sum / cyto.size();
         final double nuclearSize = nucleus.size() * pixelSurface;
         final double cytoSize = cyto.size() * pixelSurface;
         if (nuclearSize > (double) minSizeN_.get() && nuclearSize < (double) maxSizeN_.get()) {
            textWindow_.append(pos + "-" + counter++ + "\t" + (int) centers.get(i).x + "\t"
                    + (int) centers.get(i).y + "\t" + nuclearSize + "\t" + nuclearAvg + "\t"
                    + nAvg + "\t" + cytoSize + "\t" + cAvg + "\t" + nAvg / cAvg + "\n");
         }
      }
      
      
      
      /**
       * Uncomment to display the nuclear and cytoplasmic masks
       */
      if ((Boolean) showMasks_.get()) {
         GrayU8 dispImg = new GrayU8(thresholdedNuc2.getWidth(), thresholdedNuc2.getHeight());
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
      }
      
      
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
   
   /**
    * Wrapper around BoofCV createSameShape.  Checks if the target already exists
    * and is the correct size.  If not, creates new image, otherwise returns the
    * existing one
    * @param template
    * @param target
    * @param c
    * @return 
    */
   public static ImageGray createSameShapeIfNeeded(ImageGray template, ImageGray target, Class c) {
      if (target == null) {
         target = template.createSameShape(c);
      }  else if (target.getHeight() != template.getHeight() || target.getWidth() != template.getWidth()) {
         target = template.createSameShape(c);
      }
      return target;
   }

}
