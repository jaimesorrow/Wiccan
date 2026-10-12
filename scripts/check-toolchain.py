"""Check package identities and toolchain contracts without downloading dependencies."""

import json
from pathlib import Path
import re
import sys
import tomllib


ROOT = Path(__file__).resolve().parent.parent
failures = []


def check(condition, label):
    print(("PASS  " if condition else "FAIL  ") + label)
    if not condition:
        failures.append(label)


def text(path):
    return (ROOT / path).read_text()


def config(path):
    return json.loads(text(path))


def main():
    app = text("app/build.gradle.kts")
    app_id = re.search(r'applicationId\s*=\s*"([^"]+)"', app).group(1)
    namespace = re.search(r'namespace\s*=\s*"([^"]+)"', app).group(1)
    check(app_id == "com.jaimesorrow.wiccan", "production application ID matches registered identity")
    check(namespace == "com.wiccan.app", "Android namespace matches the product namespace")
    kotlin_files = list((ROOT / "app/src").glob("*/kotlin/**/*.kt"))
    check(bool(kotlin_files) and all(
        re.search(r"^package " + re.escape(app_id) + r"(?:\.|\s|$)", p.read_text(), re.MULTILINE)
        for p in kotlin_files
    ), "existing Kotlin class packages remain stable for upgrades")
    manifest = text("app/src/main/AndroidManifest.xml")
    for component in ("WiccanApplication", "MainActivity", "notification.WiccanMessagingService",
                      "widget.WiccanWidgetReceiver", "widget.WiccanThemeDateChangeReceiver"):
        check('android:name="' + app_id + '.' + component + '"' in manifest,
              "manifest preserves " + component + " component identity")
    check((ROOT / ("app/schemas/" + app_id + ".data.local.WiccanDatabase/1.json")).exists()
          and (ROOT / ("app/schemas/" + app_id + ".data.local.WiccanDatabase/2.json")).exists(),
          "Room migration schemas follow the database class package")
    projects = config(".firebaserc")["projects"]
    configured_environments = {}
    for path, expected in [
        ("app/google-services.ci.json", app_id),
        ("app/src/prod/google-services.json", app_id),
        ("app/src/dev/google-services.json", app_id),
        ("app/src/staging/google-services.json", app_id),
    ]:
        if not (ROOT / path).exists() and "/prod/" not in path and "/src/" in path:
            print("SKIP  " + path + " (environment not configured)")
            continue
        firebase = config(path)
        clients = firebase["client"]
        packages = {c["client_info"]["android_client_info"]["package_name"] for c in clients}
        check(expected in packages, path + " registers " + expected)
        for flavor, alias in (("dev", "default"), ("staging", "staging")):
            if path == "app/src/" + flavor + "/google-services.json":
                project = firebase["project_info"]["project_id"]
                configured_environments[flavor] = project
                check(project == projects[alias], flavor + " Firebase project matches " + alias + " alias")
                check(project != projects["production"], flavor + " Firebase project is separate from production")
    if len(configured_environments) == 2:
        check(configured_environments["dev"] != configured_environments["staging"],
              "dev and staging use separate Firebase projects")
    check(config("app/src/prod/google-services.json")["project_info"]["project_id"]
          == projects["production"], "production Firebase project matches alias")
    production_clients = [
        client for client in config("app/src/prod/google-services.json")["client"]
        if client["client_info"]["android_client_info"]["package_name"] == app_id
    ]
    check(any(
        oauth.get("client_type") == 3
        and isinstance(oauth.get("client_id"), str)
        and oauth["client_id"].strip()
        for client in production_clients
        for oauth in client.get("oauth_client", [])
    ), "production Firebase client has a nonblank web OAuth client")
    for flavor in ("dev", "staging", "prod"):
        block = re.search(r'create\("' + flavor + r'"\)\s*\{([^}]+)\}', app).group(1)
        check(re.search(r"\bapplicationId(?:Suffix)?\s*=", block) is None,
              flavor + " inherits registered Firebase package")
    check(len(re.findall(r"\bapplicationId\s*=", app)) == 1
          and re.search(r"\bapplicationIdSuffix\s*=", app) is None,
          "all Android variants inherit the registered Firebase package")

    package = config("functions/package.json")
    lock = config("functions/package-lock.json")["packages"][""]
    node = package["engines"]["node"]
    check(node == "22" and text(".nvmrc").strip() == node, "local Node selection matches Functions runtime 22")
    check(lock.get("engines") == package["engines"], "lockfile root engines match package.json")
    for group in ("dependencies", "devDependencies"):
        check(lock.get(group) == package.get(group), "lockfile root " + group + " match package.json")
    workflow = text(".github/workflows/ci.yml")
    node_versions = re.findall(r"node-version:\s*['\"]?(\d+)", workflow)
    node_files = re.findall(r"node-version-file:\s*['\"]?([^\s'\"]+)", workflow)
    check(bool(node_versions or node_files)
          and all(version == node for version in node_versions)
          and all(path == "functions/package.json" for path in node_files),
          "CI uses Functions runtime Node " + node)
    for function in config("firebase.json")["functions"]:
        check(function.get("runtime", "nodejs" + node) == "nodejs" + node, "Firebase runtime agrees with package engines")
    backend = text("functions/src/index.ts")
    check("const packageName = process.env.ANDROID_PACKAGE_NAME;" in backend
          and "if (!packageName)" in backend
          and not re.search(r"process\.env\.ANDROID_PACKAGE_NAME\s*\?\?", backend),
          "backend package configuration fails closed per environment")

    versions = tomllib.loads(text("gradle/libs.versions.toml"))["versions"]
    check(int(versions["agp"].split(".")[0]) >= 9, "AGP 9 supplies built-in Kotlin support")
    gradle_files = text("build.gradle.kts") + app
    check("libs.plugins.kotlin.android" not in gradle_files
          and "org.jetbrains.kotlin.android" not in gradle_files
          and "kotlinOptions" not in app, "Gradle avoids conflicting legacy Kotlin plugin and DSL")
    check("JvmTarget.JVM_17" in app and app.count("JavaVersion.VERSION_17") == 2,
          "Java and Kotlin compilation target JVM 17")
    android_job = re.search(r"^  android:\n(.*?)(?=^  [a-zA-Z0-9_-]+:|\Z)", workflow,
                            re.MULTILINE | re.DOTALL).group(1)
    java_versions = re.findall(r"java-version:\s*['\"]?(\d+)", android_job + text(".github/workflows/release.yml"))
    check(java_versions == ["17", "17"], "CI and release use Java 17")
    check('version.set("1.8.0")' in app and int(versions["ktlint"].split(".")[0]) >= 14,
          "ktlint checks AGP 9 source sets with existing engine rules")
    print(str(len(failures)) + " failure(s)")
    return int(bool(failures))


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError, TypeError, AttributeError) as error:
        print("FAIL  Cannot validate configuration: " + str(error), file=sys.stderr)
        sys.exit(1)
