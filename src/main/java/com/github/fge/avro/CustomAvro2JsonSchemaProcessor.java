/*
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

package com.github.fge.avro;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.processing.RawProcessor;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.tree.CanonicalSchemaTree;
import com.github.fge.jsonschema.core.tree.JsonTree;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import java.io.IOException;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;

import static com.github.fge.avro.translators.AvroTranslatorUtil.*;


/**
 * Customized class to allow extension and modifications to be separate from original class. Original class is marked as final
 * which makes it impossible to extend externally and makes it less flexible in general. The naming change is also applied to other
 * customized classes for the same reasons, especially for some of the translator subtypes which needed to be changed to not extend
 * subtypes. Separation is also important since all of the code in this repo is under LGPL licensing.
 * Changes from original:
 *
 * Translator selection logic has been modified to be able to modify the translation process. Modification to specific translators and
 * logic outlined in each translator and the selector util.
 *
 * Sanitizes class names to shortname in translation, this results in cleaner output types which work with code generation.
 */
public class CustomAvro2JsonSchemaProcessor extends RawProcessor<JsonTree, SchemaTree> {
  public CustomAvro2JsonSchemaProcessor()
  {
    super("avroSchema", "schema");
  }

  @Override
  public SchemaTree rawProcess(final ProcessingReport report,
      final JsonTree input)
      throws ProcessingException
  {
    final JsonNode node = input.getBaseNode();

    final Schema avroSchema;
    try {
      final String s = node.toString();
      avroSchema = new Schema.Parser().parse(s);
    } catch (AvroRuntimeException e) {
      /*
       * There is a SchemaParseException, but it does not cover all cases.
       *
       * This schema, for instance, throws a AvroRuntimeException:
       *
       * { "type": [ "null", "null" ] }
       *
       */
      throw new IllegalAvroSchemaException(e);
    }

    final MutableTree tree = new MutableTree();
    final Schema.Type avroType = avroSchema.getType();
    selectTranslator(avroType).translate(avroSchema, tree, report);

    JsonNode outputTree;
    String rawJson = JacksonUtils.prettyPrint(tree.getBaseNode());
    // Remove fully qualified name with avro type prepended
    String sanitizedJson = rawJson.replaceAll("[a-z]+:([a-z0-9]+\\.)+", "");
    try {
      outputTree = JsonLoader.fromString(sanitizedJson);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return new CanonicalSchemaTree(outputTree);
  }
}
