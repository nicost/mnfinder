
package org.micromanager.micronuclei.internal.data;

import java.util.ArrayList;
import java.util.List;
import org.micromanager.PropertyMap;
import org.micromanager.propertymap.MutablePropertyMapView;

/**
 *
 * @author Nico
 */
public class ChannelInfoSettings {
   final private MutablePropertyMapView settings_;
   
   public ChannelInfoSettings(MutablePropertyMapView settings) {
      settings_ = settings;
   }
   
   /**
    * Looks for a channel with the given purpose in the propertymap
    * If no such channel is found, return a new, empty channel
    * @param key key under which to find the propertyMaps with ChannelInfo
    * @param purpose purpose that the caller is looking for
    * @return ChannelInfo object that was found, or empty ChannelInfo when not found
    */
   public ChannelInfo getChannelInfoByPurpose (final String key, final String purpose) {
      if (settings_.containsPropertyMapList(key)) {
         List<PropertyMap> pmList = settings_.getPropertyMapList(key);
         for (PropertyMap pm : pmList) {
            ChannelInfo ci = new ChannelInfo(pm);
            if (ci.purpose_.equals(purpose) ) {
               return ci;
            }
         }     
      }
      ChannelInfo ci = new ChannelInfo();
      ci.purpose_ = purpose;
      return ci;
   }
   
   public void storeChannelByPurpose(final String key, final ChannelInfo channel) {
      List<PropertyMap> pml = new ArrayList<PropertyMap>();
      if (settings_.containsPropertyMapList(key)) {
         pml = settings_.getPropertyMapList(key, pml);
      } 
      boolean present = false;
      for (int i = 0; i < pml.size(); i++) {
         if (pml.get(i).containsString(ChannelInfo.PURPOSEKEY) &&
                 pml.get(i).getString(ChannelInfo.PURPOSEKEY, "").
                         equals(channel.purpose_) ) {
            pml.set(i, channel.toPropertyMap());
            present = true;
         }
      }
      if (!present) {
         pml.add(channel.toPropertyMap());
      }
      settings_.putPropertyMapList(key, pml);
   }
         
   
   public void storeChannels(final String key, List<ChannelInfo> channels) {
      List<PropertyMap> pmMap = new ArrayList<PropertyMap>(channels.size());
      for (ChannelInfo channel : channels) {
         pmMap.add(channel.toPropertyMap());
      }
      settings_.putPropertyMapList(key, pmMap);      
   }
   
   public List<ChannelInfo> retrieveChannels (final String key) {
      List<ChannelInfo> ciList = new ArrayList<ChannelInfo>();
      if (settings_.containsPropertyMapList(key)) {
         List<PropertyMap> pmList = settings_.getPropertyMapList(key);
         for (PropertyMap pm : pmList) {
            ciList.add(new ChannelInfo(pm));
         }        
      }
      return ciList;
   }
      
}
