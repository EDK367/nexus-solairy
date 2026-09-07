package org.nexus.nexussolairy.model.semantic;

import org.nexus.nexussolairy.model.enums.DataType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class StructInfo {
    public final String name;
    public final Map<String, DataType> fields = new LinkedHashMap<>();
    public final Map<String, String> fieldStructTypes = new LinkedHashMap<>();
    public final Map<String, Boolean> fieldIsArray = new LinkedHashMap<>();

    public StructInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addField(String fieldName, DataType type) {
        fields.put(fieldName, type);
    }

    public void addField(String fieldName, DataType type, String structType, boolean isArray) {
        fields.put(fieldName, type);
        if (structType != null) {
            fieldStructTypes.put(fieldName, structType);
        }
        fieldIsArray.put(fieldName, isArray);
    }

    public boolean hasField(String fieldName) {
        return fields.containsKey(fieldName);
    }

    public DataType getFieldType(String fieldName) {
        return fields.get(fieldName);
    }

    public String getFieldStructType(String fieldName) {
        return fieldStructTypes.get(fieldName);
    }

    public boolean isFieldArray(String fieldName) {
        return Boolean.TRUE.equals(fieldIsArray.get(fieldName));
    }

    public Map<String, DataType> getFields() {
        return Collections.unmodifiableMap(fields);
    }
}
