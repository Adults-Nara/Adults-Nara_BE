{
  "family": "asn-core-api",
  "taskRoleArn": "${TASK_ROLE_ARN}",
  "executionRoleArn": "${AWS_EXE_ROLE_ARN}",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [
    {
      "name": "core-api",
      "image": "${CORE_API_IMAGE_URI}",
      "portMappings": [{ "containerPort": 8080, "protocol": "tcp" }],
      "environment": [
        { "name": "SPRING_PROFILES_ACTIVE",               "value": "prod" },
        { "name": "CORE_API_PORT",                         "value": "8080" },
        { "name": "AWS_REGION",                            "value": "${AWS_REGION}" },
        { "name": "DB_URL",                                "value": "${DB_URL}" },
        { "name": "DB_USER",                               "value": "${DB_USER}" },
        { "name": "DB_PASSWORD",                           "value": "${DB_PASSWORD}" },
        { "name": "REDIS_HOST",                            "value": "${REDIS_HOST}" },
        { "name": "REDIS_PORT",                            "value": "${REDIS_PORT}" },
        { "name": "REDIS_PASSWORD",                        "value": "${REDIS_PASSWORD}" },
        { "name": "KAFKA_BOOTSTRAP_SERVERS",               "value": "${KAFKA_BOOTSTRAP_SERVERS}" },
        { "name": "KAFKA_ACKS",                            "value": "${KAFKA_ACKS}" },
        { "name": "KAFKA_TOPIC_MEDIA_TRANSCODE",           "value": "${KAFKA_TOPIC_MEDIA_TRANSCODE}" },
        { "name": "ES_URL",                                "value": "${ES_URL}" },
        { "name": "ES_USERNAME",                           "value": "${ES_USERNAME}" },
        { "name": "ES_PASSWORD",                           "value": "${ES_PASSWORD}" },
        { "name": "S3_BUCKET",                             "value": "${S3_BUCKET}" },
        { "name": "AWS_CLOUDFRONT_DOMAIN",                 "value": "${AWS_CLOUDFRONT_DOMAIN}" },
        { "name": "AWS_CLOUDFRONT_KEY_PAIR_ID",            "value": "${AWS_CLOUDFRONT_KEY_PAIR_ID}" },
        { "name": "AWS_CLOUDFRONT_PRIVATE_KEY_PEM_B64",    "value": "${AWS_CLOUDFRONT_PRIVATE_KEY_PEM_B64}" },
        { "name": "JWT_SECRET",                            "value": "${JWT_SECRET}" },
        { "name": "KAKAO_CLIENT_ID",                       "value": "${KAKAO_CLIENT_ID}" },
        { "name": "KAKAO_CLIENT_SECRET",                   "value": "${KAKAO_CLIENT_SECRET}" },
        { "name": "OAUTH2_REDIRECT_URI",                   "value": "${OAUTH2_REDIRECT_URI}" }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/core-api",
          "awslogs-region": "${AWS_REGION}",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}