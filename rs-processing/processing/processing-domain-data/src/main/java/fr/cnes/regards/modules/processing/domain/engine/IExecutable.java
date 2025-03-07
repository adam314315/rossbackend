/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import io.vavr.Function1;
import io.vavr.Function2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * This interface defines an executable which is a function taking a context
 * and returning an updated context.
 * <p>
 * Executables can be chained using {@link #andThen(String, IExecutable)}.
 * <p>
 * This interface also defines several basic constructors for executables.
 *
 * @author gandrieu
 */
public interface IExecutable {

    Logger LOGGER = LoggerFactory.getLogger(IExecutable.class);

    /**
     * An executable receives execution context, and returns a new context in a Mono.
     *
     * @param context the execution parameters
     */
    Mono<ExecutionContext> execute(ExecutionContext context);

    static IExecutable wrap(Mono<ExecutionContext> mono) {
        return context -> mono;
    }

    static IExecutable wrap(Function<ExecutionContext, Mono<ExecutionContext>> fn) {
        return fn::apply;
    }

    static IExecutable sendEvent(Function<ExecutionContext, ExecutionEvent> eventFn) {
        return context -> context.sendEvent(eventFn.apply(context));
    }

    static IExecutable sendEvent(ExecutionEvent event) {
        return sendEvent(ctx -> event);
    }

    /**
     * IExecutable instances are chainable, thanks to the {@link Mono#flatMap(Function)} method.
     *
     * @param next the next executable to launch after this one.
     * @return a new executable with this and next in sequence.
     */
    default IExecutable andThen(String name, IExecutable next) {
        return ctx1 -> execute(ctx1).doOnNext(c -> LOGGER.info("Starting execution of '{}'", name))
                                    .flatMap(next::execute)
                                    .doOnNext(c -> LOGGER.info("Finished execution of '{}'", name));
    }

    default IExecutable onError(Function2<ExecutionContext, Throwable, Mono<ExecutionContext>> recover) {
        return context -> execute(context).onErrorResume(recover.apply(context));
    }

    default IExecutable onErrorThen(Function1<Throwable, IExecutable> recover) {
        return context -> execute(context).onErrorResume(t -> {
            LOGGER.error("Failure on context {}", context, t);
            return recover.apply(t).execute(context);
        });
    }

    default IExecutable interrupt() {
        return context -> execute(context).flatMap(c -> Mono.empty());
    }

    default String getExecutableDescription() {
        return "";
    }
}
