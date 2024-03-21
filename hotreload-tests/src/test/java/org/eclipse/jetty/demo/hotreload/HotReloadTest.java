package org.eclipse.jetty.demo.hotreload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.tests.testers.JettyHomeTester;
import org.eclipse.jetty.tests.testers.Tester;
import org.eclipse.jetty.toolchain.test.FS;
import org.eclipse.jetty.toolchain.test.MavenPaths;
import org.eclipse.jetty.toolchain.test.jupiter.WorkDir;
import org.eclipse.jetty.toolchain.test.jupiter.WorkDirExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(WorkDirExtension.class)
public class HotReloadTest
{
    public static final int START_TIMEOUT = Integer.getInteger("home.start.timeout", 30);

    private HttpClient client;

    protected void startHttpClient() throws Exception
    {
        client = new HttpClient();
        client.start();
    }

    @Test
    public void testHotReload(WorkDir workDir) throws Exception
    {
        Path jettyBase = workDir.getEmptyPathDir();
        String jettyVersion = System.getProperty("jettyVersion");
        String projectVersion = System.getProperty("projectVersion");
        JettyHomeTester distribution = JettyHomeTester.Builder.newInstance()
            .jettyVersion(jettyVersion)
            .jettyBase(jettyBase)
            .build();

        List<String> setupArgs = List.of(
            "--add-modules=http,ee10-deploy,ee10-webapp,ee10-annotations"
        );
        try (JettyHomeTester.Run setupRun = distribution.start(setupArgs))
        {
            assertTrue(setupRun.awaitFor(START_TIMEOUT, TimeUnit.SECONDS));
            assertEquals(0, setupRun.getExitValue());
            Path etcDir = jettyBase.resolve("etc");
            FS.ensureDirExists(etcDir);
            Path webappsDir = jettyBase.resolve("webapps");
            FS.ensureDirExists(webappsDir);
            Path altappsDir = jettyBase.resolve("altapps");
            FS.ensureDirExists(altappsDir);
            FS.ensureDirExists(jettyBase.resolve("foowork"));
            FS.ensureDirExists(jettyBase.resolve("foowork/demo-temp"));

            Path war = distribution.resolveArtifact("org.eclipse.demo:hotreload-webapp:war:" + projectVersion);
            Files.copy(war, altappsDir.resolve("demo.war"));

            Path demoXml = MavenPaths.findTestResourceFile("xmls/demo.xml");
            Path xmlfoo = Files.copy(demoXml, webappsDir.resolve("demo.xml"));

            Path baseWorkDir = jettyBase.resolve("work");
            FS.ensureDirExists(baseWorkDir);

            String loggingConfig = """
                org.eclipse.jetty.LEVEL=INFO
                org.eclipse.jetty.util.resource.LEVEL=DEBUG
                org.eclipse.jetty.deploy.LEVEL=DEBUG
                org.eclipse.jetty.util.IO.LEVEL=DEBUG
                org.eclipse.jetty.ee10.webapp.WebInfConfiguration.LEVEL=DEBUG
                """;
            Path resourceDir = jettyBase.resolve("resources");
            FS.ensureDirExists(resourceDir);
            Files.writeString(resourceDir.resolve("jetty-logging.properties"), loggingConfig, StandardCharsets.UTF_8);

            int port = Tester.freePort();

            List<String> startArgs = List.of(
                "jetty.deploy.scanInterval=1",
                "jetty.http.port=" + port
                // "jetty.server.dumpAfterStart=true"
            );
            try (JettyHomeTester.Run startRun = distribution.start(startArgs))
            {
                assertTrue(startRun.awaitConsoleLogsFor("Started oejs.Server@", START_TIMEOUT, TimeUnit.SECONDS));

                startHttpClient();
                ContentResponse response = client.GET("http://localhost:" + port + "/demo/rez/");
                assertEquals(HttpStatus.OK_200, response.getStatus());
                assertThat(response.getContentAsString(), containsString("servletContext.attr[Rez]=This is the foo.txt, it has content."));

                System.err.println("### Wait 2 seconds");
                Thread.sleep(2000);
                System.err.println("### Touch " + xmlfoo);
                touch(xmlfoo);

                assertTrue(startRun.awaitConsoleLogsFor("Staxrted oeje10w.WebAppContext@", START_TIMEOUT, TimeUnit.SECONDS));
                response = client.GET("http://localhost:" + port + "/demo/rez/");
                assertEquals(HttpStatus.OK_200, response.getStatus());
            }
        }
    }

    private static void touch(Path path) throws IOException
    {
        FileTime now = FileTime.fromMillis(System.currentTimeMillis());
        Files.setLastModifiedTime(path, now);
    }
}
