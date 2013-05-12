/**
 * sbt-osgi-manager - OSGi development bridge based on Bndtools and Tycho.
 *
 * Copyright (c) 2013 Alexey Aksenov ezh@ezh.msk.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbt.osgi.manager.bndtools

import java.io.PrintWriter
import java.io.StringWriter

import org.apache.felix.resolver.{ Logger => FLogger }
import org.osgi.framework.ServiceReference
import org.osgi.service.log.LogService

/**
 * Log service implementation for biz.aQute.resolve.ResolveProcess
 */
class Logger(streams: sbt.Keys.TaskStreams) extends org.apache.felix.resolver.Logger(FLogger.LOG_DEBUG) with LogService {
  /**
   * Logs a message associated with a specific {@code ServiceReference}
   * object.
   *
   * <p>
   * The {@code Throwable} field of the {@code LogEntry} will be set to
   * {@code null}.
   *
   * @param sr The {@code ServiceReference} object of the service that this
   *        message is associated with or {@code null}.
   * @param level The severity of the message. This should be one of the
   *        defined log levels but may be any integer that is interpreted in a
   *        user defined way.
   * @param message Human readable string describing the condition or
   *        {@code null}.
   * @see #LOG_ERROR
   * @see #LOG_WARNING
   * @see #LOG_INFO
   * @see #LOG_DEBUG
   */
  def log(sr: ServiceReference[_], level: Int, message: String) = doLog(sr, level, message, null)

  /**
   * Logs a message with an exception associated and a
   * {@code ServiceReference} object.
   *
   * @param sr The {@code ServiceReference} object of the service that this
   *        message is associated with.
   * @param level The severity of the message. This should be one of the
   *        defined log levels but may be any integer that is interpreted in a
   *        user defined way.
   * @param message Human readable string describing the condition or
   *        {@code null}.
   * @param exception The exception that reflects the condition or
   *        {@code null}.
   * @see #LOG_ERROR
   * @see #LOG_WARNING
   * @see #LOG_INFO
   * @see #LOG_DEBUG
   */
  def log(sr: ServiceReference[_], level: Int, message: String, throwable: Throwable) = doLog(sr, level, message, throwable)

  protected def doLog(sr: ServiceReference[_], level: Int, message: String, throwable: Throwable) {
    val writer = new StringWriter()
    val printWriter = new PrintWriter(writer)
    printWriter.println(message)
    if (sr != null) printWriter.println("ServiceReference: " + sr)
    if (throwable != null) throwable.printStackTrace(printWriter)
    level match {
      case FLogger.LOG_DEBUG => streams.log.debug(writer.toString())
      case FLogger.LOG_ERROR => streams.log.error(writer.toString())
      case FLogger.LOG_INFO => streams.log.info(writer.toString())
      case FLogger.LOG_WARNING => streams.log.warn(writer.toString())
      case _ => streams.log.error("UNKNOWN LOG LEVEL: " + writer.toString())
    }
  }
}
