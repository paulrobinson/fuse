/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.itests.basic;

import java.util.Arrays;
import org.apache.karaf.admin.AdminService;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
//@Ignore("[FABRIC-700] Fix fabric basic ExtendedJoinTest")
public class ExtendedJoinTest extends FabricTestSupport {

    private static final String WAIT_FOR_JOIN_SERVICE = "wait-for-service io.fabric8.boot.commands.service.Join";

	@After
	public void tearDown() throws InterruptedException {
	}

	/**
	 * This is a test for FABRIC-353.
	 */
	@Test
	public void testJoinAndAddToEnsemble() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        FabricService fabricService = getFabricService();
        AdminService adminService = ServiceLocator.getOsgiService(AdminService.class);
        String version = System.getProperty("fabric.version");
        System.err.println(executeCommand("admin:create --featureURL mvn:io.fabric8/fuse-fabric/" + version + "/xml/features --feature fabric-git --feature fabric-agent --feature fabric-boot-commands child1"));
        System.err.println(executeCommand("admin:create --featureURL mvn:io.fabric8/fuse-fabric/" + version + "/xml/features --feature fabric-git --feature fabric-agent --feature fabric-boot-commands child2"));
		try {
			System.err.println(executeCommand("admin:start child1"));
			System.err.println(executeCommand("admin:start child2"));
            Provision.instanceStarted(Arrays.asList("child1", "child2"), PROVISION_TIMEOUT);
            System.err.println(executeCommand("admin:list"));
            String joinCommand = "fabric:join -f --zookeeper-password "+ fabricService.getZookeeperPassword() +" " + fabricService.getZookeeperUrl();

            String response = "";
            for (int i = 0; i < 10 && !response.contains("true"); i++) {
                response = executeCommand("ssh -l admin -P admin -p " + adminService.getInstance("child1").getSshPort() + " localhost " + WAIT_FOR_JOIN_SERVICE);
                Thread.sleep(1000);
            }
            response = "";
            for (int i = 0; i < 10 && !response.contains("true"); i++) {
                response = executeCommand("ssh -l admin -P admin -p " + adminService.getInstance("child2").getSshPort() + " localhost " + WAIT_FOR_JOIN_SERVICE);
                Thread.sleep(1000);
            }

            System.err.println(executeCommand("ssh -l admin -P admin -p " + adminService.getInstance("child1").getSshPort() + " localhost " + joinCommand));
            System.err.println(executeCommand("ssh -l admin -P admin -p " + adminService.getInstance("child2").getSshPort() + " localhost " + joinCommand));
            Provision.containersExist(Arrays.asList("child1", "child2"), PROVISION_TIMEOUT);
			Container child1 = fabricService.getContainer("child1");
			Container child2 = fabricService.getContainer("child2");
            Provision.containersStatus(Arrays.asList(child1, child2), "success", PROVISION_TIMEOUT);
			System.err.println(executeCommand("fabric:ensemble-add --force --migration-timeout 240000 child1 child2", 240000L, false));
            getCurator().getZookeeperClient().blockUntilConnectedOrTimedOut();
			System.err.println(executeCommand("fabric:container-list"));
			System.err.println(executeCommand("fabric:ensemble-remove --force --migration-timeout 240000 child1 child2", 240000L, false));
            getCurator().getZookeeperClient().blockUntilConnectedOrTimedOut();
			System.err.println(executeCommand("fabric:container-list"));
		} finally {
			System.err.println(executeCommand("admin:stop child1"));
			System.err.println(executeCommand("admin:stop child2"));
		}
	}


	@Configuration
	public Option[] config() {
		return new Option[]{
				new DefaultCompositeOption(fabricDistributionConfiguration()),
				editConfigurationFilePut("etc/system.properties", "karaf.name", "myroot"),
				editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.getArtifactVersion("io.fabric8", "fuse-fabric"))
		};
	}
}
