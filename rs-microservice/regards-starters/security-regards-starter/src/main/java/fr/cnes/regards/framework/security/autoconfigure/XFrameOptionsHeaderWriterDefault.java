package fr.cnes.regards.framework.security.autoconfigure;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.header.HeaderWriter;

/**
 * Set the value of header X-FRAME-OPTIONS to "DENY" if none has been provided by dev. Respect the given value otherwise
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class XFrameOptionsHeaderWriterDefault implements HeaderWriter {

    /**
     * Default value
     */
    private static final String DEFAULT_VALUE = "DENY";

    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        if ((response != null) && Strings.isNullOrEmpty(response.getHeader(HttpHeaders.X_FRAME_OPTIONS))) {
            response.setHeader(HttpHeaders.X_FRAME_OPTIONS, DEFAULT_VALUE);
        }
    }
}
