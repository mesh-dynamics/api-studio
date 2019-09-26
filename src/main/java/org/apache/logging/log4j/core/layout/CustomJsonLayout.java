package org.apache.logging.log4j.core.layout;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import io.cube.agent.CommonUtils;

// REFER TO
// https://github.com/metamx/sumologic-log4j2-appender/blob/master/src/main/java/com/sumologic/log4j/core/SumoJsonLayout.java#L117-L237
// https://github.com/apache/logging-log4j2/blob/dad62657690ded5ab1f0e8524fed4efd5f418edb/log4j-core/src/main/java/org/apache/logging/log4j/core/layout/JsonLayout.java

/**
 * Appends a series of JSON events as strings serialized as bytes.
 *
 * <h3>Complete well-formed JSON vs. fragment JSON</h3>
 * <p>
 * If you configure {@code complete="true"}, the appender outputs a well-formed JSON document. By default, with
 * {@code complete="false"}, you should include the output as an <em>external file</em> in a separate file to form a
 * well-formed JSON document.
 * </p>
 * <p>
 * If {@code complete="false"}, the appender does not write the JSON open array character "[" at the start
 * of the document, "]" and the end, nor comma "," between records.
 * </p>
 * <h3>Encoding</h3>
 * <p>
 * Appenders using this layout should have their {@code charset} set to {@code UTF-8} or {@code UTF-16}, otherwise
 * events containing non ASCII characters could result in corrupted log files.
 * </p>
 * <h3>Pretty vs. compact JSON</h3>
 * <p>
 * By default, the JSON layout is not compact (a.k.a. "pretty") with {@code compact="false"}, which means the
 * appender uses end-of-line characters and indents lines to format the text. If {@code compact="true"}, then no
 * end-of-line or indentation is used. Message content may contain, of course, escaped end-of-lines.
 * </p>
 * <h3>Additional Fields</h3>
 * <p>
 * This property allows addition of custom fields into generated JSON.
 * {@code <JsonLayout><KeyValuePair key="foo" value="bar"/></JsonLayout>} inserts {@code "foo":"bar"} directly
 * into JSON output. Supports Lookup expressions.
 * </p>
 */
