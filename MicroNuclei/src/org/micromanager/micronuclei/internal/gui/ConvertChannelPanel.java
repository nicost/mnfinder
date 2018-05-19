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
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableColumnModel;
import net.miginfocom.swing.MigLayout;
import org.micromanager.Studio;
import org.micromanager.micronuclei.internal.data.ChannelInfo;
import org.micromanager.propertymap.MutablePropertyMapView;

/**
 *
 * @author nico
 */
public final class ConvertChannelPanel extends JPanel implements BasePanel {
   
   private static final String CONVERTCHANNELDATA = "ConvertChannelData";
   
   private final  MutablePropertyMapView settings_;
   private final ConvertChannelTableModel convertChannelTableModel_;
   
   public ConvertChannelPanel(final Studio studio, final Class profileClass) {
      
      settings_ = studio.getUserProfile().getSettings(profileClass);
      final ConvertChannelTable table = new ConvertChannelTable(studio, this);
      convertChannelTableModel_ = new ConvertChannelTableModel();
      updateChannelsFromProfile();

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
      ChannelInfo.storeChannelsInProfile(settings_, 
              CONVERTCHANNELDATA, getChannels());
   }
   
   public void updateChannelsFromProfile() {
      List<ChannelInfo> channels = ChannelInfo.restoreChannelsFromProfile(
              settings_, CONVERTCHANNELDATA);
      /*
      if (channels.size() <= ConvertChannelTableModel.NRCHANNELS) {
         for (int i = 0; i < channels.size(); i++) {
            convertChannelTableModel_.setChannel(channels.get(i), i);
         }
      }
      */
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
      return convertChannelTableModel_.getPurpose(rowIndex);
   }
   
}