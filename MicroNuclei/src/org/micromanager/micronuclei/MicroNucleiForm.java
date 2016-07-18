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
import ij.WindowManager;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.text.TextPanel;
import ij.text.TextWindow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
import org.micromanager.data.SummaryMetadata;
import org.micromanager.display.DisplayWindow;


import org.micromanager.internal.utils.FileDialogs;
import org.micromanager.internal.utils.ImageUtils;
import org.micromanager.internal.utils.MMFrame;
import org.micromanager.internal.utils.MMScriptException;


import org.micromanager.projector.internal.ProjectorControlForm;
import org.micromanager.micronuclei.analysis.MicroNucleiAnalysisModule;
import org.micromanager.micronuclei.analysisinterface.AnalysisModule;
import org.micromanager.micronuclei.analysisinterface.AnalysisException;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;
import org.micromanager.micronuclei.analysisinterface.PropertyException;
import org.micromanager.micronuclei.gui.ResultsListener;
import org.micromanager.micronuclei.gui.DragDropUtil;
import org.micromanager.micronuclei.gui.PropertyGUI;
import org.micromanager.micronuclei.utilities.Utils;


/**
 *
 * @author nico
 */
public class MicroNucleiForm extends MMFrame {
   
   private final Studio gui_;
   private final Font arialSmallFont_;
   private final Dimension buttonSize_;
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
   private final JTextField backgroundTextField_;
   private final JTextField flatfieldTextField_;
   private final Preferences prefs_;
   
   private ImagePlus background_;
   private ImagePlus flatfield_;
   
   private final String SAVELOCATION = "SaveLocation";
   private final String IMAGINGCHANNEL = "ImagingChannel";
   private final String SECONDIMAGINGCHANNEL = "SecondImagingChannel";
   private final String ZAPCHANNEL = "ZapChannel";
   private final String AFTERZAPCHANNEL = "AfterZapChannel";
   private final String DOZAP = "DoZap";
   private final String SHOWMASKS = "ShowMasks";
   private final String BACKGROUNDLOCATION = "BackgroundLocation";
   private final String FLATFIELDLOCATION = "FlatfieldLocation";
   
   private final AtomicBoolean stop_ = new AtomicBoolean(false);
   
   private final AnalysisModule analysisModule_;
   
