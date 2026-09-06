CREATE TABLE users (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE routines (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name text NOT NULL CHECK (length(trim(name)) > 0),
    notes text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE routine_days (
    id uuid PRIMARY KEY,
    routine_id uuid NOT NULL REFERENCES routines(id) ON DELETE CASCADE,
    day_number smallint NOT NULL CHECK (day_number BETWEEN 1 AND 7),
    name text NOT NULL,
    UNIQUE (routine_id, day_number),
    UNIQUE (routine_id, id)
);

CREATE TABLE routine_exercises (
    id uuid PRIMARY KEY,
    routine_day_id uuid NOT NULL REFERENCES routine_days(id) ON DELETE CASCADE,
    name text NOT NULL CHECK (length(trim(name)) > 0),
    sets integer NOT NULL CHECK (sets > 0),
    reps integer NOT NULL CHECK (reps > 0),
    weight_kg numeric(8,2) NOT NULL CHECK (weight_kg >= 0),
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE routine_snapshots (
    id uuid PRIMARY KEY,
    routine_id uuid NOT NULL REFERENCES routines(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL
);

CREATE TABLE snapshot_exercises (
    id uuid PRIMARY KEY,
    snapshot_id uuid NOT NULL REFERENCES routine_snapshots(id) ON DELETE CASCADE,
    exercise_key text NOT NULL,
    name text NOT NULL,
    sets integer NOT NULL CHECK (sets > 0),
    reps integer NOT NULL CHECK (reps > 0),
    weight_kg numeric(8,2) NOT NULL CHECK (weight_kg >= 0),
    UNIQUE (snapshot_id, exercise_key)
);

CREATE INDEX routines_user_idx ON routines(user_id);
CREATE INDEX snapshots_routine_created_idx ON routine_snapshots(routine_id, created_at DESC);
CREATE INDEX exercises_day_order_idx ON routine_exercises(routine_day_id, sort_order);
