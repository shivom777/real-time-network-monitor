# NetPulse — Real-Time Network Monitor

A Java 21 OOP mini-project based on the supplied project report. It provides a browser dashboard, TCP port checks, HTTP endpoint checks, bounded concurrency, status-change event logging, and availability/response-time summaries.

## Requirements
- JDK 21 (or a recent JDK supporting `java.net.http`)
- A modern browser
- Internet access for monitoring public websites

## Run on Windows
1. Extract the project folder.
2. Double-click `run.bat` (or open a terminal in this folder and run `run.bat`).
3. Open **http://localhost:8080** in Chrome or Edge.
4. Click **Add target** to add an HTTP URL or a TCP host and port.
5. Keep the terminal open while using the dashboard. Press Ctrl+C to stop.

## Run on Linux/macOS
```bash
./run.sh
```
Then open http://localhost:8080.

## Features
- Add/remove HTTP endpoint and TCP port targets
- Automatic checks every 15 seconds
- Maximum five checks executing concurrently
- UP/DOWN/PENDING status and response time
- Event log when a target changes state
- Per-target and overall availability summaries
- Responsive dark dashboard

## OOP concepts
- **Encapsulation:** `Target` keeps state private and updates it through methods.
- **Abstraction:** abstract `Checker` defines the common `check(Target)` contract.
- **Inheritance:** `TCPChecker` and `EndpointChecker` extend `Checker`.
- **Polymorphism:** the monitoring flow invokes `check()` through the `Checker` abstraction; the selected checker supplies its own implementation.

## Project structure
```text
RealTimeNetworkMonitor/
├── run.bat
├── run.sh
├── README.md
└── src/main/
    ├── java/com/networkmonitor/
    │   ├── NetworkMonitorServer.java
    │   ├── Target.java
    │   ├── Checker.java
    │   ├── TCPChecker.java
    │   ├── EndpointChecker.java
    │   ├── CheckResult.java
    │   ├── ConcurrencyController.java
    │   └── EventLog.java
    └── resources/
        ├── index.html
        ├── style.css
        └── app.js
```

## Notes
- The app listens on port 8080. If that port is already in use, stop the other service before starting this one.
- A TCP check tests whether a connection to the selected host and port can be established; it does not inspect application-level traffic.
- HTTP endpoints returning status codes 200–399 are treated as UP.
- This is a local educational mini-project, not a replacement for an enterprise monitoring platform. The event log and metrics are held in memory and reset when the app restarts. The application was compiled and its local API was smoke-tested in the build environment; external endpoint checks should be verified on your own internet connection.
