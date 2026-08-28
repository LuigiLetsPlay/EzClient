import os
import shutil
import glob

appdata = os.getenv("APPDATA")
source_jar = os.path.join(os.path.dirname(os.path.abspath(__file__)), "backend", "assets", "EzClient.jar")

if not os.path.exists(source_jar):
    print("Source JAR not found:", source_jar)
    exit(1)

print(f"Source JAR: {source_jar} ({os.path.getsize(source_jar)} bytes)")

profiles_dir = os.path.join(appdata, ".ezclient", "profiles")
updated_count = 0

if os.path.isdir(profiles_dir):
    for prof in os.listdir(profiles_dir):
        prof_path = os.path.join(profiles_dir, prof)
        if os.path.isdir(prof_path):
            mods_dir = os.path.join(prof_path, "mods")
            os.makedirs(mods_dir, exist_ok=True)
            dest = os.path.join(mods_dir, "EzClient.jar")
            print("Copying to profile:", dest)
            try:
                shutil.copy2(source_jar, dest)
                updated_count += 1
            except Exception as e:
                print("Failed to copy to", dest, e)

# Also check for any existing EzClient*.jar in .ezclient
for dest in glob.glob(os.path.join(appdata, ".ezclient", "**", "EzClient*.jar"), recursive=True):
    if "backend" not in dest:
        try:
            shutil.copy2(source_jar, dest)
            print("Refreshed:", dest)
        except Exception:
            pass

print(f"Successfully updated {updated_count} profile(s).")
