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
import mmcorej.TaggedImage;
import org.json.JSONObject;
import org.micromanager.utils.MMScriptException;

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

  
   public abstract Roi[] analyze (TaggedImage img, JSONObject parms) throws MMScriptException;
   
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
