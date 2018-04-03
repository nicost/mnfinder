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
import java.util.ArrayList;
import java.util.List;
import org.micromanager.propertymap.MutablePropertyMapView;

/**
 *
 * @author Nico
 */
public class ChannelInfo  {
   public final static String USEKEY = "Use";
   public final static String CHANNELNAMEKEY = "ChannelName";
   public final static String EXOSURETIMEKEY = "ExposureTime";
   public final static String  DISPLAYCOLOR = "DisplayColor";
   
   
   public boolean use_ = true;
   public String channelName_ = "";
   public double exposureTimeMs_ = 100.0;
   public Color displayColor_ = null;
   
   
   
    /**
    * Storing the arraylist as an object led to problems, so write everything out
    * individually
    * @param settings   PropertyMap to safe channels to
    * @param key       Yet another key to key the data
    * @param channels  List of channeInfos to be stored in Prefs
    */
   public static void storeChannelsInProfile(MutablePropertyMapView settings, 
           final String key, List<ChannelInfo> channels) {
      for (int i = 0; i < channels.size(); i++) {
         settings.putBoolean(key + i + ChannelInfo.USEKEY, 
                 channels.get(i).use_);
         settings.putString(key + i + ChannelInfo.CHANNELNAMEKEY, 
                 channels.get(i).channelName_);
        settings.putDouble(key + i + ChannelInfo.EXOSURETIMEKEY, 
                channels.get(i).exposureTimeMs_);
         if (channels.get(i).displayColor_ != null) {
            settings.putInteger(key + i + ChannelInfo.DISPLAYCOLOR, 
                    channels.get(i).displayColor_.getRGB());
         }
      }
      settings.putInteger(key + "nr", channels.size());
   }
   
   /**
    * restore Channel List from Preferences
    * @param settings   MutablePropertyMapView to read data from
    * @param key        String key to add to Class Key
    * @return           List of ChannelInfo
    */
   public static List<ChannelInfo> restoreChannelsFromProfile(
           MutablePropertyMapView settings, 
           final String key) {
      int nrChannels = settings.getInteger(key + "nr", 0);
      List<ChannelInfo> channelList= new ArrayList<ChannelInfo>(nrChannels);
      for (int i = 0; i < nrChannels; i++) {
         ChannelInfo ci = new ChannelInfo();
         ci.use_ = settings.getBoolean(key + i + ChannelInfo.USEKEY, true);
         ci.channelName_ = settings.getString(
                 key + i + ChannelInfo.CHANNELNAMEKEY, "");
         ci.exposureTimeMs_ = settings.getDouble(
                 key + i + ChannelInfo.EXOSURETIMEKEY, 100.0);
         ci.displayColor_ = new Color( settings.getInteger(
                 key + i + ChannelInfo.DISPLAYCOLOR, 0) );
         channelList.add(ci);
      }
      return channelList;
   }

}
