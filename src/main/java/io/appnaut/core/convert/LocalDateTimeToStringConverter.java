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

package io.appnaut.core.convert;

import io.appnaut.core.util.ISO8601Util;
import io.micronaut.context.annotation.Context;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.inject.Singleton;

/**
 * The <b>LocalDateTimeToStringConverter</b> class implements the converter that converts a
 * <b>LocalDateTime</b> type into a <b>String</b> type.
 *
 * @author Marcus Portmann
 */
@Singleton
public class LocalDateTimeToStringConverter implements TypeConverter<LocalDateTime, String> {

  @Override
  public Optional<String> convert(LocalDateTime object, Class<String> targetType) {
    return TypeConverter.super.convert(object, targetType);
  }

  @Override
  public Optional<String> convert(LocalDateTime object, Class<String> targetType,
      ConversionContext context) {
    return Optional.of(ISO8601Util.fromLocalDateTimeAsUTC(object));
  }
}
