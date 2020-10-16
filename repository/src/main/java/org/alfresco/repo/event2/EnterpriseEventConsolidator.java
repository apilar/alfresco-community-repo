/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.repo.event2;

import java.util.Set;
import java.util.stream.Collectors;
import org.alfresco.enterprise.repo.event.v1.model.EnterpriseEventData;
import org.alfresco.repo.event.v1.model.DataAttributes;
import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessPermission;

public class EnterpriseEventConsolidator extends EventConsolidator{

  private final NodeResourceHelper helper;

  public EnterpriseEventConsolidator(NodeResourceHelper nodeResourceHelper) {
    super(nodeResourceHelper);
    helper = nodeResourceHelper;
  }

  protected DataAttributes<NodeResource> buildEventData(EventInfo eventInfo, NodeResource resource, EventType eventType)
  {
    Long nodeAclId = helper.nodeService.getNodeAclId(nodeRef);
    Set<AccessPermission> permissionSet = helper.permissionService.getAllSetPermissions(nodeRef);

    Set<String> readers = permissionSet
        .stream()
        .map(AccessPermission::getAuthority)
        .collect(Collectors.toSet());

    EnterpriseEventData.Builder<NodeResource> eventDataBuilder =  EnterpriseEventData.<NodeResource>builder()
        .setEventGroupId(eventInfo.getTxnId())
        .setResourceReaderAuthorities(readers)
        .setResourceAccessControlId(nodeAclId)
        .setResource(resource);

    if (eventType == EventType.NODE_UPDATED)
    {
      eventDataBuilder.setResourceBefore(buildNodeResourceBeforeDelta(resource));
    }

    return eventDataBuilder.build();
  }

  public void onChangePermission(NodeRef nodeRef) {
    addEventType(EventType.PERMISSION_UPDATED);
    /* Ff this value isn't set to false the event will not be dispatched
     * We should try to understand if the ACL really changed. Only when we move a folder
     * we could have the same ACL, in other cases this method will be invoked only if the
     * ACL changed.
     */
    setResourceBeforeAllFieldsNull(false);
    this.createBuilderIfAbsent(nodeRef);
  }
  
}