@Plugin(name = "CustomJsonLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class CustomJsonLayout extends AbstractJacksonLayout {

    private static final String DEFAULT_FOOTER = "]";

    private static final String DEFAULT_HEADER = "[";

    static final String CONTENT_TYPE = "application/json";

/*    protected CustomJsonLayout(Configuration config, ObjectWriter objectWriter, Charset charset, boolean compact,
                               boolean complete, boolean eventEol, Serializer headerSerializer,
                               Serializer footerSerializer, boolean includeNullDelimiter, KeyValuePair[] additionalFields) {
        super(config, objectWriter, charset, compact, complete, eventEol, headerSerializer,
            footerSerializer, includeNullDelimiter, additionalFields);
    }*/

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static class Builder<B extends Builder<B>> extends AbstractJacksonLayout.Builder<B>
        implements org.apache.logging.log4j.core.util.Builder<CustomJsonLayout> {

        @PluginBuilderAttribute
        private boolean propertiesAsList;

        @PluginElement("Extras")
        private KeyValuePair[] extras;

        public Builder() {
            super();
            setCharset(StandardCharsets.UTF_8);
        }

        @Override
        public CustomJsonLayout build() {
            final boolean encodeThreadContextAsList = isProperties() && propertiesAsList;
            final String headerPattern = toStringOrNull(getHeader());
            final String footerPattern = toStringOrNull(getFooter());
            return new CustomJsonLayout(getConfiguration(), isLocationInfo(), isProperties(), encodeThreadContextAsList,
                isComplete(), isCompact(), getEventEol(), headerPattern, footerPattern, getCharset(),
                isIncludeStacktrace(), isStacktraceAsString(), isIncludeNullDelimiter(), true,
                getExtras());
        }

        public boolean isPropertiesAsList() {
            return propertiesAsList;
        }

        public B setPropertiesAsList(final boolean propertiesAsList) {
            this.propertiesAsList = propertiesAsList;
            return asBuilder();
        }

        public KeyValuePair[] getExtras() {
            return extras;
        }

        public B setExtras(KeyValuePair[] extras) {
            this.extras = extras;
            return asBuilder();
        }
    }

    private Map<String, Object> extras = null;

    /**
     * @deprecated Use {@link #newBuilder()} instead
     */
    @Deprecated
    protected CustomJsonLayout(final Configuration config, final boolean locationInfo, final boolean properties,
                         final boolean encodeThreadContextAsList,
                         final boolean complete, final boolean compact, final boolean eventEol, final String headerPattern,
                         final String footerPattern, final Charset charset, final boolean includeStacktrace) {

        super(config, getObjectWriter(encodeThreadContextAsList, includeStacktrace, false,
            true, locationInfo, properties, compact),
            charset, compact, complete, eventEol,
            PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(headerPattern).setDefaultPattern(DEFAULT_HEADER).build(),
            PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(footerPattern).setDefaultPattern(DEFAULT_FOOTER).build(),
            false);

        extras = null;
    }

    private static ObjectWriter getObjectWriter(boolean encodeThreadContextAsList, boolean includeStacktrace,
                                         boolean stacktraceAsString, boolean objectMessageAsJsonObject,
                                         boolean locationInfo, boolean properties, boolean compact) {
        final SimpleFilterProvider filters = new SimpleFilterProvider();
        SimpleBeanPropertyFilter simpleBeanPropertyFilter = SimpleBeanPropertyFilter.serializeAllExcept(Set
            .of("loggerFqcn" , "endOfBatch" , "contextMap" , "threadId" , "threadPriority" , "source" , "nanoTime" , "instant"));
        filters.addFilter(Log4jLogEvent.class.getName(), simpleBeanPropertyFilter);
        return new JacksonFactory.JSON(encodeThreadContextAsList, includeStacktrace, stacktraceAsString , objectMessageAsJsonObject).newWriter(
            locationInfo, properties, compact).with(filters);

    }


    private CustomJsonLayout(final Configuration config, final boolean locationInfo, final boolean properties,
                             final boolean encodeThreadContextAsList,
                             final boolean complete, final boolean compact, final boolean eventEol,
                             final String headerPattern, final String footerPattern, final Charset charset,
                             final boolean includeStacktrace, final boolean stacktraceAsString,
                             final boolean includeNullDelimiter, final boolean objectMessageAsJsonObject,
                             final KeyValuePair[] extras) {

        super(config, getObjectWriter(encodeThreadContextAsList, includeStacktrace, stacktraceAsString,
            true, locationInfo, properties, compact), charset, compact, complete, eventEol,
            PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(headerPattern).setDefaultPattern(DEFAULT_HEADER).build(),
            PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(footerPattern).setDefaultPattern(DEFAULT_FOOTER).build(),
            includeNullDelimiter, extras);

        if (extras != null && extras.length > 0) {
            final Map<String, Object> extrasMap = new LinkedHashMap<>();

            for (KeyValuePair pair : extras) {
                extrasMap.put(pair.getKey(), pair.getValue());
            }

            this.extras = Collections.unmodifiableMap(extrasMap);
        } else {
            this.extras = null;
        }
    }

    /**
     * Returns appropriate JSON header.
     *
     * @return a byte array containing the header, opening the JSON array.
     */
    @Override
    public byte[] getHeader() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        final String str = serializeToString(getHeaderSerializer());
        if (str != null) {
            buf.append(str);
        }
        buf.append(this.eol);
        return getBytes(buf.toString());
    }

    /**
     * Returns appropriate JSON footer.
     *
     * @return a byte array containing the footer, closing the JSON array.
     */
    @Override
    public byte[] getFooter() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(this.eol);
        final String str = serializeToString(getFooterSerializer());
        if (str != null) {
            buf.append(str);
        }
        buf.append(this.eol);
        return getBytes(buf.toString());
    }

    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>();
        result.put("version", "2.0");
        return result;
    }

    /**
     * @return The content type.
     */
    @Override
    public String getContentType() {
        return CONTENT_TYPE + "; charset=" + this.getCharset();
    }


    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }


    @Override
    public void toSerializable(final LogEvent event, final Writer writer) throws IOException {
        if (complete && eventCount > 0) {
            writer.append(", ");
        }
        super.toSerializable(event, writer);
    }

    @Override
    protected Object wrapLogEvent(LogEvent event) {
        Object result = super.wrapLogEvent(event);
        return new LogEventWithExtras(result, Map.of("traceId",
            CommonUtils.getCurrentTraceId().orElse("N/A") , "timestamp" , simpleDateFormat.format(new Date())));

    }

    public static class LogEventWithExtras {

        private final Object logEvent;
        private final Map<String, Object> extras;

        public LogEventWithExtras(Object logEvent, Map<String, Object> extras) {
            this.logEvent = logEvent;
            this.extras = extras;
        }

        @JsonUnwrapped
        public Object getLogEvent() {
            return logEvent;
        }

        @JsonAnyGetter
        public Map<String, Object> getExtras() {
            return extras;
        }
    }

}
