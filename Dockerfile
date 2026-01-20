#First stage, buid application
FROM alpine/java:21-jdk AS app-builder
RUN apk add wget

#install maven
RUN wget https://archive.apache.org/dist/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.tar.gz -P /opt \
    && tar xf /opt/apache-maven-3.9.11-bin.tar.gz -C /opt \
    && ln -s /opt/apache-maven-3.9.11 /opt/maven

ENV M2_HOME=/opt/maven
ENV PATH=${M2_HOME}/bin:${PATH}
RUN mkdir ~/.m2

RUN mkdir /working && chmod a+rw /working
ADD . /working
WORKDIR /working

#Verify and build appplication package
RUN mvn verify \
    && mvn package -DskipTests


#Secong stage, create app image
FROM alpine/java:21.0.4-jre
ARG APPLICATION_USER=appuser


RUN addgroup --system $APPLICATION_USER \
    && adduser --system $APPLICATION_USER --ingroup $APPLICATION_USER \
    && mkdir /app && chown -R $APPLICATION_USER /app
COPY --chown=$APPLICATION_USER:$APPLICATION_USER --from=app-builder /working/target/*.jar /app/app.jar
RUN chmod a+rw -R /app

WORKDIR /app
USER $APPLICATION_USER

EXPOSE 8080
RUN ls /app
ENTRYPOINT exec java $JAVA_OPTS -jar /app/app.jar
