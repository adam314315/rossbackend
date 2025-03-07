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

import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

/**
 * Wrapper for feign response to get a closeable  {@link InputStream}.
 * Used to close {@link Response} from feign at the same time that the associated stream.
 * Generally used to proxify a Feign client response from an other service as a stream.
 *
 * @author Sébastien Binda
 */
public class ResponseStreamProxy extends InputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseStreamProxy.class);

    private final feign.Response response;

    private final InputStream stream;

    private final Function<ResponseStreamProxy, Void> beforeClose;

    private long streamReadCount = 0L;

    public ResponseStreamProxy(feign.Response response) throws IOException {
        this.response = response;
        this.stream = response.body().asInputStream();
        this.beforeClose = null;
    }

    public ResponseStreamProxy(feign.Response response, Function<ResponseStreamProxy, Void> beforeClose)
        throws IOException {
        this.response = response;
        this.stream = response.body().asInputStream();
        this.beforeClose = beforeClose;
    }

    @Override
    public int read() throws IOException {
        int count = stream.read();
        if (count != -1) {
            this.streamReadCount++;
        }
        return count;
    }

    @Override
    public void close() throws IOException {
        LOGGER.trace("Close feign http response");
        if (this.beforeClose != null) {
            this.beforeClose.apply(this);
        }
        this.stream.close();
        this.response.close();
    }

    @Override
    public synchronized void reset() throws IOException {
        this.stream.reset();
    }

    public long getStreamReadCount() {
        return streamReadCount;
    }

}
