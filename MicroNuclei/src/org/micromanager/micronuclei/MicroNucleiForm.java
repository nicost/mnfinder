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

package org.micromanager.micronuclei;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import ij.plugin.frame.RoiManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import mmcorej.TaggedImage;
import net.miginfocom.swing.MigLayout;
import org.micromanager.acquisition.MMAcquisition;
import org.micromanager.api.MultiStagePosition;
import org.micromanager.api.PositionList;
import org.micromanager.api.ScriptInterface;
import org.micromanager.projector.ProjectorControlForm;
import org.micromanager.utils.FileDialogs;
import org.micromanager.utils.ImageUtils;
import org.micromanager.utils.MMFrame;
import org.micromanager.utils.MMScriptException;
import org.micromanager.utils.ReportingUtils;


/**
 *
 * @author nico
 */
public class MicroNucleiForm extends MMFrame {
   
   private final ScriptInterface gui_;
   private final Font arialSmallFont_;
   private final Dimension buttonSize_;
   private int nucleiPerWell_ = 0;
   private int zappedNucleiPerWell_ = 0;
   private String saveLocation_;
   private final JTextField saveTextField_;
   private String imagingChannel_;
   private final JComboBox channelComboBox_;
   private String secondImagingChannel_;
   private final JComboBox secondChannelComboBox_;
   private String zapChannel_;
   private final JComboBox zapChannelComboBox_;
   private String afterZapChannel_;
   private final JComboBox AfterZapChannelComboBox_;
   private final JCheckBox doZap_;
   private final JCheckBox showMasks_;
   private final Preferences prefs_;
   
   private final String SAVELOCATION = "SaveLocation";
   private final String IMAGINGCHANNEL = "ImagingChannel";
   private final String SECONDIMAGINGCHANNEL = "SecondImagingChannel";
   private final String ZAPCHANNEL = "ZapChannel";
   private final String AFTERZAPCHANNEL = "AfterZapChannel";
   private final String DOZAP = "DoZap";
   private final String SHOWMASKS = "ShowMasks";
   
   private final AtomicBoolean stopFlag_ = new AtomicBoolean(false);
   
