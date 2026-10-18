#!/usr/bin/env sh
set -eu

project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_root"

for variable in \
    PAWCHAN_KEYSTORE \
    PAWCHAN_KEY_ALIAS \
    PAWCHAN_STORE_PASSWORD \
    PAWCHAN_KEY_PASSWORD \
    PAWCHAN_UPDATE_MANIFEST_URL
do
    eval "value=\${$variable:-}"
    if [ -z "$value" ]; then
        printf "RELEASE PACKAGE: missing environment variable '%s'\n" "$variable" >&2
        exit 1
    fi
done

case "$PAWCHAN_UPDATE_MANIFEST_URL" in
    https://?*) ;;
    *)
        printf '%s\n' \
            "RELEASE PACKAGE: PAWCHAN_UPDATE_MANIFEST_URL must be an absolute HTTPS URL" >&2
        exit 1
        ;;
esac

./scripts/local-check.sh
./gradlew :app:assembleRelease \
    "-Ppawchan.updateManifestUrl=$PAWCHAN_UPDATE_MANIFEST_URL" \
    --no-configuration-cache \
    --warning-mode=fail

version_name=$(
    python3 -c \
        'import json; print(max(json.load(open("update/publishing/versions.json"))["versions"], key=lambda item: item["code"])["name"])'
)
apk_name="Pawchan-$version_name.apk"
mkdir -p dist
cp app/build/outputs/apk/release/app-release.apk "dist/$apk_name"
python3 update/publishing/tools/update_manifest.py "dist/$apk_name"
printf '\nRELEASE PACKAGE: PASS\nAPK: %s/dist/%s\n' "$project_root" "$apk_name"
