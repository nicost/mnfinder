
package org.micromanager.micronuclei;

import ij.gui.Roi;
import mmcorej.TaggedImage;
import org.json.JSONObject;
import org.micromanager.utils.MMScriptException;

/**
 *
 * @author nico
 */
public interface AnalysisModule  {
   // Keys to be used in JSONObject to exchange parameters
   // not every analysis module will know about all keys!
   public final static String SHOWMASKS = "ShowMasks";
   public final static String RESETCOUNT = "ResetCount";
   public final static String CELLCOUNT = "CellCount";
   public final static String OBJECTCOUNT = "ObjectCount";
   
   
   public Roi[] analyze (TaggedImage img, JSONObject parms) throws MMScriptException;
}
