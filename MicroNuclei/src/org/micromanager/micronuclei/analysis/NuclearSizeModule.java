
package org.micromanager.micronuclei.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.text.TextWindow;
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
   private final AnalysisProperty skipWellsWithEdges_;
   private final AnalysisProperty maxStdDev_;
   private final AnalysisProperty maxMeanIntensity_;
   private final AnalysisProperty minSizeN_;
   private final AnalysisProperty maxSizeN_;
   private final AnalysisProperty minSizeSN_;
   private final AnalysisProperty maxSizeSN_;
   private final AnalysisProperty sizeFilter_;
   private final AnalysisProperty maxCirc_;
   private final AnalysisProperty minCirc_;
   private final AnalysisProperty sdFilter_;
   
   private TextWindow textWindow_;

   private final EdgeDetectorSubModule edgeDetector_;
   private RoiManager roiManager_;

   public NuclearSizeModule() {
      // note: the type of the value when creating the AnalysisProperty determines
      // the allowed type, and can create problems when the user enters something
      // different
      
      skipWellsWithEdges_ = new AnalysisProperty(this.getClass(),
              "<html>Skip wells with edges</html",
              "Skips wells with edges when checked",
              true,
              null);
      maxStdDev_ = new AnalysisProperty(this.getClass(),
              "Maximum Image Std. Dev.",
              "<html>Std. Dev. of grayscale values of original image<br>"
              + "Used to exclude images with edges</html>", 12500.0, null);
      maxMeanIntensity_ = new AnalysisProperty(this.getClass(),
              "Maximum Image Mean Int.",
              "<html>If the average intensity of the image is higher<br>"
              + "than this number, the image will be skipped", 20000.0, null);
      minSizeN_ = new AnalysisProperty(this.getClass(),
              "<html>Minimum nuclear size (&micro;m<sup>2</sup>)</html>",
              "<html>Smallest size of nucleus in "
              + "&micro;m<sup>2</sup></html><br>"
              + "Used to identify nuclei", 35.0, null);
      maxSizeN_ = new AnalysisProperty(this.getClass(),
              "<html>Maximum nuclear size (&micro;m<sup>2</sup>)</html>",
              "<html>Largest size of nucleus in "
              + "&micro;m<sup>2</sup></html><br> +"
              + "Used to identify nuclei", 1800.0, null);
      minSizeSN_ = new AnalysisProperty(this.getClass(),
              "<html>Minimum positive size (&micro;m<sup>2</sup>)</html>",
              "<html>Smallest size of selected nucleus in "
              + "&micro;m<sup>2</sup></html><br>"
              + "Used to generate hits", 450.0, null);
      maxSizeSN_ = new AnalysisProperty(this.getClass(),
              "<html>Maximum positive size (&micro;m<sup>2</sup>)</html>",
              "<html>Largest size of nucleus in "
              + "&micro;m<sup>2</sup></html><br> +"
              + "Used to generate hits", 1800.0, null);
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

      List<AnalysisProperty> apl = new ArrayList<>();
      for (AnalysisProperty ap : edgeDetector_.getAnalysisProperties()) {
         apl.add(ap);
      }
      
      apl.add(skipWellsWithEdges_);
      apl.add(maxStdDev_);
      apl.add(maxMeanIntensity_);
      apl.add(minSizeN_);
      apl.add(maxSizeN_);
      apl.add(minSizeSN_);
      apl.add(maxSizeSN_);
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
   public ResultRois analyze(Studio mm, Image[] imgs, Roi roi, JSONObject parms) 
           throws AnalysisException {
      Roi restrictToThisRoi = edgeDetector_.analyze(mm, imgs);
            
      if (restrictToThisRoi != null && ((Boolean) skipWellsWithEdges_.get()) ) {
         int pos = imgs[0].getCoords().getStagePosition();
         mm.alerts().postAlert("Skip image", JustNucleiModule.class,
                 "Edge detected at position " + pos );
         return new ResultRois(null, null, null, this.getName());
      }
      
      Image img = imgs[0];
      ImageProcessor iProcessor = mm.data().ij().createProcessor(img);

      Rectangle userRoiBounds = null;
      if (roi != null) {
         iProcessor.setRoi(roi);
         iProcessor = iProcessor.crop();
         userRoiBounds = roi.getBounds();
      }
      ImagePlus ip = (new ImagePlus(UINAME, iProcessor.duplicate()));
      Duplicator duppie = new Duplicator();
      ImagePlus originalIp = duppie.run(ip);

      Calibration calibration = ip.getCalibration();
      calibration.pixelWidth = img.getMetadata().getPixelSizeUm();
      calibration.pixelHeight = img.getMetadata().getPixelSizeUm();
      calibration.setUnit("um");
      
      if (restrictToThisRoi != null) {
         ip.setRoi(restrictToThisRoi);
         //IJ.run("setBackgroundColor(0, 0, 0)");
         // this will set the pixels outside of the ROI to the backgroundcolor
         // The automatic thresholding will not look at these pixels 
         // (it only analyzes within the ROI)
         IJ.run(ip, "Clear Outside", "");
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
         return new ResultRois(null, null, null, this.getName());
      }
      if (mean > maxMean) {
         mm.alerts().postAlert("Skip image", JustNucleiModule.class,
                 "Mean intensity of image at position " + pos + " (" + 
                  NumberUtils.doubleToDisplayString(mean) +
                  ") is higher than the limit you set: " + maxMean);
         return new ResultRois(null, null, null, this.getName());
      }


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

      // Now measure and store all nuclei in ROI manager
      IJ.run("Set Measurements...", "center area integrated redirect=None decimal=2");
      String analyzeParticlesParameters =  "size=" + (Double) minSizeN_.get() + "-" + 
              (Double) maxSizeN_.get() + "clear display exclude add";
      // this roiManager reset is needed since Analyze Particles will not take 
      // this action if it does not find any Rois, leading to erronous results
      roiManager_.reset();
      IJ.run(ip, "Analyze Particles...", analyzeParticlesParameters);
      ResultsTable resultsTable = Analyzer.getResultsTable();
      final Roi[] allNuclei = roiManager_.getRoisAsArray();
      
      // Report results
      /*
      if (textWindow_ == null || !textWindow_.isVisible()) {
         textWindow_ = new TextWindow("Nuclear Size", resultsTable.getColumnHeadings(), 450, 300);
      }
      for (int row = 0; row < resultsTable.size(); row++) {
          
        textWindow_.append( resultsTable.getRowAsString(row));
      }
      textWindow_.setVisible(true);
       */    

      // Now measure and store size-selected nuclei in ROI manager      
      IJ.run("Set Measurements...", "area centroid center bounding fit shape redirect=None decimal=2");
      analyzeParticlesParameters =  "size=" + (Double) minSizeSN_.get() + "-" + 
                (Double) maxSizeSN_.get() + "circularity=" + (Double) minCirc_.get() + "-" + (Double) maxCirc_.get() + " clear exclude add";
      // added by Xiaowei, select for circularity to exclude lost focus cells
      // this roiManager reset is needed since Analyze Particles will not take 
      // this action if it does not find any Rois, leading to erronous results
      roiManager_.reset();
      IJ.run(ip, "Analyze Particles...", analyzeParticlesParameters);
      Roi[] selectedNuclei = roiManager_.getRoisAsArray();
      
      // Now screen through size/circ-selected nuclei to make sure them meeting standard deviation requirement (further exclude clustered cells)
      // Didn't run this in JustNucleiModule to save time
      ResultsTable rt = new ResultsTable();
      Analyzer analyzer = new Analyzer(originalIp, Analyzer.MEAN + Analyzer.STD_DEV, rt);

      List<Roi> sdFilteredList = new ArrayList<Roi>();
      
      for (Roi nuc : selectedNuclei) {
         originalIp.setRoi(nuc);
         analyzer.measure();
         // IJ.run(cellImgIp2, "Measure", "");
         int counter = rt.getCounter();
         int col = rt.getColumnIndex("Mean");
         double meanVal = rt.getValueAsDouble(col, counter - 1); //all the Area values
         int sdCol = rt.getColumnIndex("StdDev");
         double sdVal = rt.getValueAsDouble(sdCol, counter - 1);
         //System.out.println("counter: " + counter + ", mean: " + meanVal + ", stdDev: " + sdVal);
         if (sdVal < (Double) sdFilter_.get()) {
            sdFilteredList.add(nuc);
         }
      }
         
      Roi[] sdFilteredNuclei = sdFilteredList.toArray(new Roi[sdFilteredList.size()]);
      
      //mm.alerts().postAlert(UINAME, JustNucleiModule.class, 
      //        "Found " + allNuclei.length + " nuclei");
      
      // prepare the masks to be send to the DMD
      List<Roi> convertRoiList = new ArrayList();
      for (Roi selectedNuclei1 : sdFilteredNuclei) {
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
      
      ResultRois rrs = new ResultRois(allNuclei, convertRois, null, this.getName());
      rrs.reportOnImg(0);
      //rrs.reportOnZapChannel(0); // Pre-Zap
      //rrs.reportOnZapChannel(2);  // Post-Zap
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
