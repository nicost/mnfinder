
package org.micromanager.micronuclei.analysisinterface;

import ij.gui.Roi;
import java.util.List;
import org.micromanager.Studio;
import org.micromanager.data.Image;

/**
 *
 * @author nico
 */
public abstract class AnalysisSubModule {
   List<AnalysisProperty> analysisProperties_;
   
   
   /**
    * Function in which the actual analysis takes place
    * Inputs are the image itself, a user definable Roi (which restricts the 
    * analysis to a part of the image, and the analysis settings.
    * The implementation returns a single Roi that contains the area of the image
    * that should be analyzed by the super-module. If the whole image should be
    * analyzed, the module can return either null or an Roi for the whole image
    * 
    * @param studio - the MMStudio object, needed by the implementation to call
    * MM functionality
    * @param imgs - The MM images to be analyzed
    * @return - Roi with the are of the image that should be analyzed
    * @throws AnalysisException 
    */
   public abstract Roi analyze (Studio studio, Image[] imgs) throws AnalysisException;
      
   /**
    * This should be called in the implementing class, preferably in the constructor
    * I looked for ways to enforce this, but could not find a nice way to do so
    * @param aps AnalysisProperties that should be set by the user
    */
   protected final void setAnalysisProperties(List<AnalysisProperty> aps) {
      analysisProperties_ = aps;
   }
   
   public final List<AnalysisProperty> getAnalysisProperties() {
      return analysisProperties_;
   }
   
}
