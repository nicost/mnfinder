/**
 * 
 */
package org.micromanager.micronuclei.analysisinterface;

import ij.gui.Roi;
import java.util.ArrayList;
import java.util.List;

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
   private final List<Integer> imgToBeReported_;
   
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
      imgToBeReported_ = new ArrayList<Integer>();
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
      // TODO may want to check if we already have this number
      imgToBeReported_.add(imgNr);
   }
   
   public int[] getImgsToBeReported() {
      int[] result = new int[imgToBeReported_.size()];
      for (int i = 0; i < imgToBeReported_.size(); i++) {
         result[i] = imgToBeReported_.get(i);
      }
      return result;
   }
   
}
