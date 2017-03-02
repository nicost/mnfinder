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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 * Generates a button that is displayed in the cell of the table
 * Clicking the button opens the color-editor
 * Resulting color is returned as the cell's value
 * 
 * @author nico
 */
public class ColorEditor extends AbstractCellEditor implements TableCellEditor {
   
   Color currentColor_;
   private final JButton button_;
   private final JColorChooser colorChooser_;
   private final JDialog dialog_;
   
   public ColorEditor() {
      button_ = new JButton();
      button_.addActionListener(new ActionListener() { 
         @Override
         public void actionPerformed(ActionEvent e) {
               button_.setBackground(currentColor_);
               colorChooser_.setColor(currentColor_);
               dialog_.setVisible(true);
               fireEditingStopped();
            }
      });
      button_.setBorderPainted(false);
      
      colorChooser_ = new JColorChooser();
      dialog_ = JColorChooser.createDialog(button_,
              "Pick a Color",
              true,
              colorChooser_,
              new ActionListener() {
                  @Override
                  public void actionPerformed(ActionEvent e) {
                     currentColor_ = colorChooser_.getColor();
                  }
              }, 
              null);
   }

   @Override
   public Object getCellEditorValue() {
      return currentColor_;
   }

   @Override
   public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      currentColor_ = (Color) value;
      return button_;
   }

     
}
