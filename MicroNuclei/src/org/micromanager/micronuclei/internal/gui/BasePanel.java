
package org.micromanager.micronuclei.internal.gui;

/**
 *
 * @author nico
 */
public interface BasePanel {
   
   static final String COL0WIDTH = "Col0Width";  
   static final String COL1WIDTH = "Col1Width";
   static final String COL2WIDTH = "Col2Width";
   static final String COL3WIDTH = "Col3Width";
   static final String COL4WIDTH = "Col4Width";
   
   static final String COLOR = "Color";
   static final String EXPOSURETIME = "ExposureTimeMs";
   
   // Store color in prefs, and force display update
   public void updateColor(int rowIndex);
    
   // Store exposureTime in prefs and force display update
   public void updateExposureTime(int rowIndex);
   
}
