plugins {
    java
    application
    jacoco
    id("org.hypertrace.jacoco-report-plugin")
}

application {
    mainClass.set("org.hypertrace.alerting.metric.anomaly.detector.MetricAnomalyDetector")
}

dependencies {
    implementation("org.hypertrace.core.serviceframework:platform-service-framework:0.1.23")
    implementation("org.hypertrace.core.serviceframework:platform-metrics:0.1.23")

    // Required for the GRPC clients.
    runtimeOnly("io.grpc:grpc-netty:1.36.1")

    implementation("com.typesafe:config:1.4.1")
    implementation("io.confluent:kafka-avro-serializer:6.0.1")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")

    // Logging
    implementation("org.slf4j:slf4j-api:1.7.30")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.14.1")
}
