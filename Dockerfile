
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY *.java ./
COPY index.html app.js style.css ./

RUN mkdir -p out && javac --add-modules jdk.httpserver -d out *.java

ENV PORT=8080

EXPOSE 8080

CMD ["sh", "-c", "java --add-modules jdk.httpserver -cp out com.networkmonitor.NetworkMonitorServer . ${PORT:-8080}"]
