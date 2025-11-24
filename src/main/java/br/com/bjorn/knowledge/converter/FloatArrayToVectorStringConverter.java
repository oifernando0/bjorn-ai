package br.com.bjorn.knowledge.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.stream.Collectors;

@Converter(autoApply = false)
public class FloatArrayToVectorStringConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        return Arrays.stream(attribute)
                .mapToObj(Float::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new float[0];
        }
        String trimmed = dbData.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return new float[0];
        }

        String[] parts = trimmed.split(",");
        float[] values = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Float.parseFloat(parts[i].trim());
        }
        return values;
    }
}
