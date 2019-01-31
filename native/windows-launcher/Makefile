NAME=otoch
# all: ../lib/icon.png ../lib/App/AppInfo/Launcher/Splash.jpg ../lib/App/AppInfo/AppIcon.ico ../svg-animation-assistant.exe
CHROME=$(NAME)/lib
ICON=$(CHROME)/App/AppInfo/AppIcon.ico
SPLASH=$(CHROME)/App/AppInfo/Launcher/Splash.jpg
BUILD=$(NAME)/build
EXEC=$(NAME)/$(NAME).exe
IMGS=../../resources/public/img/

all: $(NAME) $(ICON) $(SPLASH) $(BUILD) $(EXEC)

$(NAME):
	mkdir -p $@

$(CHROME):
	@echo "Unpack the ChromiumPortable distribution into ./$(CHROME)"
	@exit 1

$(BUILD): ../../build/**/**
	rsync -avt --exclude=.git ../../build/ $(BUILD)
	touch $(BUILD)

$(ICON): $(IMGS)/icon.svg $(CHROME)
	inkscape -z -e icon.png -w 256 -h 256 $<
	convert -background transparent "icon.png" -define icon:auto-resize=16,24,32,48,64,72,96,128,256 "$@"
	rm icon.png

$(SPLASH): $(IMGS)/splash.svg $(CHROME)
	inkscape -z -e splash.png -w 416 -h 257 $<
	convert splash.png $@
	rm splash.png

$(EXEC): launch.c iaa.res
	i686-w64-mingw32-gcc -o $@ $^ -Wl,-subsystem,windows

iaa.res: iaa.rc AppIcon.ico
	i686-w64-mingw32-windres $< -O coff -o $@

AppIcon.ico: $(ICON)
	cp $< $@

clean:
	rm -rf iaa.res $(NAME)/build AppIcon.ico
