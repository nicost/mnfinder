
package org.micromanager.micronuclei.internal.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import net.miginfocom.swing.MigLayout;
import org.micromanager.micronuclei.internal.data.ChannelInfo;

/**
 *
 * @author Nico
 */
public class ChannelFrame extends JFrame {
   final private ChannelTableModel channelTableModel_;
   
   public ChannelFrame() {
      channelTableModel_ = new ChannelTableModel();
      super.setLayout(new MigLayout());
      
      JScrollPane tableScrollPane = new JScrollPane();      
      tableScrollPane.setHorizontalScrollBarPolicy(
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      tableScrollPane.setVerticalScrollBarPolicy(
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      
      final JTable table = new JTable(channelTableModel_);
      tableScrollPane.setViewportView(table);
      
      final JButton plusButton = new JButton("+");
      plusButton.setIcon(new ImageIcon(getClass().getResource(
              "/org/micromanager/icons/plus.png")));
      plusButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            channelTableModel_.addChannel(new ChannelInfo());
         }
      });
      
      final JButton minusButton = new JButton("-");
      minusButton.setIcon(new ImageIcon(getClass().getResource(
               "/org/micromanager.icons/minus.png")));
      minusButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            for (int row : table.getSelectedRows()) {
               channelTableModel_.removeChannel(row);
            }
         }
      });
      
   }
   
}
