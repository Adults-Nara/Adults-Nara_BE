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
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/batch-analytics",
          "awslogs-region": "${AWS_REGION}",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}