   public MicroNucleiForm(ScriptInterface gui) {
      gui_ = gui;
      loadAndRestorePosition(100, 100, 200, 200);
      prefs_ = Preferences.userNodeForPackage(this.getClass());
      
      arialSmallFont_ = new Font("Arial", Font.PLAIN, 12);
      buttonSize_ = new Dimension(70, 21);

      this.setLayout(new MigLayout("flowx, fill, insets 8"));
      this.setTitle("MicroNuclei Analyze");
      
      
      add(myLabel(arialSmallFont_,"Save here:"));
      
      saveTextField_ = new JTextField();
      saveLocation_ = prefs_.get(SAVELOCATION, saveLocation_);
      saveTextField_.setText(saveLocation_);
      saveTextField_.setMinimumSize(new Dimension(200, 12));
      add(saveTextField_);
      
      final JButton dirButton = myButton(buttonSize_, arialSmallFont_, "...");
      dirButton.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            dirActionPerformed(e);
         }
      });
      add(dirButton, "wrap");
      
      add(myLabel(arialSmallFont_, "Imaging Channel: "));
      channelComboBox_ = new JComboBox();
      imagingChannel_ = prefs_.get(IMAGINGCHANNEL, imagingChannel_);
      updateChannels(channelComboBox_, imagingChannel_, false);
      channelComboBox_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
            channelActionPerformed(ae);
         }
      } );
      add(channelComboBox_, "span 2, left, wrap");
      
      add(myLabel(arialSmallFont_, "2nd Imaging Channel: "));
      secondChannelComboBox_ = new JComboBox();
      secondImagingChannel_ = prefs_.get(SECONDIMAGINGCHANNEL, secondImagingChannel_);
      updateChannels(secondChannelComboBox_, secondImagingChannel_, true);
      secondChannelComboBox_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
            secondChannelActionPerformed(ae);
         }
      } );
      add(secondChannelComboBox_, "span 2, left, wrap");
      
      add(myLabel(arialSmallFont_, "Zap Channel: "));
      zapChannelComboBox_ = new JComboBox();
      zapChannel_ = prefs_.get(ZAPCHANNEL, zapChannel_);
      updateChannels(zapChannelComboBox_, zapChannel_, false);
      zapChannelComboBox_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
            zapChannelActionPerformed(ae);
         }
      } );
      add(zapChannelComboBox_, "span 2, left, wrap");
      
      add(myLabel(arialSmallFont_, "After Zap Channel: "));
      AfterZapChannelComboBox_ = new JComboBox();
      afterZapChannel_ = prefs_.get(AFTERZAPCHANNEL, afterZapChannel_);
      updateChannels(AfterZapChannelComboBox_, afterZapChannel_, false);
      AfterZapChannelComboBox_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
            afterZapChannelActionPerformed(ae);
         }
      } );
      add(AfterZapChannelComboBox_, "span 2, left, wrap");
      
      doZap_ = new JCheckBox("Zap");
      doZap_.setSelected(prefs_.getBoolean(DOZAP, false));
      doZap_.setFont(arialSmallFont_);
      doZap_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
              prefs_.putBoolean(DOZAP, doZap_.isSelected());
         }
      });
      add (doZap_);
      
      showMasks_  = new JCheckBox("Show Masks");
      showMasks_.setSelected (prefs_.getBoolean(SHOWMASKS, false));
      showMasks_.setFont(arialSmallFont_);
      showMasks_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
              prefs_.putBoolean(SHOWMASKS, showMasks_.isSelected());
         }
      });
      add (showMasks_, "wrap");
      
            
      final JButton runButton = myButton(buttonSize_, arialSmallFont_, "Run");
      runButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            RunAll myThread = new RunAll();
            myThread.init(false);

         }
      });
      add(runButton, "span 3, split 3, center");

      final JButton stopButton = myButton(buttonSize_, arialSmallFont_, "Stop");
      stopButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            stopFlag_.set(true);
         }
      } );
      add(stopButton, "center");
      
      final JButton testButton = myButton(buttonSize_, arialSmallFont_, "Test");
      testButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            RunAll myThread = new RunAll();
            myThread.init(true);
         }
      } );
      add(testButton, "center, wrap");
            

      loadAndRestorePosition(100, 100, 350, 250);
      
      pack();
      
      this.setResizable(false);

   }
   
   @Override
   public void dispose() {
      super.dispose();
   }
   
   private JButton myButton(Dimension buttonSize, Font font, String text) {
      JButton button = new JButton();
      button.setPreferredSize(buttonSize);
      button.setMinimumSize(buttonSize);
      button.setFont(font);
      button.setMargin(new Insets(0, 0, 0, 0));
      button.setText(text);
      
      return button;
   }
   
   private JLabel myLabel(Font font, String text) {
      JLabel label = new JLabel(text);
      label.setFont(font);
      return label;
   }

   private void updateChannels(JComboBox box, String selectedChannel, 
           boolean addEmpty) {
      box.removeAllItems();
      if (addEmpty) {
         box.addItem("");
      }
      try {
         String channelGroup = gui_.getMMCore().getChannelGroup();
         String[] channels = gui_.getMMCore().getAvailableConfigs(channelGroup).toArray();
         for (String channel : channels) {
            box.addItem(channel);
         }
      } catch (Exception ex) {
         ReportingUtils.logError(ex);
      }
      box.setSelectedItem(selectedChannel);
   }

   private void dirActionPerformed(java.awt.event.ActionEvent evt) {                                                  
      File f = FileDialogs.openDir(this, "Save location",
              new FileDialogs.FileType("Dir", "Save Location",
              saveLocation_, true, "") );
      if (f != null) {
         saveLocation_ = f.getAbsolutePath();
         saveTextField_.setText(saveLocation_);
         if (prefs_ != null)
               prefs_.put(SAVELOCATION, saveLocation_);    
      }
   }   
   
   private void channelActionPerformed(ActionEvent evt) {
      imagingChannel_ = (String) channelComboBox_.getSelectedItem();
      if (prefs_ != null)
         prefs_.put(IMAGINGCHANNEL, imagingChannel_);
   }
   
   private void secondChannelActionPerformed(ActionEvent evt) {
      secondImagingChannel_ = (String) secondChannelComboBox_.getSelectedItem();
      if (prefs_ != null) 
         prefs_.put(SECONDIMAGINGCHANNEL, secondImagingChannel_);
   }
   
   private void zapChannelActionPerformed(ActionEvent evt) {
      zapChannel_ = (String) zapChannelComboBox_.getSelectedItem();
      if (prefs_ != null) 
         prefs_.put(ZAPCHANNEL, zapChannel_);
   }
   
   private void afterZapChannelActionPerformed(ActionEvent evt) {
      afterZapChannel_ = (String) AfterZapChannelComboBox_.getSelectedItem();
      if (prefs_ != null) 
        prefs_.put(AFTERZAPCHANNEL, afterZapChannel_);
   }
         
   
   private class RunAll implements Runnable {
      private boolean running_ = false;
      private boolean testing_ = false;
      public RunAll() {
      }
      @Override
      public void run() {
         try {
            running_ = true;
            if (!testing_) {
               runAnalysisAndZapping(saveLocation_);
            } else {
               runTest();
            }
               
         } catch (MMScriptException ex) {
            ReportingUtils.showError(ex, "Error during acquisition");
         } catch (Exception ex) {
            ReportingUtils.showError(ex, "Error during acquisition");
         } finally {
            running_ = false;
         }
      }
     
      public void init(boolean testing) {
         if (running_) {
            return;
         }
         testing_ = testing;
         stopFlag_.set(false);
         Thread t = new Thread(this);
         t.start();
      }
   }
   
   public void runTest() throws MMScriptException {
      ImagePlus ip = IJ.getImage();
      if (ip == null) {
         ReportingUtils.showError("No image selected");
      } else {
         TaggedImage tImg = ImageUtils.makeTaggedImage(ip.getProcessor());
         analyze(tImg, doZap_.isSelected(), showMasks_.isSelected());
      }
      
   }
   
   public void runAnalysisAndZapping(String saveLocation) throws IOException, MMScriptException, Exception {
      String channelGroup = gui_.getMMCore().getChannelGroup();
      
      //prefs = Preferences.userNodeForPackage(this.getClass());
      gui_.closeAllAcquisitions();
      new File(saveLocation).mkdirs();
      //new File(saveZappedLocation).mkdirs();
      File resultsFile = new File(saveLocation + File.separator + "results.txt");
      resultsFile.createNewFile();
      BufferedWriter resultsWriter = new BufferedWriter(new FileWriter(resultsFile));

      PositionList posList = gui_.getPositionList();
      MultiStagePosition[] positions = posList.getPositions();
      String currentWell = "";
      int nrChannels = 1;
      if (secondImagingChannel_.length() > 1) {
         nrChannels = 2;
      }
      
      int nrImagesPerWell = 0;
      int wellCount = 0;
      // figure out how many sites per there are, we actually get that number 
      // from the last well
      for (MultiStagePosition msp : positions) {
         String label = msp.getLabel();
         String well = label.split("-")[0];
         if (!currentWell.equals(well)) {
            currentWell = well;
            wellCount++;
            nrImagesPerWell = 1;
         } else
            nrImagesPerWell++;
      }
      gui_.message("Images per well: " + nrImagesPerWell);
      int count = 0;
      int siteCount = 0;
      for (MultiStagePosition msp : positions) {
         if (stopFlag_.get()) {
            resultsWriter.close();
            return;
         }
         String label = msp.getLabel();
         String well = label.split("-")[0];
         if (!currentWell.equals(well)) {
            // new well
            gui_.message("Starting well: " + well);
            if (!currentWell.equals("")) {
               recordResults(resultsWriter, currentWell);
            }
            currentWell = well;
            siteCount = 0;
            gui_.openAcquisition(well, saveLocation, 1, nrChannels + 1, 1, nrImagesPerWell, true, true);
         }
         MultiStagePosition.goToPosition(msp, gui_.getMMCore());
         gui_.getMMCore().waitForSystem();
         gui_.message("Site: " + msp.getLabel());
         gui_.getMMCore().setConfig(channelGroup, imagingChannel_);
         gui_.getMMCore().snapImage();
         TaggedImage tImg = gui_.getMMCore().getTaggedImage();
         gui_.addImageToAcquisition(well, 0, 0, 0, siteCount, tImg);
         if (nrChannels == 2) {
            gui_.getMMCore().setConfig(channelGroup, secondImagingChannel_);
            gui_.getMMCore().snapImage();
            tImg = gui_.getMMCore().getTaggedImage();
            gui_.addImageToAcquisition(well, 0, 1, 0, siteCount, tImg);
            MMAcquisition acqObject = gui_.getAcquisition(well);
            try {
               acqObject.setChannelColor(1, new Color(0, 0, 255).getRGB());
            } catch (MMScriptException ex) {
               // ignore since we do not want to crash our acquisition  
            }
         }
         gui_.getMMCore().setConfig(channelGroup, zapChannel_);
         // analyzer the second channel if that is the one we took
         int zappedNow = analyze(tImg, doZap_.isSelected(), showMasks_.isSelected());
         // zappedNow = prefs.getInt("zappedNow", 0);
         if (zappedNow > 0) {
            String acq2 = msp.getLabel();
            gui_.message("Imaging zapped cells at site: " + acq2);
            // take the red image and save it
            gui_.getMMCore().setConfig(channelGroup, afterZapChannel_);
            gui_.getMMCore().snapImage();
            TaggedImage tImg2 = gui_.getMMCore().getTaggedImage();
            gui_.addImageToAcquisition(well, 0, nrChannels, 0, siteCount, tImg2);
            MMAcquisition acqObject = gui_.getAcquisition(well);
            try {
               acqObject.setChannelColor(nrChannels, new Color(255, 0, 0).getRGB());
            } catch (MMScriptException ex) {
                // ignore since we do not want to crash our acquisition  
            }
         }
         siteCount++;
         count++;
      }

      // record the results from the last well:
      recordResults(resultsWriter, currentWell);

      resultsWriter.close();
      String msg = "Analyzed " + count + " images, in " + wellCount + " wells.";
      gui_.message(msg);
      ReportingUtils.showMessage(msg);
   }
   
   private void recordResults(BufferedWriter resultsWriter, String currentWell) throws IOException, MMScriptException {
      resultsWriter.write(currentWell + "\t" + nucleiPerWell_ + "\t"
              + zappedNucleiPerWell_);
      resultsWriter.newLine();
      resultsWriter.flush();
      gui_.message(currentWell + " " + nucleiPerWell_ + "   "
              + zappedNucleiPerWell_);
      nucleiPerWell_ = 0;
      zappedNucleiPerWell_ = 0;
   }
   
   
   /**
    * 
    * @param zap whether or not to photoactivate
    * @param showMasks whether or not to show binary masks, set to false 
    * when using the IA plugin!
    * 
    * @return number of zapped cells
    */
   private int analyze(TaggedImage tImg, boolean zap, boolean showMasks) throws MMScriptException {

      long startTime = System.currentTimeMillis();
      
      // microNuclei allowed sizes
      final double mnMinSize = 0.5;
      final double mnMaxSize = 800.0;
      // nuclei allowed sized
      final double nMinSize = 80;
      final double nMaxSize = 900;
      // max distance a micronucleus can be separated from a nucleus
      final double maxDistance = 20;
      // min distance a micronucleus should be from the edge of the image
      final double minEdgeDistance = 10.0; // in microns
      // minimum number of "micronuclei" we want per nucleus to score as a hit
      final double minNumMNperNucleus = 4;
      // do not analyze images whose stdev is above this value
      // Use this to remove images showing well edges
      final double maxStdDev = 7000;
      // name of the faltfield image in ImageJ.  Open this image first
      final String flatfieldName = "flatfield.tif";

      double pixelSize; // not sure why, but imp.getCalibration is unreliable

      
     

      // start of the main code
      List<Point2D.Double> microNuclei = new ArrayList<Point2D.Double>();
      Map<Point2D.Double, Roi> microNucleiROIs = new HashMap<Point2D.Double, Roi>();
      Map<Point2D.Double, ArrayList<Point2D.Double> > nuclei = 
              new HashMap<Point2D.Double, ArrayList<Point2D.Double> >();
      //nucleiContents = new ArrayList();
      Map<Point2D.Double, Roi> nucleiRois = new HashMap<Point2D.Double, Roi>();
      List<Point2D.Double> zapNuclei = new ArrayList<Point2D.Double>();

      // check if there is a flatfield image
      ImagePlus flatField = ij.WindowManager.getImage(flatfieldName);
      if (flatField == null) {
         gui_.message("No flatfield found");
      }

      // clean results table	
      ResultsTable res = ij.measure.ResultsTable.getResultsTable();
      res.reset();

      ImagePlus imp = new ImagePlus ("tmp", ImageUtils.makeProcessor(tImg));
      Calibration cal = imp.getCalibration();
      // remove images that have the well edge in them
      double stdDev = imp.getStatistics().stdDev;
      if (stdDev > maxStdDev) {
         return 0;
      }

      int width = imp.getProcessor().getWidth();
      int height = imp.getProcessor().getHeight();
      double widthUm = cal.getX(width);
      double heightUm = cal.getY(height);
      pixelSize = cal.getX(1.0);

      //gui.message("PixelSize: " + cal.getX(1));
      // maintain some form of persistence using prefs
      Preferences prefs = Preferences.userNodeForPackage(this.getClass());

      ImagePlus imp2 = (new Duplicator()).run(imp, 1, 1);
      ImageCalculator ic = new ImageCalculator();
      if (flatField != null) {
         imp2 = ic.run("Divide, float, 32", imp2, flatField);
      }

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
//IJ.run(microNucleiImp, "Erode", "");
      IJ.run(microNucleiImp, "Watershed", "");
//IJ.run("Set Measurements...", "area centroid center bounding fit shape redirect=None decimal=2");
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
         Point2D.Double cn = closest(mn, nuclei);
         if (cn != null && maxDistance > distance(mn, cn)) {
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

      nucleiPerWell_ += nuclei.size();
      zappedNucleiPerWell_ += zapNuclei.size();
      
      long endTime = System.currentTimeMillis();
      gui_.message("Analysis took: " + (endTime - startTime) + " millisec");
      
      if (zap) {
         // get a list with rois that we want to zap
         ArrayList<Roi> zapRois = new ArrayList<Roi>();
         for (Point2D.Double p  : zapNuclei) {
            //for (p : nuclei) {  // use this to zap all nuclei
            Roi roi = nucleiRois.get(p);
            //gui.message("Zap the roi: " + roi.getBounds());
            zapRois.add(roi);
         }

         gui_.message("mn: " + microNuclei.size() + ", n: " + nuclei.size() + 
                 ", zap: " + zapRois.size());

         //gui.message("ZapRoi x: " + zapRois.get(0).x + ", y: " + zapRois.get(0).y);
         // convert zapRois in a Roi[] of Polygon Rois
         ProjectorControlForm pcf = 
                 ProjectorControlForm.showSingleton(gui_.getMMCore(), gui_);
         Roi[] rois = new Roi[zapRois.size()];
         int i;
         for (i = 0; i < zapRois.size(); i++) {
            rois[i] = (Roi) zapRois.get(i);
            Polygon poly = rois[i].getConvexHull();
            rois[i] = new PolygonRoi(poly, Roi.POLYGON);
         }

         prefs.putInt("zappedNow", rois.length);

         // send to the galvo device and zap them for real
         pcf.setNrRepetitions(5);
         for (i = 0; i < rois.length; i++) {
            gui_.message("Zapping " + (i + 1) + " of " + rois.length);
            Roi[] theRois = {rois[i]};
            pcf.setROIs(theRois);
            pcf.updateROISettings();
            pcf.getDevice().waitForDevice();
            pcf.runRois();
            pcf.getDevice().waitForDevice();
         }
         

         
         return rois.length;

      }
      
      return 0;

   }

   
   /**
    * calculates distance between two points
    */
   private double distance(Point2D.Double p1, Point2D.Double p2) {
      double x = p1.x - p2.x;
      double y = p1.y - p2.y;
      double total = x * x + y * y;
      return Math.sqrt(total);
   }
   
   /**
    * Find the closest point in the HashMap for now, uses brute force search
    */
   private Point2D.Double closest(Point2D.Double p, 
           Map<Point2D.Double, ArrayList<Point2D.Double> > l) {
      if (l.isEmpty()) {
         return null;
      }
      Set<Point2D.Double> keySet =  l.keySet();
      Point2D.Double[] pointList = new Point2D.Double[l.size()];
      int counter = 0;
      for (Object key : keySet) {
         pointList[counter] = (Point2D.Double) key;
         counter++;
      }
      Point2D.Double closestNucleus = pointList[0];
      double d = distance(p, closestNucleus);
      for (Point2D.Double p2   : pointList) {
         double dNew = distance(p, p2);
         if (dNew < d) {
            d = dNew;
            closestNucleus = p2;
         }
      }
      return closestNucleus;
   }
   
        
   /**
    * Calculate the size of an ImageJ ROI
    */
   private long roiSize(Roi r) {
      return r.getBounds().width * r.getBounds().height;
   }



}
