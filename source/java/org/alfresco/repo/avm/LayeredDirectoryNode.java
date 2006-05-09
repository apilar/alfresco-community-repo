/*
 * Copyright (C) 2006 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */

package org.alfresco.repo.avm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.avm.hibernate.DirectoryEntry;
import org.alfresco.repo.avm.hibernate.LayeredDirectoryNodeBean;
import org.alfresco.repo.avm.hibernate.LayeredDirectoryNodeBeanImpl;

/**
 * Interface for a layered directory node.  Stub.
 * @author britt
 */
public class LayeredDirectoryNode extends DirectoryNode
{
    /**
     * The underlying bean data.
     */
    private LayeredDirectoryNodeBean fData;
    
    /**
     * Make one up from Bean data.
     * @param data The bean with the persistent data.
     */
    public LayeredDirectoryNode(LayeredDirectoryNodeBean data)
    {
        fData = data;
        setDataBean(data);
    }
    
    /**
     * Make a new one from a specified indirection path.
     * @param indirection The indirection path to set.
     * @param repository The repository that owns this node.
     */
    public LayeredDirectoryNode(String indirection, Repository repos)
    {
        fData = new LayeredDirectoryNodeBeanImpl(repos.getSuperRepository().issueID(),
                                                 -1,
                                                 -1,
                                                 null,
                                                 null,
                                                 null,
                                                 repos.getDataBean(),
                                                 -1,
                                                 indirection);
        setDataBean(fData);
        repos.getSuperRepository().getSession().save(fData);
    }
    
    /**
     * Kind of copy constructor, sort of.
     * @param other The LayeredDirectoryNode we are copied from.
     * @param repos The Repository object we use.
     */
    public LayeredDirectoryNode(LayeredDirectoryNode other,
                                    Repository repos)
    {
        fData = new LayeredDirectoryNodeBeanImpl(repos.getSuperRepository().issueID(),
                                                 -1,
                                                 -1,
                                                 null,
                                                 null,
                                                 null,
                                                 repos.getDataBean(),
                                                 -1,
                                                 other.getUnderlying());
        fData.setAdded(((LayeredDirectoryNodeBean)other.getDataBean()).getAdded());
        fData.setDeleted(((LayeredDirectoryNodeBean)other.getDataBean()).getDeleted());
        fData.setPrimaryIndirection(((LayeredDirectoryNodeBean)other.getDataBean()).getPrimaryIndirection());
        repos.getSuperRepository().getSession().save(fData);
    }
    
    /**
     * Construct one from a PlainDirectoryNode.
     * @param other The PlainDirectoryNode.
     * @param repos The Repository we should belong to.
     * @param lPath The Lookup object.
     */
    public LayeredDirectoryNode(PlainDirectoryNode other,
                                Repository repos,
                                Lookup lPath)
    {
        fData = new LayeredDirectoryNodeBeanImpl(repos.getSuperRepository().issueID(),
                                                 -1,
                                                 -1,
                                                 null,
                                                 null,
                                                 null,
                                                 repos.getDataBean(),
                                                 -1,
                                                 null);
        // TODO Is this right?
        fData.setAdded(other.getListing(lPath, -1));
        fData.setPrimaryIndirection(false);
        repos.getSuperRepository().getSession().save(fData);
    }

    public LayeredDirectoryNode(DirectoryNode dir,
                                Repository repo,
                                Lookup srcLookup,
                                String name)
    {
/*
        fAdded = new HashMap<String, RepoNode>();
        fDeleted = new HashSet<String>();
        fProxied = srcLookup.getIndirectionPath() + "/" + name;
        fIsPrimaryIndirection = true;
*/
    }   
    
    /**
     * Does this node have a primary indirection.
     * @returns Whether this is a primary indirection.
     */
    public boolean hasPrimaryIndirection()
    {
        return fData.getPrimaryIndirection();
    }
    
    /**
     * Set whether this has a primary indirection.
     * @param has Whether this has a primary indirection.
     */
    public void setPrimaryIndirection(boolean has)
    {
        fData.setPrimaryIndirection(has);
    }
    
    /**
     * Get the raw underlying indirection.  Only meaningful
     * for a node that hasPrimaryIndirection().
     */
    public String getUnderlying()
    {
        return fData.getIndirection();
    }
    
    /**
     * Get the underlying indirection in the context of a Lookup.
     * @param lPath The lookup path.
     */
    public String getUnderlying(Lookup lPath)
    {
        if (fData.getPrimaryIndirection())
        {
            return fData.getIndirection();
        }
        return lPath.getCurrentIndirection();
    }
    
    /**
     * Get the layer id for this node.
     * @return The layer id.
     */
    public long getLayerID()
    {
        return fData.getLayerID();
    }
    
    /**
     * Set the layer id for this node.
     * @param layerID The id to set.
     */
    public void setLayerID(long id)
    {
        fData.setLayerID(id);
    }
    
    /**
     * Handle post copy on write details.
     * @param parent
     */
    public void handlePostCopy(DirectoryNode parent)
    {
        if (parent instanceof LayeredDirectoryNode)
        {
            LayeredDirectoryNode dir = (LayeredDirectoryNode)parent;
            setLayerID(dir.getLayerID());
            // TODO Is this right?
            setRepository(parent.getRepository());
        }
    }

