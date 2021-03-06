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

package org.micromanager.micronuclei.analysisinterface;

import java.util.prefs.Preferences;

/**
 *
 * @author nico
 * @param <T> value that will be stored in this property
 */
public class AnalysisProperty<T> {
   private final String description_;
   private T t_;
   private final Preferences prefs_;
   
   /**
    * Stores description/value pairs, made persistent through Java preferences
    * Uses generics to store Integer, Double, String and Boolean values
    * Values are stored keyed by the calling class (and description) 
    * 
    * @param caller calling class
    * @param description String to be displayed to the user
    * @param t variable that will be get or set.  Type should be Integer, Double,
    * String, or Boolean
    * @throws PropertyException Exception thrown when the type of the provided variable
    * is not one of Integer, Double, String, or Boolean
    */
   public AnalysisProperty(Class caller, String description, T t) throws PropertyException {
      prefs_ = Preferences.userNodeForPackage(caller);
      description_ = description;
      if (!  ( (t instanceof Integer ) || (t  instanceof Double) ||
              (t instanceof String) || (t instanceof Boolean)  ) ) {
         throw new PropertyException("This type is not supported");
      }
      t_ = getValueFromPrefs(t);
   }
   
   public void set(T t) {
      t_ = t;
      setValueToPrefs(t);
   }
   
   public T get() {
      return t_;
   }
   
   public String getDescription() {
      return description_;
   }  
   
   private T getValueFromPrefs(T t) {
      if (t instanceof Integer) {
         Integer i = prefs_.getInt(description_, (Integer) t);
         t = (T) i;
      }
      if (t instanceof Double) {
         Double i = prefs_.getDouble(description_, (Double) t);
         t = (T) i;
      }
      if (t instanceof String) {
         String i = prefs_.get(description_, (String) t);
         t = (T) i;
      }      
      if (t instanceof Boolean) {
         Boolean i = prefs_.getBoolean(description_, (Boolean) t);
         t = (T) i;
      }
      return t;
   }
   
   private void setValueToPrefs(T t) {
      if (t instanceof Integer) {
         prefs_.putInt(description_, (Integer) t);
      }
      if (t instanceof Double) {
         prefs_.putDouble(description_, (Double) t);
      }
      if (t instanceof String) {
         prefs_.put(description_, (String) t);
      }
      if (t instanceof Boolean) {
         prefs_.putBoolean(description_, (Boolean) t);
      }
   }
   
   public boolean isBoolean() {
      return t_ instanceof Boolean;
   }
   
   public boolean isString() {
      return t_ instanceof String;
   }
   
   public boolean isNumeric() {
      return t_ instanceof Integer || t_ instanceof Double;
   }
   
}
