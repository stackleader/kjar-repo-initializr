/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.kjar.repo.initializr;

import com.google.common.base.Splitter;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.appformer.maven.integration.ArtifactResolver;
import org.appformer.maven.integration.DependencyDescriptor;
import org.appformer.maven.integration.MavenRepository;
import org.appformer.maven.integration.embedder.MavenSettings;
import org.appformer.maven.support.AFReleaseIdImpl;
import org.appformer.maven.support.DependencyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws ZipException {
        
        final String artifactName = args[0];
        final String outputFile = args[1];
        
        final AFReleaseIdImpl afReleaseIdImpl = new AFReleaseIdImpl(artifactName);
        ArtifactResolver artifactResolver = ArtifactResolver.getResolverFor(afReleaseIdImpl, false);
        Collection<DependencyDescriptor> allDependecies = artifactResolver.getAllDependecies(DependencyFilter.COMPILE_FILTER);
        
        Set<File> repoFiles = new HashSet<>();
        repoFiles.add(getArtifactRepoPath(new DependencyDescriptor(afReleaseIdImpl)));
        File repo = Files.createTempDir();

        MavenRepository mavenRepository = MavenRepository.getMavenRepository();
        
        addDependencyToRepo(repo, new DependencyDescriptor(afReleaseIdImpl));
        for (DependencyDescriptor dependencyDescriptor : allDependecies) {
            try {
                LOG.info("resolving {}", dependencyDescriptor);
                mavenRepository.resolveArtifact(dependencyDescriptor.getReleaseId().toExternalForm(), true);
            } catch (Throwable t) {
                LOG.warn("Could not resolve {}", dependencyDescriptor.getReleaseId(), t);
            }
            addDependencyToRepo(repo, dependencyDescriptor);
        }
        File repoZip = new File(outputFile);
        repoZip.delete();
        ZipFile zipFile = new ZipFile(outputFile);

        for (File repoDirectory : repo.listFiles()) {
            zipFile.addFolder(repoDirectory);
        }

        LOG.info(outputFile + " written");
    }

    public static File getArtifactRepoPath(DependencyDescriptor dependencyDescriptor) {
        String localRepository = MavenSettings.getSettings().getLocalRepository();
        final String groupIdPath = dependencyDescriptor.getGroupId().replace('.', File.separatorChar);
        final String artfactIdPath = dependencyDescriptor.getArtifactId().replace('.', File.separatorChar);
        final String artifactDirectoryPath = localRepository + File.separatorChar + groupIdPath + File.separatorChar + artfactIdPath + File.separatorChar + dependencyDescriptor.getVersion();
        File f = new File(artifactDirectoryPath);
        return f;
    }

    private static void addDependencyToRepo(File repo, DependencyDescriptor dependencyDescriptor) {
        String localRepository = MavenSettings.getSettings().getLocalRepository();
        final String groupIdPath = dependencyDescriptor.getGroupId().replace('.', File.separatorChar);
        final String artfactIdPath = dependencyDescriptor.getArtifactId().replace('.', File.separatorChar);
        final String artifactDirectoryPath = localRepository + File.separatorChar + groupIdPath + File.separatorChar + artfactIdPath + File.separatorChar + dependencyDescriptor.getVersion();
        List<String> groupPathSegments = Splitter.on(".").splitToList(dependencyDescriptor.getGroupId());
        List<String> artfactPathSegments = Splitter.on(".").splitToList(dependencyDescriptor.getArtifactId());

        String prefix = "";
        for (String groupPathSegment : groupPathSegments) {
            File prefixFile = prefix.isEmpty() ? repo : new File(repo, prefix);
            File segment = new File(prefixFile, groupPathSegment);
            segment.mkdir();
            if (prefix.isEmpty()) {
                prefix += groupPathSegment;
            } else {
                prefix = prefix + File.separatorChar + groupPathSegment;
            }
        }
        File groupFolder = new File(repo, groupIdPath);
        for (String artifactSegment : artfactPathSegments) {
            File prefixFile = prefix.isEmpty() ? repo : new File(repo, prefix);
            File segment = new File(prefixFile, artifactSegment);
            segment.mkdir();
            if (prefix.isEmpty()) {
                prefix += artifactSegment;
            } else {
                prefix = prefix + File.separatorChar + artifactSegment;
            }
        }
        File artifactFolder = new File(groupFolder, artfactIdPath);
        try {
            FileUtils.copyDirectory(new File(artifactDirectoryPath), new File(artifactFolder, dependencyDescriptor.getVersion()));
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

}
