// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.pds.config;

import java.util.ArrayList;
import java.util.List;

public class PDSProdutParameterSetup {

    private List<PDSProductParameterDefinition> mandatory = new ArrayList<>();
    private List<PDSProductParameterDefinition> optional = new ArrayList<>();

    public List<PDSProductParameterDefinition> getMandatory() {
        return mandatory;
    }

    public List<PDSProductParameterDefinition> getOptional() {
        return optional;
    }

    public void setMandatory(List<PDSProductParameterDefinition> mandatory) {
        this.mandatory = mandatory;
    }

    public void setOptional(List<PDSProductParameterDefinition> optional) {
        this.optional = optional;
    }
}
