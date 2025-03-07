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
package fr.cnes.regards.modules.authentication.rest;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.client.IAccessRightSettingClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.client.IRegistrationClient;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * @author Sylvain Vissiere-Guerinet
 */
//@ComponentScan(basePackages = { "fr.cnes.regards.framework.authentication.role" })
@EnableAutoConfiguration
@Configuration
public class BorrowRoleITConfiguration {

//    @Bean
//    public IProjectsClient projectsClient() {
//        return Mockito.mock(IProjectsClient.class);
//    }

    @Bean
    public IProjectUsersClient projectUsersClient() {
        return Mockito.mock(IProjectUsersClient.class);
    }

//    @Bean
//    public IAccountsClient accountsClient() {
//        return Mockito.mock(IAccountsClient.class);
//    }

    @Bean
    public IAccessRightSettingClient accessSettingsClient() {
        return Mockito.mock(IAccessRightSettingClient.class);
    }

    @Bean
    public IRegistrationClient registrationClient() {
        return Mockito.mock(IRegistrationClient.class);
    }

    @Bean
    public IRolesClient rolesClient() {
        Role rolePublic = new Role(DefaultRole.PUBLIC.toString(), null);
        rolePublic.setNative(true);
        Role roleRegisteredUser = new Role(DefaultRole.REGISTERED_USER.toString(), rolePublic);
        roleRegisteredUser.setNative(true);
        Role roleAdmin = new Role(DefaultRole.ADMIN.toString(), roleRegisteredUser);
        roleAdmin.setNative(true);
        Role roleProjectAdmin = new Role(DefaultRole.PROJECT_ADMIN.toString(), null);
        roleProjectAdmin.setNative(true);
        IRolesClient roleClient = Mockito.mock(IRolesClient.class);
        List<Role> borrowables = Lists.newArrayList(roleAdmin, roleRegisteredUser, rolePublic);
        Mockito.when(roleClient.getBorrowableRoles())
               .thenReturn(new ResponseEntity<>(HateoasUtils.wrapList(borrowables), HttpStatus.OK));
        return roleClient;
    }

}
