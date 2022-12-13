package org.cid15.aem.groovy.console.replication

import com.day.cq.commons.jcr.JcrConstants
import com.google.common.base.Charsets
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.ResourceResolverFactory
import org.apache.sling.api.resource.observation.ResourceChange
import org.apache.sling.api.resource.observation.ResourceChangeListener
import org.cid15.aem.groovy.console.GroovyConsoleService
import org.cid15.aem.groovy.console.api.context.ScriptContext
import org.cid15.aem.groovy.console.api.impl.ResourceScriptContext
import org.jetbrains.annotations.NotNull
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference

import javax.jcr.Session

import static com.google.common.base.Preconditions.checkNotNull

@Component(service = ResourceChangeListener.class, property = [
        "resource.paths=/var/groovyconsole/replication/**/*.groovy",
        "resource.change.types=ADDED"
])
public class ReplicatedScriptListener implements ResourceChangeListener {
    @Reference
    private GroovyConsoleService consoleService;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void onChange(@NotNull List<ResourceChange> list) {
        list.each { change ->
            resourceResolverFactory.getServiceResourceResolver(null).withCloseable { resourceResolver ->
                consoleService.runScript(getScriptContext(resourceResolver, change.path));
            }
        }
    }

    private ScriptContext getScriptContext(ResourceResolver resourceResolver, String scriptPath) {
        def outputStream = new ByteArrayOutputStream()

        new ResourceScriptContext(
                resourceResolver: resourceResolver,
                outputStream: outputStream,
                printStream: new PrintStream(outputStream, true, Charsets.UTF_8.name()),
                script: checkNotNull(loadScript(resourceResolver, scriptPath), "Script cannot be empty.")
        )
    }

    private String loadScript(ResourceResolver resourceResolver, String scriptPath) {
        def session = resourceResolver.adaptTo(Session)

        def binary = session.getNode(scriptPath)
                .getNode(JcrConstants.JCR_CONTENT)
                .getProperty(JcrConstants.JCR_DATA)
                .binary

        def script = binary.stream.text

        binary.dispose()

        script
    }
}
