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

package org.micromanager.micronuclei.analysisinterface;

import ij.gui.Roi;
import java.util.List;
import org.json.JSONObject;
import org.micromanager.Studio;
import org.micromanager.data.Image;

/**
 *
 * @author nico
 */
public abstract class AnalysisModule  {
   // Keys to be used in JSONObject to exchange parameters
   // not every analysis module will know about all keys!
   public final static String SHOWMASKS = "ShowMasks";
   public final static String RESETCOUNT = "ResetCount";
   public final static String CELLCOUNT = "CellCount";
   public final static String OBJECTCOUNT = "ObjectCount";
   
   private List<AnalysisProperty> analysisProperties_;

   /**
    * Function in which the actual analysis takes place
    * Inputs are the image itself, a user definable Roi (which restricts the 
    * analysis to a part of the image, and the analysis settings.
    * The implementation returns an Array of Rois that contains the objects
    * of interest
    * @param studio - the MMStudio object, needed by the implementation to call
    * MM functionality
    * @param img - The MM image to be analyzed
    * @param roi - an ImageJ ROI that should restrict the are to be analyzed
    * if null, the whole image should be analyzed
    * @param parms - Parameters as Key-Value pairs in JSONFormat
    * @return - array of Rois containing the objects of interest
    * @throws AnalysisException 
    */
   public abstract ResultRois analyze (Studio studio, Image img, Roi roi, JSONObject parms) 
           throws AnalysisException;
  
   
   /**
    * Resets the module so that it can be re-used without side effects
    * Can for instance be used to reset counters
    */
   public abstract void reset();
   
   /**
    * UI name for the analysis module
    * @return String to be displayed to the user
    */
   public abstract String name();
   
   /**
    * This should be called in the implementing class, preferably in the constructor
    * I looked for ways to enforce this, but could not find a nice way to do so
    * @param aps 
    */
   protected final void setAnalysisProperties(List<AnalysisProperty> aps) {
      analysisProperties_ = aps;
   }
   
   public final List<AnalysisProperty> getAnalysisProperties() {
      return analysisProperties_;
   }
   
   
}
