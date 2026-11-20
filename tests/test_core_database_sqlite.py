import pathlib
import re
import sqlite3
import pytest

REPO_ROOT = pathlib.Path(__file__).resolve().parents[1]
ENTITY_DIR = REPO_ROOT / "core/database/src/main/kotlin/com/lifeos/core/database/entity"
DAO_FILE = REPO_ROOT / "core/database/src/main/kotlin/com/lifeos/core/database/dao/LifeOsDao.kt"
DB_FILE = REPO_ROOT / "core/database/src/main/kotlin/com/lifeos/core/database/LifeOsDatabase.kt"
CONVERTER_FILE = REPO_ROOT / "core/database/src/main/kotlin/com/lifeos/core/database/converter/LifeOsTypeConverters.kt"


DDL_STATEMENTS = """
PRAGMA foreign_keys = ON;

CREATE TABLE habits (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    hex_color TEXT NOT NULL,
    target_days_mask INTEGER NOT NULL,
    current_streak INTEGER NOT NULL DEFAULT 0,
    longest_streak INTEGER NOT NULL DEFAULT 0,
    is_archived INTEGER NOT NULL DEFAULT 0,
    description TEXT NOT NULL DEFAULT '',
    icon_key TEXT NOT NULL DEFAULT 'check',
    daily_goal_value REAL NOT NULL DEFAULT 1.0,
    unit_label TEXT NOT NULL DEFAULT 'check-in',
    created_at_epoch_day INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX index_habits_is_archived ON habits (is_archived);
CREATE INDEX index_habits_category ON habits (category);

CREATE TABLE habit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    habit_id INTEGER NOT NULL,
    epoch_day INTEGER NOT NULL,
    completed INTEGER NOT NULL,
    value_logged REAL NOT NULL DEFAULT 1.0,
    logged_at_millis INTEGER NOT NULL DEFAULT 0,
    note TEXT,
    FOREIGN KEY (habit_id) REFERENCES habits (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX index_habit_logs_habit_id_epoch_day ON habit_logs (habit_id, epoch_day);
CREATE INDEX index_habit_logs_epoch_day ON habit_logs (epoch_day);

CREATE TABLE workout_routines (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    day_of_week INTEGER NOT NULL,
    calendar_event_id INTEGER,
    estimated_duration_minutes INTEGER NOT NULL DEFAULT 45,
    notes TEXT
);
CREATE INDEX index_workout_routines_day_of_week ON workout_routines (day_of_week);

CREATE TABLE exercises (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    routine_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    target_sets INTEGER NOT NULL,
    target_reps INTEGER NOT NULL,
    rest_seconds INTEGER NOT NULL DEFAULT 90,
    order_index INTEGER NOT NULL,
    completed_sets_today INTEGER NOT NULL DEFAULT 0,
    last_completed_epoch_day INTEGER,
    last_weight_kg REAL NOT NULL DEFAULT 0.0,
    FOREIGN KEY (routine_id) REFERENCES workout_routines (id) ON DELETE CASCADE
);
CREATE INDEX index_exercises_routine_id ON exercises (routine_id);
CREATE INDEX index_exercises_routine_id_order_index ON exercises (routine_id, order_index);

CREATE TABLE study_topics (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    subject TEXT NOT NULL,
    topic TEXT NOT NULL,
    stage INTEGER NOT NULL DEFAULT 0,
    next_review_epoch_day INTEGER NOT NULL,
    alarm_request_code INTEGER,
    last_reviewed_epoch_day INTEGER,
    review_count INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX index_study_topics_next_review_epoch_day ON study_topics (next_review_epoch_day);
CREATE INDEX index_study_topics_last_reviewed_epoch_day ON study_topics (last_reviewed_epoch_day);
CREATE INDEX index_study_topics_subject ON study_topics (subject);

CREATE TABLE financial_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    amount REAL NOT NULL,
    type TEXT NOT NULL,
    category TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    note TEXT,
    epoch_day INTEGER NOT NULL DEFAULT 0,
    currency_code TEXT NOT NULL DEFAULT 'MZN',
    payment_method TEXT NOT NULL DEFAULT 'WALLET'
);
CREATE INDEX index_financial_records_timestamp ON financial_records (timestamp);
CREATE INDEX index_financial_records_type_epoch_day ON financial_records (type, epoch_day);
CREATE INDEX index_financial_records_category ON financial_records (category);

CREATE TABLE books (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    title TEXT NOT NULL,
    total_pages INTEGER NOT NULL,
    current_page INTEGER NOT NULL DEFAULT 0,
    pages_per_hour REAL NOT NULL DEFAULT 0.0,
    status TEXT NOT NULL DEFAULT 'READING',
    author TEXT NOT NULL DEFAULT '',
    daily_goal_pages INTEGER NOT NULL DEFAULT 20,
    pages_read_today INTEGER NOT NULL DEFAULT 0,
    last_read_epoch_day INTEGER
);
CREATE INDEX index_books_status ON books (status);

CREATE TABLE book_quotes (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    book_id INTEGER NOT NULL,
    quote_text TEXT NOT NULL,
    page_number INTEGER NOT NULL DEFAULT 0,
    captured_at_millis INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE
);
CREATE INDEX index_book_quotes_book_id ON book_quotes (book_id);
CREATE INDEX index_book_quotes_captured_at_millis ON book_quotes (captured_at_millis);
"""


