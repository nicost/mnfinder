
package org.micromanager.micronuclei.internal.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.micromanager.micronuclei.internal.data.ChannelInfo;

/**
 *
 * @author Nico
 */
public class ChannelTableModel extends AbstractTableModel {
   private static final String[] COLUMNNAMES = {"Use", "Channel", "Exp.", "Color"};
   private final List<ChannelInfo> rowData_;
   
   public ChannelTableModel() {
      rowData_ = new ArrayList<ChannelInfo>();
   }
   
   public void addChannel(ChannelInfo channelInfo) {
      rowData_.add(channelInfo);
   }
   
   public void removeChannel(int nr) {
      rowData_.remove(nr);
      super.fireTableRowsDeleted(nr, nr);
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
   public Object getValueAt(int rowIndex, int columnIndex) {
      switch (columnIndex)  {
         case 0: return rowData_.get(rowIndex).use_;
         case 1: return rowData_.get(rowIndex).channelName_;
         case 2: return rowData_.get(rowIndex).exposureTimeMs_;
         case 3: return rowData_.get(rowIndex).displayColor_;
      }
      return null;
   }
   
   @Override
   public void setValueAt(Object value, int rowIndex, int columnIndex) {
      switch (columnIndex) {
         case 0:
            rowData_.get(rowIndex).use_ = (Boolean) value;
            break;
         case 1:
            rowData_.get(rowIndex).channelName_ = (String) value;
            break;
         case 2:
            rowData_.get(rowIndex).exposureTimeMs_ = (Double) value;
            break;
         case 3:
            rowData_.get(rowIndex).displayColor_ = (Color) value;
         default:
            break;
      }
      fireTableCellUpdated(rowIndex, columnIndex);
   }
   
}
