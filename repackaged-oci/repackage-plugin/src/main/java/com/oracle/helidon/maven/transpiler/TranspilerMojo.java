/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.maven.transpiler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.annotation.Annotation;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE;

@Mojo(name = "repackage-oci-libs", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = COMPILE)
class TranspilerMojo extends AbstractMojo {

    private static final Map<String, String> FQDN_MAP = new HashMap<>();
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;
    @Parameter(property = "artifacts.artifact")
    private ArrayList<ArtifactMapping> artifacts;
    @Parameter(property = "classes.class")
    private ArrayList<ClassMapping> classes;

    static CtClass lookUpClass(String fqdn, ClassPool cp) {
        try {
            return cp.get(fqdn);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // ./target/classes
            String outDir = project.getBuild().getOutputDirectory();
            repackageArtifacts(outDir);

            new File(outDir).mkdirs();
            var cp = ClassPool.getDefault();
            for (String o : project.getCompileClasspathElements()) {
                cp.appendClassPath(o);
            }

            for (ClassMapping m : classes) {
                CtClass ctClass = cp.get(m.getSrcFqdn());
                ClassFile cf = ctClass.getClassFile();

                if (m.getParentClassName().isPresent()) {
                    cf.setSuperclass(m.getParentClassName().get());
                }

                // Set new class name
                cf.setName(m.getTargetFqdn());

                // Remap all references of all the mappings
                cf.renameClass(fqdnMap());
                cf.renameClass(m.getRefMap());

                // Changes
                for (Change change : m.getModifierChanges()) {
                    if (change.getName().isPresent()) {
                        modifyMethod(ctClass, change, cp);
                    } else {
                        modifyConstructor(ctClass, change, cp);
                    }
                }

                File dir = new File(outDir, m.getTargetPkg().replaceAll("\\.", File.separator));
                dir.mkdirs();
                File outputFile = new File(dir, m.getName() + ".class");

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

    void modifyMethod(CtClass ctClass, Change change, ClassPool cp)
            throws NotFoundException, CannotCompileException {

        getLog().debug("Modifying method " + change);

        CtMethod cmOld = ctClass.getDeclaredMethod(change.getName().get());

        if (!change.getSubstitutions().isEmpty()) {
            cmOld.instrument(new SubstitutionEditor(change));
        }

        CtClass returnType;
        if (change.getNewReturnType().isPresent()) {
            returnType = lookUpClass(change.getNewReturnType().get(), cp);
        } else {
            returnType = cmOld.getReturnType();
        }

        CtClass[] params;
        if (change.getNewParams().isEmpty()) {
            params = cmOld.getParameterTypes();
        } else {
            params = change.getNewParams().stream()
                    .map(s -> lookUpClass(s, cp))
                    .toArray(CtClass[]::new);
        }

        CtMethod cmNew = new CtMethod(returnType,
                                      change.getNewName().orElse(cmOld.getName()),
                                      params,
                                      cmOld.getDeclaringClass());

        ctClass.removeMethod(cmOld);
        ctClass.addMethod(cmNew);

        if (change.getNewBody().isPresent()) {
            cmNew.setBody(change.getNewBody().get());
        } else {
            cmNew.setBody(cmOld, null);
        }

        if (change.isModifierChange()) {
            cmNew.setModifiers(change.computeFlags(cmOld.getModifiers()));
        } else {
            cmNew.setModifiers(cmOld.getModifiers());
        }
    }

    private void modifyConstructor(CtClass ctClass, Change change, ClassPool cp)
            throws NotFoundException, CannotCompileException {
        getLog().debug("Modifying constructor " + change);

        CtConstructor ccOld = ctClass.getDeclaredConstructor(change.getParams()
                                                                     .stream()
                                                                     .map(s -> lookUpClass(s, cp))
                                                                     .toArray(CtClass[]::new));

        if (!change.getSubstitutions().isEmpty()) {
            ccOld.instrument(new SubstitutionEditor(change));
        }

        CtClass[] params;
        if (change.getNewParams().isEmpty()) {
            params = ccOld.getParameterTypes();
        } else {
            params = change.getNewParams().stream()
                    .map(s -> lookUpClass(s, cp))
                    .toArray(CtClass[]::new);
        }

        CtConstructor ccNew = new CtConstructor(params, ccOld.getDeclaringClass());
        ccNew.setBody(ccOld, null);

        //copy annotations
        ConstPool constPool = ccNew.getMethodInfo2().getConstPool();
        ccOld.getMethodInfo2()
                .getAttributes()
                .stream()
                .filter(AnnotationsAttribute.class::isInstance)
                .map(AnnotationsAttribute.class::cast)
                .forEach(attributeInfo -> {
                    for (Annotation annotation : attributeInfo.getAnnotations()) {
                        if (!annotation.getTypeName().startsWith("javax")) {
                            return;
                        }
                        Annotation annotationNew = new Annotation(annotation.getTypeName().replaceFirst("javax", "jakarta"),
                                                                  constPool);
                        for (String memberName : Optional.ofNullable(annotation.getMemberNames()).orElse(Set.of())) {
                            annotationNew.addMemberValue(memberName, annotation.getMemberValue(memberName));
                        }
                        attributeInfo.removeAnnotation(annotation.getTypeName());
                        attributeInfo.setAnnotation(annotationNew);
                    }

                    ccNew.getMethodInfo().addAttribute(attributeInfo);
                });

        ctClass.removeConstructor(ccOld);
        ctClass.addConstructor(ccNew);

        //Not needed yet, but same approach like in modifyMethod will do
    }

    private Map<String, String> fqdnMap() {
        if (FQDN_MAP.isEmpty()) {
            classes.forEach(m -> {
                FQDN_MAP.put(m.getSrcFqdn(), m.getTargetFqdn());
                FQDN_MAP.put(Descriptor.toJvmName(m.getSrcFqdn()), Descriptor.toJvmName(m.getTargetFqdn()));
            });
        }
        return FQDN_MAP;
    }

    private void repackageArtifacts(String outDir) throws IOException {
        if (artifacts.isEmpty()) {
            return;
        }

        for (Object a : project.getArtifacts()) {
            Artifact artifact = (Artifact) a;
            Optional<ArtifactMapping> mapping = repackageArtifact(artifact.getGroupId(), artifact.getArtifactId());
            if (mapping.isEmpty()) {
                continue;
            }
            try (JarFile jar = new JarFile(artifact.getFile())) {
                Enumeration<JarEntry> enumEntries = jar.entries();
                while (enumEntries.hasMoreElements()) {
                    JarEntry je = enumEntries.nextElement();
                    File f = new File(outDir + File.separator + je.getName());
                    if (je.isDirectory()) {
                        f.mkdirs();
                        continue;
                    }

                    if (mapping.get().getExclude().stream().anyMatch(s -> je.getRealName().matches(s))) {
                        continue;
                    }

                    f.getParentFile().mkdirs();
                    try (var is = jar.getInputStream(je);
                            var fos = new FileOutputStream(f)) {
                        while (is.available() > 0) {
                            fos.write(is.read());
                        }
                    }
                }
            }
        }
    }

    private Optional<ArtifactMapping> repackageArtifact(String groupId, String artifactId) {
        return artifacts.stream()
                .filter(m -> m.getGroupId().map(gi -> gi.equals(groupId)).orElse(Boolean.FALSE)
                        && m.getArtifactId().map(ai -> ai.equals(artifactId)).orElse(Boolean.FALSE))
                .findFirst();
    }
}
