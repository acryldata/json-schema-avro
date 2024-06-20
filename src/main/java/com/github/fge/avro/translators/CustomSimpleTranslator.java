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

import com.github.fge.avro.MutableTree;
import com.github.fge.jackson.NodeType;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import org.apache.avro.Schema;


/**
 * Adds missing description.
 */
public class CustomSimpleTranslator extends AvroTranslator {
  private final NodeType type;

  public static final AvroTranslator STRING_INSTANCE = new CustomSimpleTranslator(NodeType.STRING);
  public static final AvroTranslator NULL_INSTANCE = new CustomSimpleTranslator(NodeType.NULL);
  public static final AvroTranslator NUMBER_INSTANCE = new CustomSimpleTranslator(NodeType.NUMBER);
  public static final AvroTranslator BOOLEAN_INSTANCE = new CustomSimpleTranslator(NodeType.BOOLEAN);

  public CustomSimpleTranslator(final NodeType type) {
    this.type = type;
  }

  @Override
  public void translate(final Schema avroSchema, final MutableTree jsonSchema, final ProcessingReport report)
      throws ProcessingException {
    if (avroSchema.getDoc() != null) {
      jsonSchema.getCurrentNode().put("description", avroSchema.getDoc());
    }
    jsonSchema.setType(type);
  }
}
