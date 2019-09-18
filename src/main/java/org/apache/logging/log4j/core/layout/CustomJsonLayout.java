package org.apache.logging.log4j.core.layout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jackson.JsonConstants;
import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import io.cube.agent.CommonUtils;

// REFER TO
// https://github.com/metamx/sumologic-log4j2-appender/blob/master/src/main/java/com/sumologic/log4j/core/SumoJsonLayout.java#L117-L237

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
public final class CustomJsonLayout extends AbstractStringLayout {



    private static final String CONTENT_TYPE = "application/json; charset=" + StandardCharsets.UTF_8.displayName();

    private final ObjectWriter objectWriter;

    @PluginFactory
    public static CustomJsonLayout createLayout(
        @PluginAttribute(value = "locationInfo", defaultBoolean = false) final boolean locationInfo
    )
    {
        final SimpleFilterProvider filters = new SimpleFilterProvider();
        final Set<String> except = new HashSet<>();
        if (!locationInfo) {
            except.add(JsonConstants.ELT_SOURCE);
        }
        except.add("loggerFqcn");
        except.add("endOfBatch");
        //except.add("contextMap");
        except.add("threadId");
        except.add("threadPriority");
        //except.add(JsonConstants.ELT_NANO_TIME);
        filters.addFilter(Log4jLogEvent.class.getName(), SimpleBeanPropertyFilter.serializeAllExcept(except));
        final ObjectWriter writer = new Log4jJsonObjectMapper().writer(new MinimalPrettyPrinter());
        return new CustomJsonLayout(writer.with(filters));
    }

    private CustomJsonLayout(ObjectWriter objectWriter)
    {
        super(StandardCharsets.UTF_8, null, null);
        this.objectWriter = objectWriter;
    }

    /**
     * Formats a {@link org.apache.logging.log4j.core.LogEvent}.
     *
     * @param event The LogEvent.
     *
     * @return The JSON representation of the LogEvent.
     */
    @Override
    public String toSerializable(final LogEvent event)
    {
        final StringBuilderWriter writer = new StringBuilderWriter();
        try {
            objectWriter.writeValue(writer, wrap(event));
            writer.write('\n');
            return writer.toString();
        }
        catch (final IOException e) {
            LOGGER.error(e);
            return Strings.EMPTY;
        }
    }

    // Overridden in tests
    LogEvent wrap(LogEvent event)
    {
        return new CubeEvent(event);
    }

    @Override
    public String getContentType()
    {
        return CONTENT_TYPE;
    }

}
