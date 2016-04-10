/*
///////////////////////////////////////////////////////////////////////////////
//FILE:          
//PROJECT:       MicroNuclei
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nico Stuurman
//
// COPYRIGHT:    University of California, San Francisco 2015
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
 */
package org.micromanager.micronuclei.analysisinterface;

/**
 *
 * @author nico
 */
public class AnalysisException extends Exception {
   private static final long serialVersionUID = -8472385639461174L;
   private Throwable cause;
   private static final String MSG_PREFIX = "Analysis error: ";

   public AnalysisException(String message) {
       super(MSG_PREFIX + message);
   }

   public AnalysisException(Throwable t) {
       super(MSG_PREFIX + t.getMessage());
       this.cause = t;
   }

   @Override
   public Throwable getCause() {
       return this.cause;
   }

}
