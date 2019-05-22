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
import ij.process.FloatPolygon;
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
import java.awt.geom.AffineTransform;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

import org.json.JSONException;
import org.json.JSONObject;

import org.micromanager.MultiStagePosition;
import org.micromanager.PositionList;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;
import org.micromanager.Studio;
import org.micromanager.data.Coordinates;
import org.micromanager.data.Coords;
import org.micromanager.data.Coords.CoordsBuilder;
import org.micromanager.data.DataProvider;
import org.micromanager.data.Datastore;
import org.micromanager.data.Image;
import org.micromanager.data.Metadata;
import org.micromanager.data.Pipeline;
import org.micromanager.data.PipelineErrorException;
import org.micromanager.data.SummaryMetadata;
import org.micromanager.data.internal.DefaultImage;
import org.micromanager.display.ChannelDisplaySettings;
import org.micromanager.display.DisplaySettings;
import org.micromanager.display.DisplayWindow;
import org.micromanager.events.ShutdownCommencingEvent;
import org.micromanager.internal.MMStudio;

import org.micromanager.internal.utils.FileDialogs;
import org.micromanager.internal.utils.MMFrame;
import org.micromanager.internal.utils.MMScriptException;
import org.micromanager.internal.utils.NumberUtils;
import org.micromanager.internal.utils.ReportingUtils;
import org.micromanager.micronuclei.analysis.GreenCellsModule;

import org.micromanager.projector.internal.ProjectorControlForm;

import org.micromanager.micronuclei.analysis.JustNucleiModule;
import org.micromanager.micronuclei.analysis.MicroNucleiAnalysisModule;
import org.micromanager.micronuclei.analysis.NuclearSizeModule;
import org.micromanager.micronuclei.analysis.NucleoCytoplasmicRatio;
import org.micromanager.micronuclei.analysisinterface.AnalysisModule;
import org.micromanager.micronuclei.analysisinterface.AnalysisException;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;
import org.micromanager.micronuclei.analysisinterface.PropertyException;
import org.micromanager.micronuclei.analysisinterface.ResultRois;
import org.micromanager.micronuclei.internal.data.ChannelInfo;
import org.micromanager.micronuclei.internal.gui.ChannelPanel;
import org.micromanager.micronuclei.internal.gui.ConvertChannelPanel;
import org.micromanager.micronuclei.internal.gui.ResultsListener;
import org.micromanager.micronuclei.internal.gui.PropertyGUI;
import org.micromanager.projector.ProjectionDevice;
import org.micromanager.projector.ProjectorActions;
import org.micromanager.propertymap.MutablePropertyMapView;


/**
 *
 * @author nico
 */
public class MicroNucleiForm extends MMFrame {
   
   private final Studio gui_;
   private final MutablePropertyMapView settings_;
   private final Font arialSmallFont_;
   private final Dimension buttonSize_;
   private final JTextField saveTextField_;
   private final ChannelPanel channelPanel_;
   private final ConvertChannelPanel convertChannelPanel_;
   private final JCheckBox doZap_;
   private final JCheckBox showMasks_;
   private final JCheckBox useDisplay_;

   private final JCheckBox useOnTheFlyProcessorPipeline_;
   
   private final String FORMNAME = "Analyze and Photoconvert";
   private final String SAVELOCATION = "SaveLocation";
   private final String DOZAP = "DoZap";
   private final String SHOWMASKS = "ShowMasks";
   private final String USEDISPLAY = "Use Display";
   
   private final AtomicBoolean stop_ = new AtomicBoolean(false);
   
   private final List<AnalysisModule> analysisModules_;
   private final List<String> analysisModulesNames_;
   private final String MODULELIST = "ActiveModuleList";
   
   private Pipeline pipeline_;
   private long startTime_;
   
