// Copyright 2015 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.analysis.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.analysis.ConfigurationCollectionFactory;
import com.google.devtools.build.lib.analysis.ConfiguredRuleClassProvider;
import com.google.devtools.build.lib.analysis.config.ConfigurationFactory;
import com.google.devtools.build.lib.analysis.config.ConfigurationFragmentFactory;
import com.google.devtools.build.lib.bazel.rules.android.AndroidNdkRepositoryFunction;
import com.google.devtools.build.lib.bazel.rules.android.AndroidNdkRepositoryRule;
import com.google.devtools.build.lib.bazel.rules.android.AndroidSdkRepositoryFunction;
import com.google.devtools.build.lib.bazel.rules.android.AndroidSdkRepositoryRule;
import com.google.devtools.build.lib.flags.InvocationPolicyEnforcer;
import com.google.devtools.build.lib.packages.PackageFactory;
import com.google.devtools.build.lib.packages.util.LoadingMock;
import com.google.devtools.build.lib.packages.util.MockCcSupport;
import com.google.devtools.build.lib.packages.util.MockToolsConfig;
import com.google.devtools.build.lib.rules.repository.LocalRepositoryFunction;
import com.google.devtools.build.lib.rules.repository.LocalRepositoryRule;
import com.google.devtools.build.lib.rules.repository.RepositoryDelegatorFunction;
import com.google.devtools.build.lib.rules.repository.RepositoryFunction;
import com.google.devtools.build.lib.rules.repository.RepositoryLoaderFunction;
import com.google.devtools.build.lib.skyframe.SkyFunctions;
import com.google.devtools.build.lib.testutil.TestConstants;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionName;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Create a mock client for the analysis phase, as well as a configuration factory. */
public abstract class AnalysisMock extends LoadingMock {

  public static AnalysisMock get() {
    try {
      Class<?> providerClass = Class.forName(TestConstants.TEST_ANALYSIS_MOCK);
      Field instanceField = providerClass.getField("INSTANCE");
      return (AnalysisMock) instanceField.get(null);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String getProductName() {
    return TestConstants.PRODUCT_NAME;
  }

  public ImmutableList<String> getEmbeddedTools() {
    return TestConstants.EMBEDDED_TOOLS;
  }

  @Override
  public PackageFactory.FactoryForTesting getPackageFactoryForTesting() {
    return TestConstants.PACKAGE_FACTORY_FACTORY_FOR_TESTING;
  }

  @Override
  public InvocationPolicyEnforcer getInvocationPolicyEnforcer() {
    return new InvocationPolicyEnforcer(TestConstants.TEST_INVOCATION_POLICY);
  }

  @Override
  public String getDefaultsPackageContent() {
    return createRuleClassProvider()
        .getDefaultsPackageContent(getInvocationPolicyEnforcer().getInvocationPolicy());
  }

  /**
   * This is called from test setup to create the mock directory layout needed to create the
   * configuration.
   */
  public abstract void setupMockClient(MockToolsConfig mockToolsConfig) throws IOException;

  /**
   * Returns the contents of WORKSPACE.
   */
  public abstract List<String> getWorkspaceContents(MockToolsConfig config);

  /**
   * This is called from test setup to create any necessary mock workspace files in the
   * <code>_embedded_binaries</code> directory.
   */
  public abstract void setupMockWorkspaceFiles(Path embeddedBinariesRoot) throws IOException;

  public abstract ConfigurationFactory createConfigurationFactory();

  public abstract ConfigurationFactory createConfigurationFactory(
      List<ConfigurationFragmentFactory> configurationFragmentFactories);

  public abstract ConfigurationCollectionFactory createConfigurationCollectionFactory();

  @Override
  public abstract ConfiguredRuleClassProvider createRuleClassProvider();

  public abstract Collection<String> getOptionOverrides();

  public abstract boolean isThisBazel();

  public abstract MockCcSupport ccSupport();

  public void setupCcSupport(MockToolsConfig config) throws IOException {
    get().ccSupport().setup(config);
  }

  public ImmutableMap<SkyFunctionName, SkyFunction> getSkyFunctions() {
    // Some tests require the local_repository rule so we need the appropriate SkyFunctions.
    RepositoryFunction localRepositoryFunction = new LocalRepositoryFunction();
    ImmutableMap<String, RepositoryFunction> repositoryHandlers = ImmutableMap.of(
        LocalRepositoryRule.NAME, localRepositoryFunction,
        AndroidSdkRepositoryRule.NAME, new AndroidSdkRepositoryFunction(),
        AndroidNdkRepositoryRule.NAME, new AndroidNdkRepositoryFunction());

    return ImmutableMap.of(
        SkyFunctions.REPOSITORY_DIRECTORY,
        new RepositoryDelegatorFunction(
            repositoryHandlers, null, new AtomicBoolean(true)),
        SkyFunctions.REPOSITORY,
        new RepositoryLoaderFunction());
  }

  public static class Delegate extends AnalysisMock {
    private final AnalysisMock delegate;

    public Delegate(AnalysisMock delegate) {
      this.delegate = delegate;
    }

    @Override
    public void setupMockClient(MockToolsConfig mockToolsConfig) throws IOException {
      delegate.setupMockClient(mockToolsConfig);
    }

    @Override
    public List<String> getWorkspaceContents(MockToolsConfig mockToolsConfig) {
      return delegate.getWorkspaceContents(mockToolsConfig);
    }

    @Override
    public void setupMockWorkspaceFiles(Path embeddedBinariesRoot) throws IOException {
      delegate.setupMockWorkspaceFiles(embeddedBinariesRoot);
    }

    @Override
    public ConfigurationFactory createConfigurationFactory() {
      return delegate.createConfigurationFactory();
    }

    @Override
    public ConfigurationFactory createConfigurationFactory(
        List<ConfigurationFragmentFactory> configurationFragmentFactories) {
      return delegate.createConfigurationFactory(configurationFragmentFactories);
    }

    @Override
    public ConfigurationCollectionFactory createConfigurationCollectionFactory() {
      return delegate.createConfigurationCollectionFactory();
    }

    @Override
    public ConfiguredRuleClassProvider createRuleClassProvider() {
      return delegate.createRuleClassProvider();
    }

    @Override
    public InvocationPolicyEnforcer getInvocationPolicyEnforcer() {
      return delegate.getInvocationPolicyEnforcer();
    }

    @Override
    public boolean isThisBazel() {
      return delegate.isThisBazel();
    }

    @Override
    public MockCcSupport ccSupport() {
      return delegate.ccSupport();
    }

    @Override
    public Collection<String> getOptionOverrides() {
      return delegate.getOptionOverrides();
    }

    @Override
    public ImmutableMap<SkyFunctionName, SkyFunction> getSkyFunctions() {
      return delegate.getSkyFunctions();
    }
  }
}
