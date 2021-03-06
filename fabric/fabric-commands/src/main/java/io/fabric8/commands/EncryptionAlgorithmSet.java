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
package io.fabric8.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.utils.Strings;
import io.fabric8.zookeeper.ZkPath;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

@Command(name = "crypt-algorithm-set", scope = "fabric", description = "Sets the encryption algorithm.")
public class EncryptionAlgorithmSet extends FabricCommand {


    @Argument(index = 0, name = "algorithm", description = "The algorithm to set for encryption.")
    private String newAlgorithm;

    @Override
    protected Object doExecute() throws Exception {
        if (!Strings.isNotBlank(newAlgorithm)) {
            setData(getCurator(), ZkPath.AUTHENTICATION_CRYPT_ALGORITHM.getPath(), newAlgorithm);
        }
        return null;
    }
}
