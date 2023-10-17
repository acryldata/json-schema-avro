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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.avro.MutableTree;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jackson.NodeType;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.avro.JsonProperties;
import org.apache.avro.Schema;
import org.apache.avro.util.internal.JacksonUtils;

import static com.github.fge.avro.translators.AvroTranslatorUtil.*;


public class CustomRecordAvroTranslator extends AvroTranslator {
  private static final ObjectMapper OLD_MAPPER = new ObjectMapper();

  private static final AvroTranslator INSTANCE = new CustomRecordAvroTranslator();

  public static AvroTranslator getInstance() {
    return INSTANCE;
  }

  @Override
  public final void translate(final Schema avroSchema, final MutableTree jsonSchema, final ProcessingReport report)
      throws ProcessingException {
    final JsonPointer pwd = jsonSchema.getPointer();
    final String avroName = avroSchema.getName();
    final JsonPointer ptr = JsonPointer.of("definitions", avroName);
    if (!jsonSchema.hasDefinition(avroName)) {
      jsonSchema.setPointer(ptr);
      doTranslate(avroSchema, jsonSchema, report);
      jsonSchema.setPointer(pwd);
    }
    jsonSchema.getCurrentNode().put("$ref", createRef(ptr));
  }

  private static String createRef(final JsonPointer pointer) {
    try {
      return new URI(null, null, pointer.toString()).toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  protected void doTranslate(final Schema avroSchema, final MutableTree jsonSchema, final ProcessingReport report)
      throws ProcessingException {
    final List<Schema.Field> fields = avroSchema.getFields();

    if (fields.isEmpty()) {
      final ArrayNode node = FACTORY.arrayNode();
      node.add(FACTORY.objectNode());
      return;
    }

    final JsonPointer pwd = jsonSchema.getPointer();
    final ObjectNode root = jsonSchema.getCurrentNode();

    if (avroSchema.getDoc() != null) {
      root.put("description", avroSchema.getDoc());
    }

    jsonSchema.setType(NodeType.OBJECT);

    final ArrayNode required = FACTORY.arrayNode();

    root.put("additionalProperties", false);

    final ObjectNode properties = FACTORY.objectNode();
    root.set("properties", properties);

    String fieldName;
    Schema fieldSchema;
    Schema.Type fieldType;
    AvroTranslator translator;
    JsonPointer ptr;
    ObjectNode propertyNode;

    /*
     * FIXME: "default" and readers'/writers' schema? Here, even with a
     *  default value, the record field is marked as required.
     *  -- Partial fix added - RH
     */
    for (final Schema.Field field : fields) {
      fieldName = field.name();
      fieldSchema = field.schema();
      fieldType = fieldSchema.getType();
      translator = selectTranslator(fieldType);
      ptr = JsonPointer.of("properties", fieldName);
      propertyNode = FACTORY.objectNode();
      properties.set(fieldName, propertyNode);
      injectDefault(propertyNode, field);
      if (!field.hasDefaultValue() || !(propertyNode.get("default") instanceof NullNode)) {
        required.add(fieldName);
        if (root.get("required") == null) {
          root.set("required", required);
        }
      }
      jsonSchema.setPointer(pwd.append(ptr));
      if (field.doc() != null) {
        jsonSchema.getCurrentNode().put("description", field.doc());
      }
      translator.translate(fieldSchema, jsonSchema, report);
      jsonSchema.setPointer(pwd);
    }
  }

  private static void injectDefault(final ObjectNode propertyNode, final Schema.Field field) {
    final JsonNode value = JacksonUtils.toJsonNode(field.defaultVal());

    if (value == null) {
      return;
    }

    /*
     * Write the value to a string using a 1.8 writer, and read it from that
     * string using a 2.1 reader... Did you say "hack"?
     */
    try {
      final String s = OLD_MAPPER.writeValueAsString(value);
      propertyNode.set("default", JsonLoader.fromString(s));
    } catch (IOException ignored) {
      // cannot happen
    }
  }
}