   public MicroNucleiForm(Studio gui) {
      gui_ = gui;
      
      super.loadAndRestorePosition(100, 100, 200, 200);
      prefs_ = Preferences.userNodeForPackage(this.getClass());
      

      // TODO: make this user selectable from available modules
      analysisModule_ = new MicroNucleiAnalysisModule();
      
      
      arialSmallFont_ = new Font("Arial", Font.PLAIN, 12);
      buttonSize_ = new Dimension(70, 21);

      super.setLayout(new MigLayout("flowx, fill, insets 8"));
      super.setTitle("MicroNuclei Analyze");
      
      
      JPanel acqPanel = new JPanel(new MigLayout(
              "flowx, fill, insets 8"));
      
      acqPanel.add(myLabel(arialSmallFont_,"Save here:"));
      
      saveTextField_ = new JTextField();
      saveTextField_.setText(prefs_.get(SAVELOCATION, ""));
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
      
      acqPanel.add(myLabel(arialSmallFont_, "Imaging Channel: "));
      channelComboBox_ = new JComboBox();
      imagingChannel_ = prefs_.get(IMAGINGCHANNEL, imagingChannel_);
      updateChannels(channelComboBox_, imagingChannel_, false);
      channelComboBox_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
            channelActionPerformed(ae);
         }
      } );
      acqPanel.add(channelComboBox_, "span 2, left, wrap");
      
      acqPanel.add(myLabel(arialSmallFont_, "2nd Imaging Channel: "));
      secondChannelComboBox_ = new JComboBox();
      secondImagingChannel_ = prefs_.get(SECONDIMAGINGCHANNEL, secondImagingChannel_);
      updateChannels(secondChannelComboBox_, secondImagingChannel_, true);
      secondChannelComboBox_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
            secondChannelActionPerformed(ae);
         }
      } );
      acqPanel.add(secondChannelComboBox_, "span 2, left, wrap");
      
      acqPanel.add(myLabel(arialSmallFont_, "Zap Channel: "));
      zapChannelComboBox_ = new JComboBox();
      zapChannel_ = prefs_.get(ZAPCHANNEL, zapChannel_);
      updateChannels(zapChannelComboBox_, zapChannel_, false);
      zapChannelComboBox_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
            zapChannelActionPerformed(ae);
         }
      } );
      acqPanel.add(zapChannelComboBox_, "span 2, left, wrap");
      
      acqPanel.add(myLabel(arialSmallFont_, "After Zap Channel: "));
      AfterZapChannelComboBox_ = new JComboBox();
      afterZapChannel_ = prefs_.get(AFTERZAPCHANNEL, afterZapChannel_);
      updateChannels(AfterZapChannelComboBox_, afterZapChannel_, false);
      AfterZapChannelComboBox_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
            afterZapChannelActionPerformed(ae);
         }
      } );
      acqPanel.add(AfterZapChannelComboBox_, "span 2, left, wrap");
      acqPanel.setBorder(makeTitledBorder("Acquisition Settings"));
      
      super.add(acqPanel, "span 3, center, wrap");
      
            
      JPanel analysisPanel = new JPanel(new MigLayout(
              "flowx, fill, insets 8"));
      analysisPanel.setBorder(makeTitledBorder("Analysis Settings"));
      
      analysisPanel.add(new JLabel("Background: "));
      backgroundTextField_ = new JTextField();
      backgroundTextField_.setText(prefs_.get(BACKGROUNDLOCATION, ""));
      backgroundTextField_.setMinimumSize(new Dimension(250, 12));
      backgroundTextField_.setMaximumSize(new Dimension(250, 20));
      backgroundTextField_.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            if (prefs_ != null)
               prefs_.put(BACKGROUNDLOCATION, backgroundTextField_.getText());
         }
      });
      analysisPanel.add(backgroundTextField_);
      DropTarget dropTarget = new DropTarget(backgroundTextField_, 
              new DragDropUtil(backgroundTextField_));
      final JButton backgroundButton = myButton(buttonSize_, arialSmallFont_, "...");
      backgroundButton.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            fileActionPerformed(e, 
                  backgroundTextField_, "Background Image", BACKGROUNDLOCATION);
         }
      });
      analysisPanel.add(backgroundButton, "wrap");
      
      analysisPanel.add(new JLabel("Flatfield: "));
      flatfieldTextField_ = new JTextField();
      flatfieldTextField_.setText(prefs_.get(FLATFIELDLOCATION, ""));
      flatfieldTextField_.setMinimumSize(new Dimension(250, 12));
      flatfieldTextField_.setMaximumSize(new Dimension(250, 20));
      flatfieldTextField_.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
             if (prefs_ != null)
               prefs_.put( FLATFIELDLOCATION, flatfieldTextField_.getText());
         }
      });
      analysisPanel.add(flatfieldTextField_);
      dropTarget = new DropTarget(flatfieldTextField_, 
              new DragDropUtil(flatfieldTextField_));
      final JButton flatfieldButton = myButton(buttonSize_, arialSmallFont_, "...");
      flatfieldButton.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            fileActionPerformed(e, 
                  flatfieldTextField_, "Flatfield Image", FLATFIELDLOCATION);
         }
      });
      analysisPanel.add(flatfieldButton, "wrap");
      
      super.add(analysisPanel, "span 3, center, wrap");
      
      JPanel modulePanel = new JPanel(new MigLayout(
              "flowx, fill, insets 8"));
      modulePanel.setBorder(makeTitledBorder(analysisModule_.name()));
      
      for (AnalysisProperty ap : analysisModule_.getAnalysisProperties()) {
         modulePanel.add(new JLabel(ap.getDescription()));
         modulePanel.add(new PropertyGUI(ap).getJComponent(), "wrap");
      }
      
      super.add(modulePanel, "span 3, center, wrap");
      
      
      doZap_ = new JCheckBox("Zap");
      doZap_.setSelected(prefs_.getBoolean(DOZAP, false));
      doZap_.setFont(arialSmallFont_);
      doZap_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
              prefs_.putBoolean(DOZAP, doZap_.isSelected());
         }
      });
      super.add (doZap_);
      
      showMasks_  = new JCheckBox("Show Masks");
      showMasks_.setSelected (prefs_.getBoolean(SHOWMASKS, false));
      showMasks_.setFont(arialSmallFont_);
      showMasks_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
              prefs_.putBoolean(SHOWMASKS, showMasks_.isSelected());
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
         if (prefs_ != null)
               prefs_.put(SAVELOCATION, f.getAbsolutePath());    
      }
   }   
   
   private void fileActionPerformed(java.awt.event.ActionEvent evt, 
           JTextField textField, String title, String prefKey) {                                                  
      File f = FileDialogs.openFile(this, title,
              new FileDialogs.FileType("File", title,
              textField.getText(), true, "tif", "tiff") );
      if (f != null) {
         textField.setText(f.getAbsolutePath());
         if (prefs_ != null)
               prefs_.put(prefKey, f.getAbsolutePath());    
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
            Opener opener = new Opener();
            if (!backgroundTextField_.getText().equals("")) 
               background_ = opener.openImage(backgroundTextField_.getText());
            if (!flatfieldTextField_.getText().equals(""))
               flatfield_ = opener.openImage(flatfieldTextField_.getText());            
            if (!testing_) {
               warnAboutMissingCorrections(background_, flatfield_);
               runAnalysisAndZapping(saveTextField_.getText());
               warnAboutMissingCorrections(background_, flatfield_);
            } else {
               warnAboutMissingCorrections(background_, flatfield_);
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
            tImg = Utils.normalize(tImg, background_, flatfield_);
            if (parms.getBoolean(AnalysisModule.SHOWMASKS)) {
               (new ImagePlus("Normalized", ImageUtils.makeProcessor(tImg))).show();
            }
            Roi[] zapRois = analysisModule_.analyze(tImg, parms);
            for (Roi roi : zapRois) {
               outTable.incrementCounter();
               Rectangle bounds = roi.getBounds();
               int x = bounds.x + (int) (0.5 * bounds.width);
               int y = bounds.y + (int) (0.5 * bounds.height);
               outTable.addValue(Terms.X, x);
               outTable.addValue(Terms.Y, y);
               outTable.show(outTableName);
            }

         } else { // MM display
            Datastore store = dw.getDatastore();
            Coords.CoordsBuilder builder = store.getAnyImage().getCoords().copy();
            int nrPositions = store.getAxisLength(Coords.STAGE_POSITION); 
            for (int p = 0; p < nrPositions && !stop_.get(); p++) {
               try {
                  Image image = store.getImage(builder.stagePosition(p).build());
                  if (image != null) {
                     TaggedImage tImg = ImageUtils.makeTaggedImage(Utils.getProcessor(image));
                     tImg.tags.put("PixelSizeUm", ip.getCalibration().pixelWidth);
                     tImg = Utils.normalize(tImg, background_, flatfield_);
                     if (parms.getBoolean(AnalysisModule.SHOWMASKS)) {
                        (new ImagePlus("Normalized", ImageUtils.makeProcessor(tImg))).show();
                     }
                     Roi[] zapRois = analysisModule_.analyze(tImg, parms);
                     for (Roi roi : zapRois) {
                        outTable.incrementCounter();
                        Rectangle bounds = roi.getBounds();
                        int x = bounds.x + (int) (0.5 * bounds.width);
                        int y = bounds.y + (int) (0.5 * bounds.height);
                        outTable.addValue(Terms.X, x);
                        outTable.addValue(Terms.Y, y);
                        outTable.addValue(Terms.POSITION, p);
                     }
                     outTable.show(outTableName);
                  }
               } catch (JSONException ex) {
               } catch (NullPointerException npe) {
                  ij.IJ.log("Null pointer exception at position : " + p);
               }
            }
         }
      } catch (AnalysisException ex) {
         ij.IJ.log("Error during analysis");
      }

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
      
      ij.IJ.log("Analyzed " + parms.getString(AnalysisModule.CELLCOUNT) + 
              " nuclei, found " + parms.getString(AnalysisModule.OBJECTCOUNT) +
                      " nuclei with micronuclei" );
      
      
   }
   
   /**
    * Analysis class, in the future we could have a choice of these
    * @param saveLocation
    * @throws IOException
    * @throws MMScriptException
    * @throws Exception 
    */
   public void runAnalysisAndZapping(String saveLocation) throws IOException, MMScriptException, Exception {
      
      String channelGroup = gui_.getCMMCore().getChannelGroup();
      
      //TODO: error checking for file IO!
      if (new File(saveLocation).exists()) {
         if (! IJ.showMessageWithCancel("Save location already exists.", 
                 saveLocation + " already exists.  Overwrite?") ) {
            return;
         }
      }
      // gui_.closeAllAcquisitions();
      new File(saveLocation).mkdirs();
      File resultsFile = new File(saveLocation + File.separator + "results.txt");
      resultsFile.createNewFile();
      BufferedWriter resultsWriter = new BufferedWriter(new FileWriter(resultsFile));

      PositionList posList = gui_.getPositionListManager().getPositionList();
      MultiStagePosition[] positions = posList.getPositions();
      String currentWell = "";
      int nrChannels = 1;
      if (secondImagingChannel_.length() > 1) {
         nrChannels = 2;
      }
      
      ResultsTable outTable = new ResultsTable();
      String outTableName = Terms.RESULTTABLENAME;
      Window oldOutTable = WindowManager.getWindow(outTableName);
      if (oldOutTable != null) {
         WindowManager.removeWindow(oldOutTable);
         oldOutTable.dispose();
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
      gui_.logs().logMessage("Images per well: " + nrImagesPerWell);
      
      // start cycling through the sites and group everything by well
      int count = 0;
      int siteCount = 0;
      JSONObject parms = analysisSettings(showMasks_.isSelected());
      currentWell = "";
      
      // prepare stuff needed to store data in MM
      Datastore data = null;
      DisplayWindow dw = null;
      int arraySize = doZap_.isSelected() ? nrChannels + 1 : nrChannels;
      String[] channelNames = new String[arraySize];
      channelNames[0] =  imagingChannel_;
      if (doZap_.isSelected()) {
         channelNames[1] = zapChannel_;
      }
      if (secondImagingChannel_.length() > 1) {
         channelNames[1] = secondImagingChannel_;
         if (doZap_.isSelected()) {
            channelNames[2] = zapChannel_;
         }
      }

      SummaryMetadata.SummaryMetadataBuilder smb = gui_.data().getSummaryMetadataBuilder();
      smb = smb.channelNames(channelNames).
              channelGroup(channelGroup).
              microManagerVersion(gui_.compat().getVersion()).
              prefix("MicroNucleiScreen").
              startDate((new Date()).toString());
      smb = smb.intendedDimensions(gui_.data().getCoordsBuilder().
              channel(channelNames.length).
              z(0).
              time(0).
              stagePosition(nrImagesPerWell).
              build());

      PropertyMap.PropertyMapBuilder pmb = gui_.data().getPropertyMapBuilder();
      
      SummaryMetadata summaryMetadata = smb.build();

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
               recordResults(resultsWriter, currentWell, parms);
            }
            currentWell = well;
            siteCount = 0;

            
            data = gui_.data().createMultipageTIFFDatastore(saveLocation + "/" + well, true, false);
            data.setSummaryMetadata(smb.build());
            dw = gui_.displays().createDisplay(data);
            gui_.displays().manage(data);
            //gui_.openAcquisition(well, saveLocation, 1, nrChannels + 1, 1, nrImagesPerWell, true, true);
            analysisModule_.reset();
            // reset cell and object counters
            parms.put(AnalysisModule.CELLCOUNT, 0);
            parms.put(AnalysisModule.OBJECTCOUNT, 0);
         }
         
         if (data != null) {
            MultiStagePosition.goToPosition(msp, gui_.getCMMCore());
            gui_.getCMMCore().waitForSystem();
            gui_.logs().logMessage("Site: " + msp.getLabel() + ", x: " + msp.get(0).x + ", y: " + msp.get(0).y);
            gui_.getCMMCore().setConfig(channelGroup, imagingChannel_);
            TaggedImage tImg = snapAndInsertImage(data, msp,siteCount, 0); 

            if (nrChannels == 2) {
               gui_.getCMMCore().setConfig(channelGroup, secondImagingChannel_);
               gui_.getCMMCore().snapImage();
               tImg = snapAndInsertImage(data, msp,siteCount, 1); 
            }
            gui_.getCMMCore().setConfig(channelGroup, zapChannel_);

            // Analyze (second channel if we had it) and zap
            tImg = Utils.normalize(tImg, background_, flatfield_);
            Roi[] zapRois = analysisModule_.analyze(tImg, parms);
            if (zapRois != null && doZap_.isSelected()) {
               zap(zapRois);
               for (Roi roi : zapRois) {
                  outTable.incrementCounter();
                  Rectangle bounds = roi.getBounds();
                  int x = bounds.x + (int) (0.5 * bounds.width);
                  int y = bounds.y + (int) (0.5 * bounds.height);
                  outTable.addValue(Terms.X, x);
                  outTable.addValue(Terms.Y, y);
                  outTable.addValue(Terms.POSITION, siteCount);
               }
               outTable.show(outTableName);

               if (zapRois.length > 0) {
                  String acq2 = msp.getLabel();
                  gui_.logs().logMessage("Imaging zapped cells at site: " + acq2);
                  // take the red image and save it
                  gui_.getCMMCore().setConfig(channelGroup, afterZapChannel_);
                  snapAndInsertImage(data, msp,siteCount, nrChannels);
               }
            }
            siteCount++;
            count++;
         }
      }

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
      recordResults(resultsWriter, currentWell, parms);

      resultsWriter.close();
      String msg = "Analyzed " + count + " images, in " + wellCount + " wells.";
      gui_.logs().logMessage(msg);
      gui_.logs().showMessage(msg);
   }

   private void recordResults(BufferedWriter resultsWriter, String currentWell,
           final JSONObject parms) throws IOException, MMScriptException {
      resultsWriter.write(currentWell + "\t"
              + parms.optInt(AnalysisModule.CELLCOUNT) + "\t"
              + parms.optInt(AnalysisModule.OBJECTCOUNT));
      resultsWriter.newLine();
      resultsWriter.flush();
      gui_.logs().showMessage(currentWell + " " + parms.optInt(AnalysisModule.CELLCOUNT)
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
   private TaggedImage snapAndInsertImage(Datastore data, MultiStagePosition msp,
            int siteCount, int channelNr) throws Exception {
      gui_.getCMMCore().snapImage();
      TaggedImage tImg = gui_.getCMMCore().getTaggedImage();
      Coords coord = gui_.data().createCoords("t=0,p=" + siteCount + 
              ",c=" + channelNr + ",z=0");
      Image img = gui_.data().convertTaggedImage(tImg);
      Metadata md = img.getMetadata();
      Metadata.MetadataBuilder mdb = md.copy();
      PropertyMap ud = md.getUserData();

      mdb = mdb.xPositionUm(msp.getX()).yPositionUm(msp.getY());

      md = mdb.positionName(msp.getLabel()).userData(ud).build();
      img = img.copyWith(coord, md);

      data.putImage(img);
      
      return tImg;
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
  
 
   private static void warnAboutMissingCorrections(ImagePlus background, 
           ImagePlus flatfield) {
      if (background == null) {
         ij.IJ.log("No background correction applied because of missing background image");
      }
      if (flatfield == null) {
         ij.IJ.log("No flatfield correction applied because of missing flatfield image");
      }
   }
           

   
}