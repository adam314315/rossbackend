package fr.cnes.regards.framework.test.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import org.assertj.core.util.Lists;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.operation.preprocess.OperationRequestPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationResponsePreprocessor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.payload.RequestFieldsSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.PathParametersSnippet;
import org.springframework.restdocs.request.QueryParametersSnippet;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;

/**
 * Allow to customize the request done thanks to {@link MockMvc}.
 * Methods "performXX" are considered terminal and so applies coherence controls on the customizations.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class RequestBuilderCustomizer {

    /**
     * Documentation snippet constraint fields
     */
    public static final String PARAM_CONSTRAINTS = "constraints";

    /**
     * Documentation snippet field type
     */
    public static final String PARAM_TYPE = "type";

    /**
     * Documentation snippet fields doc title
     */
    public static final String PARAM_TITLE = "title";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestBuilderCustomizer.class);

    /**
     * Default request headers
     */
    private static final HttpHeaders DEFAULT_HEADERS = new HttpHeaders();

    static {
        // let's initiate the default headers!
        DEFAULT_HEADERS.add(HttpConstants.CONTENT_TYPE, "application/json");
        DEFAULT_HEADERS.add(HttpConstants.ACCEPT, "application/json");
    }

    /**
     * Headers
     */
    private final HttpHeaders headers = new HttpHeaders();

    /**
     * Request parameter builder
     */
    private final RequestParamBuilder requestParamBuilder = new RequestParamBuilder();

    /**
     * Documentation snippets
     */
    private final List<Snippet> docSnippets = Lists.newArrayList();

    /**
     * Request result expectations
     */
    private final List<ResultMatcher> expectations = Lists.newArrayList();

    /**
     * Gson builder
     */
    private final GsonBuilder gsonBuilder;

    /**
     * Should the doc be skipped
     */
    private boolean skipDocumentation = false;

    /**
     * Constructor setting the parameter as attribute
     */
    public RequestBuilderCustomizer(GsonBuilder gsonBuilder) {
        this.gsonBuilder = gsonBuilder;
    }

    /**
     * @return the customizer configured to skip the documentation
     */
    public RequestBuilderCustomizer skipDocumentation() {
        skipDocumentation = true;
        return this;
    }

    /**
     * Allows to perform GET request
     */
    protected ResultActions performGet(MockMvc mvc,
                                       String urlTemplate,
                                       String authToken,
                                       String errorMsg,
                                       Object... urlVariables) {
        return performRequest(mvc, getRequestBuilder(authToken, HttpMethod.GET, urlTemplate, urlVariables), errorMsg);
    }

    /**
     * Allows to perform DELETE request
     */
    protected ResultActions performDelete(MockMvc mvc,
                                          String urlTemplate,
                                          String authToken,
                                          String errorMsg,
                                          Object... urlVariables) {
        return performRequest(mvc,
                              getRequestBuilder(authToken, HttpMethod.DELETE, urlTemplate, urlVariables),
                              errorMsg);
    }

    /**
     * Allows to perform POSTn request
     */
    protected ResultActions performPost(MockMvc mvc,
                                        String urlTemplate,
                                        String authToken,
                                        Object content,
                                        String errorMsg,
                                        Object... urlVariables) {
        return performRequest(mvc,
                              getRequestBuilder(authToken, HttpMethod.POST, content, urlTemplate, urlVariables),
                              errorMsg);
    }

    /**
     * Allows to perform Delete request with body
     */
    protected ResultActions performDelete(MockMvc mvc,
                                          String urlTemplate,
                                          String authToken,
                                          Object content,
                                          String errorMsg,
                                          Object... urlVariables) {
        return performRequest(mvc,
                              getRequestBuilder(authToken, HttpMethod.DELETE, content, urlTemplate, urlVariables),
                              errorMsg);
    }

    /**
     * Allows to perform PUT request
     */
    protected ResultActions performPut(MockMvc mvc,
                                       String urlTemplate,
                                       String authToken,
                                       Object content,
                                       String errorMsg,
                                       Object... urlVariables) {
        return performRequest(mvc,
                              getRequestBuilder(authToken, HttpMethod.PUT, content, urlTemplate, urlVariables),
                              errorMsg);
    }

    /**
     * Allows to perform PATCH request
     */
    protected ResultActions performPatch(MockMvc mvc,
                                         String urlTemplate,
                                         String authToken,
                                         Object content,
                                         String errorMsg,
                                         Object... urlVariables) {
        return performRequest(mvc,
                              getRequestBuilder(authToken, HttpMethod.PATCH, content, urlTemplate, urlVariables),
                              errorMsg);
    }

    /**
     * Allows to perform multipart request providing the multiple parts
     */
    protected ResultActions performFileUpload(MockMvc mvc,
                                              String urlTemplate,
                                              String authToken,
                                              List<MockMultipartFile> files,
                                              String errorMsg,
                                              Object... urlVariables) {
        return performRequest(mvc, getMultipartRequestBuilder(authToken, files, urlTemplate, urlVariables), errorMsg);
    }

    /**
     * Allows to perform multipart request providing the path of the file that should be uploaded
     */
    protected ResultActions performFileUpload(MockMvc mvc,
                                              String urlTemplate,
                                              String authToken,
                                              Path filePath,
                                              String errorMsg,
                                              Object... urlVariables) {
        return performRequest(mvc,
                              getMultipartRequestBuilder(authToken, filePath, urlTemplate, urlVariables),
                              errorMsg);
    }

    /**
     * @return {@link MockHttpServletRequestBuilder} customized with RequestBuilderCustomizer#headers or default ones if
     * none has been specified
     */
    private MockHttpServletRequestBuilder getRequestBuilder(String authToken,
                                                            HttpMethod method,
                                                            Object content,
                                                            String urlTemplate,
                                                            Object... urlVariables) {
        MockHttpServletRequestBuilder requestBuilder = getRequestBuilder(authToken, method, urlTemplate, urlVariables);
        String jsonContent = gson(content);
        requestBuilder.content(jsonContent);
        return requestBuilder;
    }

    /**
     * @return {@link MockHttpServletRequestBuilder} customized with RequestBuilderCustomizer#headers or default ones if
     * none has been specified
     */
    protected MockHttpServletRequestBuilder getRequestBuilder(String authToken,
                                                              HttpMethod httpMethod,
                                                              String urlTemplate,
                                                              Object... urlVars) {
        checkCustomizationCoherence(httpMethod);
        MockHttpServletRequestBuilder requestBuilder = RestDocumentationRequestBuilders.request(httpMethod,
                                                                                                urlTemplate,
                                                                                                urlVars);
        addSecurityHeader(requestBuilder, authToken);

        requestBuilder.headers(getHeaders());

        return requestBuilder;
    }

    /**
     * @return jsonified object using GSON
     */
    protected String gson(Object object) {
        if (object instanceof String) {
            return (String) object;
        }
        Gson gson = gsonBuilder.create();
        return gson.toJson(object);
    }

    /**
     * Add name/values request parameter to the request
     */
    public RequestBuilderCustomizer addParameter(String name, String... values) {
        requestParamBuilder.param(name, values);
        return this;
    }

    /**
     * Set or add given value to associated header name values.<br/>
     * Warning : calling this method, {@link #DEFAULT_HEADERS} are not added to the request so you have to do it by yourself if needed.
     */
    @SuppressWarnings("javadoc")
    public RequestBuilderCustomizer addHeader(String name, String value) {
        headers.add(name, value);
        return this;
    }

    /**
     * Set or replace given values to associated header name values.<br/>
     * Warning : calling this method, {@link #DEFAULT_HEADERS} are not added to the request so you have to do it by yourself if needed.
     */
    @SuppressWarnings("javadoc")
    public RequestBuilderCustomizer addHeader(String name, List<String> values) {
        headers.put(name, values);
        return this;
    }

    /**
     * Set or replace given values to associated header name values.<br/>
     * Warning : calling this method, {@link #DEFAULT_HEADERS} are not added to the request so you have to do it by yourself if needed.
     */
    @SuppressWarnings("javadoc")
    public RequestBuilderCustomizer addHeaders(Map<String, List<String>> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * Low level getter to directly customize request headers
     */
    public HttpHeaders headers() {
        return headers;
    }

    /**
     * Add a ResultMatcher to be matched.
     */
    public RequestBuilderCustomizer expect(ResultMatcher matcher) {
        expectations.add(matcher);
        return this;
    }

    /**
     * Add a ResultMatcher status OK to be matched
     */
    public RequestBuilderCustomizer expectStatus(HttpStatus status) {
        return expect(MockMvcResultMatchers.status().is(status.value()));
    }

    /**
     * Add a ResultMatcher status OK to be matched
     */
    public RequestBuilderCustomizer expectStatusOk() {
        return expect(MockMvcResultMatchers.status().isOk());
    }

    /**
     * Add a ResultMatcher on specific Content-disposition header to be match
     */
    public RequestBuilderCustomizer expectHeaderContentDispositionFileName(String fileName) {
        return expect(MockMvcResultMatchers.header()
                                           .string(HttpHeaders.CONTENT_DISPOSITION,
                                                   ContentDisposition.builder("attachment")
                                                                     .filename(fileName)
                                                                     .build()
                                                                     .toString()));
    }

    /**
     * Add a ResultMatcher status CREATED to be matched
     */
    public RequestBuilderCustomizer expectStatusCreated() {
        return expect(MockMvcResultMatchers.status().isCreated());
    }

    /**
     * Add a ResultMatcher status NOT_FOUND to be matched
     */
    public RequestBuilderCustomizer expectStatusNotFound() {
        return expect(MockMvcResultMatchers.status().isNotFound());
    }

    /**
     * Add a ResultMatcher status BAD_REQUEST to be matched
     */
    public RequestBuilderCustomizer expectStatusBadRequest() {
        return expect(MockMvcResultMatchers.status().isBadRequest());
    }

    /**
     * Add a ResultMatcher status NO_CONTENT to be matched
     */
    public RequestBuilderCustomizer expectStatusNoContent() {
        return expect(MockMvcResultMatchers.status().isNoContent());
    }

    /**
     * Add a ResultMatcher status FORBIDDEN to be matched
     */
    public RequestBuilderCustomizer expectStatusForbidden() {
        return expect(MockMvcResultMatchers.status().isForbidden());
    }

    /**
     * Add a ResultMatcher status CONFLICT to be matched
     */
    public RequestBuilderCustomizer expectStatusConflict() {
        return expect(MockMvcResultMatchers.status().isConflict());
    }

    /**
     * Add a ResultMatcher expecting given contentType to be matched
     */
    public RequestBuilderCustomizer expectContentType(String contentType) {
        return expect(MockMvcResultMatchers.content().contentType(contentType));
    }

    /**
     * Add a ResultMatcher expecting given jsonPath is not empty
     */
    public RequestBuilderCustomizer expectIsNotEmpty(String jsonPath) {
        return expect(MockMvcResultMatchers.jsonPath(jsonPath).isNotEmpty());
    }

    /**
     * Add a ResultMatcher expecting given jsonPath is empty
     */
    public RequestBuilderCustomizer expectIsEmpty(String jsonPath) {
        return expect(MockMvcResultMatchers.jsonPath(jsonPath).isEmpty());
    }

    /**
     * Add a ResultMatcher expecting given jsonPath is an array
     */
    public RequestBuilderCustomizer expectIsArray(String jsonPath) {
        return expect(MockMvcResultMatchers.jsonPath(jsonPath).isArray());
    }

    /**
     * Add a ResultMatcher expecting given jsonPath has given value
     */
    public RequestBuilderCustomizer expectValue(String jsonPath, Object value) {
        return expect(MockMvcResultMatchers.jsonPath(jsonPath).value(value));
    }

    /**
     * Add a ResultMatcher expecting given jsonPath matches given pattern
     */
    public RequestBuilderCustomizer expectValueMatchesPattern(String jsonPath, String regex) {
        return expect(MockMvcResultMatchers.jsonPath(jsonPath, Matchers.matchesPattern(regex)));
    }

    /**
     * Add a ResultMatcher expecting given jsonPath has given value
     */
    public RequestBuilderCustomizer expectValueContains(String jsonPath, Object value) {
        return expect(MockMvcResultMatchers.jsonPath(jsonPath, Matchers.contains(value)));
    }

    /**
     * Add a ResultMatcher expecting given jsonPath has given value
     */
    public RequestBuilderCustomizer expectArrayContains(String jsonPath, Object value) {
        return expect(MockMvcResultMatchers.jsonPath(jsonPath, Matchers.hasItem(value)));
    }

    /**
     * Add a ResultMatcher expecting given jsonPath (corresponding to an array) to have given size
     */
    public RequestBuilderCustomizer expectToHaveSize(String jsonPath, int size) {
        return expect(MockMvcResultMatchers.jsonPath(jsonPath, Matchers.hasSize(size)));
    }

    /**
     * Add a ResultMatcher expecting given jsonPath to have given toString() string value
     */
    public RequestBuilderCustomizer expectToHaveToString(String jsonPath, String expectedToString) {
        return expect(MockMvcResultMatchers.jsonPath(jsonPath, Matchers.hasToString(expectedToString)));
    }

    /**
     * Add a ResultMatcher expecting given jsonPath to be missing
     */
    public RequestBuilderCustomizer expectDoesNotExist(String jsonPath) {
        return expect(MockMvcResultMatchers.jsonPath(jsonPath).doesNotExist());
    }

    /**
     * Add snippets to be used to generate specific documentation.
     * To document request parameters (aka GET params) you better use {@link #documentRequestParameters(List)} <br/>
     * To document path parameters you better use {@link #documentPathParameters(List)} <br/>
     * To document request body you better use {@link #documentRequestBody(List)} <br/>
     * To document response body you better use {@link #documentResponseBody(List)} <br/>
     *
     * @param snippet documentation snippet to be added.
     */
    public RequestBuilderCustomizer document(Snippet snippet) {
        docSnippets.add(snippet);
        return this;
    }

    public RequestBuilderCustomizer documentRequestParameters(List<ParameterDescriptor> descriptors) {
        return documentRequestParameters(descriptors.toArray(new ParameterDescriptor[0]));
    }

    public RequestBuilderCustomizer documentRequestParameters(ParameterDescriptor... descriptors) {
        Optional<Snippet> optionalExistingSnippet = docSnippets.stream()
                                                               .filter(docSnippet -> docSnippet instanceof QueryParametersSnippet)
                                                               .findFirst();
        // Check if an existing request params exists
        if (optionalExistingSnippet.isPresent()) {
            throw new RuntimeException("You cannot call this method several time.");
        } else {
            // Create another
            docSnippets.add(RequestDocumentation.queryParameters(descriptors));
        }
        return this;
    }

    public RequestBuilderCustomizer documentRequestBody(List<FieldDescriptor> descriptors) {
        return documentRequestBody(descriptors.toArray(new FieldDescriptor[0]));
    }

    public RequestBuilderCustomizer documentRequestBody(FieldDescriptor... descriptors) {
        Optional<Snippet> optionalExistingSnippet = docSnippets.stream()
                                                               .filter(docSnippet -> docSnippet instanceof RequestFieldsSnippet)
                                                               .findFirst();
        // Check if an existing request params exists
        if (optionalExistingSnippet.isPresent()) {
            throw new RuntimeException("You cannot call this method several time.");
        } else {
            // Create another
            docSnippets.add(PayloadDocumentation.relaxedRequestFields(descriptors));
        }
        return this;
    }

    public RequestBuilderCustomizer documentPathParameters(List<ParameterDescriptor> descriptors) {
        return documentPathParameters(descriptors.toArray(new ParameterDescriptor[0]));
    }

    public RequestBuilderCustomizer documentPathParameters(ParameterDescriptor... descriptors) {
        Optional<Snippet> optionalExistingSnippet = docSnippets.stream()
                                                               .filter(docSnippet -> docSnippet instanceof PathParametersSnippet)
                                                               .findFirst();
        // Check if an existing path params exists
        if (optionalExistingSnippet.isPresent()) {
            // Add to existing path params
            PathParametersSnippet snippet = (PathParametersSnippet) optionalExistingSnippet.get();
            snippet.and(descriptors);
        } else {
            // Create another
            docSnippets.add(RequestDocumentation.pathParameters(descriptors));
        }
        return this;
    }

    public RequestBuilderCustomizer documentResponseBody(List<FieldDescriptor> descriptors) {
        return documentResponseBody(descriptors.toArray(new FieldDescriptor[0]));
    }

    public RequestBuilderCustomizer documentResponseBody(FieldDescriptor... descriptors) {
        Optional<Snippet> optionalExistingSnippet = docSnippets.stream()
                                                               .filter(docSnippet -> docSnippet instanceof ResponseFieldsSnippet)
                                                               .findFirst();
        // Check if an existing path params exists
        if (optionalExistingSnippet.isPresent()) {
            // Add to existing path params
            ResponseFieldsSnippet snippet = (ResponseFieldsSnippet) optionalExistingSnippet.get();
            snippet.and(descriptors);
        } else {
            // Create another
            docSnippets.add(PayloadDocumentation.relaxedResponseFields(descriptors));
        }
        return this;
    }

    /**
     * perform a request and generate the documentation
     */
    private ResultActions performRequest(MockMvc mvc, MockHttpServletRequestBuilder requestBuilder, String errorMsg) {
        Assert.assertFalse("At least one expectation is required", expectations.isEmpty());
        try {
            // lets create the attributes and description for the documentation snippet
            requestBuilder.params(requestParamBuilder.getParameters());
            ResultActions request = mvc.perform(requestBuilder);
            for (ResultMatcher matcher : expectations) {
                request = request.andExpect(matcher);
            }
            if (!skipDocumentation) {
                OperationRequestPreprocessor reqPreprocessor = preprocessRequest(prettyPrint(),
                                                                                 removeHeaders("Authorization",
                                                                                               "Host",
                                                                                               "Content-Length"));
                OperationResponsePreprocessor respPreprocessor = preprocessResponse(prettyPrint(),
                                                                                    removeHeaders("Content-Length"));
                request.andDo(MockMvcRestDocumentation.document("{ClassName}/{methodName}",
                                                                reqPreprocessor,
                                                                respPreprocessor,
                                                                docSnippets.toArray(new Snippet[0])));
            }
            return request;
        } catch (Exception e) { // NOSONAR
            LOGGER.error(errorMsg, e);
            throw new AssertionError(errorMsg, e);
        }
    }

    /**
     * Build a multi-part request builder based on file {@link Path}
     *
     * @param authToken   authorization token
     * @param filePath    {@link Path}
     * @param urlTemplate URL template
     * @param urlVars     URL vars
     * @return {@link MockMultipartHttpServletRequestBuilder}
     */
    protected MockMultipartHttpServletRequestBuilder getMultipartRequestBuilder(String authToken,
                                                                                Path filePath,
                                                                                String urlTemplate,
                                                                                Object... urlVars) {

        try {
            MockMultipartFile file = new MockMultipartFile("file", Files.newInputStream(filePath));
            List<MockMultipartFile> fileList = new ArrayList<>(1);
            fileList.add(file);
            return getMultipartRequestBuilder(authToken, fileList, urlTemplate, urlVars);
        } catch (IOException e) {
            String message = String.format("Cannot create input stream for file %s", filePath);
            LOGGER.error(message, e);
            throw new AssertionError(message, e);
        }
    }

    /**
     * Build a multi-part request builder based on file {@link Path}
     *
     * @param authToken   authorization token
     * @param files       {@link MockMultipartFile}s
     * @param urlTemplate URL template
     * @param urlVars     URL vars
     * @return {@link MockMultipartHttpServletRequestBuilder}
     */
    protected MockMultipartHttpServletRequestBuilder getMultipartRequestBuilder(String authToken,
                                                                                List<MockMultipartFile> files,
                                                                                String urlTemplate,
                                                                                Object... urlVars) {
        // we check with HttpMethod POST because fileUpload method generates a POST request.
        checkCustomizationCoherence(HttpMethod.POST);

        MockMultipartHttpServletRequestBuilder multipartRequestBuilder = RestDocumentationRequestBuilders.multipart(
            urlTemplate,
            urlVars);
        files.forEach(multipartRequestBuilder::file);
        addSecurityHeader(multipartRequestBuilder, authToken);
        multipartRequestBuilder.headers(getHeaders());
        return multipartRequestBuilder;
    }

    /**
     * Check if the request customizer is coherent towards the multiple options used
     */
    protected void checkCustomizationCoherence(HttpMethod httpMethod) {
        // constraints are only on DELETE and PUT, for now, as they cannot have request parameters
        if (httpMethod.equals(HttpMethod.DELETE) || httpMethod.equals(HttpMethod.PUT)) {
            if (!requestParamBuilder.getParameters().isEmpty()) {
                throw new IllegalStateException(String.format("Method %s cannot have request parameters", httpMethod));
            }
        }
    }

    /**
     * Add the authorization header to the request
     */
    protected void addSecurityHeader(MockHttpServletRequestBuilder requestBuilder, String authToken) {
        requestBuilder.header(HttpConstants.AUTHORIZATION, HttpConstants.BEARER + " " + authToken);
    }

    /**
     * Contains logic on which headers should be used for a request.
     *
     * @return default headers if no header customization has been done. Customized headers otherwise.
     */
    protected HttpHeaders getHeaders() {
        if (headers.isEmpty()) {
            return DEFAULT_HEADERS;
        } else {
            return headers;
        }
    }
}
