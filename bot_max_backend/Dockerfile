FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY certs/russian_trusted_root_ca.crt /usr/local/share/ca-certificates/
COPY certs/russian_trusted_sub_ca.crt /usr/local/share/ca-certificates/

RUN apk add --no-cache ca-certificates curl && \
    curl -o /usr/local/share/ca-certificates/russian_trusted_root_ca.crt \
      "https://gu-st.ru/content/lending/russian_trusted_root_ca_pem.crt" && \
    curl -o /usr/local/share/ca-certificates/russian_trusted_sub_ca.crt \
      "https://gu-st.ru/content/lending/russian_trusted_sub_ca_pem.crt" && \
    update-ca-certificates && \
    keytool -importcert -noprompt -trustcacerts -alias russian_trusted_root \
      -file /usr/local/share/ca-certificates/russian_trusted_root_ca.crt \
      -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit && \
    keytool -importcert -noprompt -trustcacerts -alias russian_trusted_sub \
      -file /usr/local/share/ca-certificates/russian_trusted_sub_ca.crt \
      -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]