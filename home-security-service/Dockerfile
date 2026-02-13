FROM openjdk:8-jre-alpine

EXPOSE 9001

ADD target/home-security-service-0.0.1-SNAPSHOT.jar home-security-service.jar

RUN /bin/sh -c 'touch /home-security-service.jar'

RUN mkdir "fileStorage"

RUN echo "Africa/Nairobi" > /etc/timezone

ENTRYPOINT ["java", "-Xmx256m", "-jar", "/home-security-service.jar"]