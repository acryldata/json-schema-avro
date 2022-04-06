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

package com.github.fge.processing;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.avro.CustomAvro2JsonSchemaProcessor;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.JsonNodeReader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.keyword.syntax.SyntaxProcessor;
import com.github.fge.jsonschema.core.messages.JsonSchemaSyntaxMessageBundle;
import com.github.fge.jsonschema.core.processing.ProcessingResult;
import com.github.fge.jsonschema.core.processing.Processor;
import com.github.fge.jsonschema.core.processing.ProcessorChain;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.tree.JsonTree;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.jsonschema.core.tree.SimpleJsonTree;
import com.github.fge.jsonschema.core.util.ValueHolder;
import com.github.fge.jsonschema.library.DraftV4Library;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;
import java.io.IOException;
import java.io.StringReader;

public class ProcessingUtil {

  private static final String LINE = "line";
  private static final String OFFSET = "offset";
  private static final String MESSAGE = "message";
  public static final String INPUT = "input";
  public static final String INVALID_INPUT = "input-invalid";

  public static final JsonNodeFactory NODE_FACTORY = JacksonUtils.nodeFactory();
  public static final JsonNodeReader nodeReader = new JsonNodeReader();
  public static final CustomAvro2JsonSchemaProcessor _customAvro2JsonSchemaProcessor = new CustomAvro2JsonSchemaProcessor();
  public static final MessageBundle bundle = MessageBundles.getBundle(JsonSchemaSyntaxMessageBundle.class);
  public static final SyntaxProcessor syntax = new SyntaxProcessor(bundle,
      DraftV4Library.get().getSyntaxCheckers());
  public static final Processor<ValueHolder<JsonTree>, ValueHolder<SchemaTree>> processor =
      ProcessorChain
          .startWith(_customAvro2JsonSchemaProcessor)
          .chainWith(syntax)
          .getProcessor();

  public static JsonNode buildResult(final String input)
      throws IOException, ProcessingException {
    final ObjectNode ret = NODE_FACTORY.objectNode();

    final boolean invalidSchema = fillWithData(ret, input, nodeReader);

    final JsonNode schemaNode = ret.remove(INPUT);

    if (invalidSchema) {
      return ret;
    }

    final JsonTree tree = new SimpleJsonTree(schemaNode);
    final ValueHolder<JsonTree> holder = ValueHolder.hold(tree);

    final ProcessingReport report = new ListProcessingReport();
    final ProcessingResult<ValueHolder<SchemaTree>> result
        = ProcessingResult.uncheckedResult(processor, report, holder);
    final boolean success = result.isSuccess();
    if (!success) {
      throw new IllegalArgumentException("JSON Schema processing error, please validate the following Avro schema: " + input);
    }

    return success
        ? result.getResult().getValue().getBaseNode()
        : buildReport(result.getReport());
  }

  public static boolean fillWithData(final ObjectNode node, final String raw, final JsonNodeReader nodeReader)
      throws IOException {
    try {
      node.set(INPUT, nodeReader.fromReader(new StringReader(raw)));
      return false;
    } catch (JsonProcessingException e) {
      node.set(INVALID_INPUT, build(e, raw.contains("\r\n")));
      return true;
    }
  }

  public static JsonNode build(final JsonProcessingException e,
      final boolean crlf) {
    final JsonLocation location = e.getLocation();
    final ObjectNode ret = JsonNodeFactory.instance.objectNode();

    final int lineNr = location.getLineNr();
    int offset = (int) location.getCharOffset();
    if (crlf)
      offset = offset - lineNr + 1;
    ret.put(LINE, lineNr);
    ret.put(OFFSET, offset);

    ret.put(MESSAGE, e.getOriginalMessage());
    return ret;
  }

  public static JsonNode buildReport(final ProcessingReport report) {
    final ArrayNode ret = NODE_FACTORY.arrayNode();
    for (final ProcessingMessage message: report) {
      ret.add(message.asJson());
    }
    return ret;
  }
}
