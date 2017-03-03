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

import com.google.common.eventbus.Subscribe;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import mmcorej.StrVector;
import org.micromanager.Studio;
import org.micromanager.events.ConfigGroupChangedEvent;

/**
 * Editor for Channel name component in Channel table
 * 
 * @author nico
 */
public final class ChannelEditor extends AbstractCellEditor implements TableCellEditor {

   private final JComboBox channelSelect_;   
   private final Studio studio_;
   private final BasePanel basePanel_;
   private String channelGroup_;
   
   public ChannelEditor(Studio studio, BasePanel basePanel) {
      studio_ = studio;
      basePanel_ = basePanel;
      channelSelect_ = new JComboBox();
      String channelGroup = studio_.core().getChannelGroup();
      if (channelGroup != null) {
         updateChannelList(channelGroup);
         channelGroup_ = channelGroup;
      }
   }
   
   @Subscribe
   public void onConfigGroupChanged (ConfigGroupChangedEvent cgce) {
      if (!channelGroup_.equals(cgce.getGroupName())) {
         channelGroup_ = cgce.getGroupName();
         updateChannelList(channelGroup_);
      }
   }
   
   public void updateChannelList (String channelGroup) {
      channelSelect_.removeAllItems();
      StrVector configs = studio_.core().getAvailableConfigs(channelGroup);
      for (String config : configs) {
         channelSelect_.addItem(config);
      }
   }
   
   @Override
   public Component getTableCellEditorComponent(JTable table, Object value,
           boolean isSelected, final int rowIndex, int colIndex) {
      
      colIndex = table.convertColumnIndexToModel(colIndex);
      channelSelect_.setSelectedItem(table.getModel().getValueAt(rowIndex, colIndex));

      // end editing on selection change
      channelSelect_.addActionListener(new ActionListener() {
         @Override
            public void actionPerformed(ActionEvent e) {
               fireEditingStopped();
               basePanel_.updateColor(rowIndex);
            }
         });

         // Return the configured component
         return channelSelect_;
   }

   @Override
   public Object getCellEditorValue() {
      return channelSelect_.getSelectedItem();
   }


   
}
