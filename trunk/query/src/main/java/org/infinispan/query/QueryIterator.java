/*
 * JBoss, Home of Professional Open Source
 * Copyright ${year}, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.infinispan.query;

import java.util.ListIterator;

/**
 * Iterates over query results
 * <p/>
 *
 * @author Manik Surtani
 * @author Navin Surtani
 */
public interface QueryIterator extends ListIterator {
   /**
    * Jumps to a specific index in the iterator.
    *
    * @param index index to jump to.
    * @throws IndexOutOfBoundsException if the index is out of bounds
    */
   void jumpToResult(int index) throws IndexOutOfBoundsException;

   /**
    * Jumps to the first result
    */
   void first();

   /**
    * Jumps to the last result
    */
   void last();

   /**
    * Jumps to the one-after-the-first result
    */
   void afterFirst();

   /**
    * Jumps to the one-before-the-last result
    */
   void beforeLast();

   /**
    * @return true if the current result is the first
    */
   boolean isFirst();

   /**
    * @return true if the current result is the last
    */
   boolean isLast();

   /**
    * @return true if the current result is one after the first
    */
   boolean isAfterFirst();

   /**
    * @return true if the current result is one before the last
    */
   boolean isBeforeLast();

   /**
    * This method must be called on your iterator once you have finished so that Lucene resources can be freed up.
    */

   void close();
}
