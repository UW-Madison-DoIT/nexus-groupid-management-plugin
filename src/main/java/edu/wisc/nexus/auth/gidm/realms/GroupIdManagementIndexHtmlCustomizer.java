/**
 * Copyright 2012, Board of Regents of the University of
 * Wisconsin System. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Board of Regents of the University of Wisconsin
 * System licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package edu.wisc.nexus.auth.gidm.realms;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.guice.bean.reflect.ClassSpace;
import org.sonatype.guice.bean.reflect.URLClassSpace;
import org.sonatype.nexus.plugins.rest.AbstractNexusIndexHtmlCustomizer;
import org.sonatype.nexus.plugins.rest.NexusIndexHtmlCustomizer;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
@Component(role = NexusIndexHtmlCustomizer.class, hint = "GroupIdManagementIndexHtmlCustomizer")
public class GroupIdManagementIndexHtmlCustomizer extends AbstractNexusIndexHtmlCustomizer {
    public static final String PROP_FILE_NAME = "/" + GroupIdManagementIndexHtmlCustomizer.class.getSimpleName() + ".properties";
    
    private final String scriptTag;

    public GroupIdManagementIndexHtmlCustomizer() {
        final Properties props = loadProperties();
        
        final String groupId = props.getProperty("groupId");
        final String artifactId = props.getProperty("artifactId");
        final String minifiedJs = props.getProperty("minifiedJs");
        
        final StringBuilder scriptTagBuilder = new StringBuilder();
        String version = getVersionFromJarFile("/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties");
        if (version != null && version.endsWith("-SNAPSHOT")) {
            //Get around caching on snapshot dev
            version = Long.toString(System.currentTimeMillis());
            
            //Search the classpath for scripts
            final ClassSpace classSpace = new URLClassSpace(this.getClass().getClassLoader());
            for (final Enumeration<URL> entries = classSpace.findEntries("/static/gidm/js/", artifactId + "-*.js", true);
                    entries.hasMoreElements();) {
                
                final URL script = entries.nextElement();
                final String path = script.getPath();
                final String file = path.substring(path.lastIndexOf('/') + 1);

                if (!file.equals(minifiedJs)) {
                    addScript(scriptTagBuilder, version, file);
                }
            }
        }
        else {
            addScript(scriptTagBuilder, version, minifiedJs);
        }

        this.scriptTag = scriptTagBuilder.toString();
    }

    private Properties loadProperties() {
        final InputStream propStream = getClass().getResourceAsStream(PROP_FILE_NAME);
        if (propStream == null) {
            throw new IllegalStateException("Could not find " + PROP_FILE_NAME + " on classpath");
        }

        final Properties props = new Properties();
        try {
            props.load(propStream);
        }
        catch (IOException e) {
            throw new IllegalStateException("Could not load " + PROP_FILE_NAME + " from classpath", e);
        }
        finally {
            IOUtils.closeQuietly(propStream);
        }
        return props;
    }

    protected void addScript(final StringBuilder scriptTags, String version, final String file) {
        scriptTags.append("<script src=\"static/gidm/js/").append(file).append("?").append(version)
                .append("\" type=\"text/javascript\" charset=\"utf-8\"></script>\n");
    }

    @Override
    public String getPostHeadContribution(Map<String, Object> ctx) {
        return this.scriptTag;
    }
}
