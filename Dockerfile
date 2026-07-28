FROM eclipse-temurin:17-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system todayeng && useradd --system --gid todayeng todayeng

WORKDIR /app

COPY build/libs/todayeng.jar app.jar

RUN chown todayeng:todayeng app.jar
USER todayeng:todayeng

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]