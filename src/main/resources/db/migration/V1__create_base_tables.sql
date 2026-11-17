CREATE TABLE academy (
                         id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         name        VARCHAR(120) NOT NULL,
                         city        VARCHAR(80),
                         created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
                          id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          academy_id     UUID NOT NULL REFERENCES academy (id),
                          name           VARCHAR(120) NOT NULL,
                          email          VARCHAR(160) NOT NULL UNIQUE CHECK (email = lower(email)),
                          password_hash  VARCHAR(100) NOT NULL,
                          role           VARCHAR(20) NOT NULL DEFAULT 'STUDENT'
                              CHECK (role IN ('STUDENT', 'INSTRUCTOR', 'ADMIN')),
                          created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE training_session (
                                  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  user_id           UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
                                  trained_on        DATE NOT NULL,
                                  type              VARCHAR(20) NOT NULL
                                      CHECK (type IN ('GI', 'NOGI', 'OPEN_MAT')),
                                  duration_minutes  INTEGER NOT NULL CHECK (duration_minutes > 0),
                                  learned_notes     TEXT,
                                  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_training_session_user_date ON training_session (user_id, trained_on);

CREATE TABLE belt_promotion (
                                id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id      UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
                                belt         VARCHAR(20) NOT NULL
                                    CHECK (belt IN ('WHITE', 'BLUE', 'PURPLE', 'BROWN', 'BLACK')),
                                degree       SMALLINT NOT NULL DEFAULT 0 CHECK (degree BETWEEN 0 AND 4),
                                promoted_on  DATE NOT NULL,
                                promoted_by  UUID REFERENCES app_user (id),
                                notes        TEXT,
                                created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_belt_promotion_user_date ON belt_promotion (user_id, promoted_on);

CREATE TABLE break_period (
                              id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id     UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
                              start_date  DATE NOT NULL,
                              end_date    DATE,
                              reason      VARCHAR(20) NOT NULL
                                  CHECK (reason IN ('INJURY', 'STUDY', 'PERSONAL', 'OTHER')),
                              notes       TEXT,
                              created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                              CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX idx_break_period_user ON break_period (user_id, start_date);

-- Academia de teste para desenvolvimento, com id fixo (remova ou mova para uma migration de seed)
INSERT INTO academy (id, name, city)
VALUES ('11111111-1111-1111-1111-111111111111', 'Academia Teste', 'Marília');