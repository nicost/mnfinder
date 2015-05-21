
package org.micromanager.micronuclei.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JTextField;


/**
 * DragDropUtil
 * Handler for drop events in Micro-Manager
 * Checks if files or folders are dropped onto Micro-Manager, and 
 * tries to open them.
 * 
 * @author nico
 * 
 */
public class DragDropUtil implements DropTargetListener {
   private final JTextField textField_;
   
   public DragDropUtil(JTextField textField) {
      textField_ = textField;
   }

   @Override
   public void dragEnter(DropTargetDragEvent dtde) {
      //throw new UnsupportedOperationException("Not supported yet.");
   }

   @Override
   public void dragOver(DropTargetDragEvent dtde) {
      //throw new UnsupportedOperationException("Not supported yet.");
   }

   @Override
   public void dropActionChanged(DropTargetDragEvent dtde) {
      //throw new UnsupportedOperationException("Not supported yet.");
   }

   @Override
   public void dragExit(DropTargetEvent dte) {
      //throw new UnsupportedOperationException("Not supported yet.");
   }

   /**
    * This function does the actual work
    */
   @Override
   public void drop(final DropTargetDropEvent dtde) {

      try {
         Transferable tr = dtde.getTransferable();
         DataFlavor[] flavors = tr.getTransferDataFlavors();
         for (DataFlavor flavor : flavors) {
            if (flavor.isFlavorJavaFileListType()) {
               dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
               java.util.List list = (java.util.List) tr.getTransferData(flavor);
               for (Object list1 : list) {
                  File f = (File) list1;
                  if (f.isFile()) {
                     textField_.setText(f.getAbsolutePath());
                  }
               }
               dtde.dropComplete(true);
               return;
            }
         }
      } catch (UnsupportedFlavorException ex) {
      } catch (IOException ex) {
      } 

   }
}
