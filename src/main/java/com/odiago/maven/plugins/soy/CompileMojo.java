/**
 * Licensed to WibiData, Inc. under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  WibiData, Inc.
 * licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.odiago.maven.plugins.soy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.template.soy.SoyFileSet;
import com.google.template.soy.jssrc.SoyJsSrcOptions;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

/**
 * A maven goal for compiling soy templates (Google Closure Templates) into javascript files.
 *
 * @goal compile
 * @phase process-sources
 */
public class CompileMojo extends AbstractMojo {
  /**
   * The soy files to be compiled into javascript files.
   *
   * @parameter property="inputFiles"
   * @required
   */
  private FileSet mInputFiles;

  /**
   * The target directory for generated javascript files.
   *
   * @parameter property="outputDirectory" expression="${soy.output.directory}" default-value="${project.build.directory}/generated-js/soy"
   * @required
   */
  private File mOutputDirectory;

  /**
   * Whether to generate code with provide/require soy namespaces for Google Closure Compiler
   * integration.
   *
   * @parameter property="shouldProvideRequireSoyNamespaces" default-value="false"
   */
  private boolean mShouldProvideRequireSoyNamespaces;

  /**
   * Whether to generate Jsdoc compatible for Google Closure Compiler integration.
   *
   * @parameter property="shouldGenerateJsdoc" default-value="false"
   */
  private boolean mShouldGenerateJsdoc;

  /**
   * Compile time globals to bind in the templates.
   *
   * @parameter property="globals"
   */
  private Map<String, String> mCompileTimeGlobals = Collections.emptyMap();

  /**
   * Sets the input soy files to compile.
   *
   * @param inputFiles An input file set.
   */
  public void setInputFiles(FileSet inputFiles) {
    mInputFiles = inputFiles;
  }

  /**
   * Sets the target directory for generated javascript files.
   *
   * @param outputDirectory A directory.
   */
  public void setOutputDirectory(File outputDirectory) {
    mOutputDirectory = outputDirectory;
  }

  /**
   * Sets whether to generate code with provide/require soy namespaces
   * for Google Closure Compiler integration.
   *
   * @param shouldProvideRequireSoyNamespaces Whether to add provide/require statements.
   */
  public void setShouldProvideRequireSoyNamespaces(boolean shouldProvideRequireSoyNamespaces) {
    mShouldProvideRequireSoyNamespaces = shouldProvideRequireSoyNamespaces;
  }

  /**
   * Sets whether Jsdoc should be generated.
   *
   * @param shouldGenerateJsdoc Whether to generate Jsdoc.
   */
  public void setShouldGenerateJsdoc(boolean shouldGenerateJsdoc) {
    mShouldGenerateJsdoc = shouldGenerateJsdoc;
  }

  /**
   * Sets the compile bime globals to bind in the templates.
   *
   * @param globals Variable name to value bindings.
   */
  public void setGlobals(Map<String, String> globals) {
    mCompileTimeGlobals = globals;
  }

  /**
   * Gets the set of input soy templates.
   *
   * @return The soy files to compile into javascript files.
   */
  private SoyFileSet getInputFileSet() {
    SoyFileSet.Builder soyFileSetBuilder = new SoyFileSet.Builder()
        .setCompileTimeGlobals(mCompileTimeGlobals);

    FileSetManager fileSetManager = new FileSetManager(getLog());
    for (String filename : fileSetManager.getIncludedFiles(mInputFiles)) {
      File soyFile = new File(mInputFiles.getDirectory(), filename);
      getLog().info("Including soy file: " + soyFile);
      soyFileSetBuilder.add(soyFile);
    }
    return soyFileSetBuilder.build();
  }

  /** {@inheritDoc} */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Compiling soy templates...");

    SoyFileSet soyFileSet = getInputFileSet();
    SoyJsSrcOptions jsSrcOptions = new SoyJsSrcOptions();
    jsSrcOptions.setShouldProvideRequireSoyNamespaces(mShouldProvideRequireSoyNamespaces);
    jsSrcOptions.setShouldGenerateJsdoc(mShouldGenerateJsdoc);
    List<String> compiledSrcs = soyFileSet.compileToJsSrc(jsSrcOptions, null);

    FileSetManager fileSetManager = new FileSetManager(getLog());
    String[] inputFilenames = fileSetManager.getIncludedFiles(mInputFiles);
    assert inputFilenames.length == compiledSrcs.size();
    if (mOutputDirectory.mkdirs()) {
      getLog().info("Created output directory: " + mOutputDirectory);
    }
    for (int i = 0; i < inputFilenames.length; i++) {
      File outputFile = new File(mOutputDirectory, inputFilenames[i] + ".js");
      File parent = outputFile.getParentFile();
      if(!parent.exists() && !parent.mkdirs()){
        throw new IllegalStateException("Couldn't create dir: " + parent);
      }
      getLog().info("Writing compiled soy: " + outputFile);
      Writer writer = null;
      try {
        if (outputFile.createNewFile()) {
          getLog().info("Created new file: " + outputFile);
        }
        writer = new FileWriter(outputFile);
        writer.write(compiledSrcs.get(i));
        writer.flush();
      } catch (IOException e) {
        IOUtils.closeQuietly(writer);
      }
    }
  }
}
