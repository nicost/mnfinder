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
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Renders the color of an entry in a table
 * Displays a JLabel with the desired background color
 * 
 * @author nico
 */
public class ColorRenderer extends JLabel implements TableCellRenderer {
   
   public ColorRenderer () {
      super.setOpaque(true);
   }

   @Override
   public Component getTableCellRendererComponent(JTable table, Object value, 
           boolean isSelected, boolean hasFocus, int row, int column) {
      Color newColor = (Color) value;
      super.setBackground(newColor);
      return this;
   }
   
}
