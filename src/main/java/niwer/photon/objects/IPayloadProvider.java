package niwer.photon.objects;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Interface for objects that can provide a payload representation of their data.
 * 
 * @author Niwer
 */
public interface IPayloadProvider {

    /**
     * Returns a map representation of the object's data, suitable for serialization or transmission.
     * 
     * @return A map containing the object's data as key-value pairs.
     */
    default public Map<String, Object> payload() {
        final Map<String, Object> PAYLOAD = new LinkedHashMap<>();

        /* Try to get all fields */
        for(final var FIELD : this.getClass().getDeclaredFields()) {
            final String FIELD_NAME = getSerializedName(FIELD); // Get the name of the field

            /* Try to get the value of the field */
            Object value = null;
            try {
                FIELD.setAccessible(true);
                value = FIELD.get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field: " + FIELD_NAME, e);
            }

            /* Put the field name and value in the payload */
            PAYLOAD.put(FIELD_NAME, convertValue(value));
        }

        return PAYLOAD;
    }

    private static boolean hasSerializedNameAnnotation(Field field) {
        return field.isAnnotationPresent(com.google.gson.annotations.SerializedName.class);
    }

    private static String getSerializedName(Field field) {
        if (hasSerializedNameAnnotation(field)) return field.getAnnotation(com.google.gson.annotations.SerializedName.class).value();
        return field.getName();
    }

    /**
     * Converts a value to a format suitable for inclusion in the payload.
     *
     * @param value The value to convert.
     * @return The converted value.
     */
    private static Object convertValue(Object value) {
        if (value instanceof Enum<?> enumValue) return enumValue.name(); // Convert enum to its name
        if (value instanceof LocalDateTime localDateTime) return localDateTime.toString(); // Convert LocalDateTime to string
        if (value instanceof LocalDate localDate) return localDate.toString(); // Convert LocalDate to string
        if (value instanceof IPayloadProvider payloadProvider) return payloadProvider.payload(); // Convert IPayloadProvider to its payload
        return value; // Return the value as is for other types
    }

    /**
     * Converts a collection of IPayloadProvider objects to a list of their payload representations.
     * 
     * @param <T> The type of the IPayloadProvider objects in the collection.
     * @param obj The collection of IPayloadProvider objects to convert.
     * @return A list of payload representations of the IPayloadProvider objects in the collection.
     */
    public static <T extends IPayloadProvider> Object payloadCollection(Collection<? extends IPayloadProvider> obj) {
        return obj.stream().map(t -> t.payload()).toList();
    }
}
