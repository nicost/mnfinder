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

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.text.TextPanel;
import ij.text.TextWindow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.border.TitledBorder;

import mmcorej.TaggedImage;

import net.miginfocom.swing.MigLayout;

import org.json.JSONException;
import org.json.JSONObject;

import org.micromanager.MultiStagePosition;
import org.micromanager.PositionList;
import org.micromanager.PropertyMap;
import org.micromanager.Studio;
import org.micromanager.data.Coords;
import org.micromanager.data.Datastore;
import org.micromanager.data.Image;
import org.micromanager.data.Metadata;
import org.micromanager.data.Pipeline;
import org.micromanager.data.SummaryMetadata;
import org.micromanager.display.DisplayWindow;
import org.micromanager.events.ShutdownCommencingEvent;
import org.micromanager.internal.MMStudio;

import org.micromanager.internal.utils.FileDialogs;
import org.micromanager.internal.utils.ImageUtils;
import org.micromanager.internal.utils.MMFrame;
import org.micromanager.internal.utils.MMScriptException;
import org.micromanager.internal.utils.NumberUtils;
import org.micromanager.micronuclei.analysis.GreenCellsModule;

import org.micromanager.projector.internal.ProjectorControlForm;

import org.micromanager.micronuclei.analysis.JustNucleiModule;
import org.micromanager.micronuclei.analysis.MicroNucleiAnalysisModule;
import org.micromanager.micronuclei.analysis.NuclearSizeModule;
import org.micromanager.micronuclei.analysisinterface.AnalysisModule;
import org.micromanager.micronuclei.analysisinterface.AnalysisException;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;
import org.micromanager.micronuclei.analysisinterface.PropertyException;
import org.micromanager.micronuclei.analysisinterface.ResultRois;
import org.micromanager.micronuclei.internal.data.ChannelInfo;
import org.micromanager.micronuclei.internal.gui.ChannelPanel;
import org.micromanager.micronuclei.internal.gui.ConvertChannelPanel;
import org.micromanager.micronuclei.internal.gui.ConvertChannelTableModel;
import org.micromanager.micronuclei.internal.gui.ResultsListener;
import org.micromanager.micronuclei.internal.gui.PropertyGUI;


/**
 *
 * @author nico
 */
public class MicroNucleiForm extends MMFrame {
   
   private final Studio gui_;
   private final Font arialSmallFont_;
   private final Dimension buttonSize_;
   private final JTextField saveTextField_;
   private final ChannelPanel channelPanel_;
   private final ConvertChannelPanel convertChannelPanel_;
   private final JCheckBox doZap_;
   private final JCheckBox showMasks_;

   private final JCheckBox useOnTheFlyProcessorPipeline_;
   
   private final String FORMNAME = "Analyze and Photoconvert";
   private final String SAVELOCATION = "SaveLocation";
   private final String DOZAP = "DoZap";
   private final String SHOWMASKS = "ShowMasks";
   
   private final AtomicBoolean stop_ = new AtomicBoolean(false);
   
   private final List<AnalysisModule> analysisModules_;
   private final List<String> analysisModulesNames_;
   private final String MODULE ="ActiveModuleName";
   
   private Pipeline pipeline_;
   
