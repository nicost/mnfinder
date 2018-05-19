/**
 * 
 */
package org.micromanager.micronuclei.analysisinterface;

import ij.gui.Roi;
import java.util.ArrayList;
import java.util.List;
import org.micromanager.micronuclei.internal.gui.ConvertChannelTableModel;

/**
 * DataStructure to hold the result of the analysismodule
 * Any or all of the Roi[] can be null, it is the responsibility of the user
 * to make sure that Roi[] are actually present
 * @author nico
 */
public class ResultRois {
   private final Roi[] allRois_;
   private final Roi[] hitRois_;
   private final Roi[] nonHitRois_;
   // Indices of channels for which intensities of ROIS should be reported to the 
   // user.  When the index exceeds the number of channels used for imaging,
   // it will be ignored.  "Zap" channels will not be reported based on this list
   private final List<Integer> channelsToBeReported_;
   // "Zap" channels will appear as the last channels in the data collection
   // There may be up to 3 channels.  They are ordered: 0-PreZap, 1-Zap, 2-PostZap
   // so including 0, and 2 will report Pre- and Post-Zap values (if those images
   // were acquired by the user"
   private final List<Integer> zapChannelsToBeReported_;
   
   /**
    *
    * @param allRois - all Rois found by the analysis module
    * @param hitRois - the Rois considered "positive" by the module
    * @param nonHitRois - the Rois considered negative by the module
   */
   public ResultRois (Roi[] allRois, Roi[] hitRois, Roi[] nonHitRois) {
      allRois_ = allRois;
      hitRois_ = hitRois;
      nonHitRois_ = nonHitRois;
      channelsToBeReported_ = new ArrayList<Integer>();
      zapChannelsToBeReported_ = new ArrayList<Integer>();
   }
   
   public Roi[] getAllRois() {
      return allRois_;
   }
   
   public Roi[] getHitRois() {
      return hitRois_;
   }
   
   public Roi[] getNonHitRois() {
      return nonHitRois_;
   }
   
   public void reportOnImg(int imgNr) {
      if (!channelsToBeReported_.contains(imgNr)) {
         channelsToBeReported_.add(imgNr);
      }
   }
   
   public void reportOnZapChannel(int zapChannel) {
      if (!zapChannelsToBeReported_.contains(zapChannel))
      {
         zapChannelsToBeReported_.add(zapChannel);
      }
   }
   
   public List<Integer>getImgsToBeReported() {
     return channelsToBeReported_;
   }
   
   public List<Integer> getZapChannelsToBeReported() {
      return zapChannelsToBeReported_;
   }
   
}
