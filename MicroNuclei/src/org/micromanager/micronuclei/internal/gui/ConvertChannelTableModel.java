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
import javax.swing.table.AbstractTableModel;
import org.micromanager.micronuclei.internal.data.ChannelInfo;

/**
 *
 * @author nico
 */
public class ConvertChannelTableModel extends AbstractTableModel {
   private static final String[] COLUMNNAMES = {"Purpose", "Use", "Channel", "Exp.", "Color"};
   public List<String> purposeList_;
   private final List<ChannelInfo> rowData_;
   
   public ConvertChannelTableModel() {
      purposeList_ = new ArrayList<String>(10);
      purposeList_.add("Pre");
      purposeList_.add("Post");
      rowData_ = new ArrayList<ChannelInfo>(purposeList_.size());
      rowData_.add(new ChannelInfo());
      rowData_.add(new ChannelInfo());
   }
   
   public void addConvertChannel(ChannelInfo channelInfo) {
      purposeList_.add(purposeList_.size() - 1, "Convert-" + (purposeList_.size() - 1));
      rowData_.add(rowData_.size() - 2, channelInfo);
      super.fireTableRowsDeleted(rowData_.size() - 2, rowData_.size() -2);
   }
   
   public void removeConvertChannel() {
      purposeList_.remove(purposeList_.size() - 2);
      rowData_.remove(rowData_.size() - 2);
      super.fireTableRowsDeleted(rowData_.size() - 2, rowData_.size() -2);
   }
   
   public void setChannel(ChannelInfo channelInfo, int index) {
      rowData_.set(index, channelInfo);
      super.fireTableRowsInserted(rowData_.size(), rowData_.size());
   }
   
   public List<ChannelInfo> getChannels() {
      return rowData_;
   }
   
   public String getPurpose(int rowIndex) {
      return purposeList_.get(rowIndex);
   }
   
   @Override
   public int getRowCount() {
      return rowData_.size();
   }

   @Override
   public int getColumnCount() {
      return COLUMNNAMES.length;
   }
   
   @Override
   public String getColumnName(int columnIndex) {
      return COLUMNNAMES[columnIndex];
   }
   
   @Override 
   public Class<?> getColumnClass(int columnIndex) {
      switch (columnIndex) {
         case 0: return String.class;
         case 1: return Boolean.class;
         case 2: return String.class;
         case 3: return Double.class;
         case 4: return Color.class;
      }
      return Object.class;
   }

   @Override
   public Object getValueAt(int rowIndex, int columnIndex) {
      switch (columnIndex)  {
         case 0: return purposeList_.get(rowIndex);
         case 1: return rowData_.get(rowIndex).use_;
         case 2: return rowData_.get(rowIndex).channelName_;
         case 3: return rowData_.get(rowIndex).exposureTimeMs_;
         case 4: return rowData_.get(rowIndex).displayColor_;
      }
      return null;
   }
   
   @Override
   public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex != 0;
   }
   
   @Override
   public void setValueAt(Object value, int rowIndex, int columnIndex) {
      switch (columnIndex) {
         case 1:
            rowData_.get(rowIndex).use_ = (Boolean) value;
            break;
         case 2:
            rowData_.get(rowIndex).channelName_ = (String) value;
            break;
         case 3:
            rowData_.get(rowIndex).exposureTimeMs_ = (Double) value;
            break;
         case 4:
            rowData_.get(rowIndex).displayColor_ = (Color) value;
         default:
            break;
      }
      fireTableCellUpdated(rowIndex, columnIndex);
   }
   
}