///////////////////////////////////////////////////////////////////////////////
//PROJECT:       MicroNuclei detection project
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nico Stuurman
//
// COPYRIGHT:    Regents of the University of California 2015
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableColumnModel;
import net.miginfocom.swing.MigLayout;
import org.micromanager.Studio;
import org.micromanager.UserProfile;
import org.micromanager.micronuclei.internal.data.ChannelInfo;

/**
 * Generates a panel with a view on a channel table, and + and - buttons 
 * that let the user add and remove channels
 * 
 * @author Nico
 */
public final class ChannelPanel extends JPanel implements BasePanel {
   
   static final String CHANNELDATA = "ChannelData";
   
   private final UserProfile userProfile_;
   private final Class profileClass_;
   private final ChannelTableModel channelTableModel_;
   
   public ChannelPanel(final Studio studio, final Class profileClass) {
      
      userProfile_ = studio.getUserProfile();
      profileClass_ = profileClass;
      final ChannelTable table = new ChannelTable(studio, this);
      channelTableModel_ = new ChannelTableModel();
      restoreChannelsFromProfile();

      studio.events().registerForEvents(channelTableModel_);
      table.setModel(channelTableModel_);
      
      super.setLayout(new MigLayout("insets 0", "[][]", "[][]"));
      
      JScrollPane tableScrollPane = new JScrollPane();      
      tableScrollPane.setHorizontalScrollBarPolicy(
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      tableScrollPane.setVerticalScrollBarPolicy(
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      
      TableColumnModel cm = table.getColumnModel();
      cm.getColumn(0).setPreferredWidth(userProfile_.getInt(profileClass_, COL0WIDTH, 20));
      cm.getColumn(1).setPreferredWidth(userProfile_.getInt(profileClass_, COL1WIDTH, 100));
      cm.getColumn(2).setPreferredWidth(userProfile_.getInt(profileClass_, COL2WIDTH, 50));
      cm.getColumn(3).setPreferredWidth(userProfile_.getInt(profileClass_, COL3WIDTH, 50));
      tableScrollPane.setViewportView(table);
      super.add(tableScrollPane, "span 1 2, hmax 75, wmax 320 ");
      
      final JButton plusButton = new JButton("");
      plusButton.setIcon(new ImageIcon(getClass().getResource(
              "/org/micromanager/icons/plus.png")));
      plusButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            ChannelInfo cInfo = new ChannelInfo();
            if (studio.core().getChannelGroup() != null && 
                    studio.core().getChannelGroup().length() > 0) {
               try {
                  String channel = studio.core().getCurrentConfigFromCache(
                          studio.core().getChannelGroup());
                  cInfo.channelName_ = channel;
               } catch (Exception ex) {
                  }
            }
            channelTableModel_.addChannel(cInfo);
            updateColor(channelTableModel_.getRowCount() - 1);
            updateExposureTime(channelTableModel_.getRowCount() - 1);
            storeChannelsInProfile();
         }
      });
      super.add(plusButton, "hmin 25, wrap");
      
      final JButton minusButton = new JButton("");
      minusButton.setIcon(new ImageIcon(getClass().getResource(
               "/org/micromanager/icons/minus.png")));
      minusButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            for (int row : table.getSelectedRows()) {
               channelTableModel_.removeChannel(row);
               storeChannelsInProfile();
            }
         }
      });
      super.add(minusButton, "hmin 25, top, wrap");
      
   }
   
   /**
    * Storing the arraylist as an object led to problems, so write everything out
    * individually
    */
   public void storeChannelsInProfile() {
      ChannelInfo.storeChannelsInProfile(userProfile_, this.getClass(), 
              CHANNELDATA, getChannels());
   }
   
   
   public void restoreChannelsFromProfile() {
      channelTableModel_.setChannels( ChannelInfo.restoreChannelsFromProfile(
              userProfile_, this.getClass(), CHANNELDATA) );
   }
   
    @Override
   public void updateExposureTime(int rowIndex) {
      ChannelInfo cInfo = channelTableModel_.getChannels().get(rowIndex);
      try {
         cInfo.exposureTimeMs_ = userProfile_.getObject(profileClass_, 
                 cInfo.channelName_ + EXPOSURETIME, 100.0);
         channelTableModel_.fireTableCellUpdated(rowIndex, 3);
      } catch (IOException ex) {
         // TODO
      }
   }
   
   public void storeChannelExposureTime(int rowIndex) {
      ChannelInfo cInfo = channelTableModel_.getChannels().get(rowIndex);
      try {
         userProfile_.setObject(profileClass_, 
                 cInfo.channelName_ + EXPOSURETIME, cInfo.exposureTimeMs_);
      } catch (IOException ex) {
         // TODO 
      }
   }
   
   
   @Override
   public void updateColor(int rowIndex) {
      ChannelInfo cInfo = channelTableModel_.getChannels().get(rowIndex);
      try {
         cInfo.displayColor_ = userProfile_.getObject(
                 profileClass_, cInfo.channelName_ + COLOR, Color.GREEN);
         channelTableModel_.fireTableCellUpdated(rowIndex, 3);
      } catch (IOException ex) {
         // TODO
      }
   }
   
   public void storeChannelColor(int rowIndex) {
      ChannelInfo cInfo = channelTableModel_.getChannels().get(rowIndex);
      try {
         userProfile_.setObject(profileClass_, 
                 cInfo.channelName_ + COLOR, cInfo.displayColor_);
      } catch (IOException ex) {
         // TODO 
      }
   }
   
   public List<ChannelInfo> getChannels () {
      return channelTableModel_.getChannels();
   }
   
}
