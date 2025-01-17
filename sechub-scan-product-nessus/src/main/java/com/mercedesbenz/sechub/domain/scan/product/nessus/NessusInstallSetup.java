// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.domain.scan.product.nessus;

import com.mercedesbenz.sechub.domain.scan.InstallSetup;
import com.mercedesbenz.sechub.domain.scan.NetworkTargetProductServerDataProvider;

public interface NessusInstallSetup extends InstallSetup, NetworkTargetProductServerDataProvider {

    public String getDefaultPolicyId();
}
