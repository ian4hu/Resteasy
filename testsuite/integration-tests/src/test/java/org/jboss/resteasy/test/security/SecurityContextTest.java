package org.jboss.resteasy.test.security;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.setup.UsersRolesSecurityDomainSetupCreaper;
import org.jboss.resteasy.test.security.resource.SecurityContextResource;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.CommandFailedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @tpSubChapter Security
 * @tpChapter Integration tests
 * @tpTestCaseDetails Basic test for RESTEasy authentication using programmatic security with javax.ws.rs.core.SecurityContext
 * @tpSince RESTEasy 3.0.16
 */
@ServerSetup({UsersRolesSecurityDomainSetupCreaper.class})
@RunWith(Arquillian.class)
@RunAsClient
public class SecurityContextTest {

    private static final String USERNAME="bill";
    private static final String PASSWORD="password1";

    private static final String USERNAME2="ordinaryUser";
    private static final String PASSWORD2="password2";

    private Client authorizedClient;
    private Client nonauthorizedClient;

    @Before
    public void initClient() throws IOException, CommandFailedException {

        // Create jaxrs client
        nonauthorizedClient = ClientBuilder.newClient();
        nonauthorizedClient.register(new BasicAuthentication(USERNAME2, PASSWORD2));

        // Create jaxrs client
        authorizedClient = ClientBuilder.newClient();
        authorizedClient.register(new BasicAuthentication(USERNAME, PASSWORD));


    }

    @After
    public void after() throws Exception {
        authorizedClient.close();
        nonauthorizedClient.close();
    }

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = TestUtil.prepareArchive(SecurityContextTest.class.getSimpleName());
        war.addAsResource(SecurityContextTest.class.getPackage(), "roles.properties", "roles.properties")
                .addAsResource(SecurityContextTest.class.getPackage(), "users.properties", "users.properties")
                .addAsWebInfResource(SecurityContextTest.class.getPackage(), "jboss-web.xml", "jboss-web.xml")
                .addAsWebInfResource(SecurityContextTest.class.getPackage(), "securityContext/web.xml", "web.xml");
        return TestUtil.finishContainerPrepare(war, null, SecurityContextResource.class);
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SecurityContextTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Correct credentials are used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testSecurityContextAuthorized() {
        Response response = authorizedClient.target(generateURL("/test")).request().get();
        Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
        Assert.assertEquals("Good user bill", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Incorrect credentials are used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testSecurityContextNonAuthorized() {
        Response response = nonauthorizedClient.target(generateURL("/test")).request().get();
        Assert.assertEquals("User ordinaryUser is not authorized", response.readEntity(String.class));
        Assert.assertEquals(HttpResponseCodes.SC_UNAUTHORIZED, response.getStatus());
    }
}
