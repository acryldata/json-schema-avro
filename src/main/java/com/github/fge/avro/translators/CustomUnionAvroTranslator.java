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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.fge.avro.MutableTree;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.avro.Schema;

import static com.github.fge.avro.translators.AvroTranslatorUtil.*;


public class CustomUnionAvroTranslator extends AvroTranslator {
  private static final AvroTranslator INSTANCE = new CustomUnionAvroTranslator();

  public static AvroTranslator getInstance() {
    return INSTANCE;
  }

  @Override
  public void translate(final Schema avroSchema, final MutableTree jsonSchema, final ProcessingReport report)
      throws ProcessingException {
    final JsonPointer pwd = jsonSchema.getPointer();
    final List<Schema> types = avroSchema.getTypes()
        .stream()
        .filter(schemaElement -> !Schema.Type.NULL.equals(schemaElement.getType()))
        .collect(Collectors.toList());
    final int size = types.size();
    if (size == 1) {
      selectTranslator(types.get(0).getType()).translate(types.get(0), jsonSchema, report);
      return;
    } else if (size == 0) {
      return;
    }
    final ArrayNode schemas = FACTORY.arrayNode();
    jsonSchema.getCurrentNode().set("oneOf", schemas);

    Schema schema;
    Schema.Type type;
    AvroTranslator translator;
    JsonPointer ptr;

    for (int index = 0; index < size; index++) {
      schema = types.get(index);
      type = schema.getType();
      translator = selectTranslator(type);
      ptr = JsonPointer.of("oneOf", index);
      schemas.add(FACTORY.objectNode());
      jsonSchema.setPointer(pwd.append(ptr));
      translator.translate(schema, jsonSchema, report);
      jsonSchema.setPointer(pwd);
    }
  }
}
