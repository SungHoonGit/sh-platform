#!/bin/bash
JWT_PRIV=$(cat /home/ubuntu/sh-platform/keys/jwt_private.pem)
JWT_PUB=$(cat /home/ubuntu/sh-platform/keys/jwt_public.pem)
exec java -jar /home/ubuntu/sh-platform/sh-platform-auth/build/libs/sh-platform-auth-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --jwt.private-key="$JWT_PRIV" \
  --jwt.public-key="$JWT_PUB"
