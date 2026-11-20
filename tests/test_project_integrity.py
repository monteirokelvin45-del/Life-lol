import json
import pathlib
import re
import xml.etree.ElementTree as ET

REPO_ROOT = pathlib.Path(__file__).resolve().parents[1]


def test_all_six_gradle_modules_exist_and_registered():
    settings = (REPO_ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
    expected_modules = [
        (":core:database", REPO_ROOT / "core/database/build.gradle.kts"),
        (":core:domain", REPO_ROOT / "core/domain/build.gradle.kts"),
        (":core:ai", REPO_ROOT / "core/ai/build.gradle.kts"),
        (":core:system", REPO_ROOT / "core/system/build.gradle.kts"),
        (":core:ui", REPO_ROOT / "core/ui/build.gradle.kts"),
        (":app", REPO_ROOT / "app/build.gradle.kts"),
    ]
    for mod_name, gradle_path in expected_modules:
        assert f'include("{mod_name}")' in settings
        assert gradle_path.exists(), f"Missing {gradle_path}"


def test_version_catalog_references_resolve():
    toml_text = (REPO_ROOT / "gradle/libs.versions.toml").read_text(encoding="utf-8")
    defined_libs = set(
        re.findall(r"^([a-zA-Z0-9_-]+)\s*=\s*\{", toml_text, flags=re.MULTILINE)
    )
    normalized_libs = {name.replace("-", ".") for name in defined_libs}

    for gradle_file in REPO_ROOT.rglob("build.gradle.kts"):
        src = gradle_file.read_text(encoding="utf-8")
        for ref in re.findall(r"libs\.([a-zA-Z0-9_.]+)", src):
            if ref.startswith("plugins.") or ref.startswith("versions."):
                continue
            assert ref in normalized_libs, (
                f"Unresolved libs.{ref} in {gradle_file.relative_to(REPO_ROOT)}"
            )


def test_zero_xml_screen_layouts_100_percent_compose():
    layout_files = list((REPO_ROOT / "app/src/main/res").rglob("layout/*.xml"))
    assert layout_files == [], f"Found forbidden screen XML layouts: {layout_files}"


def test_no_nested_block_comments_in_kotlin_sources():
    for kt_file in list((REPO_ROOT / "core").rglob("*.kt")) + list((REPO_ROOT / "app").rglob("*.kt")):
        src = kt_file.read_text(encoding="utf-8")
        depth = 0
        in_string = False
        in_line_comment = False
        i = 0
        while i < len(src):
            ch = src[i]
            nxt = src[i + 1] if i + 1 < len(src) else ""
            if in_line_comment:
                if ch == "\n":
                    in_line_comment = False
                i += 1
                continue
            if depth == 0 and in_string:
                if ch == "\\" and nxt:
                    i += 2
                    continue
                if ch == '"':
                    in_string = False
                i += 1
                continue
            if depth == 0 and ch == '"':
                in_string = True
                i += 1
                continue
            if depth == 0 and ch == "/" and nxt == "/":
                in_line_comment = True
                i += 2
                continue
            if ch == "/" and nxt == "*":
                depth += 1
                assert depth == 1, f"Nested /* comment in {kt_file.relative_to(REPO_ROOT)}"
                i += 2
                continue
            if ch == "*" and nxt == "/" and depth > 0:
                depth -= 1
                i += 2
                continue
            i += 1
        assert depth == 0, f"Unclosed block comment in {kt_file.relative_to(REPO_ROOT)}"


def test_android_manifest_declares_all_pillar_3_permissions_and_whatsapp_share():
    manifest_path = REPO_ROOT / "app/src/main/AndroidManifest.xml"
    tree = ET.parse(manifest_path)
    root = tree.getroot()
    ns = {"android": "http://schemas.android.com/apk/res/android"}

    permissions = {
        elem.attrib.get(f"{{{ns['android']}}}name")
        for elem in root.findall("uses-permission")
    }
    required_permissions = {
        "android.permission.SCHEDULE_EXACT_ALARM",
        "android.permission.USE_EXACT_ALARM",
        "android.permission.WAKE_LOCK",
        "android.permission.VIBRATE",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",
        "android.permission.CAMERA",
        "android.permission.READ_MEDIA_DOCUMENTS",
    }
    assert required_permissions.issubset(permissions)

    app_node = root.find("application")
    assert app_node is not None
    activity = app_node.find("activity")
    assert activity is not None
    actions = {
        act.attrib.get(f"{{{ns['android']}}}name")
        for flt in activity.findall("intent-filter")
        for act in flt.findall("action")
    }
    assert "android.intent.action.SEND" in actions

    receivers = {
        r.attrib.get(f"{{{ns['android']}}}name")
        for r in app_node.findall("receiver")
    }
    services = {
        s.attrib.get(f"{{{ns['android']}}}name")
        for s in app_node.findall("service")
    }
    assert "com.lifeos.core.system.receiver.ExactAlarmReceiver" in receivers
    assert ".features.widget.LifeOsWidgetReceiver" in receivers
    assert "com.lifeos.core.system.service.ActiveExecutionService" in services


def test_ai_extraction_json_schema_is_valid_json_draft_07():
    schema_kt = (
        REPO_ROOT
        / "core/ai/src/main/kotlin/com/lifeos/core/ai/schema/LifeOsExtractionSchema.kt"
    ).read_text(encoding="utf-8")
    match = re.search(r'STRICT_JSON_SCHEMA:\s*String\s*=\s*"""(.*?)"""', schema_kt, re.DOTALL)
    assert match is not None
    raw_json = match.group(1).replace("${'$'}", "$").strip()
    parsed = json.loads(raw_json)
    assert parsed["$schema"] == "http://json-schema.org/draft-07/schema#"
    assert set(parsed["required"]) == {
        "detected_habits",
        "detected_workouts",
        "detected_studies",
        "detected_finances",
    }
