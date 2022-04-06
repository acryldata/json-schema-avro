/*
 * MODIFIED: Support custom use cases for generating JSON Schema
 * Copyright 2022 Acryl Data, Inc.
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of both licenses is available under the src/resources/ directory of
 * this project (under the names LGPL-3.0.txt and ASL-2.0.txt respectively).
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.avro.translators;

import org.apache.avro.Schema;


public class AvroTranslatorUtil {

  private AvroTranslatorUtil() {

  }

  public static AvroTranslator selectTranslator(Schema.Type avroType) {
    AvroTranslator translator;
    switch (avroType) {
      case RECORD:
        translator = CustomRecordAvroTranslator.getInstance();
        break;
      case UNION:
        translator = CustomUnionAvroTranslator.getInstance();
        break;
      case ARRAY:
        translator = CustomArrayAvroTranslator.getInstance();
        break;
      case MAP:
        translator = CustomMapAvroTranslator.getInstance();
        break;
      case LONG:
        translator = CustomLongAvroTranslator.getInstance();
        break;
      case STRING:
        translator = CustomSimpleTranslator.STRING_INSTANCE;
        break;
      case BOOLEAN:
        translator = CustomSimpleTranslator.BOOLEAN_INSTANCE;
        break;
      case NULL:
        translator = CustomSimpleTranslator.NULL_INSTANCE;
        break;
      case FLOAT:
      case DOUBLE:
        translator = CustomSimpleTranslator.NUMBER_INSTANCE;
        break;
      case FIXED:
        translator = CustomFixedTranslator.getInstance();
        break;
      case INT:
        translator = CustomIntTranslator.getInstance();
        break;
      case BYTES:
        translator = CustomByteTranslator.getInstance();
        break;
      default:
        translator = AvroTranslators.getTranslator(avroType);
    }
    return translator;
  }
}
