// (c) Copyright 2012 WibiData, Inc.

package com.odiago.maven.plugins.soy;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * A maven goal for compiling soy templates (Google Closure Templates) into javascript files.
 *
 * @goal compile
 * @phase process-sources
 */
public class CompileMojo extends AbstractMojo {
  /**
   * The target directory for generated javascript files.
   *
   * @parameter property="outputDirectory" expression="${soy.output.directory} default-value="${project.build.directory}/generated-js/soy"
   * @required
   */
  private File mOutputDirectory;

  public void execute() throws MojoExecutionException {
    System.out.println("Compiling...");
  }
}
