/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.helidon.maven.transpiler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.ClassPool;
import javassist.bytecode.ClassFile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE;

@Mojo(name = "transpile-classes", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = COMPILE)
public class TranspilerMojo extends AbstractMojo {

    private static final Map<String, String> FQDN_MAP = new HashMap<>();
    private static final List<ClassMapping> CLASS_MAPPINGS = new ArrayList<>();

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "mapping.map")
    private String[] mapping;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // ./target/classes
            String outDir = project.getBuild().getOutputDirectory();

            var cp = ClassPool.getDefault();
            for (var o : project.getCompileClasspathElements()) {
                cp.appendClassPath(o);
            }

            for (ClassMapping m : mapping()) {
                ClassFile cf = cp.get(m.from().fqdn())
                        .getClassFile();

                // Set new class name
                cf.setName(m.to().fqdn());

                // Remap all references of all the mappings
                cf.renameClass(fqdnMap());

                File dir = new File(outDir, m.to().pkg().replaceAll("\\.", File.separator));
                dir.mkdirs();
                File outputFile = new File(dir, m.to().simpleName() + ".class");

                getLog().debug("Saving to " + outputFile.getAbsolutePath());

                try (FileOutputStream out = new FileOutputStream(outputFile)) {
                    cf.write(new DataOutputStream(out));
                }
                project.getCompileClasspathElements().add(outputFile.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new MojoFailureException(e);
        }

    }

    private Map<String, String> fqdnMap() {
        if (FQDN_MAP.isEmpty()) {
            mapping().forEach(m -> FQDN_MAP.put(m.from().fqdn(), m.to().fqdn()));
        }
        return FQDN_MAP;
    }

    private List<ClassMapping> mapping() {
        if (CLASS_MAPPINGS.isEmpty()) {
            Arrays.stream(mapping).map(ClassMapping::parse).forEach(CLASS_MAPPINGS::add);
        }
        return CLASS_MAPPINGS;
    }

}
