package org.hypertrace.alert.engine.metric.anomaly.detector;

import java.util.Iterator;
import java.util.Map;
import org.hypertrace.core.query.service.api.QueryRequest;
import org.hypertrace.core.query.service.api.ResultSetChunk;
import org.hypertrace.core.query.service.client.QueryServiceClient;

public class MetricDataFetcher {

  private final int qsRequestTimeout;
  private final QueryServiceClient queryServiceClient;

  public MetricDataFetcher(int qsRequestTimeout, QueryServiceClient queryServiceClient) {
    this.qsRequestTimeout = qsRequestTimeout;
    this.queryServiceClient = queryServiceClient;
  }

  public Iterator<ResultSetChunk> executeQuery(
      Map<String, String> requestHeaders, QueryRequest aggQueryRequest) {
    return queryServiceClient.executeQuery(aggQueryRequest, requestHeaders, qsRequestTimeout);
  }
}
