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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.avro.MutableTree;
import com.github.fge.jackson.NodeType;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import org.apache.avro.Schema;

import static com.github.fge.avro.translators.AvroTranslatorUtil.*;


/**
 * Adds missing description for top level array and modifies selector logic for array sub items to customized util method
 */
public class CustomArrayAvroTranslator extends AvroTranslator {
  private static final AvroTranslator INSTANCE = new CustomArrayAvroTranslator();

  public static AvroTranslator getInstance() {
    return INSTANCE;
  }

  @Override
  public void translate(final Schema avroSchema, final MutableTree jsonSchema, final ProcessingReport report)
      throws ProcessingException {
    jsonSchema.setType(NodeType.ARRAY);

    final JsonPointer pwd = jsonSchema.getPointer();
    final ObjectNode subSchema = FACTORY.objectNode();
    final Schema valuesSchema = avroSchema.getElementType();

    if (avroSchema.getDoc() != null) {
      jsonSchema.getCurrentNode().put("description", avroSchema.getDoc());
    }

    jsonSchema.getCurrentNode().set("items", subSchema);

    jsonSchema.setPointer(pwd.append("items"));
    selectTranslator(valuesSchema.getType()).translate(valuesSchema, jsonSchema, report);
    jsonSchema.setPointer(pwd);
  }
}
