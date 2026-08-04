FROM alpine:3.23 AS builder
RUN apk add --no-cache g++ cmake make openssl-dev git
WORKDIR /app
COPY CMakeLists.txt ./
COPY src/ ./src/
COPY tests/ ./tests/
RUN cmake -S . -B build -DCMAKE_BUILD_TYPE=Release \
    && cmake --build build --parallel \
    && ctest --test-dir build --output-on-failure

FROM alpine:3.23 AS runner
RUN apk add --no-cache libstdc++ libgcc openssl \
    && addgroup -S -g 10001 appgroup \
    && adduser -S -u 10001 -G appgroup appuser
WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /app/build/streaming_engine ./streaming_engine
USER appuser
EXPOSE 8081
CMD ["./streaming_engine"]
