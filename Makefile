CSS=build/css/style.css
APP=build/js/compiled/otoch.js
IDX=build/index.html
IMG=build/img/*.png
IMG_PUBLIC=$(subst build,resources/public,$(IMG))
SFX_SOURCE=$(wildcard resources/public/sfx/*.wav)
SFX_OGGS=$(subst .wav,.ogg,$(SFX_SOURCE))
SFX_MP3S=$(subst .wav,.mp3,$(SFX_SOURCE))
SFX=$(subst resources/public,build,$(SFX_OGGS)) $(subst resources/public,build,$(SFX_MP3S))
MUSIC_SOURCE=$(wildcard resources/public/music/*.ogg)
MUSIC_SOURCE_MP3S=$(subst .ogg,.mp3,$(MUSIC_SOURCE))
MUSIC=$(subst resources/public,build,$(MUSIC_SOURCE)) $(subst resources/public,build,$(MUSIC_SOURCE_MP3S))
ME=$(shell basename $(shell pwd))
REPO=git@github.com:retrogradeorbit/otoch.git

all: $(SFX_OGGS) $(SFX_MP3S) $(APP) $(CSS) $(IDX) $(IMG) $(SFX) $(MUSIC)

$(SFX_OGGS): $(SFX_SOURCE)
	oggenc -o $@ $(subst .ogg,.wav,$@)

$(SFX_MP3S): $(SFX_SOURCE)
	lame $(subst .mp3,.wav,$@) $@

$(CSS): resources/public/css/style.css
	mkdir -p $(dir $(CSS))
	cp $< $@

$(APP): src/**/** project.clj
	rm -f $(APP)
	lein cljsbuild once min

$(IDX): resources/public/index.html
	cp $< $@

$(IMG): $(IMG_PUBLIC)
	mkdir -p build/img/
	cp $? build/img/

$(SFX): $(SFX_OGGS) $(SFX_MP3S)
	mkdir -p build/sfx/
	cp $? build/sfx/

$(MUSIC_SOURCE_MP3S): $(MUSIC_SOURCE)
	ogg123 -d wav -f $(subst .ogg,.wav,$<) $<
	lame $(subst .ogg,.wav,$<) $@

$(MUSIC): $(MUSIC_SOURCE) $(MUSIC_SOURCE_MP3S)
	mkdir -p build/music/
	cp $? build/music/

clean:
	lein clean
	rm -rf $(CSS) $(APP) $(IDX) $(IMG) $(SFX) $(MUSIC) $(SFX_OGGS) $(SFX_MP3S)

test-server: all
	cd build && python -m SimpleHTTPServer

setup-build-folder:
	git clone $(REPO) build/
	cd build && git checkout gh-pages

create-initial-build-folder:
	git clone $(REPO) build/
	cd build && git checkout --orphan gh-pages && git rm -rf .
	@echo "now make release build into build/, cd into build and:"
	@echo "git add ."
	@echo "git commit -a -m 'First release'"
	@echo "git push origin gh-pages"

checkouts:
	mkdir checkouts
	cd checkouts/ && ln -s ../../infinitelives.pixi
	cd checkouts/ && ln -s ../../infinitelives.utils
