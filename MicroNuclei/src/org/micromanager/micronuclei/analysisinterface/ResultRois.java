/**
 * 
 */
package org.micromanager.micronuclei.analysisinterface;

import ij.gui.Roi;

/**
 * DataStructure to hold the result of the analysismodule
 * Any or all of the Roi[] can be null, it is the responsibility of the user
 * to make sure that Roi[] are actually present
 * @author Nico
 */
public class ResultRois {
   private final Roi[] allRois_;
   private final Roi[] hitRois_;
   private final Roi[] nonHitRois_;
   
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
   
}
