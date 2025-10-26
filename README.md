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
   ```

6. Restart Tomcat to see changes:

   ```cmd
   cd C:\tomcat\bin
   shutdown.bat
   startup.bat
   ```

---

## 4️⃣ Production Deployment

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

| Environment | Command             | What Happens                                                      |
| ----------- | ------------------- | ----------------------------------------------------------------- |
| Local Dev   | `mvn compile`       | Compiles `.class` files into `WEB-INF\classes\` for Tomcat to use |
| Production  | `mvn clean package` | Generates `trivia-app.war` in `target\` ready to deploy           |

* Local edits → compile → restart Tomcat (local dev)
* Production deploy → build WAR → copy WAR → restart Tomcat (for hosting)

---
