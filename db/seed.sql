
USE trivia_app;

-- Seed roles
INSERT INTO roles (role_id, role_name) VALUES
  (100,'user'), (200,'staff'), (300,'admin')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- Seed users (passwords are example hashes)
INSERT INTO users (username, email, password_hash, role_id)
VALUES
  ('alex','alex@example.com','$2y$12$hashA',300),
  ('jane','jane@example.com','$2y$12$hashB',100)
ON DUPLICATE KEY UPDATE email = VALUES(email), role_id = VALUES(role_id);

-- Seed categories
INSERT INTO categories (name, description, display_order) VALUES
('60s Movies','Classic movies from the 1960s',1),
('Science','General science trivia',2)
ON DUPLICATE KEY UPDATE description = VALUES(description), display_order = VALUES(display_order);

-- Example question
INSERT INTO questions (category_id, xml_question, youtube_url, question_text, answers_option, answers_key, points, uploaded_by)
VALUES
(1,
'<?xml version="1.0" encoding="UTF-8"?><TriviaQuestion><QuestionText>Who is the main character in this movie?</QuestionText></TriviaQuestion>',
'https://youtube.com/watch?v=dQw4w9WgXcQ',
'Who is the main character in this movie?',
JSON_ARRAY(JSON_OBJECT('key','A','text','Option A'),
           JSON_OBJECT('key','B','text','Option B'),
           JSON_OBJECT('key','C','text','Option C'),
           JSON_OBJECT('key','D','text','Option D')),
'A', 10, 1);

-- One individual answer attempt (alex answers the question correctly)
INSERT INTO individual_answers (question_id, user_id, selected_answer, is_correct, score)
VALUES (1, 1, 'A', 1, 10);

-- Create a session and a moderated answer
INSERT INTO sessions (host_user_id, session_name, max_participants, status)
VALUES (1, 'Trivia Night - Team Alpha', 20, 'active');

INSERT INTO moderated_answers (session_id, question_id, participant_id, selected_answer, is_correct, score)
VALUES (1, 1, 2, 'B', 0, 0);
