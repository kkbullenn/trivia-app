This branch (db-team-DAO) workspace was cleaned on your request.

Actions performed:
- Created standard Maven layout directories: src/main/java, src/main/webapp
- Moved existing runtime compiled outputs to backup/WEB-INF-classes-backup-<timestamp> and backup/WEB-INF-lib-backup-<timestamp>

Notes:
- If you need to restore compiled classes or libs, check the backup/ directory and move files back to WEB-INF/classes or WEB-INF/lib.
- Next steps: place your Java sources under src/main/java with proper package declarations and web files under src/main/webapp. Then run `mvn clean compile` per team guidelines.
- .gitignore updated to avoid committing compiled artifacts.
