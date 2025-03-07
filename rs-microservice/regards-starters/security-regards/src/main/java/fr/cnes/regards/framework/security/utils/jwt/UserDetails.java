/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.security.utils.jwt;

import java.io.Serializable;
import java.util.Set;

/**
 * This object store REGARDS security principal<br/>
 * After request authentication, this object can be retrieved calling {@link JWTAuthentication#getPrincipal()}
 *
 * @author msordi
 * @author Christophe Mertz
 */
public class UserDetails implements Serializable {

    /**
     * Tenant the user is requesting
     */
    private String tenant;

    /**
     * User email
     */
    private String email;

    /**
     * User login
     */
    private String login;

    /**
     * User role name
     */
    private String role;

    /**
     * Optional access groups
     */
    private Set<String> accessGroups;

    public UserDetails(String tenant, String email, String login, String role) {
        super();
        this.tenant = tenant;
        this.email = email;
        this.login = login;
        this.role = role;
    }

    public UserDetails(String tenant, String email, String role) {
        super();
        this.tenant = tenant;
        this.email = email;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the tenant
     */
    public String getTenant() {
        return tenant;
    }

    /**
     * Set the tenant
     */
    public void setTenant(String pTenant) {
        tenant = pTenant;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String pRole) {
        role = pRole;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Fluent API to set optional access groups
     */
    public UserDetails withAccessGroups(Set<String> accessGroups) {
        this.accessGroups = accessGroups;
        return this;
    }

    public Set<String> getAccessGroups() {
        return this.accessGroups;
    }
}
