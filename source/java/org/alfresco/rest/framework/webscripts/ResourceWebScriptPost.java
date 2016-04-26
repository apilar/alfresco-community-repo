/*
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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
package org.alfresco.rest.framework.webscripts;

import java.util.Arrays;
import java.util.List;

import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.rest.framework.core.ResourceInspector;
import org.alfresco.rest.framework.core.ResourceLocator;
import org.alfresco.rest.framework.core.ResourceMetadata;
import org.alfresco.rest.framework.core.ResourceParameter;
import org.alfresco.rest.framework.core.ResourceWithMetadata;
import org.alfresco.rest.framework.core.exceptions.DeletedResourceException;
import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.alfresco.rest.framework.core.exceptions.UnsupportedResourceOperationException;
import org.alfresco.rest.framework.resource.actions.interfaces.EntityResourceAction;
import org.alfresco.rest.framework.resource.actions.interfaces.MultiPartResourceAction;
import org.alfresco.rest.framework.resource.actions.interfaces.MultiPartRelationshipResourceAction;
import org.alfresco.rest.framework.resource.actions.interfaces.RelationshipResourceAction;
import org.alfresco.rest.framework.resource.parameters.CollectionWithPagingInfo;
import org.alfresco.rest.framework.resource.parameters.Params;
import org.alfresco.rest.framework.resource.parameters.Params.RecognizedParams;
import org.apache.commons.lang.StringUtils;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptRequestImpl;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.servlet.FormData;
import org.springframework.http.HttpMethod;

/**
 * Handles the HTTP POST for a Resource, equivalent to CRUD Create
 * 
 * @author Gethin James
 */
public class ResourceWebScriptPost extends AbstractResourceWebScript implements ParamsExtractor
{

    public ResourceWebScriptPost()
    {
       super();
       setHttpMethod(HttpMethod.POST);
       setParamsExtractor(this);
    }
    
    
    @Override
    public Params extractParams(ResourceMetadata resourceMeta, WebScriptRequest req)
    {
        final RecognizedParams params = ResourceWebScriptHelper.getRecognizedParams(req);
        
        switch (resourceMeta.getType())
        {
            case ENTITY:

                String entityIdCheck = req.getServiceMatch().getTemplateVars().get(ResourceLocator.ENTITY_ID);
                if (StringUtils.isNotBlank(entityIdCheck))
                {
                    throw new UnsupportedResourceOperationException("POST is executed against the collection URL");
                }
                else
                {
                    Object postedObj = processRequest(resourceMeta, req);
                    return Params.valueOf(null, params, postedObj);
                }
            case RELATIONSHIP:
                String entityId = req.getServiceMatch().getTemplateVars().get(ResourceLocator.ENTITY_ID);
                String relationshipId = req.getServiceMatch().getTemplateVars().get(ResourceLocator.RELATIONSHIP_ID);
                if (StringUtils.isNotBlank(relationshipId))
                {
                    throw new UnsupportedResourceOperationException("POST is executed against the collection URL");
                }
                else
                {
                    Object postedRel = processRequest(resourceMeta, req);
                    return Params.valueOf(entityId, params, postedRel);
                }
            default:
                throw new UnsupportedResourceOperationException("POST not supported for Actions");
        }
    }

    /**
     * If the request content-type is <i><b>multipart/form-data</b></i> then it
     * returns the {@link FormData}, otherwise it tries to extract the required
     * object from the JSON payload.
     */
    private Object processRequest(ResourceMetadata resourceMeta, WebScriptRequest req)
    {
        if (WebScriptRequestImpl.MULTIPART_FORM_DATA.equals(req.getContentType()))
        {
            return (FormData) req.parseContent();
        }

        return extractObjFromJson(resourceMeta, req);
    }

