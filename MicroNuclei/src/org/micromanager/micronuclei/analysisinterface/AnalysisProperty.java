
package org.micromanager.micronuclei.analysisinterface;

import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;

/**
 *
 * @author nico
 * @param <T> value that will be stored in this property
 */
public class AnalysisProperty<T> {
   private final String description_;
   private T t_;
   private final Preferences prefs_;
   private JComponent jc_;
   
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
      makeJC();
   }
   
   public void set(T t) {
      t_ = t;
      setValueToPrefs(t);
   }
   
   public T get() {
      if (t_ instanceof Double) {
         JFormattedTextField jf = (JFormattedTextField) jc_;
         Double d = Double.parseDouble(jf.getText());
         t_ = (T) d;
      }
      setValueToPrefs(t_);
      return t_;
   }
   
   public String getDescription() {
      return description_;
   }
   
   /**
    * UI for this property.  This could live in a separate class...
    * @return component ready for display.  It will handle 
    */
   public JComponent getUI() {
      return jc_;
   }
   
   private void makeJC() {
      if (t_ instanceof Double) {
         JFormattedTextField jf = new JFormattedTextField();
         jf.setValue( ((Double) t_).toString() );
         jf.setColumns(4);
         jc_ = jf;
      }
      
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
   
}
