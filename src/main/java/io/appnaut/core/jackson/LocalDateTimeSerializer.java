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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.appnaut.core.util.ISO8601Util;
import java.io.IOException;
import java.time.LocalDateTime;
import javax.inject.Singleton;

/**
 * The <b>LocalDateTimeSerializer</b> class provides a custom JSON serializer that serializes a
 * LocalDateTime value as a string using the ISO 8601 UTC format.
 *
 * @author Marcus Portmann
 */
@Singleton
@SuppressWarnings("unused")
public class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

  @Override
  public void serialize(LocalDateTime value, JsonGenerator generator,
      SerializerProvider serializers)
      throws IOException {
    generator.writeString(ISO8601Util.fromLocalDateTimeAsUTC(value));
  }
}
