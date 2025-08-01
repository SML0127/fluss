/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.fluss.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static com.alibaba.fluss.utils.Preconditions.checkState;

/**
 * Base class for some tests related to the plugin mechanism. Provides access to some common test
 * resources.
 */
abstract class PluginTestBase {

    /** Optional prefix to the jar folder if run from an IDE. */
    private static final String OPT_PREFIX = "target/";

    static final String PLUGIN_A = "plugin-a-test-jar.jar";
    static final String PLUGIN_B = "plugin-b-test-jar.jar";
    static final ClassLoader PARENT_CLASS_LOADER = PluginTestBase.class.getClassLoader();

    URL createPluginJarURLFromString(String fileString) throws MalformedURLException {
        File file = locateJarFile(fileString);
        return file.toURI().toURL();
    }

    static File locateJarFile(String fileString) {
        File file = new File(fileString);
        if (!file.exists()) {
            file = new File(OPT_PREFIX + fileString);
        }
        checkState(file.exists(), "Unable to locate jar file for test: " + fileString);
        return file;
    }
}
