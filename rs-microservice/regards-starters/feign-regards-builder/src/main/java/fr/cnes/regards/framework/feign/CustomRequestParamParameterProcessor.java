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
package fr.cnes.regards.framework.feign;

import feign.MethodMetadata;
import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Improve the default {@link org.springframework.cloud.openfeign.annotation.RequestParamParameterProcessor} in order
 * to teach it how to handle Map<String, String> parameters in REST controllers,
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */
public class CustomRequestParamParameterProcessor implements AnnotatedParameterProcessor {

    private static final Class<RequestParam> ANNOTATION = RequestParam.class;

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return ANNOTATION;
    }

    @Override
    public boolean processArgument(AnnotatedParameterContext context, Annotation pAnnotation, Method pMethod) {
        String name = ANNOTATION.cast(pAnnotation).value();
        if (!ObjectUtils.isEmpty(name)) {
            context.setParameterName(name);

            MethodMetadata data = context.getMethodMetadata();
            Collection<String> query = context.setTemplateParameter(name, data.template().queries().get(name));
            data.template().query(name, query);
        } else {
            MethodMetadata data = context.getMethodMetadata();
            data.queryMapIndex(context.getParameterIndex());
        }
        return true;
    }

}