def extract_dao_queries(dao_source: str) -> list[str]:
    queries = []
    # Match triple-quoted @Query("""...""") and single-line @Query("...")
    pattern = re.compile(
        r'@Query\(\s*(?:"""(.*?)"""|"([^"]*)")\s*\)',
        re.DOTALL,
    )
    for match in pattern.finditer(dao_source):
        sql = (match.group(1) or match.group(2)).strip()
        queries.append(sql)
    return queries


@pytest.fixture()
def conn():
    db = sqlite3.connect(":memory:")
    db.row_factory = sqlite3.Row
    db.executescript(DDL_STATEMENTS)
    yield db
    db.close()


def test_all_seven_entities_registered_in_life_os_database():
    db_src = DB_FILE.read_text(encoding="utf-8")
    expected_entities = [
        "HabitEntity::class",
        "HabitLogEntity::class",
        "WorkoutRoutineEntity::class",
        "ExerciseEntity::class",
        "StudyTopicEntity::class",
        "FinancialRecordEntity::class",
        "BookEntity::class",
        "BookQuoteEntity::class",
    ]
    for entity in expected_entities:
        assert entity in db_src, f"Missing {entity} in LifeOsDatabase"
    assert "SupportFactory" in db_src
    assert "SQLiteDatabase.loadLibs" in db_src


def test_kotlin_entity_columns_match_sqlite_ddl(conn):
    for kt_file in sorted(ENTITY_DIR.glob("*Entity.kt")):
        src = kt_file.read_text(encoding="utf-8")
        table_match = re.search(r'tableName\s*=\s*"([^"]+)"', src)
        assert table_match, f"Missing tableName in {kt_file.name}"
        table_name = table_match.group(1)

        kt_columns = set(re.findall(r'@ColumnInfo\(\s*name\s*=\s*"([^"]+)"\s*\)', src))
        pragma_cols = {
            row["name"]
            for row in conn.execute(f"PRAGMA table_info({table_name})").fetchall()
        }
        assert kt_columns == pragma_cols, (
            f"Column mismatch in {table_name}: Kotlin={kt_columns} vs DDL={pragma_cols}"
        )


def test_every_query_in_life_os_dao_compiles_and_uses_valid_schema(conn):
    dao_src = DAO_FILE.read_text(encoding="utf-8")
    queries = extract_dao_queries(dao_src)
    assert len(queries) >= 20, f"Expected >=20 @Query annotations, found {len(queries)}"

    for sql in queries:
        # Bind dummy parameters for all :named Room bind args
        param_names = set(re.findall(r":([a-zA-Z_][a-zA-Z0-9_]*)", sql))
        params = {name: 1 for name in param_names}
        if "status" in params:
            params["status"] = "READING"
        plan_rows = conn.execute(f"EXPLAIN QUERY PLAN {sql}", params).fetchall()
        assert len(plan_rows) >= 1


