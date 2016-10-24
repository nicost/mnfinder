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

package org.micromanager.micronuclei.utilities;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Utility function used for distance measurements
 * 
 * @author nico
 */
public class Distance {
   
    /**
    * calculates distance between two points
    * @param p1 First point
    * @param p2 Second point
    * @return Distance between the two points
    */
   public static double distance(Point2D.Double p1, Point2D.Double p2) {
      double x = p1.x - p2.x;
      double y = p1.y - p2.y;
      double total = x * x + y * y;
      return Math.sqrt(total);
   }
   
   /**
    * Find the closest point in the HashMap for now, uses brute force search
    * @param p Search point, i.e. we are looking for the point closes to this one
    * @param l Map with points in which we are looking for the closest one
    * @return Point that is closest by
    */
   public static Point2D.Double closest(Point2D.Double p, 
           Map<Point2D.Double, ArrayList<Point2D.Double> > l) {
      if (l.isEmpty()) {
         return null;
      }
      Set<Point2D.Double> keySet =  l.keySet();
      Point2D.Double[] pointList = new Point2D.Double[l.size()];
      int counter = 0;
      for (Object key : keySet) {
         pointList[counter] = (Point2D.Double) key;
         counter++;
      }
      Point2D.Double closestPoint = pointList[0];
      double d = distance(p, closestPoint);
      for (Point2D.Double p2   : pointList) {
         double dNew = distance(p, p2);
         if (dNew < d) {
            d = dNew;
            closestPoint = p2;
         }
      }
      return closestPoint;
   }
   
}
