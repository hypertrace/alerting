package org.hypertrace.alert.engine.metric.anomaly.task.manager.rulereader;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.Attribute;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.Filter;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.LeafFilter;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.LhsExpression;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.MetricAggregationFunction;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.MetricAnomalyEventCondition;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.MetricSelection;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.NotificationRule;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.RhsExpression;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.Severity;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.StaticThresholdCondition;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.StaticThresholdOperator;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.ValueOperator;
import org.hypertrace.alert.engine.eventcondition.config.service.v1.ViolationCondition;
import org.hypertrace.alert.engine.metric.anomaly.datamodel.AlertTask;
import org.hypertrace.alert.engine.metric.anomaly.task.manager.job.AlertTaskConverter;
import org.hypertrace.alert.engine.metric.anomaly.task.manager.rulesource.FSRuleSource;
import org.hypertrace.alert.engine.metric.anomaly.task.manager.rulesource.RuleSource;
import org.hypertrace.core.documentstore.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AlertTaskTest {

  @Test
  void testAlertTask() throws Exception {
    URL url = Thread.currentThread().getContextClassLoader().getResource("rules.json");

    File file = Paths.get(url.toURI()).toFile();
    String absolutePath = file.getAbsolutePath();

    Config fsConfig = ConfigFactory.parseMap(Map.of("path", absolutePath));
    RuleSource ruleSource = new FSRuleSource(fsConfig);
    List<Document> documents = ruleSource.getAllEventConditions("MetricAnomalyEventCondition");
    Assertions.assertTrue(documents.size() > 0);

    AlertTask alertTask = AlertTaskConverter.toAlertTask(documents.get(0), 1);
    Assertions.assertEquals("MetricAnomalyEventCondition", alertTask.getEventConditionType());

    MetricAnomalyEventCondition actual =
        MetricAnomalyEventCondition.parseFrom(alertTask.getEventConditionValue());
    Assertions.assertEquals(prepareMetricAnomalyEventCondition(), actual);
  }

  private NotificationRule prepareNotificationRule() {
    NotificationRule.Builder nBuilder = NotificationRule.newBuilder();
    nBuilder.setId("notification_rule_1");
    nBuilder.setRuleName("high_avg_latency");
    nBuilder.setDescription("Alert for high avg latency of payment service");
    nBuilder.setEventConditionId("event_condition_1");
    nBuilder.setEventConditionType("MetricAnomalyEventCondition");
    return nBuilder.build();
  }

  private MetricAnomalyEventCondition prepareMetricAnomalyEventCondition() {
    MetricAnomalyEventCondition.Builder builder = MetricAnomalyEventCondition.newBuilder();
    builder.setMetricSelection(
        MetricSelection.newBuilder()
            .setMetricAttribute(
                Attribute.newBuilder().setKey("duration").setScope("SERVICE").build())
            .setMetricAggregationFunction(
                MetricAggregationFunction.METRIC_AGGREGATION_FUNCTION_TYPE_AVG)
            .setMetricAggregationInterval("PT15S")
            .setFilter(
                Filter.newBuilder()
                    .setLeafFilter(
                        LeafFilter.newBuilder()
                            .setValueOperator(ValueOperator.VALUE_OPERATOR_EQ)
                            .setLhsExpression(
                                LhsExpression.newBuilder()
                                    .setAttribute(
                                        Attribute.newBuilder()
                                            .setKey("id")
                                            .setScope("SERVICE")
                                            .build())
                                    .build())
                            .setRhsExpression(
                                RhsExpression.newBuilder().setStringValue("1234").build())
                            .build())
                    .build())
            .build());

    builder.addViolationCondition(
        ViolationCondition.newBuilder()
            .setStaticThresholdCondition(
                StaticThresholdCondition.newBuilder()
                    .setOperator(StaticThresholdOperator.STATIC_THRESHOLD_OPERATOR_GT)
                    .setMinimumViolationDuration("PT5M")
                    .setValue(15)
                    .setSeverity(Severity.SEVERITY_CRITICAL)
                    .build())
            .build());

    return builder.build();
  }
}