   public MicroNucleiForm(final Studio gui) {
      gui_ = gui;
      final MicroNucleiForm ourForm = this;
      ToolTipManager.sharedInstance().setInitialDelay(2000);
              
      // Hard coded analysis modules.  This should be changed to make the
      // modules disoverable at run-time      
      analysisModules_ = new ArrayList<AnalysisModule>();
      // For now, whenever you write a new module, add it here
      analysisModules_.add(new MicroNucleiAnalysisModule());
      analysisModules_.add(new JustNucleiModule());
      analysisModules_.add(new GreenCellsModule());
      analysisModules_.add(new NuclearSizeModule());
      analysisModulesNames_ = new ArrayList<String>();
      for (AnalysisModule module : analysisModules_) {
         analysisModulesNames_.add(module.getName());
      }
      
      arialSmallFont_ = new Font("Arial", Font.PLAIN, 12);
      buttonSize_ = new Dimension(70, 21);

      super.setLayout(new MigLayout("flowx, fill, insets 8"));
      super.setTitle(FORMNAME);     
      
      JPanel acqPanel = new JPanel(new MigLayout(
              "flowx, fill, insets 8"));
      
      acqPanel.add(myLabel(arialSmallFont_,"Save here:"));
      
      saveTextField_ = new JTextField();
      saveTextField_.setText(gui_.profile().getString(MicroNucleiForm.class, SAVELOCATION, ""));
      saveTextField_.setMinimumSize(new Dimension(200, 12));
      acqPanel.add(saveTextField_);
      
      final JButton dirButton = myButton(buttonSize_, arialSmallFont_, "...");
      dirButton.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            dirActionPerformed(e);
         }
      });
      acqPanel.add(dirButton, "wrap");

      // TODO: the channel drop-downs are populated from the channelgroup.
      // make this clear to the user and update the contents of the dropdowns
      // when the channel group changes.
      
      acqPanel.add(myLabel(arialSmallFont_, "Imaging Channels:"), "span 3, wrap");
      channelPanel_ = new ChannelPanel(gui_, ChannelPanel.class);
      acqPanel.add(channelPanel_, "span 3, wrap");
      
      acqPanel.add(myLabel(arialSmallFont_, "Conversion Channels:"), "span 3, wrap");
      convertChannelPanel_ = new ConvertChannelPanel(gui_, ConvertChannelPanel.class);
      acqPanel.add(convertChannelPanel_, "span 3, wrap");
      
      acqPanel.setBorder(makeTitledBorder("Acquisition Settings"));
      
      super.add(acqPanel, "span 3, center, wrap");
      
            
      JPanel analysisPanel = new JPanel(new MigLayout(
              "flowx, fill, insets 8"));
      analysisPanel.setBorder(makeTitledBorder("Analysis Settings"));
      
      useOnTheFlyProcessorPipeline_ = new JCheckBox("Use On-The-Fly Processor Pipeline");
      useOnTheFlyProcessorPipeline_.setSelected(
              !gui.getDataManager().getApplicationPipelineConfigurators().isEmpty());
      useOnTheFlyProcessorPipeline_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
            if (useOnTheFlyProcessorPipeline_.isSelected()) {
               ((MMStudio) gui).getPipelineFrame().setVisible(true);
            }
         }
      });
      analysisPanel.add(useOnTheFlyProcessorPipeline_, "span 2, center, wrap");
      
      final JPanel modulePanel = new JPanel(new MigLayout(
              "flowx, fill, insets 8"));
      final JComboBox analysisModulesBox = new JComboBox (analysisModulesNames_.toArray()); 
      final JLabel analysisMethodLabel = new JLabel("Analysis Method:");
      analysisModulesBox.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            AnalysisModule module = moduleFromName(
                    (String) analysisModulesBox.getSelectedItem());
            gui_.profile().setString(MicroNucleiForm.class, MODULE, module.getName());
            analysisMethodLabel.setToolTipText(module.getDescription());
            analysisModulesBox.setToolTipText(module.getDescription());
            modulePanel.removeAll();
            modulePanel.setBorder(makeTitledBorder(module.getName()));
            modulePanel.setToolTipText(module.getDescription());
      
            for (AnalysisProperty ap : module.getAnalysisProperties()) {
               JLabel jl = new JLabel(ap.getName());
               if (ap.getTooltip() != null) {
                  jl.setToolTipText(ap.getTooltip());
               }
               modulePanel.add(jl);
               modulePanel.add(new PropertyGUI(ap).getJComponent(), "width 70px, wrap");
            }
            modulePanel.revalidate();
            ourForm.pack();
         }
      });
      
      String moduleName = gui_.profile().getString(MicroNucleiForm.class, 
              MODULE, analysisModulesNames_.get(0));
      analysisModulesBox.setSelectedItem(moduleName);
      if (! moduleName.equals(analysisModulesBox.getSelectedItem())) {
         moduleName = (String) analysisModulesBox.getItemAt(0);
      }
      AnalysisModule module = moduleFromName(moduleName);
      analysisMethodLabel.setToolTipText(module.getDescription());
      analysisModulesBox.setToolTipText(module.getDescription());

      analysisPanel.add(analysisMethodLabel);
      analysisPanel.add(analysisModulesBox, "center, wrap");
            
      super.add(analysisPanel, "span 3, center, wrap");
      super.add(modulePanel, "span 3, center, wrap");
      
      
      doZap_ = new JCheckBox("Zap");
      doZap_.setSelected(gui_.profile().getBoolean(MicroNucleiForm.class, DOZAP, false));
      doZap_.setFont(arialSmallFont_);
      doZap_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
              gui_.profile().setBoolean(MicroNucleiForm.class, DOZAP, doZap_.isSelected());
         }
      });
      super.add (doZap_);
      
      showMasks_  = new JCheckBox("Show Masks");
      showMasks_.setSelected (gui_.profile().getBoolean(MicroNucleiForm.class, 
              SHOWMASKS, false));
      showMasks_.setFont(arialSmallFont_);
      showMasks_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
              gui_.profile().setBoolean(MicroNucleiForm.class, SHOWMASKS, showMasks_.isSelected());
         }
      });
      super.add (showMasks_, "wrap");
      
            
      final JButton runButton = myButton(buttonSize_, arialSmallFont_, "Run");
      runButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            RunAll myThread = new RunAll();
            myThread.init(false);

         }
      });
      super.add(runButton, "span 3, split 3, center");

      final JButton stopButton = myButton(buttonSize_, arialSmallFont_, "Stop");
      stopButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            stop_.set(true);
         }
      } );
      super.add(stopButton, "center");
      
      final JButton testButton = myButton(buttonSize_, arialSmallFont_, "Test");
      testButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            RunAll myThread = new RunAll();
            myThread.init(true);
         }
      } );
      super.add(testButton, "center, wrap");
            

      super.loadAndRestorePosition(100, 100, 350, 250);
      
      super.pack();
      
      super.setResizable(false);

   }
   
   @Subscribe
   public void onShutdownCommencing(ShutdownCommencingEvent cse) {
      this.dispose();
   } 
   
   @Override
   public void dispose() {
      try {
         gui_.events().unregisterForEvents(this);
      } catch (java.lang.IllegalArgumentException iae) {}

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
         String channelGroup = gui_.getCMMCore().getChannelGroup();
         String[] channels = gui_.getCMMCore().
                 getAvailableConfigs(channelGroup).toArray();
         for (String channel : channels) {
            box.addItem(channel);
         }
      } catch (Exception ex) {
         gui_.getLogManager().logError(ex);
      }
      box.setSelectedItem(selectedChannel);
   }

   private void dirActionPerformed(java.awt.event.ActionEvent evt) {                                                  
      File f = FileDialogs.openDir(this, "Save location",
              new FileDialogs.FileType("Dir", "Save Location",
              saveTextField_.getText(), true, "") );
      if (f != null) {
         saveTextField_.setText(f.getAbsolutePath());
         gui_.profile().setString(MicroNucleiForm.class, SAVELOCATION, f.getAbsolutePath());    
      }
   }   
   
         
   /**
    * Looks for a module with the given name in our list of 
    * analysisModules
    * @param name - desired name of analysis module
    * @return the first AnalysisModule with this name or null if not found
    */
   private AnalysisModule moduleFromName(String name) {
      for (AnalysisModule am : analysisModules_) {
         if (am.getName().equals(name)) {
            return am;
         }
      }
      return null;
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
               runAnalysisAndZapping(saveTextField_.getText());
            } else {
               runTest();
            }
               
         } catch (MMScriptException ex) {
            gui_.getLogManager().showError(ex, "Error during acquisition");
         } catch (org.micromanager.data.DatastoreRewriteException drex) {
            gui_.getLogManager().showError(drex, "Failed to save data.");
         } catch (Exception ex) {
            gui_.getLogManager().showError(ex, "Error during acquisition");
         } finally {
            running_ = false;
         }
      }
     
      public void init(boolean testing) {
         if (running_) {
            return;
         }
         testing_ = testing;
         stop_.set(false);
         Thread t = new Thread(this);
         t.start();
      }
   }
   
   /**
    * Runs the selected analysis test on the currently selected image
    * If this is a Micro-Manager window, the code will be run on all positions
    * otherwise only on the current image
    * Results will be shown in an ImageJ ResultsTable which has graphical feedback
    * to the original image
    * 
    * @throws MMScriptException
    * @throws JSONException 
    * @throws org.micromanager.micronuclei.analysisinterface.PropertyException 
    */
   public void runTest() throws MMScriptException, JSONException, PropertyException {
      ImagePlus ip;
      try {
         ip = IJ.getImage();
      } catch (Exception ex) {
         return;
      }
      
      AnalysisModule analysisModule = moduleFromName(gui_.profile().getString(MicroNucleiForm.class, 
              MODULE, ""));
      if (analysisModule == null) {
         throw new MMScriptException("AnalysisModule not found");
      }
      
      ResultsTable outTable = new ResultsTable();
      String outTableName = Terms.RESULTTABLENAME;
      Window oldOutTable = WindowManager.getWindow(outTableName);
      if (oldOutTable != null) {
         WindowManager.removeWindow(oldOutTable);
         oldOutTable.dispose();
      }

      JSONObject parms = analysisSettings(showMasks_.isSelected());

      DisplayWindow dw = gui_.displays().getCurrentWindow();

      try {
         if (dw == null || ip != dw.getImagePlus()) {
            // ImageJ window.  Forget everything about MM windows:
            dw = null;
            TaggedImage tImg = ImageUtils.makeTaggedImage(ip.getProcessor());
            tImg.tags.put("PixelSizeUm", ip.getCalibration().pixelWidth);
            Image[] imgs = new Image[1];
            imgs[0] = gui_.data().convertTaggedImage(tImg);
            ResultRois rr = analysisModule.analyze(gui_, imgs, ip.getRoi(), parms);
            RoiManager.getInstance2().reset();
            for (Roi roi : rr.getHitRois()) {
               outTable.incrementCounter();
               Rectangle bounds = roi.getBounds();
               int x = bounds.x + (int) (0.5 * bounds.width);
               int y = bounds.y + (int) (0.5 * bounds.height);
               outTable.addValue(Terms.X, x);
               outTable.addValue(Terms.Y, y);
               outTable.show(outTableName);
               RoiManager.getInstance2().addRoi(roi);
            }
            if (parms.getBoolean(AnalysisModule.SHOWMASKS)) {
               RoiManager.getInstance2().runCommand("Show All");
            }
            

         } else { // MM display
            Datastore store = dw.getDatastore();
            Roi userRoi = dw.getImagePlus().getRoi();
            if (parms.getBoolean(AnalysisModule.SHOWMASKS)) {
               RoiManager.getInstance().runCommand("Show All");
               dw.getImagePlus().setRoi(userRoi);
            }
            
            Coords.CoordsBuilder builder = store.getAnyImage().getCoords().copy();
            builder.channel(0);
            int nrPositions = store.getAxisLength(Coords.STAGE_POSITION); 
            int nrChannels = store.getAxisLength(Coords.CHANNEL);
            Image[] imgs = new Image[nrChannels];
            for (int p = 0; p < nrPositions && !stop_.get(); p++) {
               try {
                  for (int ch = 0; ch < nrChannels; ch++) {
                     Coords coords = builder.stagePosition(p).channel(ch).build();
                     imgs[ch] = store.getImage(coords);
                  }
                  if (imgs[0] != null) {
                     dw.setDisplayedImageTo( builder.stagePosition(p).channel(0).build());
                     ResultRois rr = analysisModule.analyze(gui_, imgs, userRoi, parms);
                     if (parms.getBoolean(AnalysisModule.SHOWMASKS)) {
                        RoiManager.getInstance().reset();
                     }
                     if (rr != null) {
                        Roi[] hitRois = rr.getHitRois();
                        if (hitRois != null) {
                           for (Roi roi : rr.getHitRois()) {
                              outTable.incrementCounter();
                              Rectangle bounds = roi.getBounds();
                              int x = bounds.x + (int) (0.5 * bounds.width);
                              int y = bounds.y + (int) (0.5 * bounds.height);
                              outTable.addValue(Terms.X, x);
                              outTable.addValue(Terms.Y, y);
                              outTable.addValue(Terms.POSITION, p);
                              if (parms.getBoolean(AnalysisModule.SHOWMASKS)) {
                                 RoiManager.getInstance().addRoi(roi);
                              }
                           }
                           outTable.show(outTableName);

                           if (rr.getAllRois() != null) {
                              gui_.alerts().postAlert(FORMNAME, MicroNucleiForm.class,
                                      "Analyzed " + rr.getAllRois().length
                                      + " objects, found " + rr.getHitRois().length
                                      + " objects to be photo-converted at position " + p);
                           }
                        }
                     }
                  }

               } catch (JSONException ex) {
               } catch (NullPointerException npe) {
                  ij.IJ.log("Null pointer exception at position : " + p);
               }
            }
         }
      } catch (AnalysisException ex) {
         gui_.logs().showError(ex);
      }
      gui_.alerts().postAlert(FORMNAME, MicroNucleiForm.class,
                          "Analyzed " + parms.getString(AnalysisModule.CELLCOUNT)
                          + " objects, found " + parms.getString(AnalysisModule.OBJECTCOUNT)
                          + " objects to be photo-converted");

      // we have the ROIs, the rest is just reporting
        
      // add listeners to our ResultsTable that let user click on row and go 
      // to cell that was found
      TextPanel tp;
      TextWindow win;
      Window frame = WindowManager.getWindow(outTableName);
      if (frame != null && frame instanceof TextWindow) {
         win = (TextWindow) frame;
         tp = win.getTextPanel();

         ResultsListener myk = new ResultsListener(ip, dw, outTable, win);
         tp.addKeyListener(myk);
         tp.addMouseListener(myk);
         frame.toFront();
         frame.setVisible(true);
      }
      
      gui_.alerts().postAlert(FORMNAME, MicroNucleiForm.class, 
             "Analyzed " + parms.getString(AnalysisModule.CELLCOUNT) + 
              " objects, found " + parms.getString(AnalysisModule.OBJECTCOUNT) +
                      " objects to be photo-converted" );
      
      
   }
   
   /**
    * @param saveLocation
    * @throws IOException
    * @throws MMScriptException
    * @throws Exception 
    */
   public void runAnalysisAndZapping(String saveLocation) throws IOException, MMScriptException, Exception {
      
      String analysisModuleName = gui_.profile().getString(MicroNucleiForm.class, 
              MODULE, "");
      AnalysisModule analysisModule = moduleFromName(analysisModuleName);
      if (analysisModule == null) {
         throw new MMScriptException(analysisModuleName + "was not found");
      }
          
      String channelGroup = gui_.getCMMCore().getChannelGroup();
      
      //TODO: error checking for file IO!
      File fd = new File(saveLocation);
      if (fd.exists()) {
         if (! IJ.showMessageWithCancel("Save location already exists.", 
                 saveLocation + " already exists.  Overwrite?") ) {
            return;
         }
         if (!delete(fd)) {
            return;
         }
      }
      
      new File(saveLocation).mkdirs();
      File summaryFile = new File(saveLocation + File.separator + "summary.txt");
      summaryFile.createNewFile();
      BufferedWriter resultsWriter = new BufferedWriter(new FileWriter(summaryFile));
      // open the output file to write the measuerements to
      // TODO: Add background cells
      File dataFile = new File(saveLocation + File.separator + "data.txt");
      dataFile.createNewFile();
      BufferedWriter dataWriter = new BufferedWriter(new FileWriter(dataFile));
      dataWriter.write ("Well" + "\t" + "Site" + "\t" + "ID" + "\t" + "Pre-Post-Status" + "\t" + 
              "X" + "\t" + "Y" + "\t" + "Mean" + "\t" + "Area");
      dataWriter.newLine();

      PositionList posList = gui_.getPositionListManager().getPositionList();
      MultiStagePosition[] positions = posList.getPositions();
      if (positions.length == 0) {
         // TODO: get current position
         gui_.positions().markCurrentPosition();
         positions = posList.getPositions();
         posList.clearAllPositions();
      }
      String currentWell = "";
      
      ResultsTable outTable = new ResultsTable();
      String outTableName = Terms.RESULTTABLENAME;
      Window oldOutTable = WindowManager.getWindow(outTableName);
      if (oldOutTable != null) {
         WindowManager.removeWindow(oldOutTable);
         oldOutTable.dispose();
      }
      
      int nrImagesPerWell = 0;
      int wellCount = 0;
      
      // figure out how many sites per well there are, we actually get that number 
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
      gui_.logs().logMessage("Images per well: " + nrImagesPerWell);
      
      // start cycling through the sites and group everything by well
      int count = 0;
      int siteCount = 0;
      JSONObject parms = analysisSettings(showMasks_.isSelected());
      currentWell = "";
      

      double originalExposure = gui_.getCMMCore().getExposure();
      
      // prepare stuff needed to store data in MM
      Datastore data = null;
      DisplayWindow dw = null;

      int nrChannels = 0;
      List<String> channelNames = new ArrayList<String>();
      for (ChannelInfo ci : channelPanel_.getChannels()) {
         if (ci.use_) {
            channelNames.add(ci.channelName_);
            nrChannels++;
         }
      }
      for (ChannelInfo ci : convertChannelPanel_.getChannels()) {
         if (ci.use_) {
            channelNames.add(ci.channelName_);
            nrChannels++;
         }
      }

      SummaryMetadata.SummaryMetadataBuilder smb = gui_.data().getSummaryMetadataBuilder();
      smb = smb.channelNames(channelNames.toArray(new String[channelNames.size()])).
              channelGroup(channelGroup).
              microManagerVersion(gui_.compat().getVersion()).
              prefix("MicroNucleiScreen").
              startDate((new Date()).toString()).
              intendedDimensions(gui_.data().getCoordsBuilder().
                  channel(nrChannels).
                  z(0).
                  time(0).
                  stagePosition(nrImagesPerWell).
              build() );

      PropertyMap.PropertyMapBuilder pmb = gui_.data().getPropertyMapBuilder();

      for (MultiStagePosition msp : positions) {
         if (stop_.get()) {
            resultsWriter.close();
            return;
         }
         String label = msp.getLabel();
         String well = label.split("-")[0];
         if (!currentWell.equals(well)) {
            // new well
            // freeze the existing datastore, but only if it exists
            // TODO: to avoid running out of memory, we may need to call 
            // close here instead of freeze
            if (data != null) {
               data.close();
            }
            gui_.logs().logMessage("Starting well: " + well);
            if (!currentWell.equals("")) {
               recordWellSummary(resultsWriter, currentWell, parms);
            }
            currentWell = well;
            siteCount = 0;

            
            data = gui_.data().createMultipageTIFFDatastore(saveLocation + 
                    File.separator + well, true, false);
            data.setSummaryMetadata(smb.build());
            dw = gui_.displays().createDisplay(data);
            gui_.displays().manage(data);
            if (useOnTheFlyProcessorPipeline_.isSelected()) {
               // Create a blocking pipeline
               pipeline_ = gui_.data().copyApplicationPipeline(data, true);
            }
            analysisModule.reset();
            // reset cell and object counters
            parms.put(AnalysisModule.CELLCOUNT, 0);
            parms.put(AnalysisModule.OBJECTCOUNT, 0);
         }
                 
         if (data != null) {
            int currentChannel = 0;
            Image[] imgs = new Image[nrChannels];
            MultiStagePosition.goToPosition(msp, gui_.getCMMCore());
            for (ChannelInfo ci : channelPanel_.getChannels()) {
               if (ci.use_) {
                  gui_.getCMMCore().waitForSystem();
                  gui_.logs().logMessage("Site: " + msp.getLabel() + ", x: " + msp.get(0).x + ", y: " + msp.get(0).y);
                  gui_.getCMMCore().setConfig(channelGroup, ci.channelName_);
                  gui_.getCMMCore().waitForConfig(channelGroup, ci.channelName_);
                  gui_.getCMMCore().setExposure(ci.exposureTimeMs_);
                  imgs[currentChannel] = snapAndInsertImage(data, msp, siteCount, currentChannel);
                  currentChannel++;
               }
            }

            // Analyze and zap
            ResultRois rr = analysisModule.analyze(gui_, imgs, null, parms);
            if (rr.getHitRois() != null && rr.getHitRois().length != 0 && 
                    doZap_.isSelected()) {
               
               // Report imaging channel intensities
               for (int nr : rr.getImgsToBeReported()) {
                  ImageProcessor iProcessortmp = gui_.data().ij().createProcessor(imgs[nr]);
                  ImagePlus ipGFP = new ImagePlus("tmp", iProcessortmp);
                  reportIntensities(dataWriter, currentWell, siteCount, ipGFP, "Hit-ch." + nr,
                          rr.getHitRois());
                  reportIntensities(dataWriter, currentWell, siteCount, ipGFP, "NoHit-ch." + nr,
                          rr.getNonHitRois());
               }
               
               String acq2 = msp.getLabel();
               gui_.logs().logMessage("Imaging cells to be zapped at site: " + acq2);
               
               for (int i = 0; i < convertChannelPanel_.getChannels().size(); i++) {
                  ChannelInfo ci = convertChannelPanel_.getChannels().get(i);
                  if (ci.use_) {
                     gui_.getCMMCore().waitForSystem();
                     gui_.logs().logMessage("Site: " + msp.getLabel() + ", x: " + msp.get(0).x + ", y: " + msp.get(0).y);
                     if (i == 1) { // zap channel
                        zap(rr.getHitRois());  // send ROIs to the device
                     }
                     gui_.getCMMCore().setConfig(channelGroup, ci.channelName_);
                     gui_.getCMMCore().waitForConfig(channelGroup, ci.channelName_);
                     gui_.getCMMCore().setExposure(ci.exposureTimeMs_);
                     imgs[currentChannel] = snapAndInsertImage(data, msp, siteCount, currentChannel);
                     if (rr.getZapChannelsToBeReported().contains(i)) {
                        ImageProcessor iProc = gui_.data().ij().createProcessor(imgs[currentChannel]);
                        ImagePlus ip = new ImagePlus("tmp", iProc);
                        reportIntensities(dataWriter, currentWell, siteCount, ip, 
                                ConvertChannelTableModel.PURPOSES[i] + "-Hit",
                                rr.getHitRois());
                        reportIntensities(dataWriter, currentWell, siteCount, ip, 
                                ConvertChannelTableModel.PURPOSES[i] + "-NoHit",
                                rr.getNonHitRois());
                     }
                     currentChannel++;
                  }
               }
               
               for (Roi roi : rr.getHitRois()) {
                  outTable.incrementCounter();
                  Rectangle bounds = roi.getBounds();
                  int x = bounds.x + (int) (0.5 * bounds.width);
                  int y = bounds.y + (int) (0.5 * bounds.height);
                  outTable.addValue(Terms.X, x);
                  outTable.addValue(Terms.Y, y);
                  outTable.addValue(Terms.POSITION, siteCount);
               }
               outTable.show(outTableName);                             
            }
            siteCount++;
            count++;
         }
      }
      gui_.getCMMCore().setExposure(originalExposure);

      // add listeners to our ResultsTable that let user click on row and go 
      // to cell that was found
      TextPanel tp;
      TextWindow win;
      Window frame = WindowManager.getWindow(outTableName);
      if (frame != null && frame instanceof TextWindow) {
         win = (TextWindow) frame;
         tp = win.getTextPanel();

         ResultsListener myk = new ResultsListener(IJ.getImage(), dw, outTable, win);
         tp.addKeyListener(myk);
         tp.addMouseListener(myk);
         frame.toFront();
         frame.setVisible(true);
      }

      // record the results from the last well:
      recordWellSummary(resultsWriter, currentWell, parms);

      resultsWriter.close();
      dataWriter.close();
      if (data != null) {
         data.freeze();
      }
      String msg = "Analyzed " + count + " images, in " + wellCount + " wells.";
      gui_.logs().logMessage(msg);
      gui_.logs().showMessage(msg);
   }

   /**
    * Be vary careful with this function as it will follow symlinks and delete
    * everything it finds
   */
   private boolean delete(File f) throws IOException {
      if (f.isDirectory()) {
         for (File c : f.listFiles()) {
            if (!delete(c) ) {
               return false;
            }
         }
      }
      if (!f.delete()) {
         gui_.logs().showError("Failed to delete " + f.getName());
         return false;
      }
      return true;
   }

   private void recordWellSummary(BufferedWriter resultsWriter, String currentWell,
           final JSONObject parms) throws IOException, MMScriptException {
      resultsWriter.write(currentWell + "\t"
              + parms.optInt(AnalysisModule.CELLCOUNT) + "\t"
              + parms.optInt(AnalysisModule.OBJECTCOUNT));
      resultsWriter.newLine();
      resultsWriter.flush();
      gui_.logs().logMessage(currentWell + " " + parms.optInt(AnalysisModule.CELLCOUNT)
              + "    " + parms.optInt(AnalysisModule.OBJECTCOUNT));
   }

   /**
    * Snaps an image and insert it into the given datastore
    * @param data - datastore into which to insert images
    * @param siteCount - Position Nr to be used to insert into store
    * @param msp - Current Multistageposition
    * @param channelNr - Channel Nr to be used to insert into store.
    * @throws Exception 
    */
   private Image snapAndInsertImage(Datastore data, MultiStagePosition msp,
            int siteCount, int channelNr) throws Exception {
      List<Image> snap = gui_.acquisitions().snap();
      Coords coord = gui_.data().createCoords("t=0,p=" + siteCount + 
              ",c=" + channelNr + ",z=0");
      Image img = snap.get(0).copyAtCoords(coord);
     
      Metadata md = img.getMetadata();
      Metadata.MetadataBuilder mdb = md.copy();
      PropertyMap ud = md.getUserData();

      mdb = mdb.xPositionUm(msp.getX()).yPositionUm(msp.getY());

      md = mdb.positionName(msp.getLabel()).userData(ud).build();
      img = img.copyWith(coord, md);

       if (pipeline_ != null) {
         pipeline_.insertImage(img);
      } else {
         data.putImage(img);
       }
      
      return data.getImage(coord);
   }
   
   /**
    * Generates an initialized JSONObject to be used to communicate analysis
    * settings
    *
    * @param showMask - whether or not to show the masks during analysis
    * @return initialized JSONObject with default analysis settings
    * @throws JSONException
    */
   private JSONObject analysisSettings(boolean showMask) throws JSONException {
      JSONObject parms = new JSONObject();
      parms.put(AnalysisModule.SHOWMASKS, showMask);
      parms.put(AnalysisModule.CELLCOUNT, 0);
      parms.put(AnalysisModule.OBJECTCOUNT, 0);
      return parms;
   }

   /**
    * Photoconverts the provided ROIs
    *
    * @param rois
    * @throws MMScriptException
    */
   private void zap(Roi[] rois) throws MMScriptException {
      if (rois == null) {
         return;
      }
      ProjectorControlForm pcf
              = ProjectorControlForm.showSingleton(gui_.getCMMCore(), gui_);
      int i;
      // convert zapRois in a Roi[] of Polygon Rois
      for (i = 0; i < rois.length; i++) {
         Polygon poly = rois[i].getConvexHull();
         rois[i] = new PolygonRoi(poly, Roi.POLYGON);
      }

      // send to the galvo device and zap them for real
      gui_.logs().logMessage("Zapping " + (i + 1) + " of " + rois.length);
      pcf.setROIs(rois);
      pcf.updateROISettings();
      pcf.getDevice().waitForDevice();
      pcf.runRois();
      pcf.getDevice().waitForDevice();
      
      /*  The following was written for a Galvo conversion system
      pcf.setNrRepetitions(5);
      for (i = 0; i < rois.length; i++) {
         gui_.logs().logMessage("Zapping " + (i + 1) + " of " + rois.length);
         Roi[] theRois = {rois[i]};
         pcf.setROIs(theRois);
         pcf.updateROISettings();
         pcf.getDevice().waitForDevice();
         pcf.runRois();
         pcf.getDevice().waitForDevice();
      }
      */

   }
   
   /**
    * makes border with centered title text
    * @param title
    * @return
    */
   public static TitledBorder makeTitledBorder(String title) {
      TitledBorder myBorder = BorderFactory.createTitledBorder(
              BorderFactory.createLineBorder(Color.gray), title);
      myBorder.setTitleJustification(TitledBorder.CENTER);
      return myBorder;
   }
        
   private void reportIntensities(BufferedWriter theFile, String well, 
           int posCounter, ImagePlus ip, String label, Roi[] rois) {
   if (rois == null)
         return;
	for (int i = 0; i < rois.length; i++) {
		ip.setRoi(rois[i]);
      ImageStatistics stats = ip.getStatistics(ImagePlus.CENTROID + ImagePlus.MEAN + ImagePlus.INTEGRATED_DENSITY + ImagePlus.AREA);
      try {
         theFile.write(well + "\t" + posCounter + "\t" + i + "\t" + label + "\t" +
                 NumberUtils.doubleToDisplayString(stats.xCentroid) + "\t" +
                 NumberUtils.doubleToDisplayString(stats.yCentroid) + "\t" +
                 NumberUtils.doubleToDisplayString(stats.mean) + "\t"  +
                 NumberUtils.doubleToDisplayString(stats.area) );
         theFile.newLine();
      } catch (IOException ex) {
         Logger.getLogger(MicroNucleiForm.class.getName()).log(Level.SEVERE, null, ex);
      }

	}
}
 
}