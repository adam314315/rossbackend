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
package fr.cnes.regards.framework.jpa.multitenant.transactional;

import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Meta annotation to manage multitenant transaction
 *
 * @author msordi
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@RegardsTransactional(transactionManager = MultitenantDaoProperties.MULTITENANT_TRANSACTION_MANAGER)
public @interface MultitenantTransactional {

    /**
     * The transaction propagation type.
     * <p>
     * Defaults to {@link Propagation#REQUIRED}.
     *
     * @see org.springframework.transaction.interceptor.TransactionAttribute#getPropagationBehavior()
     */
    @AliasFor(annotation = Transactional.class) Propagation propagation() default Propagation.REQUIRED;

    @AliasFor(annotation = Transactional.class) Isolation isolation() default Isolation.DEFAULT;

    @AliasFor(annotation = Transactional.class) boolean readOnly() default false;

    @AliasFor(annotation = Transactional.class) Class<? extends Throwable>[] noRollbackFor() default {};

}
