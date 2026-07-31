

CREATE TABLE IF NOT EXISTS booking (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    user_name    VARCHAR(100),
    workspace    VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS user (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    user_name    VARCHAR(100),
    email    VARCHAR(100)
);