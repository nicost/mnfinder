///////////////////////////////////////////////////////////////////////////////
//PROJECT:       MicroNuclei detection project
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nico Stuurman
//
// COPYRIGHT:    Regents of the University of California 2017
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

package org.micromanager.micronuclei.internal.data;

import java.awt.Color;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;

/**
 *
 * @author Nico
 */
public class ChannelInfo  {
   public final static String PURPOSEKEY = "Purpose";
   public final static String USEKEY = "Use";
   public final static String CHANNELNAMEKEY = "ChannelName";
   public final static String EXPOSURETIMEKEY = "ExposureTime";
   public final static String DISPLAYCOLOR = "DisplayColor";
   
   
   public String purpose_ = "";
   public boolean use_ = true;
   public String channelName_ = "";
   public double exposureTimeMs_ = 100.0;
   public Color displayColor_ = null;
   
   public PropertyMap toPropertyMap() {
      PropertyMap.Builder builder = PropertyMaps.builder();
      return builder.putString(PURPOSEKEY, purpose_).
              putBoolean(USEKEY, use_). 
              putString(CHANNELNAMEKEY, channelName_). 
              putDouble(EXPOSURETIMEKEY, exposureTimeMs_). 
              putColor(DISPLAYCOLOR, displayColor_).build();
   }
   
   public ChannelInfo() {};
   
   public ChannelInfo(PropertyMap pm) {
      if (pm.containsString(PURPOSEKEY)) { 
         purpose_ = pm.getString(PURPOSEKEY, purpose_);
      }
      if (pm.containsBoolean(USEKEY)) { use_ = pm.getBoolean(USEKEY, use_); }
      if (pm.containsString(CHANNELNAMEKEY)) {
         channelName_ = pm.getString(CHANNELNAMEKEY, channelName_);
      }
      if (pm.containsDouble(EXPOSURETIMEKEY)) {
         exposureTimeMs_ = pm.getDouble(EXPOSURETIMEKEY, exposureTimeMs_);
      }
      if (pm.containsColor(DISPLAYCOLOR)) {
         displayColor_ = pm.getColor(DISPLAYCOLOR, displayColor_);
      }
      
   }

}
