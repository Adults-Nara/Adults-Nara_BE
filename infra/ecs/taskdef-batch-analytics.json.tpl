{
  "family": "asn-batch-analytics",
  "taskRoleArn": "${TASK_ROLE_ARN}",
  "executionRoleArn": "${AWS_EXE_ROLE_ARN}",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [
    {
      "name": "log_router",
      "image": "grafana/fluent-bit-plugin-loki:latest",
      "essential": true,
      "firelensConfiguration": {
        "type": "fluentbit",
        "options": {
          "enable-ecs-log-metadata": "true"
        }
      },
      "memoryReservation": 50,
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/log_router",
          "awslogs-region": "${AWS_REGION}",
          "awslogs-stream-prefix": "ecs"
        }
      }
    },
    {
      "name": "batch-analytics",
      "image": "${BATCH_ANALYTICS_IMAGE_URI}",
      "portMappings": [{ "containerPort": 8082, "protocol": "tcp" }],
      "environment": [
        { "name": "SPRING_PROFILES_ACTIVE", "value": "prod" },
        { "name": "BATCH_ANALYTICS_PORT",   "value": "8082" },
        { "name": "DB_URL",                 "value": "${DB_URL}" },
        { "name": "DB_USER",                "value": "${DB_USER}" },
        { "name": "DB_PASSWORD",            "value": "${DB_PASSWORD}" }
      ],
      "logConfiguration": {
        "logDriver": "awsfirelens",
        "options": {
          "Name": "loki",
          "Host": "15.165.11.129",
          "Port": "3100",
          "Labels": "job=ecs-fargate, app=batch-analytics, env=prod"
        }
      }
    }
  ]
}