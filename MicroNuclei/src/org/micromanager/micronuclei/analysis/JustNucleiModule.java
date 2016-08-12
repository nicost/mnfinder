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
import ij.gui.OvalRoi;
import ij.gui.Roi;
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
import org.micromanager.micronuclei.analysisinterface.AnalysisException;
import org.micromanager.micronuclei.analysisinterface.AnalysisModule;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;
import org.micromanager.micronuclei.analysisinterface.PropertyException;

/**
 *
 * @author nico
 */
public class JustNucleiModule extends AnalysisModule {
   private final String UINAME = "Zap Some Nuclei";
   
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
                 "Percentage of nuclei to be converted", 10.0 );
         maxStdDev_ = new AnalysisProperty(this.getClass(),
                  "Maximum Std. Dev.", 12500.0);
         maxMeanIntensity_ = new AnalysisProperty(this.getClass(),
                  "Maximum Std. Dev.", 20000.0);
         minSizeN_ = new AnalysisProperty(this.getClass(),
                  "Minimum nuclear size", 300.0);
         maxSizeN_ = new AnalysisProperty(this.getClass(),
                  "Maximum nuclear size", 1800.0);
         
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
   public Roi[] analyze(Studio mm, Image img, JSONObject parms) throws AnalysisException {
      ImageProcessor iProcessor = mm.data().ij().createProcessor(img);
      ImagePlus ip = (new ImagePlus("tmp", iProcessor)).duplicate();

      // check for edges by calculating stdev
      ImageStatistics stat = ip.getStatistics();
      double stdDev = stat.stdDev;
      double mean = stat.mean;
      if (stdDev > (Double) maxStdDev_.get() || mean > (Double) maxMeanIntensity_.get() ) {
         mm.alerts().postAlert(UINAME, JustNucleiModule.class, 
                 "Std. Dev. (" + stdDev + ") or intenisty (" + mean + ") too high, skipping this position"); 
      } 
	
      //dw.getImagePlus().setRoi(null); // make sure that there is no roi on the image
      //ip = dw.getImagePlus().duplicate();

      // Pre-filter to improve nuclear detection and slightly enlarge the masks
      IJ.run(ip, "Smooth", "");
      IJ.run(ip, "Gaussian Blur...", "sigma=5.0");

      // get the nuclear masks 
      IJ.setAutoThreshold(ip, "Otsu dark");
      // Fill holes and watershed to split large nuclei
      IJ.run(ip, "Convert to Mask", "");
      IJ.run(ip, "Dilate", "");
      IJ.run(ip, "Erode", "");
      IJ.run(ip, "Watershed", "");

      // Now measure and store masks in ROI manager
      IJ.run("Set Measurements...", "area centroid center bounding fit shape redirect=None decimal=2");
      IJ.run(ip, "Analyze Particles...", "size=" + (Double) minSizeN_.get() + "-" + 
              (Double) maxSizeN_.get() + " pixel exclude clear add");

      // prepare the masks to be send to the DMD
      Roi[] allNuclei = roiManager_.getRoisAsArray();
      //mm.alerts().postAlert(UINAME, JustNucleiModule.class, 
      //        "Found " + allNuclei.length + " nuclei");
      List convertRoiList = new ArrayList();
      int nrNucleiToSkip = (int) (1 / ((Double) percentageOfNuclei_.get() / 100.0));
      for (int i = 0; i < allNuclei.length; i++) {
         if (i % nrNucleiToSkip == 0) {
         	Rectangle rect = allNuclei[i].getBounds();
         	convertRoiList.add (new OvalRoi(rect.x, rect.y, rect.width, rect.height));
         } 
      }
      Roi[] convertRois = new OvalRoi[convertRoiList.size()];
      convertRois = (Roi[]) convertRoiList.toArray(convertRois);
      
      try {
         parms.put(CELLCOUNT, allNuclei.length);
         parms.put(OBJECTCOUNT, convertRois.length);
      } catch (JSONException jse) {
         throw new AnalysisException (jse.getMessage());
      }
      
      return convertRois;
   }

   @Override
   public void reset() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public String name() {
      return UINAME;
   }
   
}
