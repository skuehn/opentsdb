// This file is part of OpenTSDB.
// Copyright (C) 2010-2012  The OpenTSDB Authors.
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or (at your
// option) any later version.  This program is distributed in the hope that it
// will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
// General Public License for more details.  You should have received a copy
// of the GNU Lesser General Public License along with this program.  If not,
// see <http://www.gnu.org/licenses/>.
package net.opentsdb.tools;

import net.opentsdb.accumulo.AccumuloClient;

import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/** Helper functions to parse arguments passed to {@code main}.  */
final class CliOptions {

  static {
    InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
  }

  /** Adds common TSDB options to the given {@code argp}.  */
  static void addCommon(final ArgP argp) {
    argp.addOption("--table", "TABLE",
                   "Name of the HBase table where to store the time series"
                   + " (default: tsdb).");
    argp.addOption("--uidtable", "TABLE",
                   "Name of the HBase table to use for Unique IDs"
                   + " (default: tsdb_quid).");
    argp.addOption("--zkquorum", "SPEC",
                   "Specification of the ZooKeeper quorum to use"
                   + " (default: localhost).");
    argp.addOption("--instance", "accumulo", "The name of the accumulo instance to use");
    argp.addOption("--auser", "root", "The user to use to write to accumulo tables");
    argp.addOption("--apass", "secret", "The password to use to for the accumulo user");
  }

  /** Adds a --verbose flag.  */
  static void addVerbose(final ArgP argp) {
    argp.addOption("--verbose",
                   "Print more logging messages and not just errors.");
    argp.addOption("-v", "Short for --verbose.");
  }

  /** Adds the --auto-metric flag.  */
  static void addAutoMetricFlag(final ArgP argp) {
    argp.addOption("--auto-metric", "Automatically add metrics to tsdb as they"
                   + " are inserted.  Warning: this may cause unexpected"
                   + " metrics to be tracked");
  }

  /**
   * Parse the command line arguments with the given options.
   * @param options Options to parse in the given args.
   * @param args Command line arguments to parse.
   * @return The remainder of the command line or
   * {@code null} if {@code args} were invalid and couldn't be parsed.
   */
  static String[] parse(final ArgP argp, String[] args) {
    try {
      args = argp.parse(args);
    } catch (IllegalArgumentException e) {
      System.err.println("Invalid usage.  " + e.getMessage());
      return null;
    }
    honorVerboseFlag(argp);
    return args;
  }

  /** Changes the log level to 'WARN' unless --verbose is passed.  */
  private static void honorVerboseFlag(final ArgP argp) {
    if (argp.optionExists("--verbose") && !argp.has("--verbose")
        && !argp.has("-v")) {
      // SLF4J doesn't provide any API to programmatically set the logging
      // level of the underlying logging library.  So we have to violate the
      // encapsulation provided by SLF4J.
      for (final Logger logger :
           ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
             .getLoggerContext().getLoggerList()) {
        logger.setLevel(Level.WARN);
      }
    }
  }

  static AccumuloClient clientFromOptions(final ArgP argp) {
    if (argp.optionExists("--auto-metric") && argp.has("--auto-metric")) {
      System.setProperty("tsd.core.auto_create_metrics", "true");
    }
    final String zkq = argp.get("--zkquorum", "localhost");
    Instance inst = new ZooKeeperInstance(argp.get("--instance"), zkq);
    final String user = argp.get("--auser", "root");
    final String passwd = argp.get("--apass", "secret");
    try {
      return new AccumuloClient(inst.getConnector(user, passwd.getBytes()));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
