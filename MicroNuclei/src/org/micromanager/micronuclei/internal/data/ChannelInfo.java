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
import org.micromanager.UserProfile;

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
    * @param profile   UserProfile to safe channels to
    * @param classKey  Class to which these prefs are keyed
    * @param key       Yet another key to key the data
    * @param channels  List of channeInfos to be stored in Prefs
    */
   public static void storeChannelsInProfile(UserProfile profile, Class classKey, 
           final String key, List<ChannelInfo> channels) {
      for (int i = 0; i < channels.size(); i++) {
         profile.setBoolean(classKey,
                 key + i + ChannelInfo.USEKEY, channels.get(i).use_);
         profile.setString(classKey,
                 key + i + ChannelInfo.CHANNELNAMEKEY, channels.get(i).channelName_);
         profile.setDouble(classKey,
                 key + i + ChannelInfo.EXOSURETIMEKEY, channels.get(i).exposureTimeMs_);
         if (channels.get(i).displayColor_ != null) {
            profile.setInt(classKey,
                 key + i + ChannelInfo.DISPLAYCOLOR, channels.get(i).displayColor_.getRGB());
         }
      }
      profile.setInt(classKey, key + "nr", channels.size());
   }
   
   /**
    * restore Channel List from Preferences
    * @param profile    UserProfile to read data from
    * @param classKey   Class Key to use
    * @param key        String key to add to Class Key
    * @return           List of ChannelInfo
    */
   public static List<ChannelInfo> restoreChannelsFromProfile(UserProfile profile, Class classKey, 
           final String key) {
      int nrChannels = profile.getInt(classKey, key + "nr", 0);
      List<ChannelInfo> channelList= new ArrayList<ChannelInfo>(nrChannels);
      for (int i = 0; i < nrChannels; i++) {
         ChannelInfo ci = new ChannelInfo();
         ci.use_ = profile.getBoolean(classKey, 
                 key + i + ChannelInfo.USEKEY, true);
         ci.channelName_ = profile.getString(classKey, 
                 key + i + ChannelInfo.CHANNELNAMEKEY, "");
         ci.exposureTimeMs_ = profile.getDouble(classKey, 
                 key + i + ChannelInfo.EXOSURETIMEKEY, 100.0);
         ci.displayColor_ = new Color( profile.getInt(classKey, 
                 key + i + ChannelInfo.DISPLAYCOLOR, 0) );
         channelList.add(ci);
      }
      return channelList;
   }

}
