
package org.micromanager.micronuclei.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import org.micromanager.micronuclei.analysisinterface.AnalysisProperty;

/**
 *
 * @author nico
 */
public class PropertyGUI {
   private final AnalysisProperty prop_;
   private final JComponent jc_;
   
   public PropertyGUI(AnalysisProperty prop) {
      super();
      prop_ = prop;
      final JFormattedTextField textField = new JFormattedTextField();
      jc_ = textField;
      if ((prop.get() instanceof Double) || (prop.get() instanceof Integer) ) {
         if (prop.get() instanceof Double)
            textField.setValue(((Double) prop.get()).toString());
         else if (prop.get() instanceof Integer)
            textField.setValue( ((Integer) prop.get()).toString());
         textField.setColumns(4);
         textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               parse(textField);
            }
         });
         textField.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
               parse(textField);
            }

            @Override
            public void keyPressed(KeyEvent e) {
               }

            @Override
            public void keyReleased(KeyEvent e) {
            }
         });
      }
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