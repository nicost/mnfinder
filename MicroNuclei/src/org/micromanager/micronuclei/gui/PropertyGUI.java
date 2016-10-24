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

package org.micromanager.micronuclei.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;

/**
 *
 * @author nico
 */
public class PropertyGUI {
   private final AnalysisProperty prop_;
   private final JComponent jc_;
   
   public PropertyGUI(AnalysisProperty prop) {
      prop_ = prop;
      if (prop_.isBoolean()) {
         final JCheckBox checkBox = new JCheckBox();
         jc_ = checkBox;
         if ((Boolean) prop_.get())
            checkBox.setSelected(true);
         else
            checkBox.setSelected(false);
         checkBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
               prop_.set((Boolean) checkBox.isSelected());
            }
         });
      } else {
         final JFormattedTextField textField = new JFormattedTextField();
         jc_ = textField;
         if ((prop.get() instanceof Double) || (prop.get() instanceof Integer)) {
            if (prop.get() instanceof Double) {
               textField.setValue(((Double) prop.get()).toString());
            } else if (prop.get() instanceof Integer) {
               textField.setValue(((Integer) prop.get()).toString());
            }
            textField.setColumns(4);
            textField.addActionListener(new ActionListener() {
               @Override
               public void actionPerformed(ActionEvent e) {
                  parse(textField);
               }
            });
            textField.getDocument().addDocumentListener(new DocumentListener() {

               @Override
               public void insertUpdate(DocumentEvent de) {
                  parse(textField);
               }

               @Override
               public void removeUpdate(DocumentEvent de) {
                  parse(textField);
               }

               @Override
               public void changedUpdate(DocumentEvent de) {
                  parse(textField);
               }
            });
         }
      }
      jc_.setToolTipText(prop.getTooltip());
   }
   
   public JComponent getJComponent() {
      return jc_;
   }
   
   private void parse(JFormattedTextField textField) {
      try {
         if (prop_.get() instanceof Double) {
            Double d = Double.parseDouble(textField.getText());
            prop_.set(d);
         } else if (prop_.get() instanceof Integer) {
            Integer i = Integer.parseInt(textField.getText());
            prop_.set(i);
         }
      } catch (NumberFormatException nfe) {
         // ignore
      }
   }
   
}