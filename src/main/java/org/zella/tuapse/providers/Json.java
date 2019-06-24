package org.zella.tuapse.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class Json {
    public static final ObjectMapper mapper = new ObjectMapper();

    static {
        //jhava.util.Optional etc
        mapper.registerModule(new Jdk8Module());
    }
}
