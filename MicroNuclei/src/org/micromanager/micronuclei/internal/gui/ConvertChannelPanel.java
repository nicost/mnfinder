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
import java.io.IOException;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableColumnModel;
import net.miginfocom.swing.MigLayout;
import org.micromanager.Studio;
import org.micromanager.UserProfile;
import org.micromanager.micronuclei.internal.data.ChannelInfo;

/**
 *
 * @author nico
 */
public final class ConvertChannelPanel extends JPanel implements BasePanel {
   private static final String COL0WIDTH = "Col0Width";  
   private static final String COL1WIDTH = "Col1Width";
   private static final String COL2WIDTH = "Col2Width";
   private static final String COL3WIDTH = "Col3Width";
   private static final String COL4WIDTH = "Col3Width";
   private static final String CONVERTCHANNELDATA = "ConvertChannelData";
   private static final String COLOR = "Color";
   
   private final UserProfile userProfile_;
   private final Class profileClass_;
   private final ConvertChannelTableModel channelTableModel_;
   
   public ConvertChannelPanel(final Studio studio, final Class profileClass) {
      
      userProfile_ = studio.getUserProfile();
      profileClass_ = profileClass;
      final ConvertChannelTable table = new ConvertChannelTable(studio, this);
      channelTableModel_ = new ConvertChannelTableModel();
      for (int i = 0; i < ConvertChannelTableModel.NRCHANNELS; i++) {
         updateChannelFromProfile(i);
      }

      studio.events().registerForEvents(channelTableModel_);
      table.setModel(channelTableModel_);
      
      super.setLayout(new MigLayout("insets 0", "[][]", "[][]"));
      
      JScrollPane tableScrollPane = new JScrollPane();      
      tableScrollPane.setHorizontalScrollBarPolicy(
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      tableScrollPane.setVerticalScrollBarPolicy(
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      
      TableColumnModel cm = table.getColumnModel();
      cm.getColumn(0).setPreferredWidth(userProfile_.getInt(profileClass_, COL0WIDTH, 40));
      cm.getColumn(1).setPreferredWidth(userProfile_.getInt(profileClass_, COL1WIDTH, 20));
      cm.getColumn(2).setPreferredWidth(userProfile_.getInt(profileClass_, COL2WIDTH, 100));
      cm.getColumn(3).setPreferredWidth(userProfile_.getInt(profileClass_, COL3WIDTH, 50));
      cm.getColumn(4).setPreferredWidth(userProfile_.getInt(profileClass_, COL4WIDTH, 50));
      tableScrollPane.setViewportView(table);
      super.add(tableScrollPane, "span 1 2, hmax 75, wmax 320 ");
      
   }
   
   
   public void storeChannelsInProfile() {
      try {
         userProfile_.setObject(profileClass_, CONVERTCHANNELDATA,
                 channelTableModel_.getChannels());
      } catch (IOException ex) {
         // TODO report exception
      }
   }
   
   
   public void updateChannelFromProfile(int channelIndex) {
      try {
         List<ChannelInfo> channels = (List<ChannelInfo>) 
              userProfile_.getObject(profileClass_, CONVERTCHANNELDATA, null);
         channelTableModel_.setChannel(channels.get(channelIndex), channelIndex);
      } catch (IOException ex) {
         // TODO Report exception
      } catch (NullPointerException ne) {
         // This is always thrown before the profile is populated
      }
   }
   
   @Override
   public void updateColor(int rowIndex) {
      ChannelInfo cInfo = channelTableModel_.getChannels().get(rowIndex);
      try {
         cInfo.displayColor_ = userProfile_.getObject(profileClass_, cInfo.channelName_ + COLOR, Color.GREEN);
      } catch (IOException ex) {
         // TODO
      }
   }
   
   public void storeChannelColor(int rowIndex) {
      ChannelInfo cInfo = channelTableModel_.getChannels().get(rowIndex);
      try {
         userProfile_.setObject(profileClass_, cInfo.channelName_ + COLOR, cInfo.displayColor_);
      } catch (IOException ex) {
         // TODO 
      }
   }
   
   public List<ChannelInfo> getChannels () {
      return channelTableModel_.getChannels();
   }
}