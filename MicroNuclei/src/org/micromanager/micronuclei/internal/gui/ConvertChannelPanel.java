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

package org.micromanager.micronuclei.internal.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableColumnModel;
import net.miginfocom.swing.MigLayout;
import org.micromanager.Studio;
import org.micromanager.micronuclei.internal.data.ChannelInfo;
import org.micromanager.micronuclei.internal.data.ChannelInfoSettings;
import org.micromanager.propertymap.MutablePropertyMapView;

/**
 *
 * @author nico
 */
public final class ConvertChannelPanel extends JPanel implements BasePanel {
   
   private static final String CONVERTCHANNELDATA = "ConvertChannelData";
   private static final String PRE = "Pre";
   private static final String POST = "Post";
   
   private final  MutablePropertyMapView settings_;
   private final ConvertChannelTableModel convertChannelTableModel_;
   private final ChannelInfoSettings channelInfoSettings_;
   
   public ConvertChannelPanel(final Studio studio, final Class profileClass) {
      
      settings_ = studio.getUserProfile().getSettings(profileClass);
      final ConvertChannelTable table = new ConvertChannelTable(studio, this);
      convertChannelTableModel_ = new ConvertChannelTableModel();
      channelInfoSettings_ = new ChannelInfoSettings(settings_);
      List<ChannelInfo> channelList = 
              channelInfoSettings_.retrieveChannels(CONVERTCHANNELDATA);
      if (channelList.size() > 0) {
         if (!channelList.get(0).purpose_.equals(PRE)) {
            channelList = new ArrayList<ChannelInfo>();
         } else if (!channelList.get(channelList.size()-1).purpose_.equals(POST)) {
            channelList = new ArrayList<ChannelInfo>();
         }
      } 
      if (channelList.isEmpty() ) {
         ChannelInfo pre = new ChannelInfo();
         pre.purpose_ = "Pre";
         channelList.add(pre);
         ChannelInfo post = new ChannelInfo();
         post.purpose_ = "Post";
         channelList.add(post);
      }
      convertChannelTableModel_.putChannels(channelList);

      studio.events().registerForEvents(convertChannelTableModel_);
      table.setModel(convertChannelTableModel_);
      
      super.setLayout(new MigLayout("insets 0", "[][]", "[][]"));
      
      JScrollPane tableScrollPane = new JScrollPane();      
      tableScrollPane.setHorizontalScrollBarPolicy(
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      tableScrollPane.setVerticalScrollBarPolicy(
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      
      TableColumnModel cm = table.getColumnModel();
      cm.getColumn(0).setPreferredWidth(settings_.getInteger(COL0WIDTH, 40));
      cm.getColumn(1).setPreferredWidth(settings_.getInteger(COL1WIDTH, 20));
      cm.getColumn(2).setPreferredWidth(settings_.getInteger(COL2WIDTH, 100));
      cm.getColumn(3).setPreferredWidth(settings_.getInteger(COL3WIDTH, 50));
      cm.getColumn(4).setPreferredWidth(settings_.getInteger(COL4WIDTH, 50));
      tableScrollPane.setViewportView(table);
      super.add(tableScrollPane, "span 1 2, hmax 75, wmax 320 ");
      
   }
   
   public void storeChannelsInProfile() {
      channelInfoSettings_.storeChannels(CONVERTCHANNELDATA, getChannels());
   }
     
   public void addConvertChannel() {
      ChannelInfo newConvertChannel = new ChannelInfo();
      convertChannelTableModel_.addConvertChannel(newConvertChannel);
   }

   public void removeConvertChannel() {
      convertChannelTableModel_.removeConvertChannel();
   }
   
   @Override
   public void updateExposureTime(int rowIndex) {
      ChannelInfo cInfo = convertChannelTableModel_.getChannels().get(rowIndex);
      cInfo.exposureTimeMs_ = settings_.getDouble(
              cInfo.channelName_ + EXPOSURETIME, 100.0);
      convertChannelTableModel_.fireTableCellUpdated(rowIndex, 3);
   }

   public void storeChannelExposureTime(int rowIndex) {
      ChannelInfo cInfo = convertChannelTableModel_.getChannels().get(rowIndex);
      settings_.putDouble(
              cInfo.channelName_ + EXPOSURETIME, cInfo.exposureTimeMs_);
   }

   @Override
   public void updateColor(int rowIndex) {
      ChannelInfo cInfo = convertChannelTableModel_.getChannels().get(rowIndex);
      cInfo.displayColor_ = settings_.getColor(
              cInfo.channelName_ + COLOR, Color.GREEN);
      convertChannelTableModel_.fireTableCellUpdated(rowIndex, 4);
   }

   public void storeChannelColor(int rowIndex) {
      ChannelInfo cInfo = convertChannelTableModel_.getChannels().get(rowIndex);
      settings_.putColor(cInfo.channelName_ + COLOR, cInfo.displayColor_);
   }
   
   public List<ChannelInfo> getChannels () {
      return convertChannelTableModel_.getChannels();
   }
   
   public String getPurpose(int rowIndex) {
      return (String) convertChannelTableModel_.getValueAt(rowIndex, 0);
   }
   
}