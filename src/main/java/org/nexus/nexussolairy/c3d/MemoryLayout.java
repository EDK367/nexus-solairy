package org.nexus.nexussolairy.c3d;

import org.nexus.nexussolairy.model.semantic.ClassSymbol;
import org.nexus.nexussolairy.model.semantic.StructInfo;
import org.nexus.nexussolairy.model.semantic.Symbol;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MemoryLayout {

    // almacenamiento para estructuras complejas
    private int globalOffset = 0;
    private final Map<String, Integer> globalVarOffsets = new HashMap<>();

    private final Map<String, Map<String, Integer>> localFrameOffsets = new HashMap<>();
    private final Map<String, Integer> frameSizes = new HashMap<>();

    private final Map<String, Map<String, Integer>> structFieldOffsets = new HashMap<>();

    private final Map<String, Map<String, Integer>> classFieldOffsets = new HashMap<>();

    public int declareGlobal(String name) {
        if (globalVarOffsets.containsKey(name)) {
            return globalVarOffsets.get(name);
        }
        int offset = globalOffset++;
        globalVarOffsets.put(name, offset);
        return offset;
    }

    public int getGlobalOffset(String name) {
        return globalVarOffsets.getOrDefault(name, -1);
    }

    public boolean isGlobal(String name, String currentRoutine) {
        if (currentRoutine == null || currentRoutine.isEmpty() || "MAIOR".equals(currentRoutine) || "main".equalsIgnoreCase(currentRoutine)) return true;
        Map<String, Integer> routineMap = localFrameOffsets.get(currentRoutine);
        if (routineMap != null && routineMap.containsKey(name)) return false;
        return globalVarOffsets.containsKey(name);
    }

    public int declareLocal(String routineName, String varName) {
        Map<String, Integer> map = localFrameOffsets.computeIfAbsent(routineName, k -> new LinkedHashMap<>());
        if (map.containsKey(varName)) {
            return map.get(varName);
        }

        int currentSize = frameSizes.getOrDefault(routineName, 1);
        map.put(varName, currentSize);
        frameSizes.put(routineName, currentSize + 1);
        return currentSize;
    }

    public int declareParam(String routineName, String paramName) {
        Map<String, Integer> map = localFrameOffsets.computeIfAbsent(routineName, k -> new LinkedHashMap<>());
        if (map.containsKey(paramName)) {
            return map.get(paramName);
        }
        int currentSize = frameSizes.getOrDefault(routineName, 1);
        map.put(paramName, currentSize);
        frameSizes.put(routineName, currentSize + 1);
        return currentSize;
    }

    public int getLocalOffset(String routineName, String varName) {
        Map<String, Integer> map = localFrameOffsets.get(routineName);
        if (map == null || !map.containsKey(varName)) return -1;
        return map.get(varName);
    }

    public int getFrameSize(String routineName) {
        return frameSizes.getOrDefault(routineName, 1);
    }

    public int getGlobalCount() {
        return globalOffset;
    }


    public int getCallFrameOffset(String callerRoutine) {
        if ("MAIOR".equals(callerRoutine) || "main".equalsIgnoreCase(callerRoutine)) {
            return Math.max(globalOffset, frameSizes.getOrDefault(callerRoutine, 1));
        }
        return frameSizes.getOrDefault(callerRoutine, 1);
    }

    public void registerStruct(StructInfo structInfo) {
        if (structInfo == null) return;
        Map<String, Integer> offsets = new LinkedHashMap<>();
        int offset = 0;
        for (String fieldName : structInfo.getFields().keySet()) {
            offsets.put(fieldName, offset++);
        }
        structFieldOffsets.put(structInfo.getName(), offsets);
    }

    public int getStructFieldOffset(String structName, String fieldName) {
        Map<String, Integer> map = structFieldOffsets.get(structName);
        return map != null ? map.getOrDefault(fieldName, -1) : -1;
    }

    public void registerClass(ClassSymbol classSymbol) {
        if (classSymbol == null) return;
        Map<String, Integer> offsets = new LinkedHashMap<>();
        int offset = 0;
        for (Symbol field : classSymbol.getFields().values()) {
            offsets.put(field.getName(), offset++);
        }
        classFieldOffsets.put(classSymbol.getName(), offsets);
    }

    public int getClassFieldOffset(String className, String fieldName) {
        Map<String, Integer> map = classFieldOffsets.get(className);
        return map != null ? map.getOrDefault(fieldName, -1) : -1;
    }

    public Map<String, Integer> getGlobalVarOffsets() {
        return java.util.Collections.unmodifiableMap(globalVarOffsets);
    }

    public Map<String, Map<String, Integer>> getLocalFrameOffsets() {
        return java.util.Collections.unmodifiableMap(localFrameOffsets);
    }

    public Map<String, Map<String, Integer>> getStructFieldOffsets() {
        return java.util.Collections.unmodifiableMap(structFieldOffsets);
    }

    public Map<String, Map<String, Integer>> getClassFieldOffsets() {
        return java.util.Collections.unmodifiableMap(classFieldOffsets);
    }
}
