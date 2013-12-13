package cz.tul.dic.opencl.test.gen;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Petr Jecmen
 */
public class ParameterSet implements Comparable<ParameterSet> {

    private final Map<Parameter, Integer> parameters;

    public ParameterSet() {
        parameters = new EnumMap<>(Parameter.class);
    }

    public void addParameter(final Parameter param, final int value) {
        parameters.put(param, value);
    }

    public int getValue(final Parameter param) {
        return parameters.get(param);
    }

    public boolean contains(final Parameter param) {
        return parameters.containsKey(param);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        for (Entry<Parameter, Integer> e : parameters.entrySet()) {
            sb.append(e.getKey());
            sb.append(":");
            sb.append(e.getValue());
            sb.append(", ");
        }
        sb.setLength(sb.length() - 2);

        return sb.toString();
    }
    
    public String toStringSmall() {
        final StringBuilder sb = new StringBuilder();        
        
        sb.append(parameters.get(Parameter.IMAGE_WIDTH));
        sb.append(",");
        sb.append(parameters.get(Parameter.IMAGE_HEIGHT));
        sb.append(",");
        sb.append(parameters.get(Parameter.FACET_SIZE));
        sb.append(",");
        sb.append(parameters.get(Parameter.DEFORMATION_COUNT));

        return sb.toString();
    }

    @Override
    public int compareTo(ParameterSet o) {
        int result = 0;

        int comp;
        for (Parameter p : Parameter.values()) {
            if (contains(p) && o.contains(p)) {
                comp = Integer.compare(getValue(p), o.getValue(p));
                if (comp != 0) {
                    result = comp;
                    break;
                }
            }
        }

        return result;
    }

    public boolean equals(ParameterSet ps, Parameter... params) {
        boolean result = true;

        for (Parameter p : params) {
            if (getValue(p) != ps.getValue(p)) {
                result = false;
                break;
            }
        }

        return result;
    }

}
