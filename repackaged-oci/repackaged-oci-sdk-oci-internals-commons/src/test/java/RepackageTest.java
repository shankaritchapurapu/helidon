/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RepackageTest {

    @Test
    public void test() throws IOException {
        SimpleFilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter("fieldFilter", com.oracle.bmc.http.internal.ExplicitlySetFilter.serializeAll())
                .setFailOnUnknownId(false);
        ObjectMapper om = new ObjectMapper();
        om.setFilterProvider(filterProvider);

        Car car = om.readValue("""
                                       {
                                         "color" : "blue",
                                         "type" : "sedan"
                                       }
                                       """, Car.class);

        assertThat(car.getType(), is("sedan"));
    }

    public static class Car {

        private String color;
        private String type;

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

}
