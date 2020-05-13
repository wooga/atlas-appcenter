package wooga.gradle.compat

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty

class ProjectLayout {

    final Project project
    final String gradleVersion
    final Object compatFactory

    ProjectLayout(Project project) {
        this.project = project
        gradleVersion = project.gradle.gradleVersion
        compatFactory = gradleVersion.startsWith("6.") ? project.objects : project.layout
    }

    DirectoryProperty directoryProperty() {
        (DirectoryProperty) compatFactory.invokeMethod("directoryProperty", null)
    }

//    DirectoryProperty directoryProperty(Provider<? extends Directory> var1) {
//        (DirectoryProperty) compatFactory.invokeMethod("directoryProperty", var1)
//    }

    RegularFileProperty fileProperty() {
        (RegularFileProperty) compatFactory.invokeMethod("fileProperty", null)
    }

//    RegularFileProperty fileProperty(Provider<? extends RegularFile> var1) {
//        (RegularFileProperty) compatFactory.invokeMethod("fileProperty", var1)
//    }
}
