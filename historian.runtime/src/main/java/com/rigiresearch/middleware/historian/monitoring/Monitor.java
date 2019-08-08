package com.rigiresearch.middleware.historian.monitoring;

import com.rigiresearch.middleware.graph.Node;
import java.io.IOException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.ToString;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;

/**
 * A graph node augmented with monitoring data.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@ToString(of = {"identifier"})
public final class Monitor extends Node {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -704880779306199040L;

    /**
     * A unique identifier for this monitor.
     */
    private String identifier;

    /**
     * The configuration properties.
     */
    private final Configuration config;

    /**
     * Input values from the context of this monitor.
     */
    private final Map<String, Object> context;

    /**
     * Input values.
     */
    private final Map<String, Object> values;

    /**
     * Empty constructor.
     */
    public Monitor() {
        this(new Node(), new PropertiesConfiguration());
    }

    /**
     * Default constructor.
     * @param node The node on which this monitor is based.
     * @param config The configuration properties
     */
    public Monitor(final Node node, final Configuration config) {
        super(
            node.getName(),
            node.getTemplate(),
            node.getParameters(false),
            node.getMetadata()
        );
        this.identifier = node.getName();
        this.config = config;
        this.context = new HashMap<>(0);
        this.values = this.getParameters(true).stream()
            .filter(com.rigiresearch.middleware.graph.Input.class::isInstance)
            .map(com.rigiresearch.middleware.graph.Input.class::cast)
            .filter(com.rigiresearch.middleware.graph.Input::hasConcreteValue)
            .collect(
                Collectors.toMap(
                    com.rigiresearch.middleware.graph.Input::getName,
                    com.rigiresearch.middleware.graph.Input::getValue
                )
            );
    }

    /**
     * A unique identifier for this monitor.
     * @return A unique identifier for this monitor
     */
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Updates this monitor's identifier.
     * @param format A format string
     * @param args Components to the identifier
     */
    public void setIdentifier(final String format, final String... args) {
        this.identifier = String.format(format, args);
    }

    /**
     * Input values.
     * @return The input values
     */
    public Map<String, Object> getValues() {
        return this.values;
    }

    /**
     * Sets or updates an input value.
     * @param name The name of the input
     * @param value The value to set
     * @return The previous value of the input or null
     */
    public Object setValue(final String name, final Object value) {
        return this.values.put(name, value);
    }

    /**
     * Input values from the context of this monitor.
     * @return The input values from the context of this monitor
     */
    public Map<String, Object> getContextValues() {
        return this.values;
    }

    /**
     * Sets or updates a context value.
     * @param name The name of the value
     * @param value The value to set
     * @return The previous value of the context value or null
     */
    public Object setContextValue(final String name, final Object value) {
        return this.context.put(name, value);
    }

    /**
     * Returns a combined map of input values from the configuration and context
     * of this monitor.
     * @return A map containing string values
     */
    public Map<String, String> allValues() {
        final Map<String, String> result =
            new HashMap<>(this.values.size() + this.context.size());
        result.putAll(
            this.values.entrySet()
                .stream()
                .map(Monitor::toValueString)
                .collect(Monitor.toMap())
        );
        result.putAll(
            this.context.entrySet()
                .stream()
                .map(Monitor::toValueString)
                .collect(Monitor.toMap())
        );
        return result;
    }

    /**
     * The inputs associated with this monitor.
     * @return A list of {@link Input}s
     */
    private List<Input> inputs() {
        String name = this.getName();
        if (this.isTemplateBased()) {
            name = this.getTemplate().getName();
        }
        final String key = String.format("%s.inputs", name);
        final String[] names = this.config.getStringArray(key);
        final List<Input> list = new ArrayList<>(names.length);
        final Map<String, String> map = this.allValues();
        for (final String str : names) {
            final String required =
                String.format("%s.inputs.%s.required", name, str);
            final String location =
                String.format("%s.inputs.%s.location", name, str);
            list.add(
                new Input(
                    str,
                    map.get(str),
                    this.config.getBoolean(required, false),
                    Input.Location.valueOf(this.config.getString(location))
                )
            );
        }
        return list;
    }

    /**
     * Collects content from the associated URL.
     * @return The collected content
     * @throws IOException If the URL is invalid or there is a problem
     *  collecting the data
     * @throws UnexpectedResponseCodeException See {@link Request#data()}
     */
    public String collect() throws IOException, UnexpectedResponseCodeException {
        String name = this.getName();
        if (this.isTemplateBased()) {
            name = this.getTemplate().getName();
        }
        final URL url = new URL(
            this.config.getString(String.format("%s.url", name))
        );
        return new Request(this.inputs(), url).data();
    }

    /**
     * A map collector for map entries.
     * From https://stackoverflow.com/a/52989113
     * @param <K> The type of the key
     * @param <V> The type of the value
     * @return A setup stream collector
     */
    private static <K, V> Collector<? super Map.Entry<K, V>, ?, Map<K, V>> toMap() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    /**
     * Creates a new entry with the string value of the argument.
     * @param entry The original entry
     * @return A new entry
     */
    private static Map.Entry<String, String> toValueString(
        final Map.Entry<String, Object> entry) {
        return new AbstractMap.SimpleEntry<>(
            entry.getKey(),
            entry.getValue().toString()
        );
    }

    /**
     * Determines whether this and another object are equivalent based on
     * their names/identifier.
     * @param object The other object
     * @return Whether the two objects are equivalent
     */
    @Override
    public boolean equals(final Object object) {
        boolean equivalent = false;
        if (object instanceof Monitor) {
            final Monitor monitor = (Monitor) object;
            equivalent = this.identifier.equals(monitor.identifier)
                && this.getName().equals(monitor.getName());
        } else if (object instanceof Node) {
            final Node node = (Node) object;
            equivalent = this.getName().equals(node.getName());
        }
        return equivalent;
    }

    /**
     * Generates a hash code for the identifier.
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.identifier);
    }

}
