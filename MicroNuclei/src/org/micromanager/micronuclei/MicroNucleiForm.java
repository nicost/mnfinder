
package org.micromanager.micronuclei;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import mmcorej.TaggedImage;
import net.miginfocom.swing.MigLayout;
import org.micromanager.api.MultiStagePosition;
import org.micromanager.api.PositionList;
import org.micromanager.api.ScriptInterface;
import org.micromanager.multichannelshading.MultiChannelShading;
import org.micromanager.utils.MMFrame;
import org.micromanager.utils.MMScriptException;


/**
 *
 * @author nico
 */
public class MicroNucleiForm extends MMFrame {
   private final ScriptInterface gui_;
   private final Font arialSmallFont_;
   private final Dimension buttonSize_;
   
   public MicroNucleiForm(ScriptInterface gui) {
      gui_ = gui;
      loadAndRestorePosition(100, 100, 200, 200);
      
      arialSmallFont_ = new Font("Arial", Font.PLAIN, 12);
      buttonSize_ = new Dimension(70, 21);
      
      final JButton runButton =  myButton(buttonSize_, arialSmallFont_, "Run");
      runButton.addActionListener(new ActionListener(){
         @Override
         public void actionPerformed(ActionEvent e) {
            try {
               runAnalysisAndZapping();
            } catch (IOException ex) {
               Logger.getLogger(MicroNucleiForm.class.getName()).log(Level.SEVERE, null, ex);
            } catch (MMScriptException ex) {
               Logger.getLogger(MicroNucleiForm.class.getName()).log(Level.SEVERE, null, ex);
            }
         }
      } );
      
      this.setLayout(new MigLayout("flowx, fill, insets 8"));
      this.setTitle(MultiChannelShading.menuName);
      
      this.add(runButton);

      loadAndRestorePosition(100, 100, 350, 250);

   }
   
   @Override
   public void dispose() {
      super.dispose();
   }
   
   public final JButton myButton(Dimension buttonSize, Font font, String text) {
      JButton button = new JButton();
      button.setPreferredSize(buttonSize);
      button.setMinimumSize(buttonSize);
      button.setFont(font);
      button.setMargin(new Insets(0, 0, 0, 0));
      
      return button;
   }
   
   public void runAnalysisAndZapping() throws IOException, MMScriptException {
      String  script = "D:\\projects\\mnfinder\\mn2.bsh";
      String channelGroup = "Channels";
      String imagingChannel = "Epi-Cy5-PhotoTarget";
      String afterZapChannel = "Epi-Cherry";
      int nrImagesPerWell = 100;
      String saveLocation = "D:\\Users\\Susana\\20150427\\screen2";
      String saveZappedLocation = saveLocation + "\\Zapped";
   
      //prefs = Preferences.userNodeForPackage(this.getClass());
      gui_.closeAllAcquisitions();
      new File(saveLocation).mkdirs();
//new File(saveZappedLocation).mkdirs();
      File resultsFile = new File(saveLocation + File.separator + "results.txt");
      resultsFile.createNewFile();
      Writer resultsWriter = new BufferedWriter(new FileWriter(resultsFile));

      PositionList posList = gui_.getPositionList();
      MultiStagePosition[] positions = posList.getPositions();
      String currentWell = "";
      int count = 0;
      for (int p = 0; p < positions.length; p++) {
         MultiStagePosition msp = positions[p];
         String label = msp.getLabel();
         String well = label.split("-")[0];
         if (!currentWell.equals(well)) {
            gui_.message("Starting well: " + well);
            if (!currentWell.equals("")) {
               nuclei = prefs.getInt("nuclei", 0);
               prefs.putInt("nuclei", 0);
               zappedNuclei = prefs.getInt("zappedNuclei", 0);
               prefs.putInt("zappedNuclei", 0);
               resultsWriter.write(currentWell + "\t" + nuclei + "\t" + zappedNuclei);
               resultsWriter.newLine();
               resultsWriter.flush();
               gui.message(currentWell + " " + nuclei + "   " + zappedNuclei);
            }
            currentWell = well;
            gui.openAcquisition(well, saveLocation, 1, 2, 1, nrImagesPerWell, true, true);
            wellCount = 0;
         }
         MultiStagePosition.goToPosition(msp, gui_.getMMCore());
         gui_.message("Site: " + msp.getLabel());
         gui_.getMMCore().setConfig(channelGroup, imagingChannel);
         gui_.getMMCore().snapImage();
         TaggedImage tImg = gui_.getMMCore().getTaggedImage();
         gui_.addImageToAcquisition(well, 0, 0, 0, wellCount, tImg);
         prefs.putInt("zappedNow", 0);
         // run the script recognizing the micronuclei and zapping them
         source(script);
         zappedNow = prefs.getInt("zappedNow", 0);
         if (zappedNow > 0) {
            acq2 = msp.getLabel();
            gui_.message("Imaging zapped cells at site: " + acq2);
            // take the red image and save it
            gui_.getMMCore().setConfig(channelGroup, afterZapChannel);
            gui_.getMMCore().snapImage();
            tImg2 = gui_.getMMCore().getTaggedImage();
            gui_.addImageToAcquisition(well, 0, 1, 0, wellCount, tImg2);
            acqObject = gui_.getAcquisition(well);
            acqObject.setChannelColor(1, new Color(255, 0, 0).getRGB());
            gui_.getMMCore().setConfig(channelGroup, imagingChannel);
         }
         wellCount++;
         count++;
      }

// record the results from the last well:
      nuclei = prefs.getInt("nuclei", 0);
      prefs.putInt("nuclei", 0);
      zappedNuclei = prefs.getInt("zappedNuclei", 0);
      prefs.putInt("zappedNuclei", 0);
      resultsWriter.write(currentWell + "\t" + nuclei + "\t" + zappedNuclei);
      resultsWriter.newLine();
      gui_.message(currentWell + " " + nuclei + "   " + zappedNuclei);

      resultsWriter.close();
      gui_.message("Analyzed " + count + " images");
   }

}