   private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

   
   public MicroNucleiForm(final Studio gui) {
      gui_ = gui;
      final MicroNucleiForm ourForm = this;
      ToolTipManager.sharedInstance().setInitialDelay(2000);
      settings_ = gui_.profile().getSettings(MicroNucleiForm.class);
              
      // Hard coded analysis modules.  This should be changed to make the
      // modules disoverable at run-time      
      analysisModules_ = new ArrayList<>();
      // For now, whenever you write a new module, add it here
      analysisModules_.add(new MicroNucleiAnalysisModule());
      analysisModules_.add(new JustNucleiModule());
      analysisModules_.add(new GreenCellsModule());
      analysisModules_.add(new NuclearSizeModule());
      analysisModules_.add(new NucleoCytoplasmicRatio());
      analysisModulesNames_ = new ArrayList<>();
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
      saveTextField_.setText(settings_.getString(SAVELOCATION, ""));
      saveTextField_.setMinimumSize(new Dimension(200, 12));
      acqPanel.add(saveTextField_);
      
      final JButton dirButton = myButton(buttonSize_, arialSmallFont_, "...");
      dirButton.addActionListener((ActionEvent e) -> {
         dirActionPerformed(e);
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
              !gui.getDataManager().getApplicationPipelineConfigurators(false).isEmpty());
      useOnTheFlyProcessorPipeline_.addActionListener((ActionEvent ae) -> {
         if (useOnTheFlyProcessorPipeline_.isSelected()) {
            ((MMStudio) gui).getPipelineFrame().setVisible(true);
         }
      });
      analysisPanel.add(useOnTheFlyProcessorPipeline_, "span 2, center, wrap");
      
      final JTabbedPane modulesPane = new JTabbedPane();
      final List<String> modulesInUse = settings_.getStringList(
              MODULELIST, "");
      
      final JComboBox analysisModulesBox = new JComboBox (unUsedModules(
              analysisModules_, modulesInUse).toArray()); 
      final JButton addModuleButton = new JButton ("Add");
      addModuleButton.addActionListener((ActionEvent e) -> {
         String moduleName = (String) analysisModulesBox.getSelectedItem();
         boolean found = false;
         for (int i = 0; i < analysisModules_.size() && !found; i++) {
            if (analysisModules_.get(i).getName().equals(moduleName)) {
               found = true;
               modulesInUse.add(moduleName);
               settings_.putStringList(MODULELIST, modulesInUse);
               JPanel panel =  makeModulePanel(new JPanel(new MigLayout(
                       "flowx, fill, insets 8")),  analysisModules_.get(i));
               modulesPane.addTab(moduleName, panel);
               analysisModulesBox.removeItem(moduleName);
               convertChannelPanel_.addConvertChannel();
               ourForm.pack();
            }
         }
      });
      
      final JButton removeModuleButton = new JButton("Remove");
      removeModuleButton.addActionListener((ActionEvent e) -> {
         String moduleName = modulesPane.getTitleAt(modulesPane.getTabCount() - 1);
         modulesPane.remove(modulesPane.getTabCount() - 1);
         modulesInUse.remove(moduleName);
         settings_.putStringList(MODULELIST, modulesInUse);
         analysisModulesBox.addItem(moduleName);
         convertChannelPanel_.removeConvertChannel();
         ourForm.pack();
      });
      
      analysisPanel.add(analysisModulesBox);
      analysisPanel.add(addModuleButton, "wrap");
      analysisPanel.add(removeModuleButton, "skip 1, wrap");
    
      for (int i = 0; i < analysisModules_.size(); i++) {
         AnalysisModule module = analysisModules_.get(i);
         String newModule = module.getName();
         if (modulesInUse.contains(newModule)) {
            JPanel panel = makeModulePanel(new JPanel(new MigLayout(
                    "flowx, fill, insets 8")), module);
            convertChannelPanel_.addConvertChannel();   
            modulesPane.addTab(newModule, panel);
         }
      }
                      
      super.add(analysisPanel, "span 3, center, wrap");
      super.add(modulesPane, "span 3, center, wrap");
      
      
      doZap_ = new JCheckBox("Zap");
      doZap_.setSelected(settings_.getBoolean(DOZAP, false));
      doZap_.setFont(arialSmallFont_);
      doZap_.addActionListener((ActionEvent ae) -> {
         settings_.putBoolean(DOZAP, doZap_.isSelected());
      });
      super.add (doZap_);
      
      useDisplay_ = new JCheckBox("Use display");
      useDisplay_.setSelected(settings_.getBoolean(USEDISPLAY, true));
      useDisplay_.setFont(arialSmallFont_);
      useDisplay_.addActionListener((ActionEvent ae) -> {
         settings_.putBoolean(USEDISPLAY, useDisplay_.isSelected());
      });
      super.add (useDisplay_);
      
      showMasks_  = new JCheckBox("Show Masks");
      showMasks_.setSelected (settings_.getBoolean(SHOWMASKS, false));
      showMasks_.setFont(arialSmallFont_);
      showMasks_.addActionListener((ActionEvent ae) -> {
         settings_.putBoolean(SHOWMASKS, showMasks_.isSelected());
      });
      super.add (showMasks_, "wrap");
      
            
      final JButton runButton = myButton(buttonSize_, arialSmallFont_, "Run");
      runButton.addActionListener((ActionEvent e) -> {
         settings_.putString(SAVELOCATION, saveTextField_.getText());
         RunAll myThread = new RunAll();
         myThread.init(false);
      });
      super.add(runButton, "span 3, split 3, center");

      final JButton stopButton = myButton(buttonSize_, arialSmallFont_, "Stop");
      stopButton.addActionListener((ActionEvent e) -> {
         stop_.set(true);
      });
      super.add(stopButton, "center");
      
      final JButton testButton = myButton(buttonSize_, arialSmallFont_, "Test");
      testButton.addActionListener((ActionEvent e) -> {
         RunAll myThread = new RunAll();
         myThread.init(true);
      });
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
   
   private List<String> unUsedModules(List<AnalysisModule> analysisModules, 
           List<String> modulesInUse) {
      List<String> unUsedModules = new ArrayList<>();
      for (AnalysisModule module : analysisModules) {
         if (!modulesInUse.contains(module.getName())) {
            unUsedModules.add(module.getName());
         }
      }
      
      return unUsedModules;
   }

   private JPanel makeModulePanel(JPanel modulePanel, AnalysisModule module) {
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
      
      return modulePanel;
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
         settings_.putString(SAVELOCATION, f.getAbsolutePath());    
      }
   }   
   
         
   /**
    * Looks for modules with the given names in our list of 
    * analysisModules
    * Maintains the same order as the input list of names
    * @param name - desired name of analysis module
    * @return the first AnalysisModule with this name or null if not found
    */
   private List<AnalysisModule> modulesFromNames(List<String> names) {
      List<AnalysisModule> lam = new ArrayList<>();
      for (String name : names) {
         for (AnalysisModule am : analysisModules_) {
            if (am.getName().equals(name)) {
               lam.add(am);
            }
         }
      }
      return lam;
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
         }
         catch (AnalysisException ae) {
            gui_.logs().showError(ae.getMessage());
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
    * Runs the selected analysis test on the currently selected image If this is
    * a Micro-Manager window, the code will be run on all positions otherwise
    * only on the current image Results will be shown in an ImageJ ResultsTable
    * which has graphical feedback to the original image
    *
    * @throws MMScriptException
    * @throws JSONException
    * @throws org.micromanager.micronuclei.analysisinterface.PropertyException
    */
   public void runTest() throws MMScriptException, JSONException, PropertyException {

      final List<String> modulesInUse = settings_.getStringList(
              MODULELIST, "");
      final List<AnalysisModule> analysisModules = modulesFromNames(modulesInUse);

      if (analysisModules.isEmpty()) {
         throw new MMScriptException("No AnalysisModule used");
      }

      for (AnalysisModule analysisModule : analysisModules) {

         ImagePlus ip = null;
         ResultsTable outTable = new ResultsTable();
         String outTableName = analysisModule.getName() + "-" + Terms.RESULTTABLENAME;
         Window oldOutTable = WindowManager.getWindow(outTableName);
         if (oldOutTable != null) {
            WindowManager.removeWindow(oldOutTable);
            oldOutTable.dispose();
         }

         JSONObject parms = analysisSettings(showMasks_.isSelected());

         DisplayWindow dw = gui_.displays().getCurrentWindow();

         try {
            if (dw == null) {
               // ImageJ window.  Forget everything about MM windows:
               dw = null;
               try {
                  ip = IJ.getImage();
               } catch (Exception ex) {
                  return;
               }

               Metadata.Builder mb = gui_.data().getMetadataBuilder();
               mb.pixelSizeUm(ip.getCalibration().pixelWidth);
               CoordsBuilder cb = Coordinates.builder();
               cb.channel(0).stagePosition(0).time(0).z(0);
               Image[] imgs = new Image[1];
               imgs[0] = new DefaultImage(ip.getProcessor().getPixels(), ip.getWidth(),
                       ip.getHeight(), ip.getBytesPerPixel(), 1, cb.build(), mb.build());
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
               DataProvider store = dw.getDataProvider();
               Roi userRoi = dw.getImagePlus().getRoi();
               ip = dw.getImagePlus();
               /*
               if (parms.getBoolean(AnalysisModule.SHOWMASKS)) {
                  RoiManager.getInstance().runCommand("Show All");
                  dw.getImagePlus().setRoi(userRoi);
               }
               */

               try {
                  Coords.CoordsBuilder builder = store.getAnyImage().getCoords().copyBuilder();
                  builder.channel(0).time(0);
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
                           dw.setDisplayedImageTo(builder.stagePosition(p).channel(0).build());
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
               } catch (IOException ioe) {
                  gui_.logs().showError(ioe);
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
                 "Analyzed " + parms.getString(AnalysisModule.CELLCOUNT)
                 + " objects, found " + parms.getString(AnalysisModule.OBJECTCOUNT)
                 + " objects to be photo-converted");

      }
   }
   
   /**
    * @param saveLocation
    * @throws IOException
    * @throws MMScriptException
    * @throws Exception 
    */
   public void runAnalysisAndZapping(String saveLocation) throws IOException, 
           MMScriptException, Exception 
   {

      final List<String> modulesInUse = settings_.getStringList(
              MODULELIST, "");
      final List<AnalysisModule> analysisModules = modulesFromNames(modulesInUse);

      if (analysisModules.isEmpty()) {
         throw new MMScriptException("No AnalysisModule used");
      }

      final String channelGroup = gui_.getCMMCore().getChannelGroup();
      
      if (channelGroup.isEmpty()) {
         ReportingUtils.showError("Please set the ChannelGroup in the main window first");
         return;
      }

      //TODO: error checking for file IO!
      File fd = new File(saveLocation);
      if (fd.exists()) {
         if (!IJ.showMessageWithCancel("Save location already exists.",
                 saveLocation + " already exists.  Overwrite?")) {
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
      dataWriter.write("Well" + "\t" + "Site" + "\t" + "ID" + "\t" + 
              "Module" + "\t" + "Pre-Post-Status" + "\t"
              + "X" + "\t" + "Y" + "\t" + "Mean" + "\t" + "Area");
      dataWriter.newLine();

      PositionList posList = gui_.getPositionListManager().getPositionList();
      MultiStagePosition[] positions = posList.getPositions();
      if (positions.length == 0) {
         gui_.positions().markCurrentPosition();
         positions = posList.getPositions();
         posList.clearAllPositions();
      }
      String currentWell = "";

     /*
      ResultsTable outTable = new ResultsTable();
      String outTableName = Terms.RESULTTABLENAME;
      Window oldOutTable = WindowManager.getWindow(outTableName);
      if (oldOutTable != null) {
         WindowManager.removeWindow(oldOutTable);
         oldOutTable.dispose();
      }
      */

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
         } else {
            nrImagesPerWell++;
         }
      }
      gui_.logs().logMessage("Images per well: " + nrImagesPerWell);

      // start cycling through the sites and group everything by well
      int count = 0;
      int siteCount = 0;
      Map<AnalysisModule, JSONObject> parmsMap = new HashMap<> 
        (analysisModules.size());
      for (AnalysisModule am : analysisModules) {
         parmsMap.put(am, analysisSettings(showMasks_.isSelected()));
      }
      currentWell = "";

      double originalExposure = gui_.getCMMCore().getExposure();

      // prepare stuff needed to store data in MM
      Datastore data = null;
      DisplayWindow dw;

      int nrChannels = 0;
      List<String> channelNames = new ArrayList<>();
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

      PropertyMap.Builder pmb = PropertyMaps.builder();
      pmb.putString("MM-Version", gui_.compat().getVersion());
      SummaryMetadata.Builder smb = gui_.data().getSummaryMetadataBuilder();
      smb = smb.channelNames(channelNames.toArray(new String[channelNames.size()])).
              channelGroup(channelGroup).
              userData(pmb.build()).
              prefix("MicroNucleiScreen").
              startDate((new Date()).toString()).
              intendedDimensions(Coordinates.builder().
                      channel(nrChannels).
                      z(0).
                      t(0).
                      stagePosition(nrImagesPerWell).
                      build());

      try {
         for (MultiStagePosition msp : positions) {
            if (stop_.get()) {
               resultsWriter.close();
               dataWriter.close();
               return;
            }
            startTime_ = System.currentTimeMillis();
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
                  for (AnalysisModule am : analysisModules) {
                     recordWellSummary(resultsWriter, am.getName(), currentWell,
                             parmsMap.get(am));
                  }
               }
               currentWell = well;
               siteCount = 0;

               data = gui_.data().createMultipageTIFFDatastore(saveLocation
                       + File.separator + well, true, false);
               data.setSummaryMetadata(smb.build());

               if (useDisplay_.isSelected()) {
                  DisplaySettings.Builder dsb = gui_.displays().getStandardDisplaySettings().copyBuilder();
                  ChannelDisplaySettings.Builder cdb
                          = gui_.displays().channelDisplaySettingsBuilder();
                  int chCounter = 0;
                  for (ChannelInfo ci : channelPanel_.getChannels()) {
                     if (ci.use_) {
                        cdb.color(ci.displayColor_);
                        dsb.channel(chCounter, cdb.build());
                        chCounter++;
                     }
                  }
                  dw = gui_.displays().createDisplay(data);
                  dw.setDisplaySettings(dsb.build());
               }
               
               if (useOnTheFlyProcessorPipeline_.isSelected()) {
                  // Create a blocking pipeline
                  pipeline_ = gui_.data().copyApplicationPipeline(data, true);
               }
               for (AnalysisModule analysisModule : analysisModules) {
                  analysisModule.reset();
                  // reset cell and object counters
                  parmsMap.get(analysisModule).put(AnalysisModule.CELLCOUNT, 0);
                  parmsMap.get(analysisModule).put(AnalysisModule.OBJECTCOUNT, 0);
               }
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
               int nrImagingChannels = currentChannel;

               // Analyze and zap
               boolean zapThem = false;
               List<ResultRois> resultRoiList = new ArrayList<>();
               for (int amNr = 0; amNr < analysisModules.size(); amNr++) {
                  AnalysisModule analysisModule = analysisModules.get(amNr);
                  ResultRois rr = analysisModule.analyze(gui_, imgs, null,
                          parmsMap.get(analysisModule));
                  rr.reportOnZapChannel(0); // Pre-Zap
                  rr.reportOnZapChannel(convertChannelPanel_.getChannels().size() - 1);  // Post-Zap
                  resultRoiList.add(rr);
                  if (rr.getHitRois() != null && rr.getHitRois().length != 0
                          && doZap_.isSelected()) {
                     zapThem = true;

                     // Report imaging channel intensities
                     for (int nr : rr.getImgsToBeReported()) {
                        ImageProcessor iProcessortmp = gui_.data().ij().createProcessor(imgs[nr]);
                        ImagePlus ipGFP = new ImagePlus("tmp", iProcessortmp);
                        reportIntensities(dataWriter, analysisModule.getName(),
                                currentWell, siteCount, ipGFP, "Hit-ch." + nr,
                                rr.getHitRois());
                        reportIntensities(dataWriter, analysisModule.getName(),
                                currentWell, siteCount, ipGFP, "NoHit-ch." + nr,
                                rr.getNonHitRois());
                     }
                  }
               }

               if (zapThem) {

                  String acq2 = msp.getLabel();
                  gui_.logs().logMessage("Imaging cells to be zapped at site: " + acq2);

                  int offset = 0;
                  for (int i = 0; i < convertChannelPanel_.getChannels().size(); i++) {
                     ChannelInfo ci = convertChannelPanel_.getChannels().get(i);
                     if (ci.use_) {
                        gui_.getCMMCore().waitForSystem();
                        gui_.logs().logMessage("Site: " + msp.getLabel() + ", x: " + msp.get(0).x + ", y: " + msp.get(0).y);
                        if (ci.purpose_.equals(ConvertChannelPanel.PRE)) {
                           offset += 1;
                        }
                        if (!ci.purpose_.equals(ConvertChannelPanel.PRE)
                                && !ci.purpose_.equals(ConvertChannelPanel.POST)) { // zap channel
                           ResultRois rr = resultRoiList.get(i - offset);

                           zap(rr.getHitRois());  // send ROIs to the device
                           if (rr.getHitRois() != null && rr.getHitRois().length > 0) {
                              gui_.getCMMCore().setConfig(channelGroup, ci.channelName_);
                              gui_.getCMMCore().waitForConfig(channelGroup, ci.channelName_);
                              gui_.getCMMCore().setExposure(ci.exposureTimeMs_);
                              imgs[currentChannel] = snapAndInsertImage(data, msp, siteCount, currentChannel);
                           }
                        } else {
                           gui_.getCMMCore().setConfig(channelGroup, ci.channelName_);
                           gui_.getCMMCore().waitForConfig(channelGroup, ci.channelName_);
                           gui_.getCMMCore().setExposure(ci.exposureTimeMs_);
                           imgs[currentChannel] = snapAndInsertImage(data, msp, siteCount, currentChannel);
                        }

                        currentChannel++;
                     }
                  }

                  // Reporting section
                  for (ResultRois rr : resultRoiList) {
                     // list Rois in outTable
                     /*
                  for (Roi roi : rr.getHitRois()) {
                     outTable.incrementCounter();
                     Rectangle bounds = roi.getBounds();
                     int x = bounds.x + (int) (0.5 * bounds.width);
                     int y = bounds.y + (int) (0.5 * bounds.height);
                     outTable.addValue(Terms.X, x);
                     outTable.addValue(Terms.Y, y);
                     outTable.addValue(Terms.POSITION, siteCount);
                  }
                      */
                     for (int i = 0; i < convertChannelPanel_.getChannels().size(); i++) {

                        if (rr.getZapChannelsToBeReported().contains(i)) {
                           ImageProcessor iProc = gui_.data().ij().createProcessor(
                                   imgs[i + nrImagingChannels]);
                           ImagePlus ip = new ImagePlus("tmp", iProc);
                           reportIntensities(dataWriter, rr.getName(), currentWell,
                                   siteCount, ip, convertChannelPanel_.getPurpose(i) + "-Hit",
                                   rr.getHitRois());
                           reportIntensities(dataWriter, rr.getName(), currentWell,
                                   siteCount, ip, convertChannelPanel_.getPurpose(i) + "-NoHit",
                                   rr.getNonHitRois());
                        }
                     }

                     // outTable.show(outTableName);
                  }
               }
               siteCount++;
               count++;
            }
         }

         if (data != null) {
            data.freeze();
            gui_.displays().manage(data);
         }

         gui_.getCMMCore().setExposure(originalExposure);

         // add listeners to our ResultsTable that let user click on row and go 
         // to cell that was found
         /*
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
          */
         // record the results from the last well:
         for (AnalysisModule am : analysisModules) {
            recordWellSummary(resultsWriter, am.getName(), currentWell,
                    parmsMap.get(am));
         }

      } catch (Exception ex) {
         throw ex;
      } finally {
         resultsWriter.close();
         dataWriter.close();
        
      }
      
      if (data != null) {
         data.freeze();  // freezing in the finally block can lead to additional exceptions
      }
      String msg = "Analyzed " + count + " images, in " + wellCount + " wells.";
      gui_.logs().logMessage(msg);
      gui_.logs().showMessage(msg);
   }


   /**
    * Be vary careful with this function as it will follow symlinks and
    * delete everything it finds
    */
   private boolean delete(File f) throws IOException {
      if (f.isDirectory()) {
         for (File c : f.listFiles()) {
            if (!delete(c)) {
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

   private void recordWellSummary(BufferedWriter resultsWriter, String moduleName, 
           String currentWell,final JSONObject parms) 
                     throws IOException, MMScriptException {
      resultsWriter.write(currentWell + "\t" + moduleName + "\t" 
              + parms.optInt(AnalysisModule.CELLCOUNT) + "\t"
              + parms.optInt(AnalysisModule.OBJECTCOUNT));
      resultsWriter.newLine();
      resultsWriter.flush();
      gui_.logs().logMessage(currentWell + " " + parms.optInt(AnalysisModule.CELLCOUNT)
              + "    " + parms.optInt(AnalysisModule.OBJECTCOUNT));
   }

   /**
    * Snaps an image and insert it into the given datastore
    *
    * @param data - datastore into which to insert images
    * @param siteCount - Position Nr to be used to insert into store
    * @param msp - Current Multistageposition
    * @param channelNr - Channel Nr to be used to insert into store.
    * @throws Exception
    */
   private Image snapAndInsertImage(Datastore data, MultiStagePosition msp,
           int siteCount, int channelNr) throws Exception {
      List<Image> snap = gui_.acquisitions().snap();
      Coords coord = gui_.data().createCoords("t=0,p=" + siteCount
              + ",c=" + channelNr + ",z=0");
      Image img = snap.get(0).copyAtCoords(coord);

      Metadata.Builder mdb = img.getMetadata().copyBuilderPreservingUUID();
      PropertyMap.Builder udb = img.getMetadata().getUserData().copyBuilder();
      PropertyMap ud = udb.putString("Time", DATE_FORMATTER.format(new Date())).build();
      

      mdb = mdb.xPositionUm(msp.getX()).yPositionUm(msp.getY());

      Metadata md = mdb.positionName(msp.getLabel()).elapsedTimeMs(
              (double) (System.currentTimeMillis() - startTime_)).userData(ud).build();
      
      img = img.copyWith(coord, md);

      if (pipeline_ != null) {
         try {
            pipeline_.insertImage(img);
         } catch (PipelineErrorException pee) {
            gui_.logs().logError(pee);
            // even when we get this error, the image is already inserted - most of the time
            if (!data.hasImage(img.getCoords())) {
               data.putImage(img);
            }
         }
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
      if (rois == null || rois.length == 0) {
         return;
      }
      int i;
      // convert zapRois in a Roi[] of Polygon Rois
      for (i = 0; i < rois.length; i++) {
         Polygon poly = rois[i].getConvexHull();
         rois[i] = new PolygonRoi(poly, Roi.POLYGON);
      }

      // send to the projection device and zap them for real
      gui_.logs().logMessage("Zapping " + (i + 1) + " of " + rois.length);
      ProjectionDevice pd = ProjectorActions.getProjectionDevice(gui_);
      if (pd == null) {
         ReportingUtils.showError("No Projection Device found.  Can not Zap");
         return;
      }
      Map<Polygon, AffineTransform> maps = ProjectorActions.loadMapping(gui_, pd);
      if (maps == null) {
         ReportingUtils.showError("ProjectionDevice is not calibrated.  Please calibrate first");
         ProjectorControlForm.showSingleton(gui_.getCMMCore(), gui_);
         return;
      }
      List<FloatPolygon> transformROIs = ProjectorActions.transformROIs(rois, maps);
      pd.loadRois(transformROIs);
      pd.waitForDevice();
      pd.runPolygons();
      pd.waitForDevice();
      /*
      pcf.setROIs(rois);
      pcf.updateROISettings();
      pcf.getDevice().waitForDevice();
      pcf.runRois();
      pcf.getDevice().waitForDevice();
      */

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
        
   private void reportIntensities(BufferedWriter theFile, String moduleName, 
           String well, int posCounter, ImagePlus ip, String label, Roi[] rois) {
   if (rois == null) {
      return;
   }
	for (int i = 0; i < rois.length; i++) {
		ip.setRoi(rois[i]);
      ImageStatistics stats = ip.getStatistics(ImagePlus.CENTROID + 
              ImagePlus.MEAN + ImagePlus.INTEGRATED_DENSITY + ImagePlus.AREA);
      try {
         theFile.write(well + "\t" + posCounter + "\t" + i + "\t" + 
                       moduleName + "\t" + label + "\t" +
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