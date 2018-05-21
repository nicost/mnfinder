
package org.micromanager.micronuclei.internal.data;

import java.util.ArrayList;
import java.util.List;
import org.micromanager.PropertyMap;
import org.micromanager.propertymap.MutablePropertyMapView;

/**
 *
 * @author NicoLocal
 */
public class ChannelInfoSettings {
   final private MutablePropertyMapView settings_;
   
   public ChannelInfoSettings(MutablePropertyMapView settings) {
      settings_ = settings;
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
