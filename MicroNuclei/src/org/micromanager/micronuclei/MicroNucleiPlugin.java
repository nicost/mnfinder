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

package org.micromanager.micronuclei;


import java.awt.event.WindowEvent;
import org.micromanager.MenuPlugin;
import org.micromanager.Studio;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

@Plugin(type = MenuPlugin.class)
public class MicroNucleiPlugin implements MenuPlugin, SciJavaPlugin {
   public static final String MENUNAME = "Auto-PhotoConverter";
   public static final String TOOLTIP_DESCRIPTION =
      "Cool screening technology";

   // Provides access to the Micro-Manager Java API (for GUI control and high-
   // level functions).
   private Studio app_;
   private MicroNucleiForm theForm_ = null;
   
   @Override
   public String getVersion() {
      return "1.0";
   }
   
   @Override
   public String getCopyright() {
      return "University of California, 2015";
   }

   @Override
   public void setContext(Studio studio) {
      app_ = studio;

   }

   @Override
   public String getName() {
      return MENUNAME;
   }

   @Override
   public String getHelpText() {
      return TOOLTIP_DESCRIPTION;
   }

   @Override
   public String getSubMenu() {
      return "";
   }

   @Override
   public void onPluginSelected() {
      if (theForm_ != null && theForm_.isDisplayable()) {
         WindowEvent wev = new WindowEvent(theForm_, WindowEvent.WINDOW_CLOSING);
         theForm_.dispatchEvent(wev);
      }
      // create brand new instance of plugin frame every time
      theForm_ = new MicroNucleiForm(app_);
      theForm_.setVisible(true);}
}
