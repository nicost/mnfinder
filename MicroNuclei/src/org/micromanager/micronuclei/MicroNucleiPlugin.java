/*
 * 
 * Copyright University of California
 * 
 * LICENSE:      This file is distributed under the BSD license.
 *               License text is included with the source distribution.
 *
 *               This file is distributed in the hope that it will be useful,
 *               but WITHOUT ANY WARRANTY; without even the implied warranty
 *               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
 */

package org.micromanager.micronuclei;

import javax.swing.JOptionPane;

import mmcorej.CMMCore;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;


public class MicroNucleiPlugin implements MMPlugin {
   public static final String menuName = "MicroNuclei";
   public static final String tooltipDescription =
      "Cool screening technology";

   // Provides access to the Micro-Manager Java API (for GUI control and high-
   // level functions).
   private ScriptInterface app_;
   
   private MicroNucleiForm theForm_ = null;

   @Override
   public void setApp(ScriptInterface app) {
      app_ = app;
      if (theForm_ != null) {
         theForm_ = new MicroNucleiForm(app_);
      }
      theForm_.setVisible(true);
   }

   @Override
   public void dispose() {
      // We do nothing here as the only object we create, our dialog, should
      // be dismissed by the user.
   }

   @Override
   public void show() {
      if (theForm_ != null) {
         theForm_ = new MicroNucleiForm(app_);
      }
      theForm_.setVisible(true);
   }
   
   @Override
   public String getInfo () {
      return "Displays a simple greeting.";
   }

   @Override
   public String getDescription() {
      return tooltipDescription;
   }
   
   @Override
   public String getVersion() {
      return "1.0";
   }
   
   @Override
   public String getCopyright() {
      return "University of California, 2015";
   }
}
