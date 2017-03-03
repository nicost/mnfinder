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

import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import org.micromanager.Studio;

/**
 *
 * @author nico
 */
public class ConvertChannelTable extends JTable {
   final private Studio studio_;
   final private ChannelEditor channelEditor_;
   final private ColorEditor colorEditor_;
   final private ConvertChannelPanel channelPanel_;
   
   public ConvertChannelTable(Studio studio, ConvertChannelPanel channelPanel) {
      studio_ = studio;
      channelEditor_ = new ChannelEditor(studio_, channelPanel);
      colorEditor_ = new ColorEditor();
      studio_.events().registerForEvents(channelEditor_);
      channelPanel_ = channelPanel;
   }
   
   @Override
   public TableCellRenderer getCellRenderer(int rowIndex, int columnIndex) {
      if (columnIndex == 4) {
         return new ColorRenderer();
      }
      return super.getCellRenderer(rowIndex, columnIndex);
   }
   
   @Override
   public TableCellEditor getCellEditor(int rowIndex, int columnIndex) {
      columnIndex = convertColumnIndexToModel(columnIndex);
      switch (columnIndex) {
         case 2:
            return channelEditor_;
         case 4:
            return colorEditor_;
      }
      return super.getCellEditor(rowIndex, columnIndex); // 0 is Boolean, and 2 is Double
   }
   
   /**
    * Implemented by calling the super methods
    * Use the opportunity to save the new values to the user profile
    * @param value
    * @param rowIndex
    * @param columnIndex 
    */
   @Override
   public void setValueAt(Object value, int rowIndex, int columnIndex) {
      super.setValueAt(value, rowIndex, columnIndex);
      if (columnIndex == 4) {
         channelPanel_.storeChannelColor(rowIndex);
      }
      channelPanel_.storeChannelsInProfile();
   }
}