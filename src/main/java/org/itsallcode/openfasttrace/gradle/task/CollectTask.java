/**
 * openfasttrace-gradle - Gradle plugin for tracing requirements using OpenFastTrace
 * Copyright (C) 2017 It's all code <christoph at users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.itsallcode.openfasttrace.gradle.task;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.itsallcode.openfasttrace.api.core.Newline;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.importer.ImportSettings;
import org.itsallcode.openfasttrace.api.importer.tag.config.PathConfig;
import org.itsallcode.openfasttrace.core.ExportSettings;
import org.itsallcode.openfasttrace.core.Oft;
import org.itsallcode.openfasttrace.core.OftRunner;
import org.itsallcode.openfasttrace.exporter.specobject.SpecobjectExporterFactory;
import org.itsallcode.openfasttrace.gradle.config.TagPathConfiguration;

public class CollectTask extends DefaultTask
{
    public final SetProperty<File> inputDirectories = getProject().getObjects()
            .setProperty(File.class);
    public final RegularFileProperty outputFile = getProject().getObjects().fileProperty();
    public final ListProperty<TagPathConfiguration> pathConfig = getProject().getObjects()
            .listProperty(TagPathConfiguration.class);

    @InputFiles
    public SetProperty<File> getInputDirectories()
    {
        return inputDirectories;
    }

    @OutputFile
    public RegularFileProperty getOutputFile()
    {
        return outputFile;
    }

    @Input
    public ListProperty<TagPathConfiguration> getPathConfig()
    {
        return pathConfig;
    }

    @TaskAction
    public void collectRequirements() throws IOException
    {
        createReportOutputDir();

        final Oft oft = new OftRunner();
        final List<SpecificationItem> importedItems = oft.importItems(getImportSettings());
        oft.exportToPath(importedItems, getOuputFileInternal().toPath(), getExportSettings());
    }

    private ExportSettings getExportSettings()
    {
        return ExportSettings.builder() //
                .outputFormat(SpecobjectExporterFactory.SUPPORTED_FORMAT) //
                .newline(Newline.UNIX) //
                .build();
    }

    private ImportSettings getImportSettings()
    {
        return ImportSettings.builder() //
                .addInputs(getAllImportFiles()) //
                .pathConfigs(getPathConfigInternal()) //
                .build();
    }

    private List<Path> getAllImportFiles()
    {
        final Stream<Path> inputDirPaths = inputDirectories.get().stream() //
                .map(File::toPath);
        final Stream<Path> inputTagPaths = pathConfig.get().stream()
                .flatMap(TagPathConfiguration::getPaths);
        return Stream.concat(inputDirPaths, inputTagPaths).collect(toList());
    }

    private List<PathConfig> getPathConfigInternal()
    {
        final List<PathConfig> paths = pathConfig.get().stream()
                .flatMap(TagPathConfiguration::getPathConfig).collect(toList());
        if (getLogger().isInfoEnabled())
        {
            getLogger().info("Got {} path configurations:\n{}", paths.size(),
                    paths.stream().map(this::formatPathConfig).collect(joining("\n")));
        }
        return paths;
    }

    private String formatPathConfig(PathConfig config)
    {
        return " - " + config.getDescription() + " (type " + config.getTagArtifactType()
                + "): covers '" + config.getCoveredItemArtifactType() + "', prefix: '"
                + config.getCoveredItemNamePrefix() + "'";
    }

    private void createReportOutputDir() throws IOException
    {
        final File outputDir = getOuputFileInternal().getParentFile();
        if (outputDir.exists())
        {
            return;
        }
        if (!outputDir.mkdirs())
        {
            throw new IOException("Error creating directory " + outputDir);
        }
    }

    private File getOuputFileInternal()
    {
        return outputFile.getAsFile().get();
    }
}
