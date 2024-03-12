/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
