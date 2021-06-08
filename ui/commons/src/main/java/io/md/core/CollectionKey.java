package io.md.core;

import com.google.common.base.MoreObjects;

public class CollectionKey {

    /**
     * @param customerId
     * @param app
     * @param instanceId
     */
    public CollectionKey(String customerId, String app, String instanceId) {
        super();
        this.customerId = customerId;
        this.app = app;
        this.instanceId = instanceId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("customerId" ,  customerId).add("app" , app)
                .add("instanceId" , instanceId).toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((app == null) ? 0 : app.hashCode());
        result = prime * result + ((customerId == null) ? 0 : customerId.hashCode());
        result = prime * result + ((instanceId == null) ? 0 : instanceId.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CollectionKey other = (CollectionKey) obj;
        if (app == null) {
            if (other.app != null) {
                return false;
            }
        } else if (!app.equals(other.app)) {
            return false;
        }
        if (customerId == null) {
            if (other.customerId != null) {
                return false;
            }
        } else if (!customerId.equals(other.customerId)) {
            return false;
        }
        if (instanceId == null) {
            if (other.instanceId != null) {
                return false;
            }
        } else if (!instanceId.equals(other.instanceId)) {
            return false;
        }
        return true;
    }

    final public String customerId;
    final public String app;
    final public String instanceId;
}
