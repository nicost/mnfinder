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
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
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
import static org.micromanager.micronuclei.analysisinterface.AnalysisModule.CELLCOUNT;
import static org.micromanager.micronuclei.analysisinterface.AnalysisModule.OBJECTCOUNT;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;
import org.micromanager.micronuclei.analysisinterface.ResultRois;

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
      Image nuclImg = imgs[(int) nuclearChannel_.get() - 1];
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

      // Even though we are flatfielding, results are much better after
      // background subtraction.  In one test, I get about 2 fold more nuclei
      // when doing this background subtraction
      IJ.run(nuclIp, "Subtract Background...", "rolling=5 sliding");
      // Pre-filter to improve nuclear detection and slightly enlarge the masks
      IJ.run(nuclIp, "Smooth", "");
      IJ.run(nuclIp, "Gaussian Blur...", "sigma=5.0");

      // get the nuclear masks 
      IJ.setAutoThreshold(nuclIp, "Otsu dark");
      // Fill holes and watershed to split large nuclei
      IJ.run(nuclIp, "Convert to Mask", "");
      // Use this instead of erode/dilate or Close since we can pad the edges this way
      // and can still reject nuclei touching the edge (which is not true when 
      // eroding normall)
      IJ.run(nuclIp, "Options...", "iterations=1 count=1 black pad edm=Overwrite do=Close");
      IJ.run(nuclIp, "Watershed", "");

      // Now measure and store masks in ROI manager
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
   
}
