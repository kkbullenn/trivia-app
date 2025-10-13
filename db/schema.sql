
-- trivia-app database schema (MySQL 8.x)
-- Engine/charset
SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE DATABASE IF NOT EXISTS trivia_app CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE trivia_app;

-- 1) roles
CREATE TABLE IF NOT EXISTS roles (
  role_id     INT          NOT NULL,
  role_name   VARCHAR(50)  NOT NULL,
  CONSTRAINT pk_roles PRIMARY KEY (role_id),
  CONSTRAINT uq_roles_name UNIQUE (role_name)
) ENGINE=InnoDB;

-- 2) users
CREATE TABLE IF NOT EXISTS users (
  user_id        INT            NOT NULL AUTO_INCREMENT,
  username       VARCHAR(255)   NOT NULL,
  email          VARCHAR(255)   NOT NULL,
  password_hash  VARCHAR(255)   NOT NULL,
  role_id        INT            NOT NULL,
  created_at     TIMESTAMP      NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_users PRIMARY KEY (user_id),
  CONSTRAINT uq_users_username UNIQUE (username),
  CONSTRAINT uq_users_email UNIQUE (email),
  CONSTRAINT fk_users_roles FOREIGN KEY (role_id) REFERENCES roles(role_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 3) categories
CREATE TABLE IF NOT EXISTS categories (
  category_id  INT            NOT NULL AUTO_INCREMENT,
  name         VARCHAR(100)   NOT NULL,
  description  TEXT           NULL,
  display_order INT           NOT NULL DEFAULT 0,
  created_at   TIMESTAMP      NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP      NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT pk_categories PRIMARY KEY (category_id)
) ENGINE=InnoDB;

-- 4) questions
CREATE TABLE IF NOT EXISTS questions (
  question_id   INT            NOT NULL AUTO_INCREMENT,
  category_id   INT            NOT NULL,
  xml_question  LONGTEXT       NULL,
  youtube_url   VARCHAR(500)   NULL,
  question_text TEXT           NOT NULL,
  answers_option JSON          NOT NULL,
  answers_key   CHAR(1)        NOT NULL,
  points        INT            NOT NULL DEFAULT 1,
  uploaded_by   INT            NOT NULL,
  created_at    TIMESTAMP      NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP      NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT pk_questions PRIMARY KEY (question_id),
  CONSTRAINT fk_questions_categories FOREIGN KEY (category_id) REFERENCES categories(category_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_questions_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_answers_key CHECK (answers_key IN ('A','B','C','D'))
) ENGINE=InnoDB;

CREATE INDEX idx_questions_category ON questions(category_id);
CREATE INDEX idx_questions_uploaded_by ON questions(uploaded_by);

-- 5) individual_answers
CREATE TABLE IF NOT EXISTS individual_answers (
  answer_id       INT        NOT NULL AUTO_INCREMENT,
  question_id     INT        NOT NULL,
  user_id         INT        NOT NULL,
  selected_answer CHAR(1)    NOT NULL,
  is_correct      TINYINT(1) NOT NULL,
  score           INT        NOT NULL,
  created_at      TIMESTAMP  NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_individual_answers PRIMARY KEY (answer_id),
  CONSTRAINT fk_indans_question FOREIGN KEY (question_id) REFERENCES questions(question_id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_indans_user FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_indans_q ON individual_answers(question_id);
CREATE INDEX idx_indans_user ON individual_answers(user_id);

-- 6) sessions
CREATE TABLE IF NOT EXISTS sessions (
  session_id      INT           NOT NULL AUTO_INCREMENT,
  host_user_id    INT           NOT NULL,
  session_name    VARCHAR(255)  NULL,
  max_participants INT          NULL,
  status          ENUM('active','completed','cancelled') NULL DEFAULT 'active',
  start_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  end_at          TIMESTAMP     NULL DEFAULT NULL,
  CONSTRAINT pk_sessions PRIMARY KEY (session_id),
  CONSTRAINT fk_sessions_host FOREIGN KEY (host_user_id) REFERENCES users(user_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_sessions_host ON sessions(host_user_id);

-- 7) moderated_answers
CREATE TABLE IF NOT EXISTS moderated_answers (
  answer_id       INT        NOT NULL AUTO_INCREMENT,
  session_id      INT        NOT NULL,
  question_id     INT        NOT NULL,
  participant_id  INT        NOT NULL,
  selected_answer CHAR(1)    NOT NULL,
  is_correct      TINYINT(1) NOT NULL,
  score           INT        NOT NULL,
  created_at      TIMESTAMP  NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_moderated_answers PRIMARY KEY (answer_id),
  CONSTRAINT fk_modans_session FOREIGN KEY (session_id) REFERENCES sessions(session_id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_modans_question FOREIGN KEY (question_id) REFERENCES questions(question_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_modans_participant FOREIGN KEY (participant_id) REFERENCES users(user_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_modans_session ON moderated_answers(session_id);
CREATE INDEX idx_modans_question ON moderated_answers(question_id);
CREATE INDEX idx_modans_participant ON moderated_answers(participant_id);
