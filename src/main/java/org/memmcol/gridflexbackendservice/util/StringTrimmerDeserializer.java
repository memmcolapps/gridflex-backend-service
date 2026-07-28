package org.memmcol.gridflexbackendservice.util;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class StringTrimmerDeserializer extends StdDeserializer<String> {

    public StringTrimmerDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        String value = p.getValueAsString();

        if (value == null) {
            return null;
        }

        value = value.strip();

        return value.isEmpty() ? null : value;
    }
}


//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.databind.DeserializationContext;
//import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
//
//import java.io.IOException;
//
//public class StringTrimmerDeserializer extends StdDeserializer<String> {
//
//    public StringTrimmerDeserializer() {
//        super(String.class);
//    }
//
//    @Override
//    public String deserialize(JsonParser p, DeserializationContext ctxt)
//            throws IOException {
//
//        String value = p.getValueAsString();
//
//        return value == null ? null : value.strip();
//    }
//}