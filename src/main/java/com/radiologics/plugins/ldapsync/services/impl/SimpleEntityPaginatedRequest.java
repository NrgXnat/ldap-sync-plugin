package com.radiologics.plugins.ldapsync.services.impl;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.nrg.framework.ajax.hibernate.HibernatePaginatedRequest;

@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class SimpleEntityPaginatedRequest extends HibernatePaginatedRequest {
    @Override
    protected String getDefaultSortColumn() {
        return "id";
    }
}