def test_foreign_key_cascade_and_streak_update_safety(conn):
    cur = conn.cursor()
    cur.execute(
        "INSERT INTO habits (name, category, hex_color, target_days_mask) VALUES (?, ?, ?, ?)",
        ("Calistenia Matinal", "HEALTH", "#10E05B", 127),
    )
    habit_id = cur.lastrowid

    # Unique (habit_id, epoch_day) replacement check
    cur.execute(
        "INSERT OR REPLACE INTO habit_logs (habit_id, epoch_day, completed, value_logged) VALUES (?, ?, ?, ?)",
        (habit_id, 20777, 1, 1.0),
    )
    cur.execute(
        "INSERT OR REPLACE INTO habit_logs (habit_id, epoch_day, completed, value_logged) VALUES (?, ?, ?, ?)",
        (habit_id, 20777, 1, 2.0),
    )
    logs = cur.execute("SELECT * FROM habit_logs WHERE habit_id = ?", (habit_id,)).fetchall()
    assert len(logs) == 1
    assert logs[0]["value_logged"] == 2.0

    # Updating streak in-place preserves child habit_logs
    cur.execute(
        """
        UPDATE habits
        SET current_streak = :currentStreak,
            longest_streak = CASE WHEN :longestStreak > longest_streak THEN :longestStreak ELSE longest_streak END
        WHERE id = :habitId
        """,
        {"currentStreak": 32, "longestStreak": 32, "habitId": habit_id},
    )
    assert cur.execute("SELECT COUNT(*) FROM habit_logs WHERE habit_id = ?", (habit_id,)).fetchone()[0] == 1

    # Deleting parent habit cascades to habit_logs
    cur.execute("DELETE FROM habits WHERE id = ?", (habit_id,))
    assert cur.execute("SELECT COUNT(*) FROM habit_logs WHERE habit_id = ?", (habit_id,)).fetchone()[0] == 0


def test_cashflow_category_and_burn_aggregations(conn):
    records = [
        (50000.0, "INCOME", "Salário", 1795000000000, 20775),
        (8500.0, "EXPENSE", "Moradia", 1795086400000, 20776),
        (3200.0, "EXPENSE", "Alimentação", 1795172800000, 20777),
        (1800.0, "EXPENSE", "Alimentação", 1795176400000, 20777),
    ]
    conn.executemany(
        "INSERT INTO financial_records (amount, type, category, timestamp, epoch_day) VALUES (?, ?, ?, ?, ?)",
        records,
    )

    summary = conn.execute(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0.0 END), 0.0) AS total_income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0.0 END), 0.0) AS total_expense
        FROM financial_records
        WHERE epoch_day BETWEEN 20770 AND 20780
        """
    ).fetchone()
    assert summary["total_income"] == 50000.0
    assert summary["total_expense"] == 13500.0

    categories = conn.execute(
        """
        SELECT category, COALESCE(SUM(amount), 0.0) AS total_amount
        FROM financial_records
        WHERE type = 'EXPENSE' AND epoch_day BETWEEN 20770 AND 20780
        GROUP BY category
        ORDER BY total_amount DESC
        """
    ).fetchall()
    assert categories[0]["category"] == "Moradia"
    assert categories[0]["total_amount"] == 8500.0
    assert categories[1]["category"] == "Alimentação"
    assert categories[1]["total_amount"] == 5000.0


def test_type_converters_no_jvm_signature_clashes():
    src = CONVERTER_FILE.read_text(encoding="utf-8")
    method_names = re.findall(r"@TypeConverter\s+fun\s+([a-zA-Z0-9_]+)\s*\(", src)
    assert len(method_names) == 14
    assert len(method_names) == len(set(method_names)), "Duplicate @TypeConverter JVM method names!"
