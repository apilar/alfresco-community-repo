/*
 * #%L
 * Alfresco Repository
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

package org.alfresco.repo.nodelocator;


import java.io.Serializable;
import java.util.Map;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Locates the Shared Home {@link NodeRef}.
 * 
 * @author Kevin Roast
 * @since 5.0.1
 */
public class SharedHomeNodeLocator extends AbstractNodeLocator
{
    public static final String NAME = "sharedhome";
    
    private Repository repoHelper;

    /**
    * {@inheritDoc}
    */
    public NodeRef getNode(NodeRef source, Map<String, Serializable> params)
    {
        return repoHelper.getSharedHome();
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public String getName()
    {
        return NAME;
    }
    
    /**
     * @param repoHelper the repoHelper to set
     */
    public void setRepositoryHelper(Repository repoHelper)
    {
        this.repoHelper = repoHelper;
    }
}
