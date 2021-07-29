/*
 * Copyright 2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appnaut.core.jackson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.appnaut.core.util.ISO8601Util;
import java.io.IOException;
import java.time.LocalDateTime;
import javax.inject.Singleton;

/**
 * The <b>LocalDateTimeSerializer</b> class provides a custom JSON deserializer that deserializes a
 * string with the ISO 8601 format as LocalDateTime value.
 *
 * @author Marcus Portmann
 */
@Singleton
@SuppressWarnings("unused")
public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

  @Override
  public LocalDateTime deserialize(JsonParser jsonParser,
      DeserializationContext deserializationContext)
      throws IOException {
    try {
      return ISO8601Util.toLocalDateTime(jsonParser.getValueAsString());
    } catch (Throwable e) {
      throw new JsonParseException(jsonParser,
          "Failed to parse the ISO 8601 string (" + jsonParser.getValueAsString() + ")", e);
    }
  }
}
