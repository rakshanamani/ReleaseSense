# ============================================================================
# Stage 1: Build the Java WAR with Maven and OpenJDK 17
# ============================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build WAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================================
# Stage 2: Runtime image on Apache Tomcat 11
# ============================================================================
FROM tomcat:11.0-jdk17-temurin

LABEL maintainer="ReleaseSense DevOps Team"
LABEL description="ReleaseSense Jakarta EE 10 Application running on Apache Tomcat 11"

# Remove default Tomcat webapps for a clean root deployment
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy packaged WAR to ROOT.war so it serves directly from http://localhost:8080/
COPY --from=builder /build/target/releasesense.war /usr/local/tomcat/webapps/ROOT.war

# Default environment variables (can be overridden by docker-compose or kubernetes)
ENV PORT=8080
ENV ML_SERVICE_URL=http://ml-service:5001/predict
ENV ORACLE_DB_URL=""
ENV ORACLE_DB_USER=""
ENV ORACLE_DB_PASSWORD=""

EXPOSE 8080

# Tomcat startup
CMD ["catalina.sh", "run"]
