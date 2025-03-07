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
package fr.cnes.regards.framework.random.generator.builder;

import fr.cnes.regards.framework.random.function.FunctionDescriptor;
import fr.cnes.regards.framework.random.generator.AbstractNoParameterRandomGenerator;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Look at spring.factories
 */
@Component
public class RandomFloatBuilder implements RandomGeneratorBuilder<RandomFloatBuilder.RandomFloat> {

    @Override
    public String getFunctionName() {
        return "float";
    }

    @Override
    public RandomFloat build(FunctionDescriptor fd) {
        return new RandomFloat(fd);
    }

    static class RandomFloat extends AbstractNoParameterRandomGenerator<Float> {

        private static final Random random = new Random();

        public RandomFloat(FunctionDescriptor fd) {
            super(fd);
        }

        @Override
        public Float random() {
            return random.nextFloat();
        }

        public Float random(Float leftLimit, Float rightLimit) {
            return leftLimit + (new Random().nextFloat() * (rightLimit - leftLimit));
        }
    }
}
