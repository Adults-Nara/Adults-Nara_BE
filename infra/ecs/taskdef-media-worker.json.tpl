{
  "family": "asn-media-worker",
  "taskRoleArn": "${TASK_ROLE_ARN}",
  "executionRoleArn": "${AWS_EXE_ROLE_ARN}",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [
    {
      "name": "media-worker",
      "image": "${MEDIA_WORKER_IMAGE_URI}",
      "portMappings": [{ "containerPort": 8081, "protocol": "tcp" }],
      "environment": [
        { "name": "SPRING_PROFILES_ACTIVE",         "value": "prod" },
        { "name": "MEDIA_WORKER_PORT",               "value": "8081" },
        { "name": "AWS_REGION",                      "value": "${AWS_REGION}" },
        { "name": "DB_URL",                          "value": "${DB_URL}" },
        { "name": "DB_USER",                         "value": "${DB_USER}" },
        { "name": "DB_PASSWORD",                     "value": "${DB_PASSWORD}" },
        { "name": "REDIS_HOST",                      "value": "${REDIS_HOST}" },
        { "name": "REDIS_PORT",                      "value": "${REDIS_PORT}" },
        { "name": "REDIS_PASSWORD",                  "value": "${REDIS_PASSWORD}" },
        { "name": "KAFKA_BOOTSTRAP_SERVERS",         "value": "${KAFKA_BOOTSTRAP_SERVERS}" },
        { "name": "KAFKA_CONSUMER_GROUP_ID",         "value": "${KAFKA_CONSUMER_GROUP_ID}" },
        { "name": "KAFKA_AUTO_OFFSET_RESET",         "value": "${KAFKA_AUTO_OFFSET_RESET}" },
        { "name": "KAFKA_TOPIC_MEDIA_TRANSCODE",     "value": "${KAFKA_TOPIC_MEDIA_TRANSCODE}" },
        { "name": "S3_BUCKET",                       "value": "${S3_BUCKET}" }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/asn-media-worker",
          "awslogs-region": "${AWS_REGION}",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
