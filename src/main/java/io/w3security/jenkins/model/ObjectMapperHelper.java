package io.w3security.jenkins.model;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ObjectMapperHelper {

  private static final JsonFactory JSON_FACTORY;

  static {
    JSON_FACTORY = new MappingJsonFactory();
  }

  private ObjectMapperHelper() {
  }

  public static W3SecurityTestResult unmarshallTestResult(String content) throws IOException {
    if (content == null || content.isEmpty()) {
      return null;
    }

    try (JsonParser parser = JSON_FACTORY.createParser(content)) {
      JsonToken token = parser.nextToken();
      if (token == null) {
        return null;
      }

      if (token == JsonToken.START_ARRAY) {
        List<W3SecurityTestResult> testStatuses = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
          testStatuses.add(readTestResult(parser));
        }
        return aggregateTestResults(testStatuses);
      } else if (token == JsonToken.START_OBJECT) {
        return readTestResult(parser);
      } else {
        return null;
      }
    }
  }

  private static W3SecurityTestResult readTestResult(JsonParser parser) throws IOException {
    W3SecurityTestResult w3securityTestResult = new W3SecurityTestResult();

    while (parser.nextToken() != JsonToken.END_OBJECT) {
      String fieldName = parser.getCurrentName();

      if ("ok".equals(fieldName)) {
        parser.nextToken();
        w3securityTestResult.ok = parser.getBooleanValue();
      } else if ("error".equals(fieldName)) {
        parser.nextToken();
        w3securityTestResult.error = parser.getText();
      } else if ("uniqueCount".equals(fieldName)) {
        parser.nextToken();
        w3securityTestResult.uniqueCount = parser.getIntValue();
      } else if ("dependencyCount".equals(fieldName)) {
        parser.nextToken();
        w3securityTestResult.dependencyCount = parser.getIntValue();
      } else {
        parser.skipChildren();
      }
    }

    return w3securityTestResult;
  }

  private static W3SecurityTestResult aggregateTestResults(List<W3SecurityTestResult> testResults) {
    W3SecurityTestResult aggregatedTestResult = new W3SecurityTestResult();

    testResults.forEach(entity -> {
      aggregatedTestResult.ok = aggregatedTestResult.ok && entity.ok;
      aggregatedTestResult.error = (entity.error != null && !entity.error.isEmpty())
          ? String.join(". ", aggregatedTestResult.error, entity.error)
          : aggregatedTestResult.error;
      aggregatedTestResult.dependencyCount += entity.dependencyCount;
      aggregatedTestResult.uniqueCount += entity.uniqueCount;
    });

    return aggregatedTestResult;
  }
}
