.DEFAULT_GOAL := apk
.PHONY: help setup doctor static test lint apk debug quick release all fresh rebuild clean distclean \
        devices phone deploy run demo expo exhibit lake workshop install reinstall launch restart stop logs log screenshot fps perf clear-data uninstall where verify check ui-test device-check build-apk release-apk \
        ship-release

GRADLE := ./tools/gradle-run.sh --no-daemon
ADB := ./tools/adb-run.sh
DEBUG_APK := releases/beauty-mirror-debug.apk
DEBUG_PACKAGE := com.beautymirror.app.debug
MAIN_ACTIVITY := com.beautymirror.app.MainActivity
COMPONENT := $(DEBUG_PACKAGE)/$(MAIN_ACTIVITY)

help:
	@printf '%s\n' \
	  'Beauty Mirror' \
	  '' \
	  '  make                 Build the installable debug APK' \
	  '  make phone           Build, install and launch on a connected phone' \
	  '  make demo / expo     Launch Stage mode in the pond exhibition scene' \
	  '  make workshop       Build/install the art-workshop pond experience' \
	  '  make ui-test         Run Compose interaction tests on the connected phone' \
	  '  make device-check    Build, test every UI control, launch and sample FPS' \
	  '  make release         Build/copy the optimized release APK' \
	  '  make fresh           Diagnose, clean and rebuild' \
	  '  make screenshot      Save the phone screen under screenshots/' \
	  '  make fps             Measure Android frame/jank statistics for 10 seconds' \
	  '  make perf            Launch exhibition mode, then measure frame statistics' \
	  '  make logs            Follow only Beauty Mirror logs' \
	  '  make doctor          Diagnose SDK, Java, disk, Gradle and adb' \
	  '  make where           Print generated APK paths' \
	  '  make check           Static checks, unit tests, lint and debug APK' \
	  '  make ship-release    Local APK build + GitHub Release upload (uses tools/ota/token.local)'

setup: doctor
	@echo 'Environment is ready. Run: make phone'

doctor:
	./tools/doctor.sh

static:
	./tools/static-checks.sh

test:
	$(GRADLE) :app:testDebugUnitTest

lint:
	$(GRADLE) :app:lintDebug :app:lintRelease

apk debug quick:
	$(GRADLE) :app:assembleDebug
	./tools/copy-apks.sh debug

release:
	$(GRADLE) :app:assembleRelease
	./tools/copy-apks.sh release

ship-release:
	chmod +x ./tools/release/publish-github-release.sh
	./tools/release/publish-github-release.sh

all:
	$(GRADLE) :app:assembleDebug :app:assembleRelease
	./tools/copy-apks.sh all

fresh: doctor clean apk
rebuild: clean apk

check: static test lint apk

ui-test:
	$(GRADLE) :app:connectedDebugAndroidTest

device-check: check ui-test expo fps

verify:
	./tools/verify.sh

devices:
	$(ADB) devices

install: apk
	$(ADB) install -r "$(DEBUG_APK)"

launch:
	-$(ADB) shell am force-stop "$(DEBUG_PACKAGE)"
	$(ADB) shell am start -n "$(COMPONENT)"

phone deploy run: install launch

reinstall: uninstall phone

build-apk: all
release-apk: release

# Exhibition shortcut: Stage preset, true mirror, controls/system bars initially hidden.
demo expo exhibit lake workshop: install
	-$(ADB) shell am force-stop "$(DEBUG_PACKAGE)"
	$(ADB) shell am start -n "$(COMPONENT)" \
	  --ez com.beautymirror.app.EXHIBITION_MODE true

fps:
	@echo 'Resetting frame statistics; keep the face moving for 10 seconds...'
	-$(ADB) shell dumpsys gfxinfo "$(DEBUG_PACKAGE)" reset >/dev/null
	@sleep 10
	@$(ADB) shell dumpsys gfxinfo "$(DEBUG_PACKAGE)" | \
	  sed -n '/Total frames rendered:/,/HISTOGRAM:/p'

perf: expo
	@$(MAKE) --no-print-directory fps

restart: launch

stop:
	-$(ADB) shell am force-stop "$(DEBUG_PACKAGE)"

logs log:
	$(ADB) logcat -v color -s AndroidRuntime BeautyRenderer CameraController FaceLandmarkerEngine BeautyMirrorApp

screenshot:
	@mkdir -p screenshots
	@name="screenshots/beauty-mirror-$$(date +%Y%m%d-%H%M%S).png"; \
	  $(ADB) exec-out screencap -p > "$$name"; \
	  echo "Screenshot: $$(pwd)/$$name"

clear-data:
	$(ADB) shell pm clear "$(DEBUG_PACKAGE)"

uninstall:
	-$(ADB) uninstall "$(DEBUG_PACKAGE)"

where:
	@find "$(CURDIR)/releases" "$(CURDIR)/app/build/outputs/apk" -type f -name '*.apk' -print 2>/dev/null || true

clean:
	$(GRADLE) clean

distclean:
	rm -rf app/build build .gradle releases screenshots
	mkdir -p releases
