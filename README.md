# trivia-app

⚠️ IMPORTANT: READ THE FILE README_DB.md IN THE /db FOLDER FOR THE FULL DATABASE CONNECTION GUIDE AND TEAM INSTRUCTIONS.

---

# Trivia App — Getting Started

⚠️ IMPORTANT: We need Maven to handle and manage dependencies (like npm node). This is not optional. For example, Java
does not have native support to parse .env file so we need an external JAR. Maven just makes this simpler. Reach out to
AI team if you are stuck.

---

## 1️⃣ Installing Maven

1. **Check if Maven is already installed**:

   Open **Command Prompt** or **PowerShell** and run:

   ```cmd
   mvn -v
   ```

2. **Download Maven**:

    * Go to [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)
    * Download the latest **binary ZIP archive**.

3. **Extract Maven**:

    * For example, extract to:

      ```
      C:\Program Files\apache-maven-3.9.5
      ```

4. **Add Maven to your system PATH**:

    * Add the `bin` directory to PATH:

      ```
      C:\Program Files\apache-maven-3.9.5\bin
      ```

5. **Verify installation**:

   ```cmd
   mvn -v
   ```

   You should see Maven version and Java version info.

---

## 2️⃣ Managing Dependencies

* All project dependencies are listed in `pom.xml`.
* **Do not manually download JAR files**. Maven handles them automatically.

### Adding a dependency:

1. Open `pom.xml`.

2. Add the dependency inside the `<dependencies>` section:

   ```xml
   <dependency>
       <groupId>org.example</groupId>
       <artifactId>example-lib</artifactId>
       <version>1.2.3</version>
   </dependency>
   ```

3. Fetch the dependency using:

   ```cmd
   mvn compile
   ```

4. To view all dependencies and their hierarchy:

   ```cmd
   mvn dependency:tree
   ```

---

## 3️⃣ Local Development

**Goal:** Edit Java code and run it directly in Tomcat without creating a WAR.

1. Open **Command Prompt** or **PowerShell** and navigate to the project folder:

   ```cmd
   cd C:\tomcat\webapps\trivia-app
   ```

2. Compile the code and download dependencies:

   ```cmd
   mvn compile
   # OR:
   mvn clean compile # to ensure old compiled files are gone
   ```

    * Compiled `.class` files will be placed in:

      ```
      WEB-INF\classes\
      ```

3. Start Tomcat:

   ```cmd
   cd C:\tomcat\bin
   startup.bat
   ```

4. Edit code in `src\main\java\` as needed.

5. Re-run:

   ```cmd
   cd C:\tomcat\webapps\trivia-app
   mvn compile
   # OR:
   mvn clean compile # to ensure old compiled files are gone
   ```

6. Restart Tomcat to see changes:

   ```cmd
   cd C:\tomcat\bin
   shutdown.bat
   startup.bat
   ```

---

## 4️⃣ Production Deployment (for DevOps team)

**Goal:** Build a clean WAR file and deploy it for hosting.

1. Build the WAR:

   ```cmd
   cd C:\tomcat\webapps\trivia-app
   mvn clean package
   ```

    * The WAR file will be located at:

      ```
      target\trivia-app.war
      ```

2. Deploy the WAR to Tomcat:

   ```cmd
   copy target\trivia-app.war C:\tomcat\webapps\
   ```

    * **Do not** place the WAR inside `WEB-INF\classes\`.
    * Tomcat will automatically unpack it and use its internal `WEB-INF\classes\`.

3. Start or restart Tomcat:

   ```cmd
   cd C:\tomcat\bin
   shutdown.bat
   startup.bat
   ```

4. Access the app:

   ```
   http://localhost:8080/trivia-app
   ```

---

## ✅ Summary

| Environment | Command                              | What Happens                                                      |
|-------------|--------------------------------------|-------------------------------------------------------------------|
| Local Dev   | `mvn compile` OR `mvn clean compile` | Compiles `.class` files into `WEB-INF\classes\` for Tomcat to use |
| Production  | `mvn clean package`                  | Generates `trivia-app-prod.war` in `target\` ready to deploy      |

* Local edits → compile → restart Tomcat (local dev)
* Production deploy → build WAR → copy WAR → restart Tomcat (for hosting)

---

# Project Structure

```text
trivia-app/
├── pom.xml                                       ← Maven project file (dependencies & build)
├── README.md
├── WEB-INF/                                      ← Local Tomcat runtime output (managed by Maven)
│   ├── web.xml                                   ← Servlet & filter configuration
│   ├── classes/                                  ← Compiled .class files from `mvn compile`
│   └── lib/                                      ← Runtime JAR dependencies copied during compile
├── src/
│   ├── ai-services/                              ← Python helpers (caption, transcription, translation)
│   │   ├── caption-server/
│   │   ├── transcription-server/
│   │   └── translation-server/
│   └── main/
│       ├── java/com/triviaapp/
│       │            ├── dao/                     ← DAO interfaces & implementations
│       │            │   └── impl/
│       │            ├── externalapi/             ← Proxies to AI/translation services
│       │            ├── servlets/                ← HTTP servlet entry points
│       │            ├── util/                    ← Shared utilities
│       │            └── websocket/               ← WebSocket endpoints
│       └── webapp/                               ← Authoritative front-end assets packaged into the WAR
│           ├── index.html
│           ├── login.html
│           ├── select-quiz.html
│           ├── user.html
│           └── js/
├── admin_landing.html                            ← Local dev copy (mirrors src/main/webapp)
├── category-lobbies.html                         ← Local dev copy (mirrors src/main/webapp)
├── ...                                           ← Additional root-level HTML used for hot-reload in Tomcat
└── target/                                       ← Maven build output (ignored by Git)
```

* The `*.html` files in the project root are generated by a resource sync plugin and are only used for local Tomcat
  hot-reload. For the final deliverable, please refer to the files under the `src/main/webapp/` directory.
* The `src/ai-services` directory contains optional Python helper services (speech transcription, subtitles,
  translation). Start them only when you need to integrate and test AI-related features.

---

# Database (.env)

Place a `.env` file in the project root (do NOT commit).  
Replace `username` and `password` with your actual database credentials.

```env
JDBC_URL=jdbc:mysql://shuttle.proxy.rlwy.net:24339/trivia_app?useSSL=true&serverTimezone=UTC
JDBC_USER=your_username
JDBC_PASS=your_password
```