    /**
     * Copy on write logic.
     * @param lPath
     * @return The copy or null.
     */
    public AVMNode possiblyCopy(Lookup lPath)
    {
        if (!shouldBeCopied())
        {
            return null;
        }
        // Otherwise we do an actual copy.
        LayeredDirectoryNode newMe = null;
        long newBranchID = lPath.getHighestBranch();
        if (!lPath.isInThisLayer())
        {
            if (hasPrimaryIndirection())
            {
                newMe = new LayeredDirectoryNode(lPath.getIndirectionPath(), 
                                                 getRepository());
            }
            else
            {
                newMe = new LayeredDirectoryNode((String)null, 
                                                 getRepository());
                newMe.setPrimaryIndirection(false);
            }
        }
        else
        {
            newMe = new LayeredDirectoryNode(this,
                                                 getRepository());
                                             
            newMe.setLayerID(getLayerID());
        }
        newMe.setAncestor(this);
        newMe.setBranchID(newBranchID);
        return newMe;
    }

    /**
     * Insert a child node without COW.
     * @param name The name to give the child.
     */
    public void putChild(String name, AVMNode node)
    {
        DirectoryEntry entry = new DirectoryEntry(node.getType(), node.getDataBean());
        fData.getAdded().put(name, entry);
        fData.getDeleted().remove(name);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.DirectoryNode#addChild(java.lang.String, org.alfresco.repo.avm.AVMNode, org.alfresco.repo.avm.Lookup)
     */
    public boolean addChild(String name, AVMNode child, Lookup lPath)
    {
        if (fData.getAdded().containsKey(name))
        {
            return false;
        }
        if (!fData.getDeleted().contains(name))
        {
            try
            {
                Lookup lookup = getRepository().getSuperRepository().lookupDirectory(-1, getUnderlying(lPath));
                DirectoryNode dir = (DirectoryNode)lookup.getCurrentNode();
                if (dir.lookupChild(lookup, name, -1) != null)
                {
                    return false;
                }
            }
            catch (AlfrescoRuntimeException re)
            {
                // Do nothing.
            }
        }
        DirectoryNode toModify = (DirectoryNode)copyOnWrite(lPath);
        toModify.putChild(name, child);
        child.setParent(toModify);
        child.setRepository(toModify.getRepository());
        return true;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.DirectoryNode#directlyContains(org.alfresco.repo.avm.AVMNode)
     */
    public boolean directlyContains(AVMNode node)
    {
        return fData.getAdded().containsValue(node.getDataBean());
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.DirectoryNode#getListing(org.alfresco.repo.avm.Lookup, int)
     */
    public Map<String, DirectoryEntry> getListing(Lookup lPath, int version)
    {
        Map<String, DirectoryEntry> baseListing = null;
        try
        {
            Lookup lookup = getRepository().getSuperRepository().lookupDirectory(version, getUnderlying(lPath));
            DirectoryNode dir = (DirectoryNode)lookup.getCurrentNode();
            baseListing = dir.getListing(lookup, version);
        }
        catch (AlfrescoRuntimeException re)
        {
            baseListing = new HashMap<String, DirectoryEntry>();
        }
        Map<String, DirectoryEntry> listing = new TreeMap<String, DirectoryEntry>();
        for (String name : baseListing.keySet())
        {
            if (fData.getDeleted().contains(name))
            {
                continue;
            }
            listing.put(name, baseListing.get(name));
        }
        for (String name : fData.getAdded().keySet())
        {
            listing.put(name, fData.getAdded().get(name));
        }
        return listing;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.DirectoryNode#lookupChild(org.alfresco.repo.avm.Lookup, java.lang.String, int)
     */
    public AVMNode lookupChild(Lookup lPath, String name, int version)
    {
        // TODO revisit the order in this.
        if (fData.getAdded().containsKey(name))
        {
            return AVMNodeFactory.CreateFromBean(fData.getAdded().get(name).getChild());
        }
        AVMNode child = null;
        try
        {
            Lookup lookup = getRepository().getSuperRepository().lookupDirectory(version, getUnderlying(lPath));
            DirectoryNode dir = (DirectoryNode)lookup.getCurrentNode();
            child = dir.lookupChild(lookup, name, version);
        }
        catch (AlfrescoRuntimeException re)
        {
            return null;
        }
        if (child ==null)
        {
            return null;
        }
        if (fData.getDeleted().contains(name))
        {
            return null;
        }
        return child;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.DirectoryNode#rawRemoveChild(java.lang.String)
     */
    public void rawRemoveChild(String name)
    {
        fData.getAdded().remove(name);
        fData.getDeleted().add(name);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.DirectoryNode#removeChild(java.lang.String, org.alfresco.repo.avm.Lookup)
     */
    public boolean removeChild(String name, Lookup lPath)
    {
        if (fData.getDeleted().contains(name))
        {
            return false;
        }
        if (!fData.getAdded().containsKey(name))
        {
            try
            {
                Lookup lookup = getRepository().getSuperRepository().lookupDirectory(-1, getUnderlying(lPath));
                DirectoryNode dir = (DirectoryNode)lookup.getCurrentNode();
                if (dir.lookupChild(lookup, name, -1) == null)
                {
                    return false;
                }
            }
            catch (AlfrescoRuntimeException re)
            {
                return false;
            }
        }
        LayeredDirectoryNode toModify =
            (LayeredDirectoryNode)copyOnWrite(lPath);
        toModify.rawRemoveChild(name);
        return true;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.AVMNode#getType()
     */
    public AVMNodeType getType()
    {
        return AVMNodeType.LAYERED_DIRECTORY;
    }
}
