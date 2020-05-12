package wooga.gradle.appcenter.internal

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import wooga.gradle.appcenter.AppCenterPluginExtension

class DefaultAppCenterPluginExtension implements AppCenterPluginExtension {
    final Property<String> apiToken
    final Property<String> owner
    final Property<String> applicationIdentifier
    final ListProperty<Map<String, String>> defaultDestinations
    final Property<Boolean> publishEnabled

    DefaultAppCenterPluginExtension(Project project) {
        apiToken = project.objects.property(String)
        owner = project.objects.property(String)
        applicationIdentifier = project.objects.property(String)
        defaultDestinations = project.objects.listProperty(Map)
        publishEnabled = project.objects.property(Boolean)
    }

    @Override
    void setApiToken(String value) {
        apiToken.set(value)
    }

    @Override
    void apiToken(String value) {
        setApiToken(value)
    }

    @Override
    void setOwner(String value) {
        owner.set(value)
    }

    @Override
    void owner(String value) {
        setOwner(value)
    }

    @Override
    void setApplicationIdentifier(String value) {
        applicationIdentifier.set(value)
    }

    @Override
    void applicationIdentifier(String value) {
        setApplicationIdentifier(value)
    }

    @Override
    void setDefaultDestinations(Iterable<String> value) {
        defaultDestinations.set(value.collect {["name": it]})
    }

    @Override
    void defaultDestination(String name) {
        defaultDestinations.add(["name": name])
    }

    @Override
    void defaultDestination(Iterable<String> destinations) {
        defaultDestinations.addAll(destinations.collect {["name": it]})
    }

    @Override
    void defaultDestination(String... destinations) {
        defaultDestinations.addAll(destinations.collect {["name": it]})
    }

    @Override
    void defaultDestinationId(String id) {
        defaultDestinations.add(["id": id])
    }

    @Override
    Property<Boolean> isPublishEnabled() {
        return publishEnabled
    }

    @Override
    void setPublishEnabled(boolean enabled) {
        this.publishEnabled.set(enabled)
    }
}
