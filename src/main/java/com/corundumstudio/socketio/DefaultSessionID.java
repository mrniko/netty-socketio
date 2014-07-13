package com.corundumstudio.socketio;

import java.util.UUID;

/**
 * Default SessionID implementation based on UUID class.
 */
public class DefaultSessionID implements SessionID {

    private UUID uuid;

    public DefaultSessionID(UUID id) {
        this.uuid = id;
    }

    @Override
    public String toString() {
        return this.uuid.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultSessionID other = (DefaultSessionID) obj;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }
}
