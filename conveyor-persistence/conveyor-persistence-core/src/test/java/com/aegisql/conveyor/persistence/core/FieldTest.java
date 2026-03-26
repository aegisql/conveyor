package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.cart.ShoppingCart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldTest {

    @Test
    void supportsDefaultAndCustomAccessors() {
        ShoppingCart<Integer, String, String> cart = new ShoppingCart<>(1, "value", "LABEL");
        cart.addProperty("AUDIT", "audit-value");

        Field<String> defaultField = new Field<>(String.class, "AUDIT");
        Field<Integer> customField = new Field<>(Integer.class, "VALUE_LENGTH", c -> ((String) c.getValue()).length());

        assertEquals(String.class, defaultField.getFieldClass());
        assertEquals("AUDIT", defaultField.getName());
        assertNotNull(defaultField.getAccessor());
        assertEquals("audit-value", defaultField.getAccessor().apply(cart));

        assertEquals(Integer.valueOf(5), customField.getAccessor().apply(cart));
    }

    @Test
    void rejectsNullConstructorArguments() {
        assertThrows(NullPointerException.class, () -> new Field<>(null, "AUDIT"));
        assertThrows(NullPointerException.class, () -> new Field<>(String.class, null));
        assertThrows(NullPointerException.class, () -> new Field<>(String.class, "AUDIT", null));
    }
}
