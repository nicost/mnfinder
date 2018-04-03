
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
import static org.micromanager.micronuclei.analysisinterface.AnalysisModule.CELLCOUNT;
import static org.micromanager.micronuclei.analysisinterface.AnalysisModule.OBJECTCOUNT;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;
import org.micromanager.micronuclei.analysisinterface.ResultRois;

/**
 *
 * @author nico
 */
public class NuclearSizeModule  extends AnalysisModule {
   private final String UINAME = "Nuclear Size";
   private final String DESCRIPTION = 
           "<html>Module that selects nuclei (imaged in the first channel), <br>" +
           "based on size<br>";
   private final AnalysisProperty maxStdDev_;
   private final AnalysisProperty maxMeanIntensity_;
   private final AnalysisProperty minSizeN_;
   private final AnalysisProperty maxSizeN_;
   private final AnalysisProperty minSizeSN_;
   private final AnalysisProperty maxSizeSN_;

   private final EdgeDetectorSubModule edgeDetector_;
   private RoiManager roiManager_;

   public NuclearSizeModule() {
      // note: the type of the value when creating the AnalysisProperty determines
      // the allowed type, and can create problems when the user enters something
      // different

      maxStdDev_ = new AnalysisProperty(this.getClass(),
              "Maximum Image Std. Dev.",
              "<html>Std. Dev. of grayscale values of original image<br>"
              + "Used to exclude images with edges</html>", 12500.0);
      maxMeanIntensity_ = new AnalysisProperty(this.getClass(),
              "Maximum Image Mean Int.",
              "<html>If the average intensity of the image is higher<br>"
              + "than this number, the image will be skipped", 20000.0);
      minSizeN_ = new AnalysisProperty(this.getClass(),
              "<html>Minimum nuclear size (&micro;m<sup>2</sup>)</html>",
              "<html>Smallest size of nucleus in "
              + "&micro;m<sup>2</sup></html><br>"
              + "Used to identify nuclei", 300.0);
      maxSizeN_ = new AnalysisProperty(this.getClass(),
              "<html>Maximum nuclear size (&micro;m<sup>2</sup>)</html>",
              "<html>Largest size of nucleus in "
              + "&micro;m<sup>2</sup></html><br> +"
              + "Used to identify nuclei", 1800.0);
      minSizeSN_ = new AnalysisProperty(this.getClass(),
              "<html>Minimum positive size (&micro;m<sup>2</sup>)</html>",
              "<html>Smallest size of selected nucleus in "
              + "&micro;m<sup>2</sup></html><br>"
              + "Used to generate hits", 300.0);
      maxSizeSN_ = new AnalysisProperty(this.getClass(),
              "<html>Maximum positive size (&micro;m<sup>2</sup>)</html>",
              "<html>Largest size of nucleus in "
              + "&micro;m<sup>2</sup></html><br> +"
              + "Used to generate hits", 1800.0);
      
      edgeDetector_ = new EdgeDetectorSubModule();

      List<AnalysisProperty> apl = new ArrayList<AnalysisProperty>();
      for (AnalysisProperty ap : edgeDetector_.getAnalysisProperties()) {
         apl.add(ap);
      }
      
      apl.add(maxStdDev_);
      apl.add(maxMeanIntensity_);
      apl.add(minSizeN_);
      apl.add(maxSizeN_);
      apl.add(minSizeSN_);
      apl.add(maxSizeSN_);

      setAnalysisProperties(apl);

      // the ImageJ roiManager	
      roiManager_ = RoiManager.getInstance();
      if (roiManager_ == null) {
         roiManager_ = new RoiManager();
      }

   }

   @Override
   public ResultRois analyze(Studio mm, Image[] imgs, Roi roi, JSONObject parms) 
           throws AnalysisException {
      Roi restrictToThisRoi = edgeDetector_.analyze(mm, imgs);
      
      Image img = imgs[0];
      ImageProcessor iProcessor = mm.data().ij().createProcessor(img);

      Rectangle userRoiBounds = null;
      if (roi != null) {
         iProcessor.setRoi(roi);
         iProcessor = iProcessor.crop();
         userRoiBounds = roi.getBounds();
      }
      ImagePlus ip = (new ImagePlus("tmp", iProcessor)).duplicate();

      Calibration calibration = ip.getCalibration();
      calibration.pixelWidth = img.getMetadata().getPixelSizeUm();
      calibration.pixelHeight = img.getMetadata().getPixelSizeUm();
      calibration.setUnit("um");
      
      if (restrictToThisRoi != null) {
         ip.setRoi(restrictToThisRoi);
      }

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
      final Roi[] allNuclei = roiManager_.getRoisAsArray();
      
            // Now measure and store masks in ROI manager
      IJ.run("Set Measurements...", "area centroid center bounding fit shape redirect=None decimal=2");
      analyzeParticlesParameters =  "size=" + (Double) minSizeSN_.get() + "-" + 
              (Double) maxSizeSN_.get() + " exclude clear add";
      // this roiManager reset is needed since Analyze Particles will not take 
      // this action if it does not find any Rois, leading to erronous results
      roiManager_.reset();
      IJ.run(ip, "Analyze Particles...", analyzeParticlesParameters);

      // prepare the masks to be send to the DMD
      Roi[] selectedNuclei = roiManager_.getRoisAsArray();
      //mm.alerts().postAlert(UINAME, JustNucleiModule.class, 
      //        "Found " + allNuclei.length + " nuclei");
      List convertRoiList = new ArrayList();
      for (Roi selectedNuclei1 : selectedNuclei) {
         if (userRoiBounds != null) {
            Rectangle r2d = selectedNuclei1.getBounds();
            selectedNuclei1.setLocation(r2d.x + userRoiBounds.x, r2d.y + userRoiBounds.y);
         }
         convertRoiList.add(selectedNuclei1);
      }
      Roi[] convertRois = new Roi[convertRoiList.size()];
      convertRois = (Roi[]) convertRoiList.toArray(convertRois);
      
      try {
         parms.put(CELLCOUNT, allNuclei.length + parms.getInt(CELLCOUNT));
         parms.put(OBJECTCOUNT, convertRois.length + parms.getInt(OBJECTCOUNT));
      } catch (JSONException jse) {
         throw new AnalysisException (jse.getMessage());
      }
      
      ResultRois rrs = new ResultRois(allNuclei, convertRois, null);
      rrs.reportOnImg(0);
      rrs.reportOnZapChannel(0); // Pre-Zap
      rrs.reportOnZapChannel(2);  // Post-Zap
      return rrs;
   }

   @Override
   public void reset() {
      // Nothing to be done (yet)
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