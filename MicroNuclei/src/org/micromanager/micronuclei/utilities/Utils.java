/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.micromanager.micronuclei.utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.Color;
import mmcorej.TaggedImage;
import org.micromanager.data.Image;
import org.micromanager.internal.utils.ImageUtils;

/**
 *
 * @author nico
 */
public class Utils {
   
    /**
    * Normalize input image as follows:  (image - background) / flatfield
    * Flatfield image should have been background subtracted and normalized 
    * at 1.0 for the average pixels values to stay the same
    * @param input Image to be normalized
    * @param background image
    * @param flatField image with average value of 1.0 representing flatness of field
    * @param displayResult
    * @return normalized image
    */
   public static TaggedImage normalize(TaggedImage input, ImagePlus background,
           ImagePlus flatField) {
      ImageCalculator ic = new ImageCalculator();
      // TODO: deal with image of incompatible size and/or type
      if (flatField != null) {
         ImagePlus imp = new ImagePlus("tmp", ImageUtils.makeProcessor(input));
         if (background != null) {
            ic.run("Subtract", imp, background);
         }
         imp = ic.run("Divide, float, 32", imp, flatField);
         IJ.run(imp, "16-bit", "");
         TaggedImage tImg = new TaggedImage(imp.getProcessor().getPixels(), 
                 input.tags);

         return tImg;
      }
      
      return input;
   }
   
   public static ImageProcessor getProcessor (Image img) {
      ImageProcessor ip = null;  
      if (img.getBytesPerPixel() == 1) {
         ip = new ByteProcessor(img.getWidth(), img.getHeight(), (byte[]) img.getRawPixels());
      } else if (img.getBytesPerPixel() == 2) {
         ip = new ShortProcessor(img.getWidth(), img.getHeight());
         ip.setPixels(img.getRawPixels());
      }
      return ip;
      
   }
   
   
}
