/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.security.action.role;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.security.action.role.PutRoleAction;
import org.elasticsearch.xpack.core.security.action.role.PutRoleRequest;
import org.elasticsearch.xpack.core.security.action.role.PutRoleResponse;
import org.elasticsearch.xpack.core.security.authz.store.ReservedRolesStore;
import org.elasticsearch.xpack.security.authz.store.NativeRolesStore;

public class TransportPutRoleAction extends HandledTransportAction<PutRoleRequest, PutRoleResponse> {

    private final NativeRolesStore rolesStore;

    @Inject
    public TransportPutRoleAction(Settings settings, ActionFilters actionFilters,
                                  NativeRolesStore rolesStore, TransportService transportService) {
        super(settings, PutRoleAction.NAME, transportService, actionFilters, PutRoleRequest::new);
        this.rolesStore = rolesStore;
    }

    @Override
    protected void doExecute(Task task, final PutRoleRequest request, final ActionListener<PutRoleResponse> listener) {
        final String name = request.roleDescriptor().getName();
        if (ReservedRolesStore.isReserved(name)) {
            listener.onFailure(new IllegalArgumentException("role [" + name + "] is reserved and cannot be modified."));
            return;
        }

        rolesStore.putRole(request, request.roleDescriptor(), new ActionListener<Boolean>() {
            @Override
            public void onResponse(Boolean created) {
                if (created) {
                    logger.info("added role [{}]", request.name());
                } else {
                    logger.info("updated role [{}]", request.name());
                }
                listener.onResponse(new PutRoleResponse(created));
            }

            @Override
            public void onFailure(Exception t) {
                listener.onFailure(t);
            }
        });
    }
}
