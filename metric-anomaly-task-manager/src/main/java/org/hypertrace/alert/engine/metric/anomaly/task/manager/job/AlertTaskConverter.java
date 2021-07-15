package org.hypertrace.alert.engine.metric.anomaly.task.manager.job;

import static org.hypertrace.alert.engine.metric.anomaly.task.manager.job.AlertTaskJobConstants.METRIC_ANOMALY_EVENT_CONDITION;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;
import com.typesafe.config.Config;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.MetricAnomalyEventCondition;
import org.hypertrace.alert.engine.metric.anomaly.datamodel.AlertTask;
import org.hypertrace.core.documentstore.Document;

public class AlertTaskConverter {
  private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Parser JSON_PARSER = JsonFormat.parser().ignoringUnknownFields();

  static final String EVENT_CONDITION_ID = "eventConditionId";
  static final String EVENT_CONDITION_TYPE = "eventConditionType";
  static final String EVENT_CONDITION = "eventCondition";
  static final String DELAY_IN_MINUTES_CONFIG = "delayInMinutes";
  static final String EXECUTION_WINDOW_IN_MINUTES_CONFIG = "executionWindowInMinutes";
  static final String TENANT_ID_CONFIG = "tenant_id";

  static final String DEFAULT_TENANT_ID = "__default";
  static final int DEFAULT_DELAY_IN_MINUTES = 1;
  static final int DEFAULT_EXECUTION_WINDOW_IN_MINUTES = 1;

  Config jobConfig;

  public AlertTaskConverter(Config jobConfig) {
    this.jobConfig = jobConfig;
  }

  public AlertTask toAlertTask(Document document) throws IOException {
    JsonNode rule = OBJECT_MAPPER.readTree(document.toJson());

    AlertTask.Builder builder = AlertTask.newBuilder();

    // set tenant
    builder.setTenantId(getTenantId());

    // set execution window
    Instant current =
        adjustDelay(roundHalfDown(Instant.now(), ChronoUnit.MINUTES), getDelayInMinutes());
    builder.setCurrentExecutionTime(current.toEpochMilli());
    builder.setLastExecutionTime(
        adjustDelay(current, getExecutionWindowInMinutes()).toEpochMilli());

    // set event condition
    String conditionType = rule.get(EVENT_CONDITION_TYPE).textValue();
    ByteBuffer eventConditionValueAsBytes =
        getEventConditionBytes(conditionType, rule.get(EVENT_CONDITION));

    builder.setEventConditionId(rule.get(EVENT_CONDITION_ID).textValue());
    builder.setEventConditionType(rule.get(EVENT_CONDITION_TYPE).textValue());
    builder.setEventConditionValue(eventConditionValueAsBytes);

    return builder.build();
  }

  private static ByteBuffer getEventConditionBytes(String conditionType, JsonNode jsonNode)
      throws JsonProcessingException, InvalidProtocolBufferException {
    switch (conditionType) {
      case METRIC_ANOMALY_EVENT_CONDITION:
        MetricAnomalyEventCondition.Builder builder = MetricAnomalyEventCondition.newBuilder();
        JSON_PARSER.merge(OBJECT_MAPPER.writeValueAsString(jsonNode), builder);
        MetricAnomalyEventCondition metricAnomalyEventCondition = builder.build();
        return ByteBuffer.wrap(metricAnomalyEventCondition.toByteArray());
      default:
        break;
    }
    throw new RuntimeException(String.format("Un-supported condition type:%s", conditionType));
  }

  private static Instant roundHalfDown(Instant instant, TemporalUnit unit) {
    return instant.minus(unit.getDuration().dividedBy(2)).truncatedTo(unit);
  }

  private static Instant adjustDelay(Instant instant, int delayInMinutes) {
    return instant.minus(Duration.of(delayInMinutes, ChronoUnit.MINUTES));
  }

  private int getExecutionWindowInMinutes() {
    return jobConfig.hasPath(EXECUTION_WINDOW_IN_MINUTES_CONFIG)
        ? jobConfig.getInt(EXECUTION_WINDOW_IN_MINUTES_CONFIG)
        : DEFAULT_EXECUTION_WINDOW_IN_MINUTES;
  }

  private int getDelayInMinutes() {
    return jobConfig.hasPath(DELAY_IN_MINUTES_CONFIG)
        ? jobConfig.getInt(DELAY_IN_MINUTES_CONFIG)
        : DEFAULT_DELAY_IN_MINUTES;
  }

  private String getTenantId() {
    return jobConfig.hasPath(TENANT_ID_CONFIG)
        ? jobConfig.getString(TENANT_ID_CONFIG)
        : DEFAULT_TENANT_ID;
  }
}