    /**
     * If the @WebApiParam has been used and set allowMultiple to false then this will get a single entry.  It
     * should error if an array is passed in.
     * @param resourceMeta ResourceMetadata
     * @param req WebScriptRequest
     * @return Either an object 
     */
    private Object extractObjFromJson(ResourceMetadata resourceMeta, WebScriptRequest req)
    {
        List<ResourceParameter> params = resourceMeta.getParameters(HttpMethod.POST);
        Class<?> objType = resourceMeta.getObjectType(HttpMethod.POST);

        if (!params.isEmpty())
        {
            for (ResourceParameter resourceParameter : params)
            {
               if (ResourceParameter.KIND.HTTP_BODY_OBJECT.equals(resourceParameter.getParamType()) && !resourceParameter.isAllowMultiple())
               {
                    // Only allow 1 value.
                    try
                    {
                        Object content = ResourceWebScriptHelper.extractJsonContent(req,jsonHelper, objType);
                        return Arrays.asList(content);
                    }
                    catch (InvalidArgumentException iae)
                    {
                        if (iae.getMessage().contains("START_ARRAY") && iae.getMessage().contains("line: 1, column: 1"))
                        {
                            throw new UnsupportedResourceOperationException("Only 1 entity is supported in the HTTP request body");
                        }
                        else
                        {
                            throw iae;
                        }
                    }
               }
            }
        }
        return ResourceWebScriptHelper.extractJsonContentAsList(req, jsonHelper, objType);
    }

    /**
     * Executes the action on the resource
     * @param resource ResourceWithMetadata
     * @param params parameters to use
     * @return anObject the result of the execute
     */
    @SuppressWarnings("unchecked")
    private Object executeInternal(ResourceWithMetadata resource, Params params)
    {
        final Object resObj = resource.getResource();
        switch (resource.getMetaData().getType())
        {
            case ENTITY:
                if (resource.getMetaData().isDeleted(EntityResourceAction.Create.class))
                {
                    throw new DeletedResourceException("(DELETE) " + resource.getMetaData().getUniqueId());
                }

                if (resObj instanceof MultiPartResourceAction.Create<?>)
                {
                    MultiPartResourceAction.Create<Object> creator = (MultiPartResourceAction.Create<Object>) resObj;
                    return creator.create((FormData) params.getPassedIn(), params);

                }
                else
                {
                    EntityResourceAction.Create<Object> creator = (EntityResourceAction.Create<Object>) resObj;
                    List<Object> created = creator.create((List<Object>) params.getPassedIn(), params);
                    if (created != null && created.size() == 1)
                    {
                        // return just one object instead of an array
                        return created.get(0);
                    }
                    else
                    {
                        return wrapWithCollectionWithPaging(created);
                    }
                }

            case RELATIONSHIP:
                if (resource.getMetaData().isDeleted(RelationshipResourceAction.Create.class))
                {
                    throw new DeletedResourceException("(DELETE) " + resource.getMetaData().getUniqueId());
                }

                if (resObj instanceof MultiPartRelationshipResourceAction.Create<?>)
                {
                    MultiPartRelationshipResourceAction.Create<Object> creator = (MultiPartRelationshipResourceAction.Create<Object>) resObj;
                    return creator.create(params.getEntityId(), (FormData) params.getPassedIn(), params);
                }
                else
                {
                    RelationshipResourceAction.Create<Object> createRelation = (RelationshipResourceAction.Create<Object>) resource.getResource();
                    List<Object> createdRel = createRelation.create(params.getEntityId(), (List<Object>) params.getPassedIn(), params);
                    if (createdRel != null && createdRel.size() == 1)
                    {
                        // return just one object instead of an array
                        return createdRel.get(0);
                    }
                    else
                    {
                        return wrapWithCollectionWithPaging(createdRel);
                    }
                }
            default:
                throw new UnsupportedResourceOperationException("POST not supported for Actions");
        }
    }

    private Object wrapWithCollectionWithPaging(List<Object> created)
    {
        if (created !=null && created.size() > 1)
        {
            return CollectionWithPagingInfo.asPagedCollection(created.toArray());
        }
        else
        {
            return created;
        }
    }

    @Override
    public void execute(final ResourceWithMetadata resource, final Params params, final ExecutionCallback executionCallback)
    {
        final String entityCollectionName = ResourceInspector.findEntityCollectionNameName(resource.getMetaData());
        transactionService.getRetryingTransactionHelper().doInTransaction(
            new RetryingTransactionCallback<Void>()
            {
                @SuppressWarnings("unchecked")
                @Override
                public Void execute() throws Throwable
                {
                    Object result = executeInternal(resource, params);
                    executionCallback.onSuccess(helper.postProcessResponse(resource.getMetaData().getApi(), entityCollectionName, params, result), DEFAULT_JSON_CONTENT);
                    return null;
                }
            }, false, true);
    }

    @Override
    protected void setSuccessResponseStatus(WebScriptResponse res)
    {
        res.setStatus(Status.STATUS_CREATED);
    }